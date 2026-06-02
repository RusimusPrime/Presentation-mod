package com.example.presentationmod.block.entity

import com.example.presentationmod.network.ModNetwork
import com.example.presentationmod.PresentationMod
import com.example.presentationmod.block.PresentationScreenBlock
import com.example.presentationmod.registry.ModRegistries
import com.example.presentationmod.util.ScreenMath
import com.example.presentationmod.util.ServerFileUtil
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import java.io.File

class PresentationScreenBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModRegistries.PRESENTATION_SCREEN_BE, pos, state) {

    var fileName: String? = null
        private set

    var currentSlideIndex: Int = 0
        private set

    var active: Boolean = true
        private set

    fun hasPresentation(): Boolean = fileName != null

    fun loadFromConfig(rawFileName: String): Boolean {
        val server = level?.server ?: return false
        val cleanName = sanitizeFileName(rawFileName) ?: return false
        val root = ServerFileUtil.presentationsRoot(server.serverDirectory)
        val file = File(root, cleanName)
        if (!file.isFile) {
            PresentationMod.LOGGER.warn("Presentation file not found: {}", file.absolutePath)
            return false
        }

        fileName = cleanName
        currentSlideIndex = 0
        PresentationMod.LOGGER.info("Presentation loaded on server: {} at {}", cleanName, worldPosition)
        syncToClients()
        return true
    }

    fun nextSlide() {
        if (!hasPresentation()) return
        currentSlideIndex += 1
        syncToClients()
    }

    fun prevSlide() {
        if (!hasPresentation()) return
        currentSlideIndex = (currentSlideIndex - 1).coerceAtLeast(0)
        syncToClients()
    }

    fun toggleActive() {
        active = !active
        updateActiveBlockStates()
        syncToClients()
    }

    private fun updateActiveBlockStates() {
        val lvl = level ?: return
        val bounds = ScreenMath.measure(lvl, worldPosition)
        for (x in 0 until bounds.width) {
            for (y in 0 until bounds.height) {
                val pos = bounds.controller.relative(bounds.right, x).relative(bounds.up, y)
                val state = lvl.getBlockState(pos)
                if (state.block == blockState.block && state.hasProperty(PresentationScreenBlock.ACTIVE)) {
                    lvl.setBlock(pos, state.setValue(PresentationScreenBlock.ACTIVE, active), 3)
                }
            }
        }
    }

    private fun sanitizeFileName(raw: String): String? {
        val normalized = raw.trim().replace('\\', '/')
        val tail = normalized.substringAfterLast('/')
        if (tail.isBlank()) return null
        if (tail.contains("..")) return null
        return tail
    }

    private fun syncToClients() {
        setChanged()
        level?.sendBlockUpdated(worldPosition, blockState, blockState, 3)
        val serverLevel = level as? net.minecraft.server.level.ServerLevel ?: return
        ModNetwork.sendSlideSync(serverLevel, worldPosition, fileName, currentSlideIndex, active)
    }

    fun applyClientSync(newFileName: String?, newIndex: Int, newActive: Boolean) {
        val lvl = level ?: return
        if (!lvl.isClientSide) return

        val fileChanged = fileName != newFileName
        fileName = newFileName
        currentSlideIndex = newIndex.coerceAtLeast(0)
        active = newActive
        PresentationMod.LOGGER.info("Presentation sync on client: file={}, slide={}, active={}, pos={}", fileName, currentSlideIndex, active, worldPosition)
        if (fileChanged) {
            com.example.presentationmod.client.ClientPresentationCache.invalidate()
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putInt("Slide", currentSlideIndex)
        tag.putBoolean("Active", active)
        fileName?.let { tag.putString("File", it) }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        currentSlideIndex = tag.getInt("Slide").coerceAtLeast(0)
        active = !tag.contains("Active") || tag.getBoolean("Active")
        fileName = if (tag.contains("File")) tag.getString("File") else null
    }

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)

    fun renderBoundingBox(): AABB {
        val lvl = level ?: return AABB(worldPosition)
        val bounds = ScreenMath.measure(lvl, worldPosition)
        val corners = listOf(
            BlockPos.ZERO,
            BlockPos.ZERO.relative(bounds.right, bounds.width - 1),
            BlockPos.ZERO.relative(bounds.up, bounds.height - 1),
            BlockPos.ZERO.relative(bounds.right, bounds.width - 1).relative(bounds.up, bounds.height - 1),
            BlockPos.ZERO.relative(bounds.facing),
        )
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x } + 1
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y } + 1
        val minZ = corners.minOf { it.z }
        val maxZ = corners.maxOf { it.z } + 1

        return AABB(
            bounds.controller.x + minX - 1.0,
            bounds.controller.y + minY - 1.0,
            bounds.controller.z + minZ - 1.0,
            bounds.controller.x + maxX + 1.0,
            bounds.controller.y + maxY + 1.0,
            bounds.controller.z + maxZ + 1.0,
        )
    }
}

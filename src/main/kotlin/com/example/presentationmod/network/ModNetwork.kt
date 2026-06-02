package com.example.presentationmod.network

import com.example.presentationmod.PresentationMod
import com.example.presentationmod.util.ServerFileUtil
import com.example.presentationmod.util.ServerPresentationCache
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import java.util.UUID

object ModNetwork {

    private val SYNC_ID = ResourceLocation(PresentationMod.MODID, "sync")
    private val LOAD_REQUEST_ID = ResourceLocation(PresentationMod.MODID, "load_request")
    private val SLIDE_IMAGE_ID = ResourceLocation(PresentationMod.MODID, "slide_image")
    private val SLIDE_REQUEST_ID = ResourceLocation(PresentationMod.MODID, "slide_request")
    private val UPLOAD_CHUNK_ID = ResourceLocation(PresentationMod.MODID, "upload_chunk")

    fun register() {
        ServerPlayNetworking.registerGlobalReceiver(LOAD_REQUEST_ID) { server, player, _, buf, _ ->
            val packet = PresentationLoadRequestPacket.decode(buf)
            server.execute { PresentationLoadRequestPacket.handle(packet, player) }
        }
        ServerPlayNetworking.registerGlobalReceiver(SLIDE_REQUEST_ID) { server, player, _, buf, _ ->
            val packet = PresentationSlideRequestPacket.decode(buf)
            server.execute { PresentationSlideRequestPacket.handle(packet, player) }
        }
        ServerPlayNetworking.registerGlobalReceiver(UPLOAD_CHUNK_ID) { server, player, _, buf, _ ->
            val packet = PresentationUploadChunkPacket.decode(buf)
            server.execute { PresentationUploadChunkPacket.handle(packet, player) }
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_ID) { client, _, buf, _ ->
            val packet = PresentationSyncPacket.decode(buf)
            client.execute { PresentationSyncPacket.handle(packet, client) }
        }
        ClientPlayNetworking.registerGlobalReceiver(SLIDE_IMAGE_ID) { client, _, buf, _ ->
            val packet = PresentationSlideImagePacket.decode(buf)
            client.execute { PresentationSlideImagePacket.handle(packet) }
        }
    }

    fun sendSlideSync(level: ServerLevel, pos: BlockPos, fileName: String?, slideIndex: Int, active: Boolean) {
        val packet = PresentationSyncPacket(pos, fileName, slideIndex, active)
        PlayerLookup.around(level, Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5), 128.0)
            .forEach { sendToPlayer(it, SYNC_ID, packet, PresentationSyncPacket::encode) }
        sendSlideImage(level, pos, fileName, slideIndex)
    }

    fun sendSlideSyncToPlayer(player: ServerPlayer, pos: BlockPos, fileName: String?, slideIndex: Int, active: Boolean) {
        sendToPlayer(player, SYNC_ID, PresentationSyncPacket(pos, fileName, slideIndex, active), PresentationSyncPacket::encode)
        sendSlideImageToPlayer(player, pos, fileName, slideIndex)
    }

    fun sendLoadRequest(pos: BlockPos, fileName: String) {
        sendToServer(LOAD_REQUEST_ID, PresentationLoadRequestPacket(pos, fileName), PresentationLoadRequestPacket::encode)
    }

    fun uploadPresentation(pos: BlockPos, fileName: String, bytes: ByteArray) {
        val uploadId = UUID.randomUUID()
        val chunkSize = PresentationUploadChunkPacket.MAX_CHUNK_BYTES
        val totalChunks = ((bytes.size + chunkSize - 1) / chunkSize).coerceAtLeast(1)

        for (chunkIndex in 0 until totalChunks) {
            val start = chunkIndex * chunkSize
            val end = minOf(start + chunkSize, bytes.size)
            val chunk = bytes.copyOfRange(start, end)
            sendToServer(
                UPLOAD_CHUNK_ID,
                PresentationUploadChunkPacket(pos, uploadId, fileName, chunkIndex, totalChunks, chunk),
                PresentationUploadChunkPacket::encode,
            )
        }
    }

    fun requestSlide(pos: BlockPos) {
        sendToServer(SLIDE_REQUEST_ID, PresentationSlideRequestPacket(pos), PresentationSlideRequestPacket::encode)
    }

    private fun sendSlideImage(level: ServerLevel, pos: BlockPos, fileName: String?, slideIndex: Int) {
        val packet = slideImagePacket(level, pos, fileName, slideIndex) ?: return
        PlayerLookup.around(level, Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5), 128.0)
            .forEach { sendToPlayer(it, SLIDE_IMAGE_ID, packet, PresentationSlideImagePacket::encode) }
    }

    private fun sendSlideImageToPlayer(player: ServerPlayer, pos: BlockPos, fileName: String?, slideIndex: Int) {
        val level = player.level as? ServerLevel ?: return
        val packet = slideImagePacket(level, pos, fileName, slideIndex) ?: return
        sendToPlayer(player, SLIDE_IMAGE_ID, packet, PresentationSlideImagePacket::encode)
    }

    private fun slideImagePacket(
        level: ServerLevel,
        pos: BlockPos,
        fileName: String?,
        slideIndex: Int,
    ): PresentationSlideImagePacket? {
        if (fileName == null) return null
        val root = ServerFileUtil.presentationsRoot(level.server.serverDirectory)
        val file = java.io.File(root, fileName)
        if (!file.isFile) return null

        val slideCount = ServerPresentationCache.slideCount(file)
        val imageBytes = ServerPresentationCache.slideBytes(file, slideIndex) ?: return null
        return PresentationSlideImagePacket(pos, fileName, slideIndex, slideCount, imageBytes)
    }

    private fun <T> sendToPlayer(player: ServerPlayer, id: ResourceLocation, packet: T, encoder: (T, FriendlyByteBuf) -> Unit) {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        encoder(packet, buf)
        ServerPlayNetworking.send(player, id, buf)
    }

    private fun <T> sendToServer(id: ResourceLocation, packet: T, encoder: (T, FriendlyByteBuf) -> Unit) {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        encoder(packet, buf)
        ClientPlayNetworking.send(id, buf)
    }
}

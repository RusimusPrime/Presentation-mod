package com.example.presentationmod.network

import com.example.presentationmod.PresentationMod
import com.example.presentationmod.util.ServerFileUtil
import com.example.presentationmod.util.ServerPresentationCache
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.simple.SimpleChannel
import java.util.UUID

object ModNetwork {

    private const val PROTOCOL = "3"
    private var channel: SimpleChannel? = null

    fun register() {
        channel = NetworkRegistry.newSimpleChannel(
            ResourceLocation(PresentationMod.MODID, "main"),
            { PROTOCOL },
            { it == PROTOCOL },
            { it == PROTOCOL }
        )

        var id = 0
        channel!!.messageBuilder(PresentationSyncPacket::class.java, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(PresentationSyncPacket::encode)
            .decoder(PresentationSyncPacket::decode)
            .consumerMainThread(PresentationSyncPacket::handle)
            .add()

        channel!!.messageBuilder(PresentationLoadRequestPacket::class.java, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(PresentationLoadRequestPacket::encode)
            .decoder(PresentationLoadRequestPacket::decode)
            .consumerMainThread(PresentationLoadRequestPacket::handle)
            .add()

        channel!!.messageBuilder(PresentationSlideImagePacket::class.java, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(PresentationSlideImagePacket::encode)
            .decoder(PresentationSlideImagePacket::decode)
            .consumerMainThread(PresentationSlideImagePacket::handle)
            .add()

        channel!!.messageBuilder(PresentationSlideRequestPacket::class.java, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(PresentationSlideRequestPacket::encode)
            .decoder(PresentationSlideRequestPacket::decode)
            .consumerMainThread(PresentationSlideRequestPacket::handle)
            .add()

        channel!!.messageBuilder(PresentationUploadChunkPacket::class.java, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(PresentationUploadChunkPacket::encode)
            .decoder(PresentationUploadChunkPacket::decode)
            .consumerMainThread(PresentationUploadChunkPacket::handle)
            .add()
    }

    fun sendSlideSync(level: ServerLevel, pos: BlockPos, fileName: String?, slideIndex: Int) {
        val packet = PresentationSyncPacket(pos, fileName, slideIndex)
        channel?.send(
            PacketDistributor.NEAR.with {
                PacketDistributor.TargetPoint(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 128.0, level.dimension())
            },
            packet
        )
        sendSlideImage(level, pos, fileName, slideIndex)
    }

    fun sendSlideSyncToPlayer(player: ServerPlayer, pos: BlockPos, fileName: String?, slideIndex: Int) {
        channel?.send(PacketDistributor.PLAYER.with { player }, PresentationSyncPacket(pos, fileName, slideIndex))
        sendSlideImageToPlayer(player, pos, fileName, slideIndex)
    }

    fun sendLoadRequest(pos: BlockPos, fileName: String) {
        channel?.sendToServer(PresentationLoadRequestPacket(pos, fileName))
    }

    fun uploadPresentation(pos: BlockPos, fileName: String, bytes: ByteArray) {
        val uploadId = UUID.randomUUID()
        val chunkSize = PresentationUploadChunkPacket.MAX_CHUNK_BYTES
        val totalChunks = ((bytes.size + chunkSize - 1) / chunkSize).coerceAtLeast(1)

        for (chunkIndex in 0 until totalChunks) {
            val start = chunkIndex * chunkSize
            val end = minOf(start + chunkSize, bytes.size)
            val chunk = bytes.copyOfRange(start, end)
            channel?.sendToServer(
                PresentationUploadChunkPacket(pos, uploadId, fileName, chunkIndex, totalChunks, chunk)
            )
        }
    }

    fun requestSlide(pos: BlockPos) {
        channel?.sendToServer(PresentationSlideRequestPacket(pos))
    }

    private fun sendSlideImage(level: ServerLevel, pos: BlockPos, fileName: String?, slideIndex: Int) {
        val packet = slideImagePacket(level, pos, fileName, slideIndex) ?: return
        channel?.send(
            PacketDistributor.NEAR.with {
                PacketDistributor.TargetPoint(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 128.0, level.dimension())
            },
            packet,
        )
    }

    private fun sendSlideImageToPlayer(player: ServerPlayer, pos: BlockPos, fileName: String?, slideIndex: Int) {
        val level = player.level() as? ServerLevel ?: return
        val packet = slideImagePacket(level, pos, fileName, slideIndex) ?: return
        channel?.send(PacketDistributor.PLAYER.with { player }, packet)
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
}

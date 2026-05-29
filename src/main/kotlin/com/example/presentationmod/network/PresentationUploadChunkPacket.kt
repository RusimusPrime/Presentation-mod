package com.example.presentationmod.network

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.util.PresentationScreenHelper
import com.example.presentationmod.util.ServerFileUtil
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraftforge.network.NetworkEvent
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.function.Supplier

data class PresentationUploadChunkPacket(
    val pos: BlockPos,
    val uploadId: UUID,
    val fileName: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val bytes: ByteArray,
) {
    companion object {
        const val MAX_CHUNK_BYTES = 30_000
        private const val MAX_FILE_BYTES = 32 * 1024 * 1024
        private val uploads = mutableMapOf<String, PendingUpload>()

        fun encode(packet: PresentationUploadChunkPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeUUID(packet.uploadId)
            buf.writeUtf(packet.fileName, 256)
            buf.writeVarInt(packet.chunkIndex)
            buf.writeVarInt(packet.totalChunks)
            buf.writeByteArray(packet.bytes)
        }

        fun decode(buf: FriendlyByteBuf): PresentationUploadChunkPacket =
            PresentationUploadChunkPacket(
                pos = buf.readBlockPos(),
                uploadId = buf.readUUID(),
                fileName = buf.readUtf(256),
                chunkIndex = buf.readVarInt(),
                totalChunks = buf.readVarInt(),
                bytes = buf.readByteArray(MAX_CHUNK_BYTES),
            )

        fun handle(packet: PresentationUploadChunkPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork {
                val player = context.sender ?: return@enqueueWork
                val cleanName = sanitizeFileName(packet.fileName)
                if (cleanName == null || packet.totalChunks <= 0 || packet.chunkIndex !in 0 until packet.totalChunks) {
                    player.displayClientMessage(Component.literal("Bad presentation upload"), true)
                    return@enqueueWork
                }

                val key = "${player.uuid}:${packet.uploadId}"
                val upload = uploads.getOrPut(key) {
                    PendingUpload(cleanName, packet.totalChunks, ByteArrayOutputStream())
                }

                if (upload.fileName != cleanName || upload.totalChunks != packet.totalChunks || upload.nextChunk != packet.chunkIndex) {
                    uploads.remove(key)
                    player.displayClientMessage(Component.literal("Presentation upload failed"), true)
                    return@enqueueWork
                }

                if (upload.buffer.size() + packet.bytes.size > MAX_FILE_BYTES) {
                    uploads.remove(key)
                    player.displayClientMessage(Component.literal("Presentation file is too large"), true)
                    return@enqueueWork
                }

                upload.buffer.write(packet.bytes)
                upload.nextChunk += 1

                if (upload.nextChunk < upload.totalChunks) return@enqueueWork

                uploads.remove(key)
                val serverDir = player.server.serverDirectory
                val target = File(ServerFileUtil.presentationsRoot(serverDir), cleanName)
                target.writeBytes(upload.buffer.toByteArray())

                val be = PresentationScreenHelper.controllerEntity(player.level(), packet.pos) as? PresentationScreenBlockEntity
                if (be != null && be.loadFromConfig(cleanName)) {
                    player.displayClientMessage(Component.literal("Loaded: $cleanName"), true)
                } else {
                    player.displayClientMessage(Component.literal("Screen not found"), true)
                }
            }
            context.packetHandled = true
        }

        private fun sanitizeFileName(raw: String): String? {
            val name = raw.trim().replace('\\', '/').substringAfterLast('/')
            if (name.isBlank() || name.contains("..")) return null
            return name
        }
    }

    private data class PendingUpload(
        val fileName: String,
        val totalChunks: Int,
        val buffer: ByteArrayOutputStream,
        var nextChunk: Int = 0,
    )

    override fun equals(other: Any?): Boolean =
        other is PresentationUploadChunkPacket &&
            pos == other.pos &&
            uploadId == other.uploadId &&
            fileName == other.fileName &&
            chunkIndex == other.chunkIndex &&
            totalChunks == other.totalChunks &&
            bytes.contentEquals(other.bytes)

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + uploadId.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + totalChunks
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

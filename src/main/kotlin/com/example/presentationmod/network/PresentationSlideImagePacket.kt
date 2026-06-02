package com.example.presentationmod.network

import com.example.presentationmod.client.ClientPresentationCache
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf

data class PresentationSlideImagePacket(
    val pos: BlockPos,
    val fileName: String,
    val slideIndex: Int,
    val slideCount: Int,
    val imageBytes: ByteArray,
) {
    companion object {
        private const val MAX_IMAGE_BYTES = 1_500_000

        fun encode(packet: PresentationSlideImagePacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeUtf(packet.fileName, 256)
            buf.writeVarInt(packet.slideIndex)
            buf.writeVarInt(packet.slideCount)
            buf.writeByteArray(packet.imageBytes)
        }

        fun decode(buf: FriendlyByteBuf): PresentationSlideImagePacket =
            PresentationSlideImagePacket(
                pos = buf.readBlockPos(),
                fileName = buf.readUtf(256),
                slideIndex = buf.readVarInt(),
                slideCount = buf.readVarInt(),
                imageBytes = buf.readByteArray(MAX_IMAGE_BYTES),
            )

        fun handle(packet: PresentationSlideImagePacket) {
            ClientPresentationCache.acceptNetworkSlide(
                packet.pos,
                packet.fileName,
                packet.slideIndex,
                packet.slideCount,
                packet.imageBytes,
            )
        }
    }

    override fun equals(other: Any?): Boolean =
        other is PresentationSlideImagePacket &&
            pos == other.pos &&
            fileName == other.fileName &&
            slideIndex == other.slideIndex &&
            slideCount == other.slideCount &&
            imageBytes.contentEquals(other.imageBytes)

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + slideIndex
        result = 31 * result + slideCount
        result = 31 * result + imageBytes.contentHashCode()
        return result
    }
}

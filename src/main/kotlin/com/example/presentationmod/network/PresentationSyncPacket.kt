package com.example.presentationmod.network

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf

data class PresentationSyncPacket(val pos: BlockPos, val fileName: String?, val slideIndex: Int, val active: Boolean) {

    companion object {
        fun encode(packet: PresentationSyncPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.fileName != null)
            packet.fileName?.let { buf.writeUtf(it, 256) }
            buf.writeVarInt(packet.slideIndex)
            buf.writeBoolean(packet.active)
        }

        fun decode(buf: FriendlyByteBuf): PresentationSyncPacket {
            val pos = buf.readBlockPos()
            val fileName = if (buf.readBoolean()) buf.readUtf(256) else null
            val slideIndex = buf.readVarInt()
            val active = buf.readBoolean()
            return PresentationSyncPacket(pos, fileName, slideIndex, active)
        }

        fun handle(packet: PresentationSyncPacket, client: Minecraft) {
            val level = client.level ?: return
            val be = level.getBlockEntity(packet.pos) as? PresentationScreenBlockEntity ?: return
            be.applyClientSync(packet.fileName, packet.slideIndex, packet.active)
        }
    }
}

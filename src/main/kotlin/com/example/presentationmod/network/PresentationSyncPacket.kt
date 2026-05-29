package com.example.presentationmod.network

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class PresentationSyncPacket(val pos: BlockPos, val fileName: String?, val slideIndex: Int) {

    companion object {
        fun encode(packet: PresentationSyncPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeBoolean(packet.fileName != null)
            packet.fileName?.let { buf.writeUtf(it, 256) }
            buf.writeVarInt(packet.slideIndex)
        }

        fun decode(buf: FriendlyByteBuf): PresentationSyncPacket {
            val pos = buf.readBlockPos()
            val fileName = if (buf.readBoolean()) buf.readUtf(256) else null
            val slideIndex = buf.readVarInt()
            return PresentationSyncPacket(pos, fileName, slideIndex)
        }

        fun handle(packet: PresentationSyncPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork {
                val level = Minecraft.getInstance().level ?: return@enqueueWork
                val be = level.getBlockEntity(packet.pos) as? PresentationScreenBlockEntity ?: return@enqueueWork
                be.applyClientSync(packet.fileName, packet.slideIndex)
            }
            context.packetHandled = true
        }
    }
}

package com.example.presentationmod.network

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class PresentationSlideRequestPacket(val pos: BlockPos) {
    companion object {
        fun encode(packet: PresentationSlideRequestPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
        }

        fun decode(buf: FriendlyByteBuf): PresentationSlideRequestPacket =
            PresentationSlideRequestPacket(buf.readBlockPos())

        fun handle(packet: PresentationSlideRequestPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork {
                val player = context.sender ?: return@enqueueWork
                val be = player.level().getBlockEntity(packet.pos) as? PresentationScreenBlockEntity ?: return@enqueueWork
                ModNetwork.sendSlideSyncToPlayer(player, packet.pos, be.fileName, be.currentSlideIndex)
            }
            context.packetHandled = true
        }
    }
}

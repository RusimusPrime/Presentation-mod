package com.example.presentationmod.network

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

data class PresentationSlideRequestPacket(val pos: BlockPos) {
    companion object {
        fun encode(packet: PresentationSlideRequestPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
        }

        fun decode(buf: FriendlyByteBuf): PresentationSlideRequestPacket =
            PresentationSlideRequestPacket(buf.readBlockPos())

        fun handle(packet: PresentationSlideRequestPacket, player: ServerPlayer) {
            val be = player.level.getBlockEntity(packet.pos) as? PresentationScreenBlockEntity ?: return
            ModNetwork.sendSlideSyncToPlayer(player, packet.pos, be.fileName, be.currentSlideIndex, be.active)
        }
    }
}

package com.example.presentationmod.network

import com.example.presentationmod.util.PresentationScreenHelper
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

data class PresentationLoadRequestPacket(val pos: BlockPos, val fileName: String) {

    companion object {
        fun encode(packet: PresentationLoadRequestPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeUtf(packet.fileName, 256)
        }

        fun decode(buf: FriendlyByteBuf): PresentationLoadRequestPacket =
            PresentationLoadRequestPacket(buf.readBlockPos(), buf.readUtf(256))

        fun handle(packet: PresentationLoadRequestPacket, player: ServerPlayer) {
            val be = PresentationScreenHelper.controllerEntity(player.level, packet.pos)
            if (be == null) {
                player.displayClientMessage(Component.literal("Screen not found"), true)
                return
            }

            if (be.loadFromConfig(packet.fileName)) {
                player.displayClientMessage(Component.literal("Loaded: ${packet.fileName}"), true)
            } else {
                player.displayClientMessage(Component.literal("File not found in config/presentationmod/presentations"), true)
            }
        }
    }
}

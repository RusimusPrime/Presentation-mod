package com.example.presentationmod.network

import com.example.presentationmod.util.PresentationScreenHelper
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class PresentationLoadRequestPacket(val pos: BlockPos, val fileName: String) {

    companion object {
        fun encode(packet: PresentationLoadRequestPacket, buf: FriendlyByteBuf) {
            buf.writeBlockPos(packet.pos)
            buf.writeUtf(packet.fileName, 256)
        }

        fun decode(buf: FriendlyByteBuf): PresentationLoadRequestPacket =
            PresentationLoadRequestPacket(buf.readBlockPos(), buf.readUtf(256))

        fun handle(packet: PresentationLoadRequestPacket, ctx: Supplier<NetworkEvent.Context>) {
            val context = ctx.get()
            context.enqueueWork {
                val player = context.sender ?: return@enqueueWork
                val be = PresentationScreenHelper.controllerEntity(player.level(), packet.pos)
                if (be == null) {
                    player.displayClientMessage(Component.literal("Screen not found"), true)
                    return@enqueueWork
                }

                if (be.loadFromConfig(packet.fileName)) {
                    player.displayClientMessage(Component.literal("Loaded: ${packet.fileName}"), true)
                } else {
                    player.displayClientMessage(Component.literal("File not found in config/presentationmod/presentations"), true)
                }
            }
            context.packetHandled = true
        }
    }
}
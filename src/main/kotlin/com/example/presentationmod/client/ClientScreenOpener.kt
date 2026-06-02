package com.example.presentationmod.client

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos

object ClientScreenOpener {
    fun openUploadScreen(pos: BlockPos) {
        Minecraft.getInstance().setScreen(PresentationUploadScreen(pos))
    }
}

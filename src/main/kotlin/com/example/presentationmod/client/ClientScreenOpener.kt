package com.example.presentationmod.client

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object ClientScreenOpener {
    fun openUploadScreen(pos: BlockPos) {
        Minecraft.getInstance().setScreen(PresentationUploadScreen(pos))
    }
}

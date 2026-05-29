package com.example.presentationmod.client

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.network.ModNetwork
import com.example.presentationmod.util.PresentationScreenHelper
import java.awt.image.BufferedImage

object PresentationRenderer {

    fun getSlide(be: PresentationScreenBlockEntity): BufferedImage? {
        val level = be.level ?: return null
        val controller = PresentationScreenHelper.controllerEntity(level, be.blockPos) ?: return null
        val file = controller.fileName ?: return null
        ClientPresentationCache.getNetworkSlide(controller.blockPos, file, controller.currentSlideIndex)?.let {
            return it
        }
        if (ClientPresentationCache.shouldRequestNetworkSlide(controller.blockPos, file, controller.currentSlideIndex)) {
            ModNetwork.requestSlide(controller.blockPos)
        }
        if (!ClientPresentationCache.hasLocalPresentation(file)) return null
        return ClientPresentationCache.getSlide(file, controller.currentSlideIndex)
    }
}

package com.example.presentationmod.client

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.network.ModNetwork
import java.awt.image.BufferedImage

object PresentationRenderer {

    fun getSlide(be: PresentationScreenBlockEntity): BufferedImage? {
        val file = be.fileName ?: return null
        ClientPresentationCache.getNetworkSlide(be.blockPos, file, be.currentSlideIndex)?.let {
            return it
        }
        if (ClientPresentationCache.shouldRequestNetworkSlide(be.blockPos, file, be.currentSlideIndex)) {
            ModNetwork.requestSlide(be.blockPos)
        }
        if (!ClientPresentationCache.hasLocalPresentation(file)) return null
        return ClientPresentationCache.getSlide(file, be.currentSlideIndex)
    }
}

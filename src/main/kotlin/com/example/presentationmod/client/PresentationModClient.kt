package com.example.presentationmod.client

import com.example.presentationmod.PresentationMod
import com.example.presentationmod.network.ModNetwork
import com.example.presentationmod.registry.ModRegistries
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry

class PresentationModClient : ClientModInitializer {

    override fun onInitializeClient() {
        PresentationMod.LOGGER.info("Registering presentation screen renderer")
        BlockEntityRendererRegistry.register(ModRegistries.PRESENTATION_SCREEN_BE, ::PresentationScreenRenderer)
        ModNetwork.registerClient()
    }
}

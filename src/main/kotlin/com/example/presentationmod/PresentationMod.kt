package com.example.presentationmod

import com.example.presentationmod.network.ModNetwork
import com.example.presentationmod.registry.ModRegistries
import com.example.presentationmod.registry.ModCreativeTabs
import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import org.slf4j.Logger

class PresentationMod : ModInitializer {

    override fun onInitialize() {
        ModRegistries.register()
        ModCreativeTabs.register()
        ModNetwork.register()
        LOGGER.info("Presentation Mod initialized")
    }

    companion object {
        const val MODID = "presentationmod"
        val LOGGER: Logger = LogUtils.getLogger()
    }
}

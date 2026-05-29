package com.example.presentationmod

import com.example.presentationmod.network.ModNetwork
import com.example.presentationmod.registry.ModRegistries
import com.mojang.logging.LogUtils
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.slf4j.Logger

@Mod(PresentationMod.MODID)
class PresentationMod {

    init {
        val bus = FMLJavaModLoadingContext.get().modEventBus
        ModRegistries.register(bus)
        ModNetwork.register()
        LOGGER.info("Presentation Mod initialized")
    }

    companion object {
        const val MODID = "presentationmod"
        val LOGGER: Logger = LogUtils.getLogger()
    }
}
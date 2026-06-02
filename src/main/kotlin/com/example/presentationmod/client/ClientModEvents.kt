package com.example.presentationmod.client

import com.example.presentationmod.PresentationMod
import com.example.presentationmod.registry.ModRegistries
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = PresentationMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientModEvents {

    @JvmStatic
    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        PresentationMod.LOGGER.info("Registering presentation screen renderer")
        event.registerBlockEntityRenderer(ModRegistries.PRESENTATION_SCREEN_BE.get(), ::PresentationScreenRenderer)
    }
}

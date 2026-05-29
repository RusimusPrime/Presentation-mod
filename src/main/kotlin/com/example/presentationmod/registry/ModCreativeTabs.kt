package com.example.presentationmod.registry

import com.example.presentationmod.PresentationMod
import net.minecraft.world.item.CreativeModeTabs
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = PresentationMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ModCreativeTabs {

    @JvmStatic
    @SubscribeEvent
    fun addItems(event: BuildCreativeModeTabContentsEvent) {
        when (event.tabKey) {
            CreativeModeTabs.FUNCTIONAL_BLOCKS -> event.accept(ModRegistries.PRESENTATION_SCREEN_ITEM)
            CreativeModeTabs.TOOLS_AND_UTILITIES -> event.accept(ModRegistries.PRESENTATION_REMOTE)
        }
    }
}

package com.example.presentationmod.registry

import com.example.presentationmod.PresentationMod
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

object ModCreativeTabs {

    val SLIDECRAFT_TAB = FabricItemGroupBuilder
        .build(ResourceLocation(PresentationMod.MODID, "slidecraft")) {
            ItemStack(ModRegistries.PRESENTATION_SCREEN_ITEM)
        }

    fun register() {}
}

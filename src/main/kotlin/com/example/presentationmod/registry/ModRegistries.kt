package com.example.presentationmod.registry

import com.example.presentationmod.PresentationMod
import com.example.presentationmod.block.PresentationScreenBlock
import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.item.PresentationRemoteItem
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.minecraft.world.level.block.SoundType

object ModRegistries {
    val PRESENTATION_SCREEN: Block = register(Registry.BLOCK, "presentation_screen",
        PresentationScreenBlock(BlockBehaviour.Properties.of(Material.STONE)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .noOcclusion())
    )

    val PRESENTATION_SCREEN_ITEM: Item = register(Registry.ITEM, "presentation_screen",
        BlockItem(PRESENTATION_SCREEN, Item.Properties().tab(ModCreativeTabs.SLIDECRAFT_TAB))
    )

    val PRESENTATION_REMOTE: Item = register(Registry.ITEM, "presentation_remote",
        PresentationRemoteItem(Item.Properties().stacksTo(1).tab(ModCreativeTabs.SLIDECRAFT_TAB))
    )

    @Suppress("UNCHECKED_CAST")
    val PRESENTATION_SCREEN_BE: BlockEntityType<PresentationScreenBlockEntity> =
        register(Registry.BLOCK_ENTITY_TYPE, "presentation_screen",
            BlockEntityType.Builder.of(
                { pos, state -> PresentationScreenBlockEntity(pos, state) },
                PRESENTATION_SCREEN
            ).build(null)
        ) as BlockEntityType<PresentationScreenBlockEntity>

    fun register() {
        // Accessing this object performs static registrations.
    }

    private fun <T> register(registry: Registry<T>, path: String, value: T): T {
        return Registry.register(registry, ResourceLocation(PresentationMod.MODID, path), value)
    }
}

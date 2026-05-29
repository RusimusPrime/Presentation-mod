package com.example.presentationmod.registry

import com.example.presentationmod.PresentationMod
import com.example.presentationmod.block.PresentationScreenBlock
import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.item.PresentationRemoteItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModRegistries {
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, PresentationMod.MODID)
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, PresentationMod.MODID)
    val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PresentationMod.MODID)

    val PRESENTATION_SCREEN: RegistryObject<Block> = BLOCKS.register("presentation_screen") {
        PresentationScreenBlock(BlockBehaviour.Properties.of()
            .strength(1.5f)
            .noOcclusion())
    }

    val PRESENTATION_SCREEN_ITEM: RegistryObject<Item> = ITEMS.register("presentation_screen") {
        BlockItem(PRESENTATION_SCREEN.get(), Item.Properties())
    }

    val PRESENTATION_REMOTE: RegistryObject<Item> = ITEMS.register("presentation_remote") {
        PresentationRemoteItem(Item.Properties().stacksTo(1))
    }

    val PRESENTATION_SCREEN_BE: RegistryObject<BlockEntityType<PresentationScreenBlockEntity>> =
        BLOCK_ENTITIES.register("presentation_screen") {
            BlockEntityType.Builder.of(
                { pos, state -> PresentationScreenBlockEntity(pos, state) },
                PRESENTATION_SCREEN.get()
            ).build(null)
        }

    fun register(bus: IEventBus) {
        BLOCKS.register(bus)
        ITEMS.register(bus)
        BLOCK_ENTITIES.register(bus)
    }
}

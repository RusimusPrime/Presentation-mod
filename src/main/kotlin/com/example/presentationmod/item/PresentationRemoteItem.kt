package com.example.presentationmod.item

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.registry.ModRegistries
import com.example.presentationmod.util.PresentationScreenHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

class PresentationRemoteItem(properties: Properties) : Item(properties) {

    companion object {
        private const val TAG_X = "boundX"
        private const val TAG_Y = "boundY"
        private const val TAG_Z = "boundZ"
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (level.isClientSide) return InteractionResultHolder.success(stack)

        val tag = stack.tag
        val pos = if (tag != null && tag.contains(TAG_X)) {
            BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z))
        } else null

        val be = pos?.let { PresentationScreenHelper.controllerEntity(level, it) }
        if (be == null) {
            player.displayClientMessage(Component.literal("Remote is not bound").withStyle(ChatFormatting.YELLOW), true)
            return InteractionResultHolder.fail(stack)
        }

        if (player.isShiftKeyDown) be.prevSlide() else be.nextSlide()
        return InteractionResultHolder.success(stack)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level.isClientSide) return InteractionResult.SUCCESS

        val player = context.player ?: return InteractionResult.CONSUME
        if (!level.getBlockState(context.clickedPos).`is`(ModRegistries.PRESENTATION_SCREEN)) {
            return InteractionResult.PASS
        }

        val controllerPos = PresentationScreenHelper.controllerPos(level, context.clickedPos)
        val be = level.getBlockEntity(controllerPos) as? PresentationScreenBlockEntity

        if (be == null) {
            player.displayClientMessage(Component.literal("This is not a presentation screen").withStyle(ChatFormatting.RED), true)
            return InteractionResult.CONSUME
        }

        val tag: CompoundTag = context.itemInHand.orCreateTag
        tag.putInt(TAG_X, controllerPos.x)
        tag.putInt(TAG_Y, controllerPos.y)
        tag.putInt(TAG_Z, controllerPos.z)

        player.displayClientMessage(Component.literal("Remote bound to $controllerPos").withStyle(ChatFormatting.GREEN), true)
        return InteractionResult.SUCCESS
    }
}

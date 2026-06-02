package com.example.presentationmod.util

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

object PresentationScreenHelper {

    fun controllerEntity(level: Level, pos: BlockPos): PresentationScreenBlockEntity? {
        val state = level.getBlockState(pos)
        val controller = ScreenMath.findController(level, pos, state)
        return level.getBlockEntity(controller) as? PresentationScreenBlockEntity
    }

    fun controllerPos(level: Level, pos: BlockPos): BlockPos {
        val state = level.getBlockState(pos)
        return ScreenMath.findController(level, pos, state)
    }
}

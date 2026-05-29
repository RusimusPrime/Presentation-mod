package com.example.presentationmod.util

import com.example.presentationmod.block.PresentationScreenBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

data class ScreenBounds(
    val controller: BlockPos,
    val width: Int,
    val height: Int,
    val facing: Direction,
    val right: Direction,
)

object ScreenMath {

    fun findController(level: Level, pos: BlockPos, state: BlockState): BlockPos {
        val facing = facingOf(state)
        val right = rightOf(facing)
        return findBottomLeft(level, pos, state, right)
    }

    fun measure(level: Level, anyPos: BlockPos): ScreenBounds {
        val state = level.getBlockState(anyPos)
        val facing = facingOf(state)
        val right = rightOf(facing)
        val controller = findBottomLeft(level, anyPos, state, right)

        var width = 1
        while (width < 64 && isSameScreen(level.getBlockState(controller.relative(right, width)), state)) width++

        var height = 1
        while (height < 32 && isSameScreen(level.getBlockState(controller.above(height)), state)) height++

        return ScreenBounds(controller, width, height, facing, right)
    }

    private fun findBottomLeft(level: Level, pos: BlockPos, state: BlockState, right: Direction): BlockPos {
        var current = pos
        var changed: Boolean
        do {
            changed = false
            val left = current.relative(right.opposite)
            if (isSameScreen(level.getBlockState(left), state)) {
                current = left
                changed = true
            }

            val below = current.below()
            if (isSameScreen(level.getBlockState(below), state)) {
                current = below
                changed = true
            }
        } while (changed)
        return current
    }

    private fun isSameScreen(candidate: BlockState, origin: BlockState): Boolean {
        if (candidate.block != origin.block) return false
        if (!candidate.hasProperty(PresentationScreenBlock.FACING)) return false
        if (!origin.hasProperty(PresentationScreenBlock.FACING)) return false
        return candidate.getValue(PresentationScreenBlock.FACING) == origin.getValue(PresentationScreenBlock.FACING)
    }

    private fun facingOf(state: BlockState): Direction =
        if (state.hasProperty(PresentationScreenBlock.FACING)) state.getValue(PresentationScreenBlock.FACING) else Direction.NORTH

    private fun rightOf(facing: Direction): Direction = facing.counterClockWise
}

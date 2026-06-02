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
    val up: Direction,
)

object ScreenMath {

    fun findController(level: Level, pos: BlockPos, state: BlockState): BlockPos {
        return measure(level, pos).controller
    }

    fun measure(level: Level, anyPos: BlockPos): ScreenBounds {
        val state = level.getBlockState(anyPos)
        val facing = facingOf(state)
        val visualFacing = facing.opposite
        val right = rightOf(visualFacing)
        val up = upOf(visualFacing)
        val controller = findBottomLeft(level, anyPos, state, right, up)

        var width = 1
        while (width < 64 && isSameScreen(level.getBlockState(controller.relative(right, width)), state)) width++

        var height = 1
        while (height < 32 && isSameScreen(level.getBlockState(controller.relative(up, height)), state)) height++

        if (width < 2 || height < 2 || !isCompleteRectangle(level, controller, state, right, up, width, height)) {
            return ScreenBounds(anyPos, 1, 1, facing, right, up)
        }

        return ScreenBounds(controller, width, height, facing, right, up)
    }

    private fun isCompleteRectangle(
        level: Level,
        controller: BlockPos,
        state: BlockState,
        right: Direction,
        up: Direction,
        width: Int,
        height: Int,
    ): Boolean {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pos = controller.relative(right, x).relative(up, y)
                if (!isSameScreen(level.getBlockState(pos), state)) return false
            }
        }
        return true
    }

    private fun findBottomLeft(level: Level, pos: BlockPos, state: BlockState, right: Direction, up: Direction): BlockPos {
        var current = pos
        var changed: Boolean
        do {
            changed = false
            val left = current.relative(right.opposite)
            if (isSameScreen(level.getBlockState(left), state)) {
                current = left
                changed = true
            }

            val below = current.relative(up.opposite)
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

    private fun rightOf(facing: Direction): Direction =
        when (facing) {
            Direction.UP -> Direction.EAST
            Direction.DOWN -> Direction.EAST
            else -> facing.counterClockWise
        }

    private fun upOf(facing: Direction): Direction =
        when (facing) {
            Direction.UP -> Direction.NORTH
            Direction.DOWN -> Direction.SOUTH
            else -> Direction.UP
        }
}

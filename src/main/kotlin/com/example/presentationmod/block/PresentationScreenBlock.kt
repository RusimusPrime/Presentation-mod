package com.example.presentationmod.block

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.client.ClientScreenOpener
import com.example.presentationmod.util.PresentationScreenHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.shapes.Shapes

class PresentationScreenBlock(properties: Properties) : BaseEntityBlock(properties) {

    companion object {
        val FACING = BlockStateProperties.FACING
        val ACTIVE: BooleanProperty = BooleanProperty.create("active")
        val PART: EnumProperty<ScreenPart> = EnumProperty.create("part", ScreenPart::class.java)
        private val NORTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0)
        private val SOUTH_SHAPE = Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0)
        private val WEST_SHAPE = Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0)
        private val EAST_SHAPE = Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0)
        private val UP_SHAPE = Block.box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0)
        private val DOWN_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0)
    }

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, true)
                .setValue(PART, ScreenPart.SINGLE)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, ACTIVE, PART)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.clickedFace)
            .setValue(ACTIVE, true)
            .setValue(PART, ScreenPart.SINGLE)
    }

    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: net.minecraft.world.entity.LivingEntity?,
        stack: net.minecraft.world.item.ItemStack
    ) {
        if (!level.isClientSide) updateNearbyParts(level, pos)
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        if (!level.isClientSide) updateNearbyParts(level, pos)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = shapeFor(state)

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape = if (state.getValue(ACTIVE)) shapeFor(state) else Shapes.empty()

    override fun getInteractionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos
    ): VoxelShape = shapeFor(state)

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PresentationScreenBlockEntity(pos, state)

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (player.isShiftKeyDown) {
            if (!level.isClientSide) {
                val be = PresentationScreenHelper.controllerEntity(level, pos) ?: return InteractionResult.CONSUME
                be.toggleActive()
                val status = if (be.active) "Screen enabled" else "Screen disabled"
                player.displayClientMessage(Component.literal(status), true)
            }
            return InteractionResult.SUCCESS
        }

        if (level.isClientSide) {
            ClientScreenOpener.openUploadScreen(pos)
            return InteractionResult.SUCCESS
        }
        return InteractionResult.CONSUME
    }

    private fun shapeFor(state: BlockState): VoxelShape =
        when (state.getValue(FACING)) {
            Direction.NORTH -> SOUTH_SHAPE
            Direction.SOUTH -> NORTH_SHAPE
            Direction.WEST -> EAST_SHAPE
            Direction.EAST -> WEST_SHAPE
            Direction.UP -> DOWN_SHAPE
            Direction.DOWN -> UP_SHAPE
            else -> NORTH_SHAPE
        }

    private fun updateNearbyParts(level: Level, origin: BlockPos) {
        val checked = mutableSetOf<BlockPos>()
        for (direction in Direction.values()) {
            updateComponentParts(level, origin.relative(direction), checked)
        }
        updateComponentParts(level, origin, checked)
    }

    private fun updateComponentParts(level: Level, pos: BlockPos, checked: MutableSet<BlockPos>) {
        if (!checked.add(pos)) return
        val state = level.getBlockState(pos)
        if (state.block != this) return

        val bounds = com.example.presentationmod.util.ScreenMath.measure(level, pos)
        val hasConnectedFrame = bounds.width >= 2 && bounds.height >= 2
        val up = bounds.up
        val right = bounds.right

        for (x in 0 until bounds.width) {
            for (y in 0 until bounds.height) {
                val blockPos = bounds.controller.relative(right, x).relative(up, y)
                checked.add(blockPos)
                val blockState = level.getBlockState(blockPos)
                if (blockState.block != this || blockState.getValue(FACING) != bounds.facing) continue

                val part = if (hasConnectedFrame) {
                    partFor(x, y, bounds.width, bounds.height)
                } else {
                    ScreenPart.SINGLE
                }

                if (blockState.getValue(PART) != part) {
                    level.setBlock(blockPos, blockState.setValue(PART, part), 3)
                }
            }
        }
    }

    private fun partFor(x: Int, y: Int, width: Int, height: Int): ScreenPart {
        val left = x == 0
        val right = x == width - 1
        val down = y == 0
        val up = y == height - 1
        return when {
            left && up -> ScreenPart.LEFT_UP
            left && down -> ScreenPart.LEFT_DOWN
            right && up -> ScreenPart.RIGHT_UP
            right && down -> ScreenPart.RIGHT_DOWN
            left -> ScreenPart.LEFT
            right -> ScreenPart.RIGHT
            up -> ScreenPart.UP
            down -> ScreenPart.DOWN
            else -> ScreenPart.MIDDLE
        }
    }
}

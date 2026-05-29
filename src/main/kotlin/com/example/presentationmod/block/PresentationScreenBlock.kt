package com.example.presentationmod.block

import com.example.presentationmod.block.entity.PresentationScreenBlockEntity
import com.example.presentationmod.client.ClientScreenOpener
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor

class PresentationScreenBlock(properties: Properties) : BaseEntityBlock(properties) {

    companion object {
        val FACING = BlockStateProperties.HORIZONTAL_FACING
        private val NORTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0)
        private val SOUTH_SHAPE = Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0)
        private val WEST_SHAPE = Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0)
        private val EAST_SHAPE = Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val facing = context.horizontalDirection.opposite
        return defaultBlockState().setValue(FACING, facing)
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
    ): VoxelShape = shapeFor(state)

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
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                Runnable { ClientScreenOpener.openUploadScreen(pos) }
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.CONSUME
    }

    private fun shapeFor(state: BlockState): VoxelShape =
        when (state.getValue(FACING)) {
            Direction.NORTH -> NORTH_SHAPE
            Direction.SOUTH -> SOUTH_SHAPE
            Direction.WEST -> WEST_SHAPE
            Direction.EAST -> EAST_SHAPE
            else -> SOUTH_SHAPE
        }
}

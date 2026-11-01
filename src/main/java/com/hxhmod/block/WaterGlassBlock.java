package com.hxhmod.block;

import com.hxhmod.nen.Divination;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** O copo no chao: clique com a mao vazia para sentar e meditar na frente dele. */
public class WaterGlassBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 8, 11);

	public WaterGlassBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			Divination.start(serverPlayer, pos);
		}
		return InteractionResult.SUCCESS;
	}
}

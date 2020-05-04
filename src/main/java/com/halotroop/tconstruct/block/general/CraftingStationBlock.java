/*
 * MIT License
 *
 * Copyright (c) 2020 SlimeKnights, halotroop2288
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.halotroop.tconstruct.block.general;

import com.halotroop.tconstruct.TConstruct;
import com.halotroop.tconstruct.block.entity.CraftingStationBlockEntity;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.Container;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

@SuppressWarnings("deprecation")
public class CraftingStationBlock extends BlockWithEntity implements Waterloggable {
	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	
	private static final VoxelShape SHAPE;
	
	static {
		SHAPE = VoxelShapes.union(Block.createCuboidShape(0, 12, 0, 16, 16, 16), Block.createCuboidShape(0, 0, 0, 4, 12, 4), Block.createCuboidShape(0, 0, 12, 4, 12, 16), Block.createCuboidShape(12, 0, 12, 16, 12, 16), Block.createCuboidShape(12, 0, 0, 16, 12, 4));
	}
	
	public CraftingStationBlock() {
		super(Block.Settings.copy(Blocks.CRAFTING_TABLE));
		this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
	}
	
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockPos blockPos = ctx.getBlockPos();
		BlockState blockState = ctx.getWorld().getBlockState(blockPos);
		if (blockState.getBlock() == this) {
			return null;
		} else {
			FluidState fluidState = ctx.getWorld().getFluidState(blockPos);
			return this.getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite()).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
		}
	}
	
	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}
	
	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
	
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
	
	@Override
	public BlockEntity createBlockEntity(BlockView view) {
		return new CraftingStationBlockEntity();
	}
	
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!world.isClient)
			ContainerProviderRegistry.INSTANCE.openContainer(new Identifier(TConstruct.MODID, "crafting_station"), player, buf -> buf.writeBlockPos(pos));
		return ActionResult.SUCCESS;
	}
	
	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		if (itemStack.hasCustomName()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof CraftingStationBlockEntity) {
				((CraftingStationBlockEntity) blockEntity).setCustomName(itemStack.getName());
			}
		}
	}
	
	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction facing, BlockState neighborState, IWorld world, BlockPos pos, BlockPos neighborPos) {
		if (state.get(WATERLOGGED)) {
			world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}
		return super.getStateForNeighborUpdate(state, facing, neighborState, world, pos, neighborPos);
	}
	
	@Override
	public void onBlockRemoved(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof CraftingStationBlockEntity) {
				ItemScatterer.spawn(world, pos, (CraftingStationBlockEntity) blockEntity);
				world.updateHorizontalAdjacent(pos, this);
			}
			super.onBlockRemoved(state, world, pos, newState, moved);
		}
	}
	
	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}
	
	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return Container.calculateComparatorOutput(world.getBlockEntity(pos));
	}
	
	@Override
	public BlockRenderType getRenderType(BlockState blockState_1) {
		return BlockRenderType.MODEL;
	}
	
	@Override
	public boolean hasSidedTransparency(BlockState blockState_1) {
		return true;
	}
	
	@Override
	public VoxelShape getOutlineShape(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1, EntityContext entityContext_1) {
		return SHAPE;
	}
}

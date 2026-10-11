package com.rekindled.embers.block;

import javax.annotation.Nullable;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.blockentity.FluidVesselBlockEntity;
import com.rekindled.embers.util.ComparatorSignalUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FluidVesselBlock extends AbstractCauldronBlock implements EntityBlock, SimpleWaterloggedBlock {

	public static final MapCodec<FluidVesselBlock> CODEC = simpleCodec(FluidVesselBlock::new);
	protected static final VoxelShape VESSEL_AABB = Shapes.or(Block.box(0,0,0,4,16,4),Block.box(12,0,0,16,16,4),Block.box(12,0,12,16,16,16),Block.box(0,0,12,4,16,16),Block.box(4,0,12,12,16,14),Block.box(4,0,2,12,16,4),Block.box(2,0,4,4,16,12),Block.box(12,0,4,14,16,12),Block.box(4,0,4,12,2,12),Block.box(6,6,0,10,10,2),Block.box(6,6,14,10,10,16),Block.box(14,6,6,16,10,10),Block.box(0,6,6,2,10,10));

	public FluidVesselBlock(Properties properties) {
		super(properties, CauldronInteraction.EMPTY);
		this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false));
	}

	@Override
	protected MapCodec<? extends AbstractCauldronBlock> codec() {
		return CODEC;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vesselEntity) {
			if (!stack.isEmpty()) {
				IFluidHandler cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, vesselEntity, hit.getDirection());
				if (cap != null) {
					boolean didFill = FluidUtil.interactWithFluidHandler(player, hand, cap);

					if (didFill) {
						return ItemInteractionResult.sidedSuccess(level.isClientSide);
					}
				}
				// Keep full vessels from allowing buckets to place fluid in the world.
				if (stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
					return ItemInteractionResult.CONSUME_PARTIAL;
				}
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return VESSEL_AABB;
	}

	@Override
	public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.block();
	}

	@Override
	public boolean isEntityInsideContent(BlockState state, BlockPos pos, Entity entity) {
		return false;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return ComparatorSignalUtil.getSignal(level, pos);
	}

	@Override
	public boolean isFull(BlockState state) {
		return false;
	}

	@Override
	public boolean canReceiveStalactiteDrip(Fluid fluid) {
		return true;
	}

	@Override
	public void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
		if (level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vesselEntity) {
			IFluidHandler cap = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, vesselEntity, Direction.UP);
			if (cap != null) {
				int amount = 333;
				if (fluid == Fluids.LAVA)
					amount = FluidType.BUCKET_VOLUME;

				cap.fill(new FluidStack(fluid, amount), FluidAction.EXECUTE);

				if (fluid.getFluidType().getTemperature() > 500) {
					level.levelEvent(LevelEvent.SOUND_DRIP_LAVA_INTO_CAULDRON, pos, 0);
				} else {
					level.levelEvent(LevelEvent.SOUND_DRIP_WATER_INTO_CAULDRON, pos, 0);
				}
			}
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
		return RegistryManager.FLUID_VESSEL_ENTITY.get().create(pPos, pState);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
		return pLevel.isClientSide && pBlockEntityType == RegistryManager.FLUID_VESSEL_ENTITY.get()
				? (level, pos, state, blockEntity) -> FluidVesselBlockEntity.clientTick(level, pos, state, (FluidVesselBlockEntity) blockEntity) : null;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return super.getStateForPlacement(pContext).setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER));
	}

	@Override
	public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
		if (pState.getValue(BlockStateProperties.WATERLOGGED)) {
			pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
		}
		return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(BlockStateProperties.WATERLOGGED);
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return pState.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
	}
}

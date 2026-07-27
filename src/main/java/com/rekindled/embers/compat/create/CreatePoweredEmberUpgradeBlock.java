package com.rekindled.embers.compat.create;

import javax.annotation.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CreatePoweredEmberUpgradeBlock extends DirectionalKineticBlock implements IBE<CreatePoweredEmberUpgradeBlockEntity>, IWrenchable {
	private final CreatePoweredUpgradeType upgradeType;

	public CreatePoweredEmberUpgradeBlock(CreatePoweredUpgradeType upgradeType, Properties properties) {
		super(properties);
		this.upgradeType = upgradeType;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	public CreatePoweredUpgradeType getUpgradeType() {
		return upgradeType;
	}

	public Direction getUpgradeSide(BlockState state) {
		Direction facing = state.getValue(FACING);
		if (upgradeType == CreatePoweredUpgradeType.MINI_BOILER) {
			return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
		}
		return facing.getOpposite();
	}

	public Direction getShaftSide(BlockState state) {
		return upgradeType == CreatePoweredUpgradeType.MINI_BOILER ? Direction.DOWN : state.getValue(FACING);
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == getShaftSide(state);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return getShaftSide(state).getAxis();
	}

	@Override
	public IRotate.SpeedLevel getMinimumRequiredSpeedLevel() {
		return IRotate.SpeedLevel.NONE;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		if (!CreateCompat.experimentalMechanicsEnabled()) {
			return null;
		}
		if (upgradeType == CreatePoweredUpgradeType.MINI_BOILER) {
			Direction upgradeSide = context.getClickedFace().getAxis().isHorizontal()
					? context.getClickedFace().getOpposite()
					: context.getHorizontalDirection();
			return defaultBlockState().setValue(FACING, upgradeSide);
		}
		return super.getStateForPlacement(context);
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		if (upgradeType != CreatePoweredUpgradeType.MINI_BOILER) {
			return super.getRotatedBlockState(originalState, targetedFace);
		}
		Direction facing = originalState.getValue(FACING);
		Direction rotated = facing.getAxis().isHorizontal() ? facing.getClockWise(Direction.Axis.Y) : Direction.NORTH;
		return originalState.setValue(FACING, rotated);
	}

	@Override
	public Class<CreatePoweredEmberUpgradeBlockEntity> getBlockEntityClass() {
		return CreatePoweredEmberUpgradeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CreatePoweredEmberUpgradeBlockEntity> getBlockEntityType() {
		return CreateCompat.CREATE_POWERED_UPGRADE_ENTITY.get();
	}
}

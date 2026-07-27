package com.rekindled.embers.compat.create;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;

public class CreatePoweredEmberUpgradeVisual extends OrientedRotatingVisual<CreatePoweredEmberUpgradeBlockEntity> {
	private static final float SHAFT_OUTSET = 1.0F / 16.0F;

	public CreatePoweredEmberUpgradeVisual(VisualizationContext context, CreatePoweredEmberUpgradeBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick, blockEntity.getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG ? Direction.UP : Direction.SOUTH,
				blockEntity.getShaftSide(),
				Models.partial(blockEntity.getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG ? AllPartialModels.SHAFT : AllPartialModels.SHAFT_HALF));
		Direction facing = blockEntity.getShaftSide();
		if (blockEntity.getUpgradeType() != CreatePoweredUpgradeType.CATALYTIC_PLUG) {
			rotatingModel.nudge(facing.getStepX() * SHAFT_OUTSET, facing.getStepY() * SHAFT_OUTSET, facing.getStepZ() * SHAFT_OUTSET);
		}
		rotatingModel.setChanged();
	}
}

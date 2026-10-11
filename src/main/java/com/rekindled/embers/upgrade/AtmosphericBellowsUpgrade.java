package com.rekindled.embers.upgrade;

import java.util.List;

import org.joml.Vector3f;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.event.HeatCoilVisualEvent;
import com.rekindled.embers.api.event.UpgradeEvent;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.blockentity.HearthCoilBlockEntity;
import com.rekindled.embers.compat.create.CreateBlazeBurnerHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;


public class AtmosphericBellowsUpgrade extends DefaultUpgradeProvider {

	public AtmosphericBellowsUpgrade(BlockEntity tile) {
		super(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "atmospheric_bellows"), tile);
	}

	@Override
	public int getPriority() {
		return -80; //after the clockwork attenuator
	}

	@Override
	public int getLimit(BlockEntity tile) {
		return tile instanceof HearthCoilBlockEntity || isCreateBlazeBurner(tile) ? 1 : 0;
	}

	@Override
	public double getSpeed(BlockEntity tile, double speed, int distance, int count) {
		return 2;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getOtherParameter(BlockEntity tile, String type, T value, int distance, int count) {
		if (type.equals("recipe_type") && value instanceof RecipeType<?>) {
			return (T) RecipeType.BLASTING;
		}
		if (type.equals("create_blaze_burner_heat") && shouldSuperheatBlazeBurner(tile)) {
			return CreateBlazeBurnerHelper.superheat(value);
		}
		if (type.equals("create_blaze_burner_superheat") && value instanceof Boolean && shouldSuperheatBlazeBurner(tile)) {
			return (T) Boolean.TRUE;
		}
		return value;
	}

	private static boolean isCreateBlazeBurner(BlockEntity tile) {
		return ModList.get().isLoaded("create")
				&& tile.getClass().getName().equals("com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity");
	}

	private boolean shouldSuperheatBlazeBurner(BlockEntity tile) {
		return isCreateBlazeBurner(tile);
	}

	@Override
	public void throwEvent(BlockEntity tile, List<UpgradeContext> upgrades, UpgradeEvent event, int distance, int count) {
		if (event instanceof HeatCoilVisualEvent visualEvent) {
			Vector3f color = visualEvent.getColor();
			visualEvent.setColor(new Vector3f(color.z, color.y, color.x));
			visualEvent.setParticles((int) (visualEvent.getParticles() * 1.5));
		}
	}
}

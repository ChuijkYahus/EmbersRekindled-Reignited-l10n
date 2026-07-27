package com.rekindled.embers.upgrade;

import java.util.List;

import com.rekindled.embers.Embers;
import com.rekindled.embers.api.event.MachineRecipeEvent;
import com.rekindled.embers.api.event.UpgradeEvent;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.blockentity.GeologicSeparatorBlockEntity;
import com.rekindled.embers.recipe.MeltingRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class GeologicSeparatorUpgrade extends DefaultUpgradeProvider {

	public GeologicSeparatorUpgrade(BlockEntity tile) {
		super(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "geologic_separator"), tile);
	}

	@Override
	public int getPriority() {
		return 100; //after everything else
	}

	@Override
	public void throwEvent(BlockEntity tile, List<UpgradeContext> upgrades, UpgradeEvent event, int distance, int count) {
		if (distance == 0
				&& event instanceof MachineRecipeEvent.Success<?> success
				&& success.getRecipe() instanceof MeltingRecipe recipe
				&& this.tile instanceof GeologicSeparatorBlockEntity separator) {
			FluidStack bonus = recipe.getBonus();
			if (!bonus.isEmpty())
				separator.getTank().fill(bonus.copy(), FluidAction.EXECUTE);
		}
	}
}

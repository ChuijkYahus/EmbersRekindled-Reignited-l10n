package com.rekindled.embers.item;

import java.util.List;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.EmbersDataComponents;
import com.rekindled.embers.block.FluidDialBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

public class FluidVesselBlockItem extends BlockItem {

	public FluidVesselBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return stack.getOrDefault(EmbersDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY).isEmpty()
				? super.getMaxStackSize(stack) : 1;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
		super.appendHoverText(stack, context, tooltip, isAdvanced);
		SimpleFluidContent fluid = stack.getOrDefault(EmbersDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
		tooltip.add(FluidDialBlock.formatFluidStack(fluid.copy(), ConfigManager.FLUID_VESSEL_CAPACITY.get()).withStyle(ChatFormatting.GRAY));
	}
}

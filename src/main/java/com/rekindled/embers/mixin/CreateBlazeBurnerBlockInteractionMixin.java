package com.rekindled.embers.mixin;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.compat.create.EmberFueledBlazeBurner;
import com.rekindled.embers.util.EmberInventoryUtil;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlazeBurnerBlock.class)
public abstract class CreateBlazeBurnerBlockInteractionMixin {
	@Inject(method = "useItemOn", at = @At("RETURN"), cancellable = true)
	private void embers$fuelWithStoredEmber(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
		if (!ConfigManager.blazeBurnerIntegrationEnabled()) {
			return;
		}
		if (cir.getReturnValue() != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION || !stack.isEmpty()) {
			return;
		}
		if (!(level.getBlockEntity(pos) instanceof EmberFueledBlazeBurner emberBurner)) {
			return;
		}

		double available = EmberInventoryUtil.getEmberTotal(player);
		double accepted = emberBurner.embers$addEmber(available, true);
		if (accepted <= 0) {
			return;
		}

		if (!level.isClientSide) {
			EmberInventoryUtil.removeEmber(player, accepted);
			emberBurner.embers$addEmber(accepted, false);
			emberBurner.embers$refreshEmberFuelState(true);
		}
		cir.setReturnValue(ItemInteractionResult.SUCCESS);
	}
}

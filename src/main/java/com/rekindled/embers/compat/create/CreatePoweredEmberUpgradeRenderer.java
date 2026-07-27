package com.rekindled.embers.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class CreatePoweredEmberUpgradeRenderer extends KineticBlockEntityRenderer<CreatePoweredEmberUpgradeBlockEntity> {
	private static final float SHAFT_OUTSET = 1.0F / 16.0F;
	private final ItemRenderer itemRenderer;

	public CreatePoweredEmberUpgradeRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	protected void renderSafe(CreatePoweredEmberUpgradeBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
		super.renderSafe(blockEntity, partialTicks, poseStack, buffer, light, overlay);
		if (blockEntity.getLevel() == null) {
			return;
		}
		if (blockEntity.getUpgradeType() == CreatePoweredUpgradeType.MNEMONIC_INSCRIBER) {
			renderMnemonicStack(blockEntity, poseStack, buffer, light);
		}
	}

	@Override
	protected BlockState getRenderedBlockState(CreatePoweredEmberUpgradeBlockEntity blockEntity) {
		return shaft(getRotationAxisOf(blockEntity));
	}

	@Override
	protected SuperByteBuffer getRotatedModel(CreatePoweredEmberUpgradeBlockEntity blockEntity, BlockState state) {
		Direction facing = blockEntity.getShaftSide();
		if (blockEntity.getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG) {
			return super.getRotatedModel(blockEntity, state);
		}
		PartialModel shaftModel = AllPartialModels.SHAFT_HALF;
		SuperByteBuffer model = CachedBuffers.partialFacing(shaftModel, blockEntity.getBlockState(), facing);
		return model.translate(facing.getStepX() * SHAFT_OUTSET, facing.getStepY() * SHAFT_OUTSET, facing.getStepZ() * SHAFT_OUTSET);
	}

	private void renderMnemonicStack(CreatePoweredEmberUpgradeBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int light) {
		ItemStack stack = blockEntity.getMnemonicStack();
		if (stack.isEmpty()) {
			return;
		}
		int seed = Item.getId(stack.getItem()) + stack.getDamageValue();
		BakedModel model = itemRenderer.getModel(stack, blockEntity.getLevel(), null, seed);
		poseStack.pushPose();
		poseStack.translate(0.5D, 0.5D, 0.5D);
		poseStack.mulPose(blockEntity.getBlockState().getValue(CreatePoweredEmberUpgradeBlock.FACING).getRotation());
		poseStack.translate(0.001D, 0.125D, 0.001D);
		poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
		poseStack.translate(0.0D, -0.125D, 0.0D);
		itemRenderer.render(stack, ItemDisplayContext.GROUND, false, poseStack, buffer, light, OverlayTexture.NO_OVERLAY, model);
		poseStack.popPose();
	}
}

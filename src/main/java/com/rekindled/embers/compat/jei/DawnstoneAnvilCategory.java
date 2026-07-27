package com.rekindled.embers.compat.jei;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.recipe.IDawnstoneAnvilRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class DawnstoneAnvilCategory implements IRecipeCategory<RecipeHolder<IDawnstoneAnvilRecipe>> {

	private final IDrawable background;
	private final IDrawable icon;
	public static Component title = Component.translatable(Embers.MODID + ".jei.recipe.dawnstone_anvil");
	public static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Embers.MODID, "textures/gui/jei_dawnstone_anvil.png");

	public DawnstoneAnvilCategory(IGuiHelper helper) {
		background = helper.createDrawable(texture, 0, 0, 110, 58);
		icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(RegistryManager.DAWNSTONE_ANVIL_ITEM.get()));
	}

	@Override
	public RecipeType<RecipeHolder<IDawnstoneAnvilRecipe>> getRecipeType() {
		return JEIPlugin.DAWNSTONE_ANVIL;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public int getWidth() {
		return 110;
	}

	@Override
	public int getHeight() {
		return 58;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void draw(RecipeHolder<IDawnstoneAnvilRecipe> recipe, IRecipeSlotsView recipeSlotsView,
			GuiGraphics guiGraphics, double mouseX, double mouseY) {
		background.draw(guiGraphics);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<IDawnstoneAnvilRecipe> holder, IFocusGroup focuses) {
		IDawnstoneAnvilRecipe recipe = holder.value();
		if (!recipe.getDisplayInputTop().isEmpty()) {
			builder.addSlot(RecipeIngredientRole.INPUT, 22, 19).addItemStacks(recipe.getDisplayInputTop());
		}
		if (!recipe.getDisplayInputBottom().isEmpty()) {
			builder.addSlot(RecipeIngredientRole.INPUT, 22, 37).addItemStacks(recipe.getDisplayInputBottom());
		}
		if (!recipe.getDisplayOutput().isEmpty()) {
			builder.addSlot(RecipeIngredientRole.OUTPUT, 77, 28).addItemStacks(recipe.getDisplayOutput());
		}
	}
}

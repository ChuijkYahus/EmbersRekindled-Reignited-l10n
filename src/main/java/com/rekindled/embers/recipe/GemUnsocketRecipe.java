package com.rekindled.embers.recipe;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.item.IInflictorGemHolder;

import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class GemUnsocketRecipe implements CraftingRecipe {

	public static final Serializer SERIALIZER = new Serializer();

	public final ResourceLocation id;

	public GemUnsocketRecipe(ResourceLocation id) {
		this.id = id;
	}

	@Override
	public boolean matches(CraftingInput container, Level level) {
		ItemStack holderStack = ItemStack.EMPTY;
		for (int i = 0; i < container.size(); i++) {
			ItemStack stack = container.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (!holderStack.isEmpty() || !(stack.getItem() instanceof IInflictorGemHolder holder)
					|| holder.getAttachedGemCount(stack) == 0) {
				return false;
			}
			holderStack = stack;
		}
		return !holderStack.isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput container, HolderLookup.Provider registryAccess) {
		for (int i = 0; i < container.size(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty() && stack.getItem() instanceof IInflictorGemHolder holder) {
				ItemStack result = stack.copy();
				int slot = firstAttachedGemSlot(holder, result);
				if (slot >= 0) {
					holder.detachGem(result, slot);
				}
				return result;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
		NonNullList<ItemStack> gems = NonNullList.withSize(container.size(), ItemStack.EMPTY);
		for (int i = 0; i < container.size(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty() && stack.getItem() instanceof IInflictorGemHolder holder) {
				ItemStack holderCopy = stack.copy();
				int slot = firstAttachedGemSlot(holder, holderCopy);
				if (slot >= 0) {
					gems.set(i, holder.detachGem(holderCopy, slot));
				}
				break;
			}
		}
		return gems;
	}

	private static int firstAttachedGemSlot(IInflictorGemHolder holder, ItemStack holderStack) {
		ItemStack[] attachedGems = holder.getAttachedGems(holderStack);
		for (int slot = 0; slot < attachedGems.length; slot++) {
			if (!attachedGems[slot].isEmpty()) {
				return slot;
			}
		}
		return -1;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 1;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
		return new ItemStack(RegistryManager.ASHEN_CLOAK.get());
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.EQUIPMENT;
	}

	public static class Serializer extends LegacyRecipeSerializer<GemUnsocketRecipe> {

		@Override
		public GemUnsocketRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
			return new GemUnsocketRecipe(recipeId);
		}

		@Override
		public @Nullable GemUnsocketRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			return new GemUnsocketRecipe(recipeId);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, GemUnsocketRecipe recipe) {
		}
	}
}

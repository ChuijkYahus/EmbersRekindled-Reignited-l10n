package com.rekindled.embers.compat.create;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.api.tile.IExtraDialInformation;
import com.rekindled.embers.block.FluidDialBlock;
import com.rekindled.embers.compat.legacy.LazyOptional;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import com.rekindled.embers.datagen.EmbersItemTags;
import com.rekindled.embers.recipe.FluidHandlerContext;
import com.rekindled.embers.recipe.IBoilingRecipe;
import com.rekindled.embers.recipe.IGaseousFuelRecipe;
import com.rekindled.embers.util.Misc;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class CreatePoweredEmberUpgradeBlockEntity extends KineticBlockEntity implements IExtraCapabilityInformation, IExtraDialInformation {
	private final FluidTank fluidTank;
	private final FluidTank gasTank;
	private final ItemStackHandler paperInventory;
	private final CreatePoweredEmberUpgradeProvider upgrade;
	private final LazyOptional<IFluidHandler> fluidHolder;
	private final LazyOptional<IFluidHandler> gasHolder;
	private final LazyOptional<IItemHandler> itemHolder;
	private IBoilingRecipe cachedBoilingRecipe;
	private IGaseousFuelRecipe cachedGaseousFuelRecipe;
	private int gaseousFuelBurnTime;
	private int activeTicks;
	private boolean mnemonicActive;

	public CreatePoweredEmberUpgradeBlockEntity(BlockPos pos, BlockState state) {
		super(CreateCompat.CREATE_POWERED_UPGRADE_ENTITY.get(), pos, state);
		this.upgrade = new CreatePoweredEmberUpgradeProvider(this);
		this.fluidTank = new FluidTank(getFluidCapacity(state)) {
			@Override
			public void onContentsChanged() {
				CreatePoweredEmberUpgradeBlockEntity.this.setChanged();
			}

		};
		this.gasTank = new FluidTank(getMiniBoilerCapacity()) {
			@Override
			public void onContentsChanged() {
				CreatePoweredEmberUpgradeBlockEntity.this.setChanged();
			}
		};
		this.paperInventory = new ItemStackHandler(1) {
			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return stack.is(EmbersItemTags.INSCRIBABLE_PAPER);
			}

			@Override
			protected void onContentsChanged(int slot) {
				CreatePoweredEmberUpgradeBlockEntity.this.setChanged();
			}
		};
		this.fluidHolder = LazyOptional.of(() -> fluidTank);
		this.gasHolder = LazyOptional.of(() -> gasTank);
		this.itemHolder = LazyOptional.of(() -> paperInventory);
	}

	public CreatePoweredUpgradeType getUpgradeType() {
		if (getBlockState().getBlock() instanceof CreatePoweredEmberUpgradeBlock block) {
			return block.getUpgradeType();
		}
		return CreatePoweredUpgradeType.MINI_BOILER;
	}

	public double getKineticEfficiency() {
		return CreateCompat.experimentalMechanicsEnabled() ? getUpgradeType().efficiency(getSpeed()) : 0.0D;
	}

	public Direction getShaftSide() {
		if (getBlockState().getBlock() instanceof CreatePoweredEmberUpgradeBlock block) {
			return block.getShaftSide(getBlockState());
		}
		return getBlockState().getValue(CreatePoweredEmberUpgradeBlock.FACING);
	}

	public Direction getUpgradeSide() {
		if (getBlockState().getBlock() instanceof CreatePoweredEmberUpgradeBlock block) {
			return block.getUpgradeSide(getBlockState());
		}
		return getShaftSide().getOpposite();
	}

	public IItemHandler getItemHandler(Direction side) {
		if (CreateCompat.experimentalMechanicsEnabled() && getUpgradeType() == CreatePoweredUpgradeType.MNEMONIC_INSCRIBER) {
			return paperInventory;
		}
		return null;
	}

	public IFluidHandler getFluidHandler(Direction side) {
		if (!CreateCompat.experimentalMechanicsEnabled()) {
			return null;
		}
		if (getUpgradeType() == CreatePoweredUpgradeType.MINI_BOILER) {
			if (side == Direction.UP) {
				return gasTank;
			}
			return side == null || side != getUpgradeSide() ? fluidTank : null;
		}
		if (getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG
				|| getUpgradeType() == CreatePoweredUpgradeType.WILDFIRE_STIRLING) {
			return side == null || side == getShaftSide() ? fluidTank : null;
		}
		return null;
	}

	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (isRemoved() || !CreateCompat.experimentalMechanicsEnabled()) {
			return LazyOptional.empty();
		}
		if (cap == EmbersCapabilities.UPGRADE_PROVIDER_CAPABILITY && isUpgradeProviderSide(side)) {
			return upgrade.getCapability(cap, side);
		}
		if (cap == ForgeCapabilities.ITEM_HANDLER && getUpgradeType() == CreatePoweredUpgradeType.MNEMONIC_INSCRIBER) {
			return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, itemHolder);
		}
		if (cap == ForgeCapabilities.FLUID_HANDLER && getFluidHandler(side) != null) {
			return side == Direction.UP && getUpgradeType() == CreatePoweredUpgradeType.MINI_BOILER
					? ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, gasHolder)
					: ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, fluidHolder);
		}
		return LazyOptional.empty();
	}

	public void invalidateCaps() {
		upgrade.invalidate();
		fluidHolder.invalidate();
		gasHolder.invalidate();
		itemHolder.invalidate();
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("FluidTank", fluidTank.writeToNBT(registries, new CompoundTag()));
		tag.put("GasTank", gasTank.writeToNBT(registries, new CompoundTag()));
		tag.put("PaperInventory", paperInventory.serializeNBT(registries));
		tag.putInt("GaseousFuelBurnTime", gaseousFuelBurnTime);
		tag.putInt("ActiveTicks", activeTicks);
		tag.putBoolean("MnemonicActive", mnemonicActive);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
		gasTank.readFromNBT(registries, tag.getCompound("GasTank"));
		paperInventory.deserializeNBT(registries, tag.getCompound("PaperInventory"));
		gaseousFuelBurnTime = tag.getInt("GaseousFuelBurnTime");
		activeTicks = tag.getInt("ActiveTicks");
		mnemonicActive = tag.getBoolean("MnemonicActive");
	}

	@Override
	public void tick() {
		super.tick();
		if (activeTicks > 0) {
			activeTicks--;
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		CreatePoweredUpgradeType type = getUpgradeType();
		tooltip.add(Component.translatable("tooltip.embers.create_powered_upgrade.range",
				(int) type.minRpm(), (int) type.optimalRpm(), (int) type.maxRpm()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.embers.create_powered_upgrade.efficiency",
				(int) Math.round(getKineticEfficiency() * 100.0D)).withStyle(ChatFormatting.GRAY));
		return true;
	}

	public void boil(double heat) {
		if (level == null || heat <= 0.0D) {
			return;
		}
		FluidStack fluid = fluidTank.getFluid();
		FluidHandlerContext context = new FluidHandlerContext(fluidTank);
		cachedBoilingRecipe = Misc.getRecipe(cachedBoilingRecipe, RegistryManager.BOILING.get(), context, level);
		if (cachedBoilingRecipe == null || fluid.getAmount() <= 0) {
			return;
		}
		int fluidBoiled = Mth.clamp((int) (ConfigManager.MINI_BOILER_HEAT_MULTIPLIER.get() * heat), 1, fluid.getAmount());
		FluidStack gas = cachedBoilingRecipe.process(context, fluidBoiled);
		if (gas == null || gas.isEmpty()) {
			return;
		}
		gasTank.fill(gas, FluidAction.EXECUTE);
		if (ConfigManager.MINI_BOILER_CAN_EXPLODE.get() && gasTank.getFluidAmount() >= gasTank.getCapacity() && !level.isClientSide()) {
			level.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 3.0F, Level.ExplosionInteraction.NONE);
			level.removeBlock(worldPosition, false);
		}
	}

	public double getGaseousFuelMultiplier() {
		if (level == null || (getUpgradeType() != CreatePoweredUpgradeType.CATALYTIC_PLUG
				&& getUpgradeType() != CreatePoweredUpgradeType.WILDFIRE_STIRLING)) {
			return 1.0D;
		}
		FluidHandlerContext context = new FluidHandlerContext(fluidTank);
		if (gaseousFuelBurnTime <= 0 || cachedGaseousFuelRecipe == null) {
			cachedGaseousFuelRecipe = Misc.getRecipe(cachedGaseousFuelRecipe, RegistryManager.GASEOUS_FUEL.get(), context, level);
		}
		return cachedGaseousFuelRecipe == null ? 1.0D : cachedGaseousFuelRecipe.getPowerMultiplier(context);
	}

	public boolean consumeGaseousFuel(int ticks) {
		if (ticks <= 0 || getGaseousFuelMultiplier() == 1.0D) {
			return false;
		}
		gaseousFuelBurnTime -= ticks;
		boolean consumed = gaseousFuelBurnTime >= 0;
		FluidHandlerContext context = new FluidHandlerContext(fluidTank);
		while (gaseousFuelBurnTime < 0) {
			cachedGaseousFuelRecipe = Misc.getRecipe(cachedGaseousFuelRecipe, RegistryManager.GASEOUS_FUEL.get(), context, level);
			if (cachedGaseousFuelRecipe == null || !cachedGaseousFuelRecipe.matches(context, level)) {
				gaseousFuelBurnTime = 0;
				return false;
			}
			int burnTime = cachedGaseousFuelRecipe.process(context, 1);
			if (burnTime <= 0) {
				gaseousFuelBurnTime = 0;
				return false;
			}
			gaseousFuelBurnTime += burnTime;
			consumed = true;
		}
		if (consumed) {
			setChanged();
		}
		return consumed;
	}

	public boolean isActive() {
		return activeTicks > 0;
	}

	public void setActive(int ticks) {
		if (ticks <= 0) {
			return;
		}
		activeTicks = Math.max(activeTicks, ticks);
		sendData();
		setChanged();
	}

	public ItemStack getMnemonicStack() {
		return paperInventory.getStackInSlot(0);
	}

	public boolean hasInscribablePaper() {
		return paperInventory.getStackInSlot(0).is(EmbersItemTags.INSCRIBABLE_PAPER);
	}

	public void setPaper(ItemStack stack) {
		paperInventory.setStackInSlot(0, stack);
		setChanged();
	}

	public void setMnemonicActive(boolean active) {
		if (mnemonicActive != active) {
			mnemonicActive = active;
			sendData();
			setChanged();
		}
	}

	public boolean isMnemonicActive() {
		return mnemonicActive;
	}

	@Override
	public void addDialInformation(Direction facing, List<Component> information, String dialType) {
		if (getUpgradeType() != CreatePoweredUpgradeType.MINI_BOILER || !FluidDialBlock.DIAL_TYPE.equals(dialType)) {
			return;
		}
		information.clear();
		information.add(FluidDialBlock.formatFluidStack(fluidTank.getFluid(), fluidTank.getCapacity()));
		information.add(FluidDialBlock.formatFluidStack(gasTank.getFluid(), gasTank.getCapacity()));
	}

	@Override
	public boolean hasCapabilityDescription(Capability<?> capability) {
		return (getUpgradeType() == CreatePoweredUpgradeType.MINI_BOILER
				|| getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG
				|| getUpgradeType() == CreatePoweredUpgradeType.WILDFIRE_STIRLING)
				&& capability == ForgeCapabilities.FLUID_HANDLER;
	}

	@Override
	public void addCapabilityDescription(List<Component> strings, Capability<?> capability, Direction facing) {
		if (capability != ForgeCapabilities.FLUID_HANDLER) {
			return;
		}
		if (getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG
				|| getUpgradeType() == CreatePoweredUpgradeType.WILDFIRE_STIRLING) {
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.INPUT, Embers.MODID + ".tooltip.goggles.fluid",
					Component.translatable(Embers.MODID + ".tooltip.goggles.fluid.steam")));
			return;
		}
		if (getUpgradeType() != CreatePoweredUpgradeType.MINI_BOILER) {
			return;
		}
		if (facing == Direction.UP) {
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.OUTPUT, Embers.MODID + ".tooltip.goggles.fluid",
					Component.translatable(Embers.MODID + ".tooltip.goggles.fluid.steam")));
		} else if (facing != getUpgradeSide()) {
			strings.add(IExtraCapabilityInformation.formatCapability(EnumIOType.INPUT, Embers.MODID + ".tooltip.goggles.fluid",
					Component.translatable(Embers.MODID + ".tooltip.goggles.fluid.water")));
		}
	}

	private boolean isUpgradeProviderSide(Direction side) {
		return side == null || side == getUpgradeSide();
	}

	private static int getMiniBoilerCapacity() {
		return ConfigManager.MINI_BOILER_CAPACITY == null ? FluidType.BUCKET_VOLUME * 16 : ConfigManager.MINI_BOILER_CAPACITY.get();
	}

	private static int getFluidCapacity(BlockState state) {
		if (state.getBlock() instanceof CreatePoweredEmberUpgradeBlock block
				&& (block.getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG
						|| block.getUpgradeType() == CreatePoweredUpgradeType.WILDFIRE_STIRLING)) {
			return FluidType.BUCKET_VOLUME * 4;
		}
		return getMiniBoilerCapacity();
	}
}

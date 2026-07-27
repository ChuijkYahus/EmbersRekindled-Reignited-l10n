package com.rekindled.embers.blockentity;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.tile.IDialEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class FluidDialBlockEntity extends BlockEntity implements IDialEntity {

	public FluidStack[] fluids = new FluidStack[0];
	public int[] capacities = new int[0];
	public int extraLines = 0;
	public boolean display = false;

	public FluidDialBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(RegistryManager.FLUID_DIAL_ENTITY.get(), pPos, pBlockState);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		ListTag tanks = nbt.getList("tanks", Tag.TAG_COMPOUND);
		fluids = new FluidStack[tanks.size()];
		capacities = new int[tanks.size()];
		if (tanks.size() > 0) {
			for (int i = 0; i < tanks.size(); i++) {
				CompoundTag tank = tanks.getCompound(i);
				CompoundTag fluid = tank.copy();
				fluid.remove("capacity");
				fluids[i] = FluidStack.parseOptional(registries, fluid);
				capacities[i] = tank.getInt("capacity");
			}
		}
		if (nbt.contains("more_lines"))
			extraLines = nbt.getInt("more_lines");
		if (nbt.contains("display"))
			display = nbt.getBoolean("display");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return getUpdateTag(100, registries);
	}

	public CompoundTag getUpdateTag(int maxLines, HolderLookup.Provider registries) {
		CompoundTag nbt = super.getUpdateTag(registries);
		BlockState state = level.getBlockState(worldPosition);
		boolean display = false;
		if (state.hasProperty(BlockStateProperties.FACING)) {
			Direction facing = state.getValue(BlockStateProperties.FACING);
			BlockPos targetPos = worldPosition.relative(facing, -1);
			if (level.getBlockEntity(targetPos) != null) {
				IFluidHandler cap = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, facing.getOpposite());
				if (cap == null)
					cap = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, null);
				if (cap != null) {
					ListTag tanks = new ListTag();
					for (int i = 0; i < cap.getTanks() && (i + extraLines) < maxLines; i++) {
						FluidStack contents = cap.getFluidInTank(i);
						CompoundTag tank = (CompoundTag) contents.saveOptional(registries);
						tank.putInt("capacity", cap.getTankCapacity(i));

						tanks.add(tank);
					}
					nbt.put("tanks", tanks);

					if (cap.getTanks() > maxLines) {
						nbt.putInt("more_lines", cap.getTanks() - maxLines);
					} else {
						nbt.putInt("more_lines", 0);
					}
					display = true;
				}
			}
		}
		nbt.putBoolean("display", display);
		return nbt;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket(int maxLines) {
		return ClientboundBlockEntityDataPacket.create(this, (blockEntity, registries) -> getUpdateTag(maxLines, registries));
	}
}

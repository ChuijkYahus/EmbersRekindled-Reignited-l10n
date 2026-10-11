package com.rekindled.embers;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EmbersDataComponents {
	public static final DeferredRegister<DataComponentType<?>> TYPES =
			DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Embers.MODID);
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENTS =
			TYPES.register("fluid_contents", () -> DataComponentType.<SimpleFluidContent>builder()
					.persistent(SimpleFluidContent.CODEC)
					.networkSynchronized(SimpleFluidContent.STREAM_CODEC)
					.build());

	private EmbersDataComponents() {
	}
}

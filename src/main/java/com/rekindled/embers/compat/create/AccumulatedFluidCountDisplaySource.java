package com.rekindled.embers.compat.create;

import com.simibubi.create.content.redstone.displayLink.source.AccumulatedItemCountDisplaySource;

public class AccumulatedFluidCountDisplaySource extends AccumulatedItemCountDisplaySource {

	@Override
	protected String getTranslationKey() {
		return "accumulate_fluids";
	}
}

package com.rekindled.embers.compat.create;

import java.util.List;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.api.event.AlchemyResultEvent;
import com.rekindled.embers.api.event.AlchemyStartEvent;
import com.rekindled.embers.api.event.EmberEvent;
import com.rekindled.embers.api.event.UpgradeEvent;
import com.rekindled.embers.api.tile.ICatalyticPlugLimitProvider;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.upgrade.CatalyticPlugUpgrade;
import com.rekindled.embers.upgrade.DefaultUpgradeProvider;
import com.rekindled.embers.upgrade.WildfireStirlingUpgrade;
import com.rekindled.embers.util.EmbersColors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class CreatePoweredEmberUpgradeProvider extends DefaultUpgradeProvider {
	private static final double KINETIC_CATALYST_MULTIPLIER = 2.0D;

	public CreatePoweredEmberUpgradeProvider(CreatePoweredEmberUpgradeBlockEntity tile) {
		super(tile.getUpgradeType().baseUpgradeId(), tile);
	}

	@Override
	public int getPriority() {
		return switch (poweredTile().getUpgradeType()) {
			case MINI_BOILER, MNEMONIC_INSCRIBER -> 100;
			default -> 0;
		};
	}

	@Override
	public int getLimit(BlockEntity tile) {
		return poweredTile().getUpgradeType() == CreatePoweredUpgradeType.CATALYTIC_PLUG && tile instanceof ICatalyticPlugLimitProvider provider
				? provider.getCatalyticPlugLimit()
				: Integer.MAX_VALUE;
	}

	@Override
	public double getSpeed(BlockEntity tile, double speed, int distance, int count) {
		double efficiency = efficiency();
		return switch (poweredTile().getUpgradeType()) {
			case CATALYTIC_PLUG -> poweredTile().getGaseousFuelMultiplier() == 1.0D
					? speed
					: speed * kineticCatalystMultiplier(distance, count, efficiency);
			default -> speed;
		};
	}

	@Override
	public boolean doWork(BlockEntity tile, List<UpgradeContext> upgrades, int distance, int count) {
		if (efficiency() <= 0.0D) {
			return false;
		}
		switch (poweredTile().getUpgradeType()) {
			case CATALYTIC_PLUG, WILDFIRE_STIRLING -> {
				if (poweredTile().consumeGaseousFuel(1)) {
					poweredTile().setActive(21);
				}
			}
			default -> {
			}
		}
		return false;
	}

	@Override
	public double transformEmberConsumption(BlockEntity tile, double ember, int distance, int count) {
		double efficiency = efficiency();
		return switch (poweredTile().getUpgradeType()) {
			case CATALYTIC_PLUG -> poweredTile().getGaseousFuelMultiplier() == 1.0D
					? ember
					: ember * kineticCatalystMultiplier(distance, count, efficiency);
			case WILDFIRE_STIRLING -> poweredTile().getGaseousFuelMultiplier() == 1.0D
					? ember
					: ember / kineticStirlingMultiplier(distance, count, efficiency);
			default -> ember;
		};
	}

	@Override
	public double getOtherParameter(BlockEntity tile, String type, double value, int distance, int count) {
		if (poweredTile().getUpgradeType() != CreatePoweredUpgradeType.CATALYTIC_PLUG || !type.equals("fuel_consumption")) {
			return value;
		}
		return poweredTile().getGaseousFuelMultiplier() == 1.0D
				? value
				: value * kineticCatalystMultiplier(distance, count, efficiency());
	}

	@Override
	public void throwEvent(BlockEntity tile, List<UpgradeContext> upgrades, UpgradeEvent event, int distance, int count) {
		double efficiency = efficiency();
		if (efficiency <= 0.0D) {
			return;
		}
		switch (poweredTile().getUpgradeType()) {
			case MINI_BOILER -> handleMiniBoiler(event, distance, count, efficiency);
			case MNEMONIC_INSCRIBER -> handleMnemonicInscriber(tile, event, efficiency);
			default -> {
			}
		}
	}

	private void handleMiniBoiler(UpgradeEvent event, int distance, int count, double efficiency) {
		if (!(event instanceof EmberEvent emberEvent) || emberEvent.getType() == EmberEvent.EnumType.TRANSFER) {
			return;
		}
		double multiplier = 1.0D;
		if (distance > 1) {
			multiplier /= distance * 0.75D;
		}
		if (count > 3) {
			multiplier /= (count - 2.0D) * 0.75D;
		}
		if (emberEvent.getType() == EmberEvent.EnumType.PRODUCE) {
			multiplier *= 0.25D;
		}
		poweredTile().boil(emberEvent.getAmount() * multiplier * efficiency);
	}

	private void handleMnemonicInscriber(BlockEntity tile, UpgradeEvent event, double efficiency) {
		if (event instanceof AlchemyStartEvent alchemyEvent && alchemyEvent.getRecipe() != null) {
			poweredTile().setMnemonicActive(true);
		}
		if (!(event instanceof AlchemyResultEvent alchemyEvent)) {
			return;
		}
		if (!alchemyEvent.isFailure()
				&& poweredTile().hasInscribablePaper()
				&& tile.getLevel().random.nextDouble() <= efficiency) {
			poweredTile().setPaper(alchemyEvent.getResult().createResultStack(new ItemStack(RegistryManager.ALCHEMICAL_NOTE.get())));
			tile.getLevel().playSound(null, poweredTile().getBlockPos(), EmbersSounds.EMBER_EMIT_BIG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
			if (tile.getLevel() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(new GlowParticleOptions(EmbersColors.EMBER_ID, new Vec3(0.0D, 0.000001D, 0.0D), 2.0F, 40),
						poweredTile().getBlockPos().getX() + 0.5D, poweredTile().getBlockPos().getY() + 0.5D, poweredTile().getBlockPos().getZ() + 0.5D,
						40, 0.12D, 0.12D, 0.12D, 0.0D);
			}
		}
		poweredTile().setMnemonicActive(false);
	}

	private double kineticCatalystMultiplier(int distance, int count, double efficiency) {
		double target = CatalyticPlugUpgrade.getMultiplier(KINETIC_CATALYST_MULTIPLIER, distance, count);
		return Mth.lerp(efficiency, 1.0D, target);
	}

	private double kineticStirlingMultiplier(int distance, int count, double efficiency) {
		double target = WildfireStirlingUpgrade.getMultiplier(KINETIC_CATALYST_MULTIPLIER, distance, count);
		return Mth.lerp(efficiency, 1.0D, target);
	}

	private double efficiency() {
		return poweredTile().getKineticEfficiency();
	}

	private CreatePoweredEmberUpgradeBlockEntity poweredTile() {
		return (CreatePoweredEmberUpgradeBlockEntity) tile;
	}
}

/*
 * Copyright (C) 2026 Kainy
 *
 * This file is part of AbyssFall.
 *
 * AbyssFall is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AbyssFall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AbyssFall.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.abyssfall.effect;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import com.abyssfall.core.AbyssFallCoreSystem;

/**
 * "Spirited" — the mirror of {@link SanBreakdownEffect}: it gives San back.
 *
 * <p>Same cadence and same magnitudes, opposite sign. Once every {@link #PERIOD_TICKS} ticks it
 * restores a share of the player's San ceiling — I gives 1%, II 2%, III 4%, IV 8%, and V, the cap,
 * 12.5% — so a level III Spirited exactly cancels a level III breakdown, and the two can be
 * reasoned about as a single scale running in both directions.
 *
 * <p>The magnitudes are read from {@link SanBreakdownEffect#drainFractionFor} rather than restated
 * here. Restating them would let the two effects drift apart the first time one side is tuned, and
 * "the same numbers, the other way" is the whole design.
 *
 * <h2>Restoration is not subject to the difficulty rules</h2>
 *
 * <p>This effect writes through {@link AbyssFallCoreSystem#addCurrent}, deliberately <em>not</em>
 * through {@code erode}. The {@code san} config block governs what the world may take from a
 * player, and Peaceful shielding a player from erosion is a statement about pressure, not about
 * healing: a Peaceful player who drinks this should get their San back like anyone else. Only
 * losses are gated.
 *
 * <p>Restoration also cannot overshoot: {@code SanState} clamps the reading to the ceiling, so a
 * player already at full simply stays there and the effect quietly does nothing.
 *
 * <h2>Players only</h2>
 *
 * <p>As with the breakdown, San exists only on players. Applying this to any other living entity
 * is harmless and does nothing.
 */
public class SanSpiritedEffect extends MobEffect {
	/**
	 * How long one restoration period lasts. Ten seconds — the same beat as the breakdown, so
	 * opposing effects land on the same schedule rather than interleaving unpredictably.
	 */
	public static final int PERIOD_TICKS = SanBreakdownEffect.PERIOD_TICKS;

	/**
	 * The highest level that behaves differently from the one below it. Level V, as an amplifier.
	 */
	public static final int MAX_AMPLIFIER = SanBreakdownEffect.MAX_AMPLIFIER;

	/**
	 * Clear pale cyan — legible against the breakdown's sickly violet at a glance, which matters
	 * when a player may be carrying both.
	 */
	private static final int COLOR = 0x7FD4C8;

	/**
	 * A fully transparent particle. See {@code SanBreakdownEffect} for why an effect that wants to
	 * emit nothing expresses it this way; the two are kept consistent on purpose.
	 */
	private static final ColorParticleOption INVISIBLE_PARTICLE =
			ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0);

	public SanSpiritedEffect() {
		super(MobEffectCategory.BENEFICIAL, COLOR, INVISIBLE_PARTICLE);
	}

	/**
	 * The fraction of the San ceiling that {@code amplifier} restores per period.
	 *
	 * <p>Identical to the breakdown's drain at the same level, by construction.
	 *
	 * @param amplifier zero-based level; anything at or above {@link #MAX_AMPLIFIER} reads as V,
	 *                  and anything negative reads as I
	 */
	public static float restoreFractionFor(int amplifier) {
		return SanBreakdownEffect.drainFractionFor(amplifier);
	}

	/**
	 * Fires once per {@link #PERIOD_TICKS}. See {@code SanBreakdownEffect} for what vanilla passes
	 * in and why the phase is not fixed.
	 */
	@Override
	public boolean shouldApplyEffectTickThisTick(int durationOrTickCount, int amplifier) {
		return durationOrTickCount % PERIOD_TICKS == 0;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity instanceof ServerPlayer player) {
			float amount = AbyssFallCoreSystem.getMax(player) * restoreFractionFor(amplifier);

			// addCurrent, not erode: erode exists to gate what the world takes away, and giving
			// San back is not something the difficulty has an opinion about.
			AbyssFallCoreSystem.addCurrent(player, amount);
		}

		// Never false: a player already at their ceiling gains nothing this period, but the buff
		// should still run for as long as it was given.
		return true;
	}
}

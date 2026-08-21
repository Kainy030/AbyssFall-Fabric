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
 * "Mental Breakdown" — a debuff that eats away at the victim's San.
 *
 * <p>Once every {@link #PERIOD_TICKS} ticks it takes a share of the player's San ceiling, with
 * the share growing steeply by level: I takes 1%, II 2%, III 4%, IV 8%, and V — the cap — 12.5%.
 * The first four steps double, and the fifth deliberately does not: an unbroken doubling would
 * reach 16% and make level V roughly twice as bad as IV, whereas the intent is a ceiling that
 * still hurts without turning into a death sentence.
 *
 * <h2>What "1% of San" means here</h2>
 *
 * <p>The share is taken from the player's <em>ceiling</em>, not from their present reading. That
 * makes the drain linear: a level I breakdown always removes the same absolute amount, so it takes
 * a hundred periods to empty a full player and it genuinely reaches zero. Taking a share of the
 * current reading instead would decay geometrically — ever smaller bites that never quite arrive —
 * which is not what a breakdown should feel like.
 *
 * <h2>Nothing is subtracted directly</h2>
 *
 * <p>The loss goes through {@link AbyssFallCoreSystem#erode}, so it is subject to the rules in the
 * {@code san} config block — presently that Peaceful difficulty shields a player. That is the whole
 * reason {@code erode} exists, and it is why this class never touches {@code addCurrent}.
 *
 * <h2>Players only</h2>
 *
 * <p>San is a player-only concept: the attachment lives on players and there is no meaningful
 * reading for a zombie. The effect can still be applied to any living entity — it simply does
 * nothing to them, rather than being impossible to hand out.
 */
public class SanBreakdownEffect extends MobEffect {
	/**
	 * How long one drain period lasts. Ten seconds.
	 */
	public static final int PERIOD_TICKS = 200;

	/**
	 * The highest level that behaves differently from the one below it. Level V, as an amplifier.
	 *
	 * <p>Amplifiers are zero-based, so this is {@code 4}. Anything above it is treated as level V:
	 * the value is a cap on effect, not a limit on what may be applied, since {@code /effect} and
	 * other mods can hand out any amplifier they like and the effect has to stay sane when they do.
	 */
	public static final int MAX_AMPLIFIER = 4;

	/**
	 * Fraction of the San ceiling drained per period, indexed by amplifier — so levels I…V.
	 *
	 * <p>A table rather than a formula because the progression is deliberately not a clean curve:
	 * the last step breaks the doubling on purpose. A formula would have to special-case it anyway
	 * and would hide the intent.
	 */
	private static final float[] DRAIN_PER_PERIOD = {0.01F, 0.02F, 0.04F, 0.08F, 0.125F};

	/**
	 * Sickly desaturated violet — the mod's abyssal palette drained of its warmth. Used for the
	 * potion item's tint and anything else that asks an effect for a colour.
	 */
	private static final int COLOR = 0x6B4A73;

	/**
	 * A fully transparent particle, which is how the effect ends up emitting nothing visible.
	 *
	 * <p>Whether an effect shows particles is decided per {@code MobEffectInstance} by whoever
	 * applied it, not by the effect itself, so an effect cannot simply declare itself invisible.
	 * What it <em>can</em> do is choose which particle it produces: vanilla's
	 * {@code SpellParticle.MobEffectProvider} takes the alpha straight off the particle's colour,
	 * so an alpha of zero yields a particle that is created and immediately invisible. The
	 * alternative would be injecting into the particle path, which is not worth a mixin for a
	 * cosmetic detail.
	 */
	private static final ColorParticleOption INVISIBLE_PARTICLE =
			ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0);

	public SanBreakdownEffect() {
		super(MobEffectCategory.HARMFUL, COLOR, INVISIBLE_PARTICLE);
	}

	/**
	 * The fraction of the San ceiling that {@code amplifier} drains per period.
	 *
	 * <p>Public because the planned anti-breakdown enchantment will need to reason about this
	 * number rather than re-deriving it.
	 *
	 * @param amplifier zero-based level; anything at or above {@link #MAX_AMPLIFIER} reads as V,
	 *                  and anything negative reads as I
	 */
	public static float drainFractionFor(int amplifier) {
		return DRAIN_PER_PERIOD[Math.clamp(amplifier, 0, MAX_AMPLIFIER)];
	}

	/**
	 * Fires once per {@link #PERIOD_TICKS}.
	 *
	 * <p>Vanilla hands this method either the remaining duration or, for an infinite effect, the
	 * entity's tick count. Both advance one per tick, so a modulo gives an even cadence in either
	 * case. What it does <em>not</em> guarantee is a fixed phase: a re-applied effect restarts the
	 * countdown, so a player can see two drains closer together than ten seconds around the moment
	 * the effect is refreshed. That is vanilla behaviour for every periodic effect — regeneration
	 * and poison included — and matching it is better than inventing a private timer that would
	 * have to be stored and synced.
	 */
	@Override
	public boolean shouldApplyEffectTickThisTick(int durationOrTickCount, int amplifier) {
		return durationOrTickCount % PERIOD_TICKS == 0;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (entity instanceof ServerPlayer player) {
			float amount = AbyssFallCoreSystem.getMax(player) * drainFractionFor(amplifier);

			// Refused outright on Peaceful, when the setting says so. Nothing else here needs to
			// know that; erode() is the single place the rule lives.
			AbyssFallCoreSystem.erode(player, amount);
		}

		// Never false: returning false would end the effect early, and a breakdown runs its
		// course regardless of whether this particular tick managed to take anything.
		return true;
	}
}

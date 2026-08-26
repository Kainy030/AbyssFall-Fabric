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

package com.abyssfall.shadercore.effect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import com.abyssfall.AbyssFall;
import com.abyssfall.shadercore.ShaderColorSource;
import com.abyssfall.shadercore.ShaderEffect;
import com.abyssfall.shadercore.ShaderEffectType;
import com.abyssfall.shadercore.color.FixedColorSource;
import com.abyssfall.shadercore.color.ShaderColorSources;

/**
 * Two effects on one mask, told apart by channel, both modulated by a shared pulse.
 *
 * <p>Green pixels are lit continuously. Blue pixels are eligible for sampling: a random subset is
 * shown for one round, then replaced by a fresh subset. In both cases the channel's <em>value</em> is
 * the opacity, so artwork can fade an effect towards an edge without any code changing.
 *
 * <p>This is one kind of effect among however many end up existing — the first one written, and the
 * reason the surrounding system is shaped the way it is rather than around this one's fields.
 *
 * @param mask              texture whose green and blue channels drive the effect
 * @param maskResolution    width of the mask in pixels, which sets the sampling grid
 * @param color             where this effect's colour comes from — see {@link ShaderColorSource}
 * @param sampleDensity     fraction of the blue-masked pixels lit in each round
 * @param samplePeriodTicks how long one round lasts before a fresh set is picked
 * @param sampleFadeEnabled whether a sampled pixel eases in and out instead of switching
 * @param pulsePeriodTicks  how long one full cycle of the shared pulse takes
 */
public record MaskedPulseEffect(Identifier mask, int maskResolution, ShaderColorSource color,
		float sampleDensity, int samplePeriodTicks, boolean sampleFadeEnabled,
		float pulsePeriodTicks) implements ShaderEffect {
	/**
	 * Length of a Minecraft day in ticks, which is the period of the clock the shader reads.
	 *
	 * <p>Not a setting: it is the divisor vanilla uses when it writes {@code GameTime} into the globals
	 * uniform, so it has to match that or every derived rate is wrong.
	 */
	private static final float TICKS_PER_DAY = 24000.0F;

	/**
	 * Shortest round accepted, in ticks. Below this the sampling reads as noise rather than as pixels
	 * appearing and disappearing, and the clock behind it stops being trustworthy: float precision in
	 * {@code GameTime} leaves each round a tick either side of its nominal length.
	 */
	public static final int MIN_SAMPLE_PERIOD_TICKS = 4;

	/** Longest round, in ticks. Past this the effect looks frozen rather than slow. */
	public static final int MAX_SAMPLE_PERIOD_TICKS = 400;

	/** Shortest pulse cycle, in ticks — a flicker rather than a pulse. */
	public static final float MIN_PULSE_PERIOD_TICKS = 4.0F;

	/** Longest pulse cycle, in ticks. Beyond this the colour appears static. */
	public static final float MAX_PULSE_PERIOD_TICKS = 400.0F;

	public static final int DEFAULT_MASK_RESOLUTION = 16;
	public static final float DEFAULT_SAMPLE_DENSITY = 0.10F;
	public static final int DEFAULT_SAMPLE_PERIOD_TICKS = 10;
	public static final boolean DEFAULT_SAMPLE_FADE_ENABLED = false;
	public static final float DEFAULT_PULSE_PERIOD_TICKS = 40.0F;

	/**
	 * The shader that draws this kind of effect. Both stages share the name, as vanilla's own do.
	 */
	public static final Identifier SHADER =
			Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "core/masked_pulse");

	public static final MapCodec<MaskedPulseEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Identifier.CODEC.fieldOf("mask")
					.forGetter(MaskedPulseEffect::mask),
			Codec.intRange(1, 4096).optionalFieldOf("mask_resolution", DEFAULT_MASK_RESOLUTION)
					.forGetter(MaskedPulseEffect::maskResolution),
			// Colour is read through a dispatching codec keyed on a "type" field, the same shape the effect
			// registry uses. It used to be a single codec with a cast on the write path, which was safe only
			// while one implementation existed; a second one now does, so the dispatch it was noted as
			// requiring is in place. An older file without a "type" still reads, as fixed.
			ShaderColorSources.LENIENT_CODEC.optionalFieldOf("color", FixedColorSource.DEFAULT)
					.forGetter(MaskedPulseEffect::color),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("sample_density", DEFAULT_SAMPLE_DENSITY)
					.forGetter(MaskedPulseEffect::sampleDensity),
			Codec.intRange(MIN_SAMPLE_PERIOD_TICKS, MAX_SAMPLE_PERIOD_TICKS)
					.optionalFieldOf("sample_period_ticks", DEFAULT_SAMPLE_PERIOD_TICKS)
					.forGetter(MaskedPulseEffect::samplePeriodTicks),
			Codec.BOOL.optionalFieldOf("sample_fade_enabled", DEFAULT_SAMPLE_FADE_ENABLED)
					.forGetter(MaskedPulseEffect::sampleFadeEnabled),
			Codec.floatRange(MIN_PULSE_PERIOD_TICKS, MAX_PULSE_PERIOD_TICKS)
					.optionalFieldOf("pulse_period_ticks", DEFAULT_PULSE_PERIOD_TICKS)
					.forGetter(MaskedPulseEffect::pulsePeriodTicks)
	).apply(instance, MaskedPulseEffect::new));

	/**
	 * Registered in {@code AbyssFallShaderCore} rather than here, so that registration order is
	 * visible in one place.
	 */
	public static final ShaderEffectType<MaskedPulseEffect> TYPE = new ShaderEffectType<>(
			Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "masked_pulse"), SHADER, MAP_CODEC);

	/**
	 * An effect on the given mask with every behaviour left at its default.
	 */
	public static MaskedPulseEffect of(Identifier mask) {
		return new MaskedPulseEffect(mask, DEFAULT_MASK_RESOLUTION, FixedColorSource.DEFAULT,
				DEFAULT_SAMPLE_DENSITY, DEFAULT_SAMPLE_PERIOD_TICKS, DEFAULT_SAMPLE_FADE_ENABLED,
				DEFAULT_PULSE_PERIOD_TICKS);
	}

	@Override
	public ShaderEffectType<?> type() {
		return TYPE;
	}

	@Override
	public Map<String, Float> shaderDefines() {
		// This effect's own values, plus whatever the colour source contributes. The source's names are
		// not inspected or filtered: this effect knows it needs a colour, not how one is arrived at, and
		// a source substituted later contributes whatever its own shader agreement requires.
		//
		// Rates are given as cycles per day because the shader's clock is a 0..1 ramp over a day.
		// Converting here keeps the shader from having to know how long a day is, and keeps the
		// multiplication out of the normalised domain where float error would move a boundary.
		Map<String, Float> defines = new HashMap<>(this.color.shaderDefines());

		defines.put("MASK_RESOLUTION", (float) this.maskResolution);
		defines.put("SAMPLE_DENSITY", this.sampleDensity);
		defines.put("SAMPLE_ROUNDS_PER_DAY", TICKS_PER_DAY / this.samplePeriodTicks);
		defines.put("PULSE_CYCLES_PER_DAY", TICKS_PER_DAY / this.pulsePeriodTicks);

		return Map.copyOf(defines);
	}

	@Override
	public Set<String> shaderFlags() {
		Set<String> flags = new HashSet<>(this.color.shaderFlags());

		if (this.sampleFadeEnabled) {
			flags.add("SAMPLE_FADE");
		}

		return Set.copyOf(flags);
	}

	/**
	 * Whether the sampled half does anything. A density of zero switches the blue channel off.
	 */
	public boolean hasSampling() {
		return this.sampleDensity > 0.0F;
	}
}

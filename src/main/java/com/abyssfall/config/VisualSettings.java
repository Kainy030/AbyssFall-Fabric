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

package com.abyssfall.config;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.abyssfall.AbyssFall;

/**
 * How loud and how busy the mod's effects are.
 *
 * <p>Both settings are multipliers rather than on/off switches, because "too much" and "none at
 * all" are different complaints and only one of them is served by a switch. Zero does still
 * silence the corresponding half completely.
 *
 * @param bloomParticleScale multiplier on the number of particles the bloom emits
 * @param bloomSoundVolume   multiplier on the volume of the bloom's sounds
 */
public record VisualSettings(float bloomParticleScale, float bloomSoundVolume) {
	/**
	 * Upper bound for both multipliers. Not a statement about taste, just a guard against a
	 * value large enough to flood the client with particles.
	 */
	public static final float MAX_SCALE = 2.0F;

	/**
	 * Unchanged from what the mod does with no configuration at all.
	 */
	public static final VisualSettings DEFAULT = new VisualSettings(1.0F, 1.0F);

	public static final Codec<VisualSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, MAX_SCALE).fieldOf("bloom_particle_scale")
					.forGetter(VisualSettings::bloomParticleScale),
			Codec.floatRange(0.0F, MAX_SCALE).fieldOf("bloom_sound_volume")
					.forGetter(VisualSettings::bloomSoundVolume)
	).apply(instance, VisualSettings::new));

	/**
	 * What the loader reads with: {@link #CODEC}, but falling back to the defaults rather than
	 * failing, and saying why.
	 */
	public static final Codec<VisualSettings> LENIENT_CODEC = CODEC.orElse(
			(Consumer<String>) error -> AbyssFall.LOGGER.warn(
					"Could not read the 'visuals' config block ({}); using its defaults", error),
			DEFAULT);

	/**
	 * Whether particles should be emitted at all.
	 */
	public boolean hasParticles() {
		return this.bloomParticleScale > 0.0F;
	}

	/**
	 * Whether sounds should be played at all.
	 */
	public boolean hasSounds() {
		return this.bloomSoundVolume > 0.0F;
	}

	/**
	 * Scales a particle count, never rounding a wanted effect away entirely.
	 *
	 * <p>A small multiplier applied to a small count rounds to zero, which would make the
	 * quieter layers of the bloom vanish and read as a bug rather than as a setting. Any
	 * non-zero multiplier therefore yields at least one particle; asking for none is done by
	 * setting the multiplier to zero, which {@link #hasParticles()} reports separately.
	 */
	public int scaleParticles(int count) {
		if (!hasParticles()) {
			return 0;
		}

		return Math.max(1, Math.round(count * this.bloomParticleScale));
	}

	/**
	 * Scales a sound volume. Only the volume is affected: the pitches the bloom uses are part
	 * of how it reads rather than a matter of preference, so they are left alone.
	 */
	public float scaleVolume(float volume) {
		return volume * this.bloomSoundVolume;
	}
}

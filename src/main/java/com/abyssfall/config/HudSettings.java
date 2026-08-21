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
 * Settings for the San readout drawn above the hotbar.
 *
 * @param showBelowPercent the San percentage at or below which the readout appears
 */
public record HudSettings(float showBelowPercent) {
	/**
	 * The readout appears as soon as San is anything less than completely full.
	 *
	 * <p>Expressed as the percentage it is compared against rather than as a ratio, because
	 * this is the number a player reads and edits, and San is reported as a percentage
	 * everywhere else the player can see it.
	 */
	public static final float DEFAULT_SHOW_BELOW_PERCENT = 100.0F;

	/**
	 * What a fresh configuration file contains: visible whenever San is not full.
	 */
	public static final HudSettings DEFAULT = new HudSettings(DEFAULT_SHOW_BELOW_PERCENT);

	public static final Codec<HudSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 100.0F).fieldOf("show_below_percent")
					.forGetter(HudSettings::showBelowPercent)
	).apply(instance, HudSettings::new));

	/**
	 * What the loader reads with: {@link #CODEC}, but falling back to the defaults rather than
	 * failing, and saying why.
	 */
	public static final Codec<HudSettings> LENIENT_CODEC = CODEC.orElse(
			(Consumer<String>) error -> AbyssFall.LOGGER.warn(
					"Could not read the 'hud' config block ({}); using its defaults", error),
			DEFAULT);

	/**
	 * Whether a San reading of {@code percent} should be shown.
	 *
	 * <p>Strictly below the threshold, which is what makes the default of {@code 100} mean
	 * "show unless completely full": a player at their ceiling reads exactly 100 and is the one
	 * case that stays hidden. The same rule read the other way means {@code 0} switches the
	 * readout off entirely, since no reading is below zero.
	 *
	 * @param percent a San percentage in {@code [0, 100]}
	 */
	public boolean shouldShow(float percent) {
		return percent < this.showBelowPercent;
	}

	/**
	 * This settings block with a different visibility threshold.
	 */
	public HudSettings withShowBelowPercent(float value) {
		return new HudSettings(value);
	}
}

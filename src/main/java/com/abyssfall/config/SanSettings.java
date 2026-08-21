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
 * Rules governing how the world is allowed to move a player's San.
 *
 * <p>Deliberately a block about <em>rules</em>, not about thresholds. Nothing here says what a
 * given San reading means or where any boundary lies — that stays the business of whatever
 * consumes the value, exactly as it is in {@link com.abyssfall.core.SanState}. What this block
 * decides is under which circumstances the mod is permitted to erode San at all.
 *
 * @param peacefulPreventsLoss whether San is immune to erosion while the world is on Peaceful
 */
public record SanSettings(boolean peacefulPreventsLoss) {
	/**
	 * Peaceful protects San.
	 *
	 * <p>On by default, and true to how the difficulty already reads elsewhere in the game:
	 * Peaceful is where hunger refills on its own and hostile mobs do not spawn, so a player who
	 * chose it has said they do not want attritional pressure. San is precisely that kind of
	 * pressure, so it defaults to following the same rule. Turning this off makes the Abyss erode
	 * a Peaceful player as readily as any other, which is a deliberate choice rather than the
	 * baseline.
	 */
	public static final boolean DEFAULT_PEACEFUL_PREVENTS_LOSS = true;

	/**
	 * What a fresh configuration file contains.
	 */
	public static final SanSettings DEFAULT = new SanSettings(DEFAULT_PEACEFUL_PREVENTS_LOSS);

	/**
	 * Describes the block for both reading and writing. {@code fieldOf} rather than
	 * {@code optionalFieldOf} for the reason given on {@link DeveloperSettings#CODEC}: an
	 * optional field is omitted on write whenever it equals the default, which would leave a
	 * generated file with nothing in this block for anyone to find.
	 */
	public static final Codec<SanSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("peaceful_prevents_loss").forGetter(SanSettings::peacefulPreventsLoss)
	).apply(instance, SanSettings::new));

	/**
	 * What the loader reads with: {@link #CODEC}, but falling back to the defaults rather than
	 * failing, and saying why.
	 */
	public static final Codec<SanSettings> LENIENT_CODEC = CODEC.orElse(
			(Consumer<String>) error -> AbyssFall.LOGGER.warn(
					"Could not read the 'san' config block ({}); using its defaults", error),
			DEFAULT);

	/**
	 * This settings block with {@code peacefulPreventsLoss} changed.
	 */
	public SanSettings withPeacefulPreventsLoss(boolean value) {
		return new SanSettings(value);
	}
}

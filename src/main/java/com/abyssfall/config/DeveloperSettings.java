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
 * Settings that exist purely to serve development, and that a released build is expected to
 * leave switched off.
 *
 * @param devInventory whether the developer creative tab and the items inside it are registered
 */
public record DeveloperSettings(boolean devInventory) {
	/**
	 * What a fresh configuration file contains, and the fallback for anything unreadable.
	 *
	 * <p>Developer tooling defaults to off. It has to be asked for deliberately rather than
	 * opted out of, because a build that ships with it enabled hands players content that was
	 * never meant for them.
	 */
	public static final DeveloperSettings DEFAULT = new DeveloperSettings(false);

	/**
	 * Describes the block for both reading and writing.
	 *
	 * <p>{@code fieldOf} rather than {@code optionalFieldOf} is deliberate, with tolerance for
	 * missing fields supplied by {@link #LENIENT_CODEC} instead. An optional field is
	 * <em>omitted on write</em> whenever its value equals the default, which would make a
	 * freshly generated file an empty object and leave nobody anything to edit. Writing through
	 * this codec instead lists every setting explicitly, while reading through
	 * {@code LENIENT_CODEC} still accepts a file that is missing some of them.
	 */
	public static final Codec<DeveloperSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("dev_inventory").forGetter(DeveloperSettings::devInventory)
	).apply(instance, DeveloperSettings::new));

	/**
	 * What the loader actually reads with: {@link #CODEC}, but treating an absent or unreadable
	 * block as the default rather than as a failure, and reporting why when that happens.
	 */
	public static final Codec<DeveloperSettings> LENIENT_CODEC = CODEC.orElse(
			(Consumer<String>) error -> AbyssFall.LOGGER.warn(
					"Could not read the 'developer' config block ({}); using its defaults", error),
			DEFAULT);

	/**
	 * This settings block with {@code devInventory} changed.
	 */
	public DeveloperSettings withDevInventory(boolean value) {
		return new DeveloperSettings(value);
	}
}

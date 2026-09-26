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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import com.abyssfall.AbyssFall;

/**
 * Settings for the Abyssal Strike — the kill resolution shared by the Final Death Omen and
 * whatever a modpack author marks with {@code abyss_striking}.
 *
 * @param deathMessages the pool a random death message is picked from for kills by
 *                      non-Omen members, as {@link String#format} patterns: {@code %1$s} is
 *                      the victim's name, {@code %2$s} the striker's. Literals, not
 *                      translation keys — this field is the customization point, and every
 *                      client reads the same words. The Final Death Omen's own messages
 *                      live in the language files and are not affected by this.
 */
public record StrikingSettings(List<String> deathMessages) {
	/**
	 * Key prefix every message entry carries: {@code death_message_1},
	 * {@code death_message_2}, and so on without an upper bound.
	 */
	public static final String KEY_PREFIX = "death_message_";

	/**
	 * The single message a fresh configuration file carries — see {@link #DEFAULT}.
	 */
	public static final String DEFAULT_DEATH_MESSAGE = "%1$s was claimed by the abyss.";

	/**
	 * What a fresh configuration file contains: one entry, written out as
	 * {@code death_message_1}.
	 */
	public static final StrikingSettings DEFAULT = new StrikingSettings(List.of(DEFAULT_DEATH_MESSAGE));

	private static final Pattern MESSAGE_KEY = Pattern.compile(Pattern.quote(KEY_PREFIX) + "(\\d+)");

	/**
	 * Describes the block for both reading and writing. Hand-rolled rather than a record
	 * codec, because the block's keys are dynamic: any number of {@code death_message_N}
	 * entries, collected in numeric order on read and renumbered from 1 on write. Keys
	 * outside the pattern are ignored on read and not preserved on write — the block is
	 * documented as message entries only.
	 */
	public static final Codec<StrikingSettings> CODEC = Codec.PASSTHROUGH.flatXmap(
			StrikingSettings::decode, StrikingSettings::encode);

	/**
	 * What the loader reads with: {@link #CODEC}, but falling back to the defaults rather
	 * than failing, and saying why.
	 */
	public static final Codec<StrikingSettings> LENIENT_CODEC = CODEC.orElse(
			(Consumer<String>) error -> AbyssFall.LOGGER.warn(
					"Could not read the 'striking' config block ({}); using its defaults", error),
			DEFAULT);

	private static DataResult<StrikingSettings> decode(Dynamic<?> dynamic) {
		if (!(dynamic.getValue() instanceof JsonElement element) || !element.isJsonObject()) {
			return DataResult.error(() -> "The 'striking' block must be an object");
		}

		record Entry(int number, String text) {
		}

		List<Entry> entries = new ArrayList<>();

		for (var member : element.getAsJsonObject().entrySet()) {
			Matcher matcher = MESSAGE_KEY.matcher(member.getKey());

			if (matcher.matches()
					&& member.getValue().isJsonPrimitive()
					&& member.getValue().getAsJsonPrimitive().isString()) {
				entries.add(new Entry(Integer.parseInt(matcher.group(1)),
						member.getValue().getAsString()));
			}
		}

		if (entries.isEmpty()) {
			return DataResult.error(() -> "The 'striking' block names no death_message_N entries");
		}

		entries.sort(Comparator.comparingInt(Entry::number));

		return DataResult.success(new StrikingSettings(
				entries.stream().map(Entry::text).toList()));
	}

	private static DataResult<Dynamic<?>> encode(StrikingSettings settings) {
		JsonObject json = new JsonObject();

		for (int i = 0; i < settings.deathMessages().size(); i++) {
			json.addProperty(KEY_PREFIX + (i + 1), settings.deathMessages().get(i));
		}

		return DataResult.success(new Dynamic<>(JsonOps.INSTANCE, json));
	}

	/**
	 * This settings block with {@code deathMessages} changed.
	 */
	public StrikingSettings withDeathMessages(List<String> value) {
		return new StrikingSettings(List.copyOf(value));
	}
}

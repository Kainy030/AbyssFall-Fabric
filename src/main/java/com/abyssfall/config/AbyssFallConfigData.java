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
 * The whole of the mod's configuration, as an immutable value.
 *
 * <p>Settings are grouped into nested blocks rather than kept in one flat list, because the
 * mod is expected to accumulate a great many of them and a flat file becomes unreadable long
 * before it becomes incomplete. Each block is its own record with its own {@link Codec}, so a
 * new group of settings is added by writing one record and adding one field here.
 *
 * <h2>Adding a setting</h2>
 *
 * <p>Add the field to the relevant block's record and to its {@code CODEC} with
 * {@code fieldOf}, and give the record's {@code DEFAULT} a value for it. Missing fields are
 * tolerated by each block's {@code LENIENT_CODEC}, which is what makes old configuration files
 * keep working: a file written before the field existed falls back to the default and nothing
 * has to be migrated. Adding a whole new block means writing the record with its {@code CODEC},
 * {@code LENIENT_CODEC} and {@code DEFAULT}, then adding one field here.
 *
 * @param developer settings that serve development and are expected to be off in a release
 * @param loot      which loot tables the flower appears in, and how often
 * @param visuals   how loud and how busy the mod's effects are
 * @param hud       when the San readout above the hotbar is shown
 */
public record AbyssFallConfigData(DeveloperSettings developer, LootSettings loot,
		VisualSettings visuals, HudSettings hud) {
	/**
	 * The configuration a fresh install gets, and the fallback whenever a file cannot be read.
	 *
	 * <p>Every block's default reproduces what the mod did before it was configurable, so a
	 * player who never opens the file sees exactly the behaviour the mod ships with. The one
	 * deliberate exception is the developer block, which is off by default because its contents
	 * were never meant for normal play.
	 */
	public static final AbyssFallConfigData DEFAULT = new AbyssFallConfigData(
			DeveloperSettings.DEFAULT, LootSettings.DEFAULT, VisualSettings.DEFAULT,
			HudSettings.DEFAULT);

	/**
	 * Every block is read leniently, so a configuration file containing only the sections a
	 * user cared to change loads exactly as well as a complete one, and a section that is
	 * present but malformed falls back to that section's defaults rather than discarding the
	 * whole file.
	 *
	 * <p>Writing, by contrast, always emits every block in full — see
	 * {@link DeveloperSettings#CODEC} for why that matters.
	 */
	public static final Codec<AbyssFallConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			DeveloperSettings.LENIENT_CODEC.fieldOf("developer")
					.orElse((Consumer<String>) error -> AbyssFall.LOGGER.warn(
							"No readable 'developer' config block ({}); using its defaults", error),
							DeveloperSettings.DEFAULT)
					.forGetter(AbyssFallConfigData::developer),
			LootSettings.LENIENT_CODEC.fieldOf("loot")
					.orElse((Consumer<String>) error -> AbyssFall.LOGGER.warn(
							"No readable 'loot' config block ({}); using its defaults", error),
							LootSettings.DEFAULT)
					.forGetter(AbyssFallConfigData::loot),
			VisualSettings.LENIENT_CODEC.fieldOf("visuals")
					.orElse((Consumer<String>) error -> AbyssFall.LOGGER.warn(
							"No readable 'visuals' config block ({}); using its defaults", error),
							VisualSettings.DEFAULT)
					.forGetter(AbyssFallConfigData::visuals),
			HudSettings.LENIENT_CODEC.fieldOf("hud")
					.orElse((Consumer<String>) error -> AbyssFall.LOGGER.warn(
							"No readable 'hud' config block ({}); using its defaults", error),
							HudSettings.DEFAULT)
					.forGetter(AbyssFallConfigData::hud)
	).apply(instance, AbyssFallConfigData::new));

	/**
	 * This configuration with a different developer block.
	 */
	public AbyssFallConfigData withDeveloper(DeveloperSettings value) {
		return new AbyssFallConfigData(value, this.loot, this.visuals, this.hud);
	}

	/**
	 * This configuration with a different loot block.
	 */
	public AbyssFallConfigData withLoot(LootSettings value) {
		return new AbyssFallConfigData(this.developer, value, this.visuals, this.hud);
	}

	/**
	 * This configuration with a different visuals block.
	 */
	public AbyssFallConfigData withVisuals(VisualSettings value) {
		return new AbyssFallConfigData(this.developer, this.loot, value, this.hud);
	}

	/**
	 * This configuration with a different hud block.
	 */
	public AbyssFallConfigData withHud(HudSettings value) {
		return new AbyssFallConfigData(this.developer, this.loot, this.visuals, value);
	}
}

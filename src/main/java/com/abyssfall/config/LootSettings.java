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

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import com.abyssfall.AbyssFall;

/**
 * Which loot tables the Flower of the Abyss can turn up in, and how often.
 *
 * @param flowerChance how often the flower appears in a listed table, as a probability in
 *                     {@code [0, 1]}
 * @param targetTables the loot tables to inject into
 */
public record LootSettings(float flowerChance, List<ResourceKey<LootTable>> targetTables) {
	/**
	 * The chance the mod shipped with before any of this was configurable: one in twenty.
	 */
	public static final float DEFAULT_FLOWER_CHANCE = 0.05F;

	/**
	 * The tables the mod shipped with — the notable reward chests of major structures.
	 *
	 * <p>Vanilla only ever places genuinely {@code EPIC} rarity items in two chest tables (the
	 * ominous trial chamber unique reward and the ancient city), so keying purely off rarity
	 * would miss the classic treasure chests such as the desert pyramid. This list follows the
	 * intent instead.
	 *
	 * <p>Written out in full when the config file is generated rather than left implicit, so
	 * that the file itself tells the reader what can be changed. Nothing restricts these to
	 * chests or to vanilla: any loot table identifier works, including another mod's, and
	 * including non-chest tables such as fishing or bartering.
	 */
	public static final List<ResourceKey<LootTable>> DEFAULT_TARGET_TABLES = List.of(
			BuiltInLootTables.DESERT_PYRAMID,
			BuiltInLootTables.JUNGLE_TEMPLE,
			BuiltInLootTables.END_CITY_TREASURE,
			BuiltInLootTables.WOODLAND_MANSION,
			BuiltInLootTables.STRONGHOLD_LIBRARY,
			BuiltInLootTables.STRONGHOLD_CORRIDOR,
			BuiltInLootTables.STRONGHOLD_CROSSING,
			BuiltInLootTables.BASTION_TREASURE,
			BuiltInLootTables.BASTION_OTHER,
			BuiltInLootTables.BASTION_BRIDGE,
			BuiltInLootTables.BASTION_HOGLIN_STABLE,
			BuiltInLootTables.SHIPWRECK_TREASURE,
			BuiltInLootTables.ANCIENT_CITY,
			BuiltInLootTables.TRIAL_CHAMBERS_REWARD_UNIQUE,
			BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE,
			BuiltInLootTables.NETHER_BRIDGE,
			BuiltInLootTables.PILLAGER_OUTPOST,
			BuiltInLootTables.BURIED_TREASURE
	);

	/**
	 * Exactly what the mod did before any of this was configurable.
	 */
	public static final LootSettings DEFAULT = new LootSettings(
			DEFAULT_FLOWER_CHANCE, DEFAULT_TARGET_TABLES);

	public static final Codec<LootSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("flower_chance")
					.forGetter(LootSettings::flowerChance),
			ResourceKey.codec(Registries.LOOT_TABLE).listOf().fieldOf("target_tables")
					.forGetter(LootSettings::targetTables)
	).apply(instance, LootSettings::new));

	/**
	 * What the loader reads with: {@link #CODEC}, but falling back to the defaults rather than
	 * failing, and saying why.
	 */
	public static final Codec<LootSettings> LENIENT_CODEC = CODEC.orElse(
			(Consumer<String>) error -> AbyssFall.LOGGER.warn(
					"Could not read the 'loot' config block ({}); using its defaults", error),
			DEFAULT);

	/**
	 * Whether the flower should be injected at all. A chance of zero switches the baseline pool
	 * off entirely rather than adding a pool that can never pay out.
	 */
	public boolean injectsBaselinePool() {
		return this.flowerChance > 0.0F;
	}

	/**
	 * Whether the flower is guaranteed, in which case no filler entry is added alongside it.
	 */
	public boolean isGuaranteed() {
		return this.flowerChance >= 1.0F;
	}

	/**
	 * The weight of the "nothing happens" entry that sits alongside a single flower entry, so
	 * that the flower turns up with probability {@link #flowerChance}.
	 *
	 * <p>A loot pool deals in integer weights, but a probability is the far more useful thing
	 * to hand a reader, so the conversion happens here. With the flower at weight one, the
	 * filler needs weight {@code (1 - p) / p} for the flower's share to come out at {@code p}.
	 *
	 * <p>Only meaningful when the chance is neither zero nor one: at zero no pool is added at
	 * all, and at one no filler entry is added, both of which are decided by
	 * {@link #injectsBaselinePool()} and {@link #isGuaranteed()} before this is consulted.
	 * Clamping to a minimum of one exists only so that a chance which rounds to no filler
	 * cannot silently become a guarantee.
	 */
	public int emptyWeight(int flowerWeight) {
		return Math.max(1, Math.round((1.0F - this.flowerChance) / this.flowerChance * flowerWeight));
	}
}

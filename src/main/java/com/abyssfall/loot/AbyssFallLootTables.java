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

package com.abyssfall.loot;

import java.util.Set;

import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import com.abyssfall.effect.AbyssFallEffects;
import com.abyssfall.item.AbyssFallItems;

/**
 * Injects the mod's items into selected vanilla loot tables.
 */
public final class AbyssFallLootTables {
	/**
	 * Relative chance of the flower actually appearing. The extra pool holds one roll
	 * split between an empty entry and the flower, so the flower shows up in roughly
	 * {@code FLOWER_WEIGHT / (FLOWER_WEIGHT + EMPTY_WEIGHT)} of generated chests.
	 */
	private static final int FLOWER_WEIGHT = 1;

	/**
	 * Weight of the "nothing happens" entry that sits alongside the flower.
	 */
	private static final int EMPTY_WEIGHT = 19;

	/**
	 * High-value structure chests that the flower can turn up in.
	 *
	 * <p>Vanilla only ever places genuinely {@code EPIC} rarity items in two chest tables
	 * (the ominous trial chamber unique reward and the ancient city), so keying purely off
	 * rarity would miss the classic treasure chests such as the desert pyramid. This list
	 * instead follows the intent: the notable "reward" chests of major structures.
	 */
	private static final Set<ResourceKey<LootTable>> TARGET_TABLES = Set.of(
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

	private AbyssFallLootTables() {
	}

	public static void initialize() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			// Only touch the tables the game itself ships. Leaving data pack tables alone means
			// a pack author who deliberately rewrote one of these chests keeps full control.
			if (!source.isBuiltin() || !TARGET_TABLES.contains(key)) {
				return;
			}

			// Baseline chance for any player.
			tableBuilder.withPool(LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.add(EmptyLootItem.emptyItem().setWeight(EMPTY_WEIGHT))
					.add(LootItem.lootTableItem(AbyssFallItems.ABYSS_FLOWER).setWeight(FLOWER_WEIGHT)));

			// Guaranteed extra flower while the opening player carries Abyss Explorer.
			//
			// LootContextParams.THIS_ENTITY is populated by RandomizableContainer#unpackLootTable
			// with the player who opened the container, so an entity property condition targeting
			// THIS sees the opener. When a chest is filled without a player the parameter is
			// absent, the condition simply fails, and this pool contributes nothing.
			tableBuilder.withPool(LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.when(LootItemEntityPropertyCondition.hasProperties(
							LootContext.EntityTarget.THIS,
							EntityPredicate.Builder.entity()
									.effects(MobEffectsPredicate.Builder.effects()
											.and(AbyssFallEffects.ABYSS_EXPLORER))))
					.add(LootItem.lootTableItem(AbyssFallItems.ABYSS_FLOWER)
							.apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))));
		});
	}
}

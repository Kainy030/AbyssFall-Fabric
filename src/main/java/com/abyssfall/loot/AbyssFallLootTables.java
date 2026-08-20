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
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;

import com.abyssfall.AbyssFall;
import com.abyssfall.config.AbyssFallConfig;
import com.abyssfall.config.LootSettings;
import com.abyssfall.effect.AbyssFallEffects;
import com.abyssfall.item.AbyssFallItems;

/**
 * Injects the Flower of the Abyss into the loot tables named by the configuration.
 *
 * <h2>Weight of one entry, expressed as a probability</h2>
 *
 * <p>A loot pool works in integer weights, but the configuration asks for a probability,
 * because that is the number a reader can reason about. The conversion lives in
 * {@link LootSettings#emptyWeight(int)}; the two ends of the range are special-cased here, since
 * a chance of zero means adding no pool at all and a chance of one means adding no filler entry
 * beside the flower.
 *
 * <h2>Listed tables are injected regardless of where they came from</h2>
 *
 * <p>There is deliberately no {@link LootTableSource#isBuiltin()} test. An identifier written
 * into the configuration is an instruction, and second-guessing it would mean the mod silently
 * declining to do what it was told whenever some other data pack happened to touch the same
 * table — with nothing in the log to explain the absence.
 *
 * <p>Injecting is not the same as overriding. Every pool added here is a <em>new, independent</em>
 * pool: no existing pool is edited and no existing entry is removed or reweighted, so whatever
 * another data pack wrote about that table still holds exactly as written. A note is logged when
 * a non-builtin table is amended, so the overlap is visible to anyone reading the log rather than
 * being either hidden or silently obeyed.
 */
public final class AbyssFallLootTables {
	/**
	 * The weight assigned to the single flower entry in the baseline pool. Kept at one so that
	 * the configured probability maps directly to the weight of the empty entry via
	 * {@link LootSettings#emptyWeight(int)}.
	 */
	private static final int FLOWER_WEIGHT = 1;

	/**
	 * Configured tables not yet seen during the current load.
	 *
	 * <p>Concurrent because loot loading may run across threads — the same reason Fabric's own
	 * loot implementation avoids plain collections here.
	 */
	private static final Set<ResourceKey<LootTable>> PENDING_TABLES = ConcurrentHashMap.newKeySet();

	private AbyssFallLootTables() {
	}

	public static void initialize() {
		LootSettings settings = AbyssFallConfig.loot();

		PENDING_TABLES.addAll(settings.targetTables());

		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!settings.targetTables().contains(key)) {
				return;
			}

			PENDING_TABLES.remove(key);

			if (!source.isBuiltin()) {
				// Not a refusal, just a note: the table has been replaced or overridden by
				// something else, and an extra pool is being appended to that version of it.
				AbyssFall.LOGGER.info("Loot table '{}' comes from {}; appending an additional "
						+ "pool to it as configured", key.identifier(), source);
			}

			if (settings.injectsBaselinePool()) {
				tableBuilder.withPool(baselinePool(settings));
			}

			tableBuilder.withPool(explorerPool());
		});

		// A table the configuration names but no data pack provides never reaches MODIFY at
		// all, so the absence can only be noticed once loading has finished.
		LootTableEvents.ALL_LOADED.register((resourceManager, lootRegistry) -> {
			for (ResourceKey<LootTable> missing : PENDING_TABLES) {
				AbyssFall.LOGGER.warn("Configured loot table '{}' was never loaded, so nothing "
						+ "was injected into it. Check the identifier, and whether the mod or "
						+ "data pack that provides it is installed.", missing.identifier());
			}

			// Refill for the next reload: MODIFY runs again for every table each time.
			PENDING_TABLES.clear();
			PENDING_TABLES.addAll(AbyssFallConfig.loot().targetTables());
		});
	}

	/**
	 * The pool every player rolls against.
	 *
	 * <p>One roll, split between the flower and a filler entry whose weight is chosen so the
	 * flower's share matches the configured probability. When the flower is guaranteed there is
	 * no filler at all, because a single-entry pool always yields that entry — leaving a
	 * minimum-weight filler in place would quietly cap the chance at one half.
	 */
	private static LootPool.Builder baselinePool(LootSettings settings) {
		LootPool.Builder pool = LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F));

		if (!settings.isGuaranteed()) {
			pool.add(EmptyLootItem.emptyItem().setWeight(settings.emptyWeight(FLOWER_WEIGHT)));
		}

		return pool.add(LootItem.lootTableItem(AbyssFallItems.ABYSS_FLOWER)
				.setWeight(FLOWER_WEIGHT));
	}

	/**
	 * An extra, guaranteed flower for a player carrying Abyss Explorer.
	 *
	 * <p>{@code LootContextParams.THIS_ENTITY} is populated by
	 * {@code RandomizableContainer#unpackLootTable} with the player who opened the container, so
	 * a condition targeting {@code THIS} sees the opener. A container filled with no player
	 * involved leaves the parameter absent, the condition fails, and this pool contributes
	 * nothing — which is why breaking a chest cannot yield this bonus.
	 */
	private static LootPool.Builder explorerPool() {
		return LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.when(LootItemEntityPropertyCondition.hasProperties(
						LootContext.EntityTarget.THIS,
						EntityPredicate.Builder.entity()
								.effects(MobEffectsPredicate.Builder.effects()
										.and(AbyssFallEffects.ABYSS_EXPLORER))))
				.add(LootItem.lootTableItem(AbyssFallItems.ABYSS_FLOWER)
						.apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))));
	}
}

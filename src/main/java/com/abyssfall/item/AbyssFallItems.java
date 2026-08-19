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

package com.abyssfall.item;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import com.abyssfall.AbyssFall;

/**
 * Registry holder for every item the mod adds.
 */
public final class AbyssFallItems {
	/**
	 * Placeholder item. It has no behaviour yet and exists so the registry, the creative
	 * tab and the resource pipeline can be exercised end to end.
	 *
	 * <p>Uses {@link Rarity#EPIC}, the highest rarity vanilla provides.
	 */
	public static final Item ABYSS_FLOWER = register("abyss_flower", Item::new,
			new Item.Properties().rarity(Rarity.EPIC));

	private AbyssFallItems() {
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, AbyssFall.id(name));

		T item = itemFactory.apply(properties.setId(itemKey));

		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return item;
	}

	public static void initialize() {
		// Registration happens purely through the static initialiser above; this call exists
		// so the class is loaded from the mod initializer.
	}
}

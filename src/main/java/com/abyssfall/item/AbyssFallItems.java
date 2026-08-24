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

	/**
	 * Cognition Lens — switches the San readout between the icon row and the percentage bar.
	 *
	 * <p>Registered here rather than in {@code AbyssFallDevInventory} even though it was built from
	 * the San Counter, because it is player-facing content: it reveals nothing the design wants
	 * hidden, only changing how an already-visible reading is drawn. Stacks to one, since a second
	 * copy would do nothing a first cannot.
	 *
	 * <p>Uses {@link Rarity#EPIC}, the highest rarity vanilla provides.
	 */
	public static final Item SAN_LENS = register("san_lens", SanLensItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	/**
	 * Gold Lens — a mirror with nothing looking back out of it.
	 *
	 * <p>No behaviour at all, and a plain {@link Item} rather than a subclass, because there is
	 * nothing yet for a subclass to do. It shares the Cognition Lens's frame and glass and differs
	 * only in having no eye, which is the whole of what it currently says: the same object, before
	 * or after whatever it is that looks through the other one.
	 *
	 * <p>Stacks to one, like the Cognition Lens. Not for that item's reason — a second copy of this
	 * one would be no less useful than the first, since neither does anything — but because the two
	 * are the same object at different moments and a pile of framed mirrors is not the register the
	 * item is written in.
	 */
	public static final Item GOLD_LENS = register("gold_lens", Item::new,
			new Item.Properties().stacksTo(1));

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

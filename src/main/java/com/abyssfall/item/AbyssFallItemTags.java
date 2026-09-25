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

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.abyssfall.AbyssFall;

/**
 * Item tags owned by the mod. The tags themselves are data files under
 * {@code data/abyssfall/tags/item/}; this class only holds their keys so that code can ask
 * membership questions.
 */
public final class AbyssFallItemTags {
	/**
	 * Bless From Abyss (深渊庇佑者) — the forgings the abyss has blessed with its verdict.
	 *
	 * <p>Membership is what {@code PlayerAttackMixin} asks about: an attack made with an item
	 * in this tag is handed to {@link FinalDeathOmen#strike} in place of the vanilla attack.
	 * The blessing is bestowed on what is forged of Abyssdium — the Final Death Omen first
	 * among them — and on anything a datapack later adds, which strikes the same way without
	 * any code changing.
	 */
	public static final TagKey<Item> BLESS_FROM_ABYSS = create("bless_from_abyss");

	/**
	 * Dig From Abyss (深渊采集者) — the forgings that dig on the abyss's behalf.
	 *
	 * <p>Membership is what {@code BlockDestroyProgressMixin} and {@code AbyssFallBedrockDrops}
	 * ask about: only a forging in this tag digs where digging is refused, and only it brings
	 * bedrock home. The Final Death Omen is deliberately absent — it is a sword, and swords
	 * do not mine. The material's two axes are separate on purpose: what a forging is
	 * <em>for</em> decides which tag it joins, and a tool meant for both joins both. Currently
	 * empty, because no Abyssdium digging tool exists yet.
	 */
	public static final TagKey<Item> DIG_FROM_ABYSS = create("dig_from_abyss");

	/**
	 * What repairs Abyssdium gear: nothing, by design.
	 *
	 * <p>The element is {@code UNBREAKABLE} and never takes damage, so a repair ingredient
	 * would be a promise with no use. The tag exists — and ships empty — because
	 * {@code ToolMaterial} requires some key and {@code Item.Properties.repairable} resolves
	 * it eagerly at registration; an empty tag tells the anvil exactly "nothing repairs
	 * this". Named after vanilla's own convention ({@code netherite_tool_materials} and its
	 * kin), so anything reading repair ingredients by tag finds it where it expects to.
	 */
	public static final TagKey<Item> ABYSSDIUM_TOOL_MATERIALS = create("abyssdium_tool_materials");

	private AbyssFallItemTags() {
	}

	private static TagKey<Item> create(String name) {
		return TagKey.create(Registries.ITEM, AbyssFall.id(name));
	}
}

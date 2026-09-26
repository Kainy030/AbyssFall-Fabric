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
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.abyssfall.AbyssFall;

/**
 * Item tags owned by the mod. The tags themselves are data files under
 * {@code data/abyssfall/tags/item/}; this class holds their keys so that code can ask
 * membership questions — and answers the one question that composes our tags with
 * vanilla's own class tags ({@link #digsFromAbyss}).
 *
 * <p>{@code abyss_striking} exists for modpack authors first: membership is the data-level
 * gate that lets another weapon join the abyss's verdict. The Final Death Omen strikes by
 * identity already and needs no tag; its membership there is the insurance, not the
 * mechanism.
 */
public final class AbyssFallItemTags {
	/**
	 * Abyssal Gaze (深渊凝视) — every abyssdium-material item: the element itself and
	 * everything forged of it.
	 *
	 * <p>Membership couples the material axis and the immortality axis into one statement,
	 * because they are one fact: an abyssdium item is indestructible by nature, so "what
	 * it is made of" and "it cannot be destroyed" say the same thing. The NeverDestroyed
	 * composition root reads it — a new abyssdium item is protected with no code change —
	 * and {@link #digsFromAbyss} starts the digging question from it.
	 */
	public static final TagKey<Item> ABYSS_GAZING = create("abyss_gazing");

	/**
	 * Abyssal Strike (深渊打击) — the kill trigger's data-level gate.
	 *
	 * <p>Membership makes an attack resolve through the abyss's strike
	 * ({@code PlayerAttackMixin}), alongside the Final Death Omen's own identity. The tag
	 * is a vocabulary for modpack authors: whatever they mark with it shares the verdict
	 * mechanically, but its victims read the configurable generic death message
	 * ({@code striking.death_message}), never the Omen's own wording.
	 */
	public static final TagKey<Item> ABYSS_STRIKING = create("abyss_striking");

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

	/**
	 * Whether the stack digs on the abyss's behalf: an Abyssdium forging whose class digs —
	 * pickaxe, shovel, axe or hoe by vanilla's own class tags. A sword is a forging too,
	 * but swords do not dig.
	 */
	public static boolean digsFromAbyss(ItemStack stack) {
		return stack.is(ABYSS_GAZING)
				&& (stack.is(ItemTags.PICKAXES)
						|| stack.is(ItemTags.SHOVELS)
						|| stack.is(ItemTags.AXES)
						|| stack.is(ItemTags.HOES));
	}

	private static TagKey<Item> create(String name) {
		return TagKey.create(Registries.ITEM, AbyssFall.id(name));
	}
}

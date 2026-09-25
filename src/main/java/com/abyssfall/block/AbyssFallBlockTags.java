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

package com.abyssfall.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.abyssfall.AbyssFall;

/**
 * Block tags owned by the mod. The tags themselves are data files under
 * {@code data/abyssfall/tags/block/}; this class only holds their keys so that code can
 * name them.
 */
public final class AbyssFallBlockTags {
	/**
	 * What an Abyssdium tool cannot harvest: nothing.
	 *
	 * <p>Vanilla's tool materials use this slot to name the blocks that resist them —
	 * {@code incorrect_for_netherite_tool} and its kin. Abyssdium's harvest tier is stated
	 * as the absence of that list: the tag ships empty, so no block is ever marked as
	 * denying this material its drops. The separate "unmineable" class — bedrock and its
	 * kin, whose destroy speed is −1 — is not a tier question at all and is answered by
	 * {@code BlockDestroyProgressMixin}, not by this tag.
	 */
	public static final TagKey<Block> INCORRECT_FOR_ABYSSDIUM_TOOL =
			create("incorrect_for_abyssdium_tool");

	private AbyssFallBlockTags() {
	}

	private static TagKey<Block> create(String name) {
		return TagKey.create(Registries.BLOCK, AbyssFall.id(name));
	}
}

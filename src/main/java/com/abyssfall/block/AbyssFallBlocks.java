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

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.abyssfall.AbyssFall;

/**
 * Registry holder for every block the mod adds.
 */
public final class AbyssFallBlocks {
	/**
	 * Abyss Dirt. Copies vanilla dirt's properties wholesale so hardness, tool behaviour,
	 * sounds and map colour match, and only its bone meal interaction differs.
	 */
	public static final Block ABYSS_DIRT = register(
			"abyss_dirt",
			AbyssDirtBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT),
			true
	);

	private AbyssFallBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory,
			BlockBehaviour.Properties properties, boolean shouldRegisterItem) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, AbyssFall.id(name));

		Block block = blockFactory.apply(properties.setId(blockKey));

		if (shouldRegisterItem) {
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, AbyssFall.id(name));

			BlockItem blockItem = new BlockItem(block,
					new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
			Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		}

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static void initialize() {
		// Registration happens in the static initialiser above; this call exists so the
		// class is loaded from the mod initializer.
	}
}

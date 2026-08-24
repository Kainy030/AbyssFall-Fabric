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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

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

	/**
	 * Tinted Glass Pane. The properties are vanilla's own {@code GLASS_PANE} line copied field for
	 * field, plus tinted glass's grey map colour so it reads correctly on a map.
	 *
	 * <p>Written out rather than using {@code ofFullCopy(Blocks.GLASS_PANE)} because a copy would
	 * also bring the map colour along and then have to override it, which reads as though the colour
	 * were an afterthought rather than the one intended difference.
	 *
	 * <p>{@code noOcclusion} stays despite the block blocking light. The two are unrelated:
	 * occlusion decides whether neighbouring faces are skipped when rendering, and a pane is not a
	 * full cube so it must not claim to hide anything. The light blocking lives in
	 * {@link TintedGlassPaneBlock} instead.
	 */
	public static final Block TINTED_GLASS_PANE = register(
			"tinted_glass_pane",
			TintedGlassPaneBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_GRAY)
					.instrument(NoteBlockInstrument.HAT)
					.strength(0.3F)
					.sound(SoundType.GLASS)
					.noOcclusion(),
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

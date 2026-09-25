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

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import com.abyssfall.item.AbyssFallItemTags;

/**
 * Makes bedrock drop itself when an Abyssdium tool brings it down.
 *
 * <p>Bedrock is declared {@code noLootTable()}, so no datapack file can give it a drop — the
 * block never names a table to fill. What it does have is the ordinary break path:
 * {@code ServerPlayerGameMode.destroyBlock} runs to completion once
 * {@code BlockDestroyProgressMixin} lets the dig finish, and this listener rides the event
 * Fabric fires at the end of it. An event rather than a second injection: the break already
 * happened, and all that remains is to place the item in the world.
 *
 * <p>The {@code preventsBlockDrops} check is vanilla's own condition for the drop it would
 * have made ({@code ServerPlayerGameMode.destroyBlock} consults the same method), copied so
 * that creative demolition stays clean for bedrock exactly as it is for every other block.
 */
public final class AbyssFallBedrockDrops {
	private AbyssFallBedrockDrops() {
	}

	public static void initialize() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!state.is(Blocks.BEDROCK)
					|| player.preventsBlockDrops()
					|| !player.getMainHandItem().is(AbyssFallItemTags.DIG_FROM_ABYSS)) {
				return;
			}

			Block.popResource(level, pos, new ItemStack(Items.BEDROCK));
		});
	}
}

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

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

import net.fabricmc.fabric.api.event.player.ItemEvents;

import com.abyssfall.advancement.AbyssFallAdvancements;

/**
 * Makes bone meal applied to a wither rose standing on abyss dirt bloom the rose.
 *
 * <p>Vanilla's {@code BoneMealItem} only looks at the block that was actually clicked, and
 * {@code FlowerBlock} does not implement {@code BonemealableBlock}. Clicking the rose would
 * therefore do nothing at all. Rather than injecting into the bone meal item, this listens
 * to {@code ItemEvents.USE_ON} and routes the growth to the dirt underneath.
 *
 * <p>Clicks on the dirt are intercepted here too, even though {@code AbyssDirtBlock} is
 * bonemealable and vanilla could drive that case by itself. Handling both here keeps the
 * interaction identical whichever block the player aimed at, and avoids vanilla's cheerful
 * green particle burst (level event 1505) firing on top of the mod's own soul-themed effects.
 */
public final class AbyssFallBoneMealHandler {
	private AbyssFallBoneMealHandler() {
	}

	public static void initialize() {
		ItemEvents.USE_ON.register(context -> {
			ItemStack stack = context.getItemInHand();

			if (!stack.is(Items.BONE_MEAL)) {
				// Returning null passes the interaction on to other listeners and vanilla.
				return null;
			}

			Level level = context.getLevel();
			BlockPos clickedPos = context.getClickedPos();

			// The player may aim at the rose itself or at the dirt it stands on. Resolve both
			// to the same dirt position so the interaction feels identical either way.
			BlockPos dirtPos = level.getBlockState(clickedPos).is(Blocks.WITHER_ROSE)
					? clickedPos.below()
					: clickedPos;

			if (!AbyssDirtBlock.hasHarvestableRose(level, dirtPos)) {
				return null;
			}

			if (level instanceof ServerLevel serverLevel) {
				boolean bloomed = AbyssDirtBlock.bloom(serverLevel, dirtPos);

				if (bloomed && context.getPlayer() instanceof ServerPlayer serverPlayer) {
					AbyssFallAdvancements.awardBloom(serverPlayer);
				}

				// Dispensers reach this path with no player attached, so the vibration is
				// only emitted when there is someone to attribute it to.
				if (context.getPlayer() != null) {
					stack.causeUseVibration(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
				}

				// No level event 1505 here: that is bone meal's cheerful green burst, and
				// AbyssDirtBlock already plays its own soul-themed effects for the bloom.
				stack.shrink(1);
			}

			return InteractionResult.SUCCESS;
		});
	}
}

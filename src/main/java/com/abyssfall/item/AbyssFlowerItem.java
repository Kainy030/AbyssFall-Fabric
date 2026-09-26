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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.abyssfall.core.AbyssFallCoreSystem;

/**
 * Flower of the Abyss — the abyss's first taste, and its recurring tithe.
 *
 * <p>As food it is a token mouthful: half a shank, and always edible, because the point of
 * eating it is never hunger — the always-edible rule is what lets the tithe be paid with
 * a full stomach.
 *
 * <h2>The first taste</h2>
 *
 * <p>Eating the flower for the first time awakens the San system — until that moment the
 * HUD draws nothing at all and every change to the reading is refused — and deliberately
 * touches no San value itself: the abyss reveals itself before it feeds. The Clear Minded
 * advancement rides the same moment, granted by the vanilla {@code consume_item} trigger
 * in its data, not by this class.
 *
 * <h2>Every taste after</h2>
 *
 * <p>Each later flower raises the San ceiling by {@link #MAX_SAN_PER_FLOWER} and leaves
 * the current reading alone: a bigger vessel is not more water in it. The figure is
 * deliberately off the integer grid, so a devoted player's ceiling stops resembling
 * everyone's round hundred.
 */
public class AbyssFlowerItem extends Item {
	/**
	 * How much the San ceiling grows per flower eaten past the first.
	 */
	private static final float MAX_SAN_PER_FLOWER = 0.7F;

	public AbyssFlowerItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity) {
		// Server only: the client runs this method as well, and both the awakening and
		// the ceiling are the server's to write.
		if (entity instanceof ServerPlayer serverPlayer) {
			if (AbyssFallCoreSystem.isActivated(serverPlayer)) {
				AbyssFallCoreSystem.addMax(serverPlayer, MAX_SAN_PER_FLOWER);
			} else {
				AbyssFallCoreSystem.activate(serverPlayer);
			}
		}

		return super.finishUsingItem(itemStack, level, entity);
	}
}

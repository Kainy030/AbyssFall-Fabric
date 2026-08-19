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

package com.abyssfall.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import com.abyssfall.AbyssFall;

/**
 * Awards the criteria that cannot be expressed as a vanilla trigger.
 *
 * <p>The bloom criterion uses {@code minecraft:impossible} and is granted from here.
 * {@code item_used_on_block} cannot be used for it: that trigger fires only after
 * {@code ItemStack#useOn} has already returned, by which point the wither rose has been
 * removed, so a {@code location_check} on the clicked position would test air and always
 * fail. The companion criterion in the same advancement — actually holding a Flower of the
 * Abyss — is still plain data, and both are required, so the advancement as a whole still
 * means "a rose was consumed and a flower was obtained".
 */
public final class AbyssFallAdvancements {
	public static final Identifier ABYSS_GARDENERS = AbyssFall.id("abyss_gardeners");

	/**
	 * Criterion name; must match the key used in the advancement JSON.
	 */
	private static final String BLOOM_CRITERION = "bloom_wither_rose";

	private AbyssFallAdvancements() {
	}

	/**
	 * Records that the given player forced a wither rose into bloom.
	 */
	public static void awardBloom(ServerPlayer player) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ABYSS_GARDENERS);

		if (advancement == null) {
			AbyssFall.LOGGER.warn("Missing advancement {}; cannot award criterion {}",
					ABYSS_GARDENERS, BLOOM_CRITERION);
			return;
		}

		player.getAdvancements().award(advancement, BLOOM_CRITERION);
	}
}

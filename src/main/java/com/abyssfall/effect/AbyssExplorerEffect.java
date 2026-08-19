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

package com.abyssfall.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * "Abyss Explorer" — a purely passive marker effect.
 *
 * <p>It deliberately overrides nothing. The effect does no per-tick work; its only purpose
 * is to be present on a player so that loot tables can test for it. The actual reward is
 * driven from the loot side in {@code AbyssFallLootTables}, where an entity property
 * condition checks the opening player for this effect.
 */
public class AbyssExplorerEffect extends MobEffect {
	/**
	 * Muted violet, picked to sit alongside the epic rarity colour used by the flower.
	 */
	private static final int COLOR = 0x9B6BC9;

	public AbyssExplorerEffect() {
		super(MobEffectCategory.BENEFICIAL, COLOR);
	}
}

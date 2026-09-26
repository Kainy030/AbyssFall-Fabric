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

import com.abyssfall.itemmechanismruntime.ItemMechanic;
import com.abyssfall.itemmechanismruntime.ItemMechanics;

/**
 * The composition root of the item mechanics: who holds them, stated in one place.
 *
 * <p>The framework in {@code com.abyssfall.itemmechanismruntime} defines the mechanics and
 * stays ignorant of this mod's content — it must never come to exist because Abyssdium
 * exists. Ownership is stated here instead, as <b>three independent insurances</b>, each
 * granted across the entire mechanic list. These three are not coupling: the endgame
 * material and whatever is forged of it are <em>meant</em> to hold every item mechanic
 * there is, so each insurance is stated on its own and stays true even if another is
 * ever removed.
 *
 * <ol>
 *   <li><b>Abyssdium and its forgings</b> — the element itself, and everything forged
 *       of it. The Final Death Omen is the only forging today; later ones join this
 *       line.</li>
 *   <li><b>The Bless From Abyss</b> (深渊庇佑者) — every member of the tag, ours or a
 *       datapack's.</li>
 *   <li><b>The Final Death Omen</b> — the blade, named separately on purpose: insurance
 *       is redundancy, not reuse.</li>
 * </ol>
 */
public final class AbyssFallItemMechanics {
	private AbyssFallItemMechanics() {
	}

	public static void initialize() {
		for (ItemMechanic mechanic : ItemMechanics.all()) {
			// 1. The element, and whatever is forged of it.
			mechanic.grant(stack -> stack.is(AbyssFallItems.ABYSSDIUM)
					|| stack.is(AbyssFallItems.FINAL_DEATH_OMEN));

			// 2. The Bless From Abyss.
			mechanic.grant(stack -> stack.is(AbyssFallItemTags.BLESS_FROM_ABYSS));

			// 3. The blade, stated on its own.
			mechanic.grant(stack -> stack.is(AbyssFallItems.FINAL_DEATH_OMEN));
		}
	}
}

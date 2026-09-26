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

package com.abyssfall.itemmechanismruntime;

import java.util.List;

/**
 * The list of item mechanics — every one the mod has, each a class of its own in this
 * package, enumerated here exactly once.
 *
 * <p>The list exists so that ownership can be granted <em>across the whole of it at
 * once</em>: the composition root ({@code AbyssFallItemMechanics}, outside this package)
 * walks it and states, for every mechanic present and future, which items hold it. Adding
 * a mechanic means adding its class and one entry here; every grant that already covers
 * the list covers the new one too.
 */
public final class ItemMechanics {
	private static final List<ItemMechanic> ALL = List.of(
			NeverDestroyed.INSTANCE);

	private ItemMechanics() {
	}

	/**
	 * Every item mechanic, in declaration order.
	 */
	public static List<ItemMechanic> all() {
		return ALL;
	}
}

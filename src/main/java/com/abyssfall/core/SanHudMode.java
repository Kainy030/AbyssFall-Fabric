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

package com.abyssfall.core;

import java.util.Locale;

/**
 * Which of the two San readouts the HUD is currently drawing.
 *
 * <p>Both readouts already existed and were built to differ in kind: the icon row is the ambient
 * display, legible at a glance and deliberately coarse, while the bar spells the reading out as a
 * percentage. This enum is what lets a player move between them, and it lives in {@code core}
 * rather than in the client package because the item that performs the switch is common code.
 *
 * <p>Deliberately not a San <em>threshold</em> or a San <em>value</em>. This is a display
 * preference and nothing else; it never affects what the reading is, only how it is drawn. The
 * three-layer visibility model still holds — the bar shows a percentage, never the underlying
 * float.
 */
public enum SanHudMode {
	/**
	 * The row of ten icons. What a player sees before they ever find the lens, and what the mod
	 * ships showing.
	 */
	ICONS,

	/**
	 * The bar with the percentage written across it.
	 */
	PERCENT;

	/**
	 * The mode a player has before anything has changed it.
	 */
	public static final SanHudMode DEFAULT = ICONS;

	/**
	 * The other mode. With only two of them, cycling and toggling are the same operation, and
	 * naming it this way keeps the call site honest about there being no ordering implied.
	 */
	public SanHudMode next() {
		return this == ICONS ? PERCENT : ICONS;
	}

	/**
	 * Translation key for this mode's name, as shown when the lens switches to it.
	 */
	public String translationKey() {
		return "item.abyssfall.san_lens.mode." + name().toLowerCase(Locale.ROOT);
	}
}

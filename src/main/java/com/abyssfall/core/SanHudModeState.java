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

import net.minecraft.util.Util;

/**
 * Holds which San readout this client is drawing.
 *
 * <h2>Why this is client state and not player data</h2>
 *
 * <p>Which readout you prefer is not a fact about your character, it is a fact about your screen —
 * the same kind of thing as a keybind or a video option. Storing it as an attachment would sync it,
 * persist it into the save, and make it something the server has an opinion about, none of which is
 * wanted: two players sharing a world should be free to read their own San differently, and the
 * choice should survive being made in one world and carried to the next.
 *
 * <p>It is therefore a single static value, held for as long as the game runs. It intentionally
 * does not persist across restarts. Writing it to a config file would mean saving on every switch
 * and would drag a display toggle into a file the project has decided is not hot-reloaded; if the
 * preference turns out to be worth keeping between sessions, that is a separate decision to make
 * deliberately rather than a side effect of implementing the switch.
 *
 * <h2>Why it lives in {@code core} rather than in the client package</h2>
 *
 * <p>The Cognition Lens is an ordinary item and therefore common code, and it is the item that
 * performs the switch. Common code cannot reach the client source set, so the state has to sit on
 * this side of the split. Nothing here touches a rendering class: the client asks which mode is
 * current and decides what to do about it.
 */
public final class SanHudModeState {
	/**
	 * How long the readout is shown outright after a switch, regardless of the reading.
	 *
	 * <p>Switching is the one moment a player is deliberately looking at the readout, so it has to
	 * be there to be looked at — including at full San, when it would otherwise be hidden and the
	 * switch would appear to have done nothing at all. Half a second is enough to register the new
	 * shape, and the one second fade that follows carries the rest of the reveal, so the row is
	 * legible for a good moment and a half in total without ever feeling stuck on.
	 *
	 * <p>The ordinary fade follows this window rather than replacing it, so the reveal ends the way
	 * the readout always ends rather than snapping off.
	 */
	public static final long REVEAL_MILLIS = 500L;

	private static SanHudMode mode = SanHudMode.DEFAULT;

	/**
	 * When the mode was last switched, from {@link Util#getMillis()}. Zero until the first switch,
	 * which is what stops a reveal from being counted as having happened at time zero.
	 */
	private static long switchedAt;

	private SanHudModeState() {
	}

	/**
	 * The readout this client is currently drawing. Never {@code null}.
	 */
	public static SanHudMode get() {
		return mode;
	}

	/**
	 * Switches to the other readout, and starts the reveal window.
	 *
	 * @return the mode now in effect, so a caller that wants to announce the change does not have
	 *         to ask again
	 */
	public static SanHudMode toggle() {
		mode = mode.next();
		switchedAt = Util.getMillis();
		return mode;
	}

	/**
	 * Sets the readout outright, and starts the reveal window. Present for completeness — nothing
	 * needs it yet, but a keybind or a config-driven default would.
	 */
	public static void set(SanHudMode value) {
		mode = value == null ? SanHudMode.DEFAULT : value;
		switchedAt = Util.getMillis();
	}

	/**
	 * The moment the post-switch reveal stops holding the readout open, from
	 * {@link Util#getMillis()}, or zero if no switch has happened yet.
	 *
	 * <p>Both readouts consult this while working out their opacity, which is what makes a switch
	 * show the row whatever the reading is. Returned as an instant rather than as a boolean so that
	 * a caller can also use it as the point the fade should be measured from — the reveal does not
	 * merely force the row visible, it postpones the fade.
	 */
	public static long revealEndsAt() {
		return switchedAt == 0L ? 0L : switchedAt + REVEAL_MILLIS;
	}
}

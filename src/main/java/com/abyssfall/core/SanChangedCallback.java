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

import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Notifies listeners that a player's San changed, and by how much.
 *
 * <p>Deliberately carries both the previous and the current state rather than a stage or a
 * category. San is a continuous parameter: any movement at all, however small, is a real event
 * that a consumer may want to act on, and where the meaningful boundaries lie is a question for
 * each consumer rather than for the core. A listener that only cares about crossing some line of
 * its own can compare {@code previous.ratio()} against {@code current.ratio()} and decide for
 * itself; a listener that scales continuously with the ratio can simply read the new value.
 *
 * <p>Fired on the server, after the new value has been stored, for every change — including ones
 * where the stored value ended up identical to the old one after clamping. Listeners that want to
 * ignore no-ops should check {@link Change#isNoOp()}.
 */
@FunctionalInterface
public interface SanChangedCallback {
	Event<SanChangedCallback> EVENT = EventFactory.createArrayBacked(SanChangedCallback.class,
			callbacks -> change -> {
				for (SanChangedCallback callback : callbacks) {
					callback.onSanChanged(change);
				}
			});

	void onSanChanged(Change change);

	/**
	 * A single San transition.
	 *
	 * @param player   the player whose San moved
	 * @param previous the state before the change
	 * @param current  the state after the change, as actually stored
	 */
	record Change(ServerPlayer player, SanState previous, SanState current) {
		/**
		 * Signed change in the absolute reading. Negative means San was lost.
		 */
		public float currentDelta() {
			return this.current.current() - this.previous.current();
		}

		/**
		 * Signed change in the ceiling. Negative means the ceiling shrank.
		 */
		public float maxDelta() {
			return this.current.max() - this.previous.max();
		}

		/**
		 * Signed change in the ratio, in {@code [-1, 1]}.
		 *
		 * <p>This is the number that matters for anything the player perceives, since it accounts
		 * for a moving ceiling: losing 20 San is a different event for a player capped at 40 than
		 * for one capped at 400.
		 */
		public float ratioDelta() {
			return this.current.ratio() - this.previous.ratio();
		}

		/**
		 * Whether nothing actually moved. Happens when a change was fully absorbed by clamping,
		 * for example draining San from a player who is already at zero.
		 */
		public boolean isNoOp() {
			return this.previous.equals(this.current);
		}

		/**
		 * Whether the ratio fell across {@code threshold} during this change — that is, it was at
		 * or above it before and is below it now.
		 *
		 * <p>Offered as a convenience for the common "do something once, the moment the player
		 * drops past here" case. It intentionally does not define any thresholds itself; the
		 * caller supplies whatever line it cares about, and different consumers are free to care
		 * about entirely different ones.
		 *
		 * @param threshold a ratio in {@code [0, 1]}
		 */
		public boolean crossedDown(float threshold) {
			return this.previous.ratio() >= threshold && this.current.ratio() < threshold;
		}

		/**
		 * Mirror of {@link #crossedDown(float)}: whether the ratio rose across {@code threshold}.
		 *
		 * @param threshold a ratio in {@code [0, 1]}
		 */
		public boolean crossedUp(float threshold) {
			return this.previous.ratio() < threshold && this.current.ratio() >= threshold;
		}
	}
}

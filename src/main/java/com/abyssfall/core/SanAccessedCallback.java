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

import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Notifies listeners that a player's San was read through the core.
 *
 * <p>Fired for reads only — writes are deliberately not accesses, since the HUD already
 * surfaces value changes through its own visibility rules. What does <em>not</em> count
 * as an access either: the core's own internal read-backs, the HUD's per-frame mirror,
 * and the potion effects' tick reads (all of which use
 * {@code AbyssFallCoreSystem#getSilently}). An access is a system or a tool deliberately
 * looking at the value, never machinery humming underneath.
 *
 * <p>Fired on whichever side the read happened, with that side's player object. A
 * listener that wants the owning client to react should check
 * {@code instanceof ServerPlayer} and send a packet; one reacting locally should check
 * the side it cares about.
 */
@FunctionalInterface
public interface SanAccessedCallback {
	Event<SanAccessedCallback> EVENT = EventFactory.createArrayBacked(SanAccessedCallback.class,
			callbacks -> player -> {
				for (SanAccessedCallback callback : callbacks) {
					callback.onSanAccessed(player);
				}
			});

	void onSanAccessed(Player player);
}

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

package com.abyssfall.client.hud;

import net.minecraft.util.Util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.abyssfall.core.AbyssFallCoreSystem;
import com.abyssfall.core.SanAccessedCallback;

/**
 * The client clock behind the San HUD's access reveal: whenever the San system is read
 * through the core, the HUD shows itself for three seconds, then fades back out along
 * the ordinary fade. Writes need no such window — the HUD already surfaces value changes
 * through its own visibility rules.
 *
 * <h2>Two feeding paths, one clock</h2>
 *
 * <p>Server-side reads arrive as the core's access packet (throttled there, so a
 * per-tick poller cannot spam the wire). Reads that happened on this very client poke the
 * clock directly through {@link SanAccessedCallback}. Both land on the same timestamp, and
 * the readouts' {@code alphaFor} takes it into the same {@code Math.max} as the
 * mode-switch reveal — during the window the row is held fully visible, and the fade is
 * measured from the window's end.
 *
 * <h2>What never pokes</h2>
 *
 * <p>The HUD's own mirror read, by design: it goes through
 * {@code AbyssFallCoreSystem#getSilently}, so the display cannot hold itself open forever.
 * The potion effects read silently too, and refused erosion announces nothing.
 */
public final class SanHudAccessPulse {
	/**
	 * How long the HUD is held fully visible after an access: three seconds, after which
	 * the ordinary fade runs.
	 */
	public static final long REVEAL_MILLIS = 3000L;

	private static long pokedAt;

	private SanHudAccessPulse() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(AbyssFallCoreSystem.AccessedPayload.TYPE,
				(payload, context) -> context.client().execute(SanHudAccessPulse::poke));

		// Reads that happen on this client poke directly; server-side ones arrive by packet.
		SanAccessedCallback.EVENT.register(player -> {
			if (player.level().isClientSide()) {
				poke();
			}
		});

		// The next world's sky owes nothing to this one's accesses.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pokedAt = 0L);
	}

	/**
	 * The moment the current reveal stops holding the HUD open, from {@link Util#getMillis()},
	 * or zero if no access has been seen yet. Consumed exactly like
	 * {@code SanHudModeState#revealEndsAt()}.
	 */
	public static long endsAt() {
		return pokedAt == 0L ? 0L : pokedAt + REVEAL_MILLIS;
	}

	private static void poke() {
		pokedAt = Util.getMillis();
	}
}

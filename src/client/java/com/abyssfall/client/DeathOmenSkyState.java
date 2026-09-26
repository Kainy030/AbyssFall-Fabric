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

package com.abyssfall.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.abyssfall.item.FinalDeathOmenSky;

/**
 * The client half of the Final Death Omen's sky darkening: the server's darkness count on
 * one side, and the darkness this client is actually showing on the other, eased toward
 * whatever the count commands once per client tick. See {@link FinalDeathOmenSky} for the
 * mechanic itself.
 *
 * <h2>Two variables, like the San pair</h2>
 *
 * <p>{@code count} is received truth — written only by the payload receiver. {@code
 * darkness} is presentation: 0.0 while no blade is drawn, a constant 1.0 — a Wither's own
 * gloom — while one or two are, and beyond two a linear climb reaching
 * {@code FULL_DARKNESS} at {@code FULL_DARKNESS_COUNT} wielders, eased rather than snapped
 * to, so the gloom deepens and lifts smoothly. Neither is an enumerated stage: every count
 * is legal, and every darkness in between is visited.
 *
 * <h2>What the depths look like</h2>
 *
 * <p>The lightmap shader mixes the world toward a reddish dusk
 * ({@code color * vec3(0.7, 0.6, 0.6)}) by this factor without clamping the factor itself,
 * so the factor is a true depth: 1.0 (one or two blades) is a Wither's own gloom, and 2.3
 * (five blades) is an oppressive dark red with greens and blues almost gone — heavy, but
 * short of the true black the same arithmetic would reach around 3.3. The heaviness is the
 * shader's own arithmetic, not a new effect.
 *
 * <h2>The easing rates are vanilla's own</h2>
 *
 * <p>Rise at 0.05 per tick, fall at 0.0125 — the exact speeds {@code GameRenderer.tick}
 * uses for the boss-overlay darkening this effect rides on. A lone blade gathers in about
 * a second, like a Wither's arrival, and even the deepest sky comes back at a Wither's
 * pace.
 *
 * <h2>Why bare statics</h2>
 *
 * <p>One writer for {@code count} (the receiver, marshalled onto the client thread), one
 * writer for {@code darkness} ({@code GameRendererMixin}, on the same thread), and neither
 * value means anything past the connection it arrived on — both are dropped on disconnect.
 */
public final class DeathOmenSkyState {
	/**
	 * {@code GameRenderer.tick}'s own boss-gloom ramp speeds — see the class comment.
	 */
	private static final float RISE_PER_TICK = 0.05F;
	private static final float FALL_PER_TICK = 0.0125F;

	private static volatile int count;
	private static float darkness;

	private DeathOmenSkyState() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(FinalDeathOmenSky.Payload.TYPE,
				(payload, context) -> context.client().execute(() -> count = payload.count()));

		// The sky of the next world owes nothing to this one; the next server's join packet
		// will say what is true there.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			count = 0;
			darkness = 0.0F;
		});
	}

	/**
	 * Eases the shown darkness one client tick toward what the count commands, and returns
	 * it. Called exactly once per client tick from {@code GameRendererMixin} inside the
	 * tick-rate-gated block, so the easing freezes with the game exactly as vanilla's own
	 * ramp does.
	 */
	public static float tickDarkness() {
		float target;

		if (count <= 0) {
			target = 0.0F;
		} else if (count <= FinalDeathOmenSky.CONSTANT_DARKNESS_COUNT) {
			target = FinalDeathOmenSky.CONSTANT_DARKNESS;
		} else {
			// The climb out of the constant zone: an equal share of the remaining depth
			// (FULL - CONSTANT) for each wielder past the constant count, so the curve is
			// continuous — the third blade picks up exactly where the plateau ends.
			target = FinalDeathOmenSky.CONSTANT_DARKNESS
					+ (Math.min(count, FinalDeathOmenSky.FULL_DARKNESS_COUNT)
							- FinalDeathOmenSky.CONSTANT_DARKNESS_COUNT)
					* (FinalDeathOmenSky.FULL_DARKNESS - FinalDeathOmenSky.CONSTANT_DARKNESS)
					/ (FinalDeathOmenSky.FULL_DARKNESS_COUNT - FinalDeathOmenSky.CONSTANT_DARKNESS_COUNT);
		}

		if (target > darkness) {
			darkness = Math.min(target, darkness + RISE_PER_TICK);
		} else {
			darkness = Math.max(target, darkness - FALL_PER_TICK);
		}

		return darkness;
	}
}

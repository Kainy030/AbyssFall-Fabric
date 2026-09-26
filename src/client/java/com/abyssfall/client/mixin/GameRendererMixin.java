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

package com.abyssfall.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ScreenEffectRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.abyssfall.client.DeathOmenSkyState;

/**
 * Hands the Final Death Omen's darkness to the same pipeline that renders a Wither's gloom.
 *
 * <h2>Why the field, not the boss-overlay question</h2>
 *
 * <p>Vanilla's boss-sky darkening is a single float, {@code bossOverlayWorldDarkening},
 * ramped inside {@code GameRenderer.tick} from a boolean question
 * ({@code BossHealthOverlay#shouldDarkenScreen}) and read back through a per-frame lerp
 * into the lightmap that shades the whole world. A boolean can only ever ask for "fully
 * dark", but the Omen's darkness is a <em>depth</em> — one or two drawn blades hold the
 * gloom at a Wither's own (1.0), and five bring the deepest dark (2.3, an oppressive red
 * dusk) — so answering the question cannot express the mechanic; the value itself is the
 * only place the depth can live. No Fabric event reaches it.
 *
 * <p>The injection lands immediately after vanilla's own update of the field, anchored on
 * the {@link ScreenEffectRenderer#tick} call that closes the same {@code runsNormally}
 * block — so the Omen freezes with the game exactly as the Wither does. The write only ever
 * raises the field toward the Omen's current darkness and never lowers what a real boss
 * event asked for; when no blade is drawn the Omen's darkness is zero and vanilla's number
 * is left exactly as it was. And because the write reuses vanilla's own field, the
 * old-value lerp, the lightmap upload and every other downstream behaviour stay vanilla's —
 * the effect is the boss gloom, merely deeper or shallower, and depths past vanilla's own
 * 1.0 are the lightmap shader's natural extrapolation of the same mix, not a new effect
 * bolted on.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	private float bossOverlayWorldDarkening;

	@Inject(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;tick()V",
			shift = At.Shift.AFTER))
	private void abyssfall$deathOmenDarkensSky(CallbackInfo ci) {
		float darkness = DeathOmenSkyState.tickDarkness();

		if (darkness > this.bossOverlayWorldDarkening) {
			this.bossOverlayWorldDarkening = darkness;
		}
	}
}

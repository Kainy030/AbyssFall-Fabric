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

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.impl.client.rendering.hud.HudElementRegistryImpl;
import net.fabricmc.fabric.impl.client.rendering.hud.HudLayer;
import net.fabricmc.fabric.impl.client.rendering.hud.HudStatusBarHeightRegistryImpl;

import com.abyssfall.AbyssFall;
import com.abyssfall.client.hud.AbyssFallSanHud;

/**
 * Keeps the San bar immediately above the hunger row, whatever else is attached there.
 *
 * <h2>⚠ This mixin targets Fabric API internals, not public API</h2>
 *
 * <p>{@link HudStatusBarHeightRegistryImpl}, {@link HudElementRegistryImpl} and
 * {@link HudLayer} all live in {@code impl} packages and carry no compatibility promise. This
 * class has been verified against <strong>Minecraft 26.2 with fabric-rendering-v1
 * 25.3.2+515ac5339e</strong> and nothing else. <strong>Any Minecraft or Fabric API upgrade must
 * re-verify it</strong>: a rename of {@code init}, a change to {@code ROOT_ELEMENTS}, or a
 * {@code layers()} list that is no longer mutable would each break it. The mixin config sets
 * {@code injectors.defaultRequire = 1}, so a missing target fails the launch loudly instead of
 * silently doing nothing — which is the intended outcome, because the alternative is a bar that
 * quietly drifts to the wrong place.
 *
 * <h2>Why a mixin is unavoidable here</h2>
 *
 * <p>Vertical order among status bars comes from the order of the layer list, and that list is
 * read exactly once — when {@code HudStatusBarHeightRegistryImpl.init()} resolves every height
 * provider and freezes the result into an immutable map. Layers are appended as each mod
 * registers during its own client initialiser, and every mod that attaches to the food bar lands
 * beside this one, with the last to register ending up closest to it. The public API offers no
 * way to say "put me last, after everyone else has had their turn": there is no priority
 * argument, and registering from a later lifecycle event is not possible because the registry is
 * frozen by then.
 *
 * <p>Injecting at the head of {@code init()} is the smallest thing that works. It is the one
 * moment that is guaranteed to be after every mod's registration and before the order is read,
 * so a single reordering there settles the layout for the whole session.
 *
 * <h2>Why this reorders the list rather than the per-frame draw</h2>
 *
 * <p>Draw order and vertical position are separate concerns in this API. Reordering the render
 * loop would only change which bar is painted first — invisible, since the bars do not overlap —
 * and would leave the vertical positions exactly as they were, because those were computed from
 * the list ahead of time. Reordering the list once, before it is read, is both cheaper than
 * touching anything per frame and the only version that actually moves the bar.
 */
@Mixin(HudStatusBarHeightRegistryImpl.class)
public class HudStatusBarHeightRegistryImplMixin {
	@Inject(method = "init", at = @At("HEAD"))
	private static void abyssfall$placeSanBarClosestToHunger(CallbackInfo info) {
		HudElementRegistryImpl.RootLayer root =
				HudElementRegistryImpl.getRoot(VanillaHudElements.FOOD_BAR);

		if (root == null) {
			AbyssFall.LOGGER.warn(
					"No food bar hud root; leaving the San bar wherever it was attached");
			return;
		}

		List<HudLayer> layers = root.layers();
		int current = indexOf(layers, AbyssFallSanHud.SAN_BAR_ID);

		if (current < 0) {
			// Not an error worth shouting about: the bar simply was not registered, which is a
			// legitimate state on a dedicated server's client-less classpath.
			return;
		}

		int vanilla = indexOf(layers, VanillaHudElements.FOOD_BAR);

		if (vanilla < 0) {
			AbyssFall.LOGGER.warn(
					"Food bar layer missing from its own root; leaving the San bar in place");
			return;
		}

		// Directly after the vanilla hunger layer is the closest a bar attached to this element
		// can sit, so anything else attached here ends up above us rather than between us and
		// hunger.
		if (current == vanilla + 1) {
			return;
		}

		HudLayer sanBar = layers.remove(current);

		// Recomputed after the removal: had the bar been sitting below the hunger layer, taking
		// it out would have shifted that layer down by one and a target worked out beforehand
		// would now be off by one.
		int target = indexOf(layers, VanillaHudElements.FOOD_BAR) + 1;

		layers.add(target, sanBar);
		AbyssFall.LOGGER.debug("Moved the San bar to sit directly above the hunger row");
	}

	private static int indexOf(List<HudLayer> layers, Identifier id) {
		for (int i = 0; i < layers.size(); i++) {
			if (layers.get(i).id().equals(id)) {
				return i;
			}
		}

		return -1;
	}
}

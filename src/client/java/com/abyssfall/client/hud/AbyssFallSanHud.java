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

import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import com.abyssfall.AbyssFall;

/**
 * Registers the San bar into the HUD, directly above the hunger row.
 *
 * <h2>Why the position is registered rather than measured</h2>
 *
 * <p>Nothing here computes a pixel offset. The element is attached after the vanilla food bar
 * and a height provider is registered alongside it, which lets the game lay the status bars out:
 * the air bubbles, the held item name and the action bar text all shift up by exactly the space
 * this bar claims, and other mods' bars take part in the same sum. Hardcoding a Y coordinate
 * would have put the bar on top of whatever else happened to be there.
 *
 * <p>Attaching <em>after</em> {@link VanillaHudElements#FOOD_BAR} rather than before is what
 * places the bar above the hunger row while leaving the hunger row itself where it was.
 * Attaching before would have taken over the hunger row's own line and pushed hunger upward.
 *
 * <p>Two further things come free from attaching to a vanilla element: the bar inherits that
 * element's render condition, so it disappears with the rest of the HUD when the player hides it,
 * and it inherits the right side of the screen for layout purposes, which is the side hunger is
 * on.
 *
 * <h2>Ordering against other mods</h2>
 *
 * <p>Being <em>immediately</em> above hunger cannot be arranged here, only nearby: every mod that
 * attaches to the same element lands next to it, and the last one to register ends up closest.
 * That is settled in {@code HudStatusBarHeightRegistryImplMixin}, which reorders the layers once
 * every mod has had its turn. This class stays a plain, well-behaved API consumer.
 */
public final class AbyssFallSanHud {
	/**
	 * Identifier of the San bar element. Registered in two places that must agree — the element
	 * registry and the status bar height registry — and used again to look the resulting offset
	 * back up while rendering.
	 */
	public static final Identifier SAN_BAR_ID = AbyssFall.id("san_bar");

	private AbyssFallSanHud() {
	}

	public static void initialize() {
		SanBarHudElement element = new SanBarHudElement();

		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, SAN_BAR_ID, element);

		// Zero whenever the bar is not drawn, so a player at full San costs the layout nothing:
		// the air bubbles and the text above the hotbar stay exactly where vanilla puts them
		// until San actually drops. The element is asked rather than the config, because it is
		// the element that knows whether it is mid-fade.
		HudStatusBarHeightRegistry.addRight(SAN_BAR_ID, player -> element.occupiedHeight());

		AbyssFall.LOGGER.debug("San bar registered above the hunger row");
	}
}

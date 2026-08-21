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

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import com.abyssfall.core.SanHudMode;
import com.abyssfall.core.SanHudModeState;

/**
 * The single HUD element the mod registers, which draws whichever readout is currently selected.
 *
 * <h2>Why one element rather than two</h2>
 *
 * <p>Both HUD registries freeze once the client has finished starting, so the number of registered
 * elements is fixed before a player can ever press the lens. Registering both readouts and hiding
 * one would also mean two entries in the status bar layout, each claiming its own height and each
 * having to know to claim zero while the other is showing — a second place for the two to disagree.
 * Registering one element that forwards keeps the layout arithmetic honest: exactly one row exists,
 * and it is exactly as tall as whatever is being drawn in it.
 *
 * <h2>Why both delegates are kept alive</h2>
 *
 * <p>They are constructed once and held, not created on demand. Each carries the animation state
 * that makes it feel responsive — the shudder on a loss, the pulse on a gain, the fade when San
 * returns to full — and rebuilding one on every switch would throw that away, so a player toggling
 * back would see the row snap to a cold start. Holding both means switching is instant and each
 * readout resumes exactly where it was.
 */
public final class SanHudDispatchElement implements HudElement {
	private final SanIconHudElement icons = new SanIconHudElement();

	private final SanBarHudElement bar = new SanBarHudElement();

	@Override
	public void render(GuiGraphics context, DeltaTracker tickCounter) {
		if (SanHudModeState.get() == SanHudMode.PERCENT) {
			this.bar.render(context, tickCounter);
		} else {
			this.icons.render(context, tickCounter);
		}
	}

	/**
	 * The vertical space the row is claiming right now, taken from whichever readout is showing.
	 *
	 * <p>Only the active one is asked. Asking both and taking the larger would reserve space for a
	 * row that is not being drawn, and both are free of side effects so asking is safe either way.
	 */
	public int occupiedHeight() {
		return SanHudModeState.get() == SanHudMode.PERCENT
				? this.bar.occupiedHeight()
				: this.icons.occupiedHeight();
	}
}

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

import net.fabricmc.api.ClientModInitializer;

import com.abyssfall.client.hud.AbyssFallSanHud;
import com.abyssfall.client.render.ShaderLayerModelPlugin;
import com.abyssfall.client.tooltip.AbyssFallTooltips;

public class AbyssFallClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Must happen during client initialisation: both HUD registries freeze once the client
		// has finished starting, so there is no later opportunity to register.
		AbyssFallSanHud.initialize();

		AbyssFallTooltips.initialize();

		ShaderLayerModelPlugin.initialize();
	}
}

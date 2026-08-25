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

package com.abyssfall.shadercore;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Supplies the effects stated in {@code AbyssFallShader.json}.
 *
 * <p>The file is not privileged: it participates as a provider like anything else, and as the first one
 * registered it is also the lowest priority. A provider reacting to the state of the game therefore
 * overrides a stated preference without either knowing about the other.
 *
 * <p>Stateless and free of allocation: the lookup returns the very record the file was parsed into, so
 * repeated frames hand back the same value and the pipeline cache keyed on it keeps hitting.
 */
final class ShaderConfigProvider implements ShaderEffectProvider {
	static final ShaderConfigProvider INSTANCE = new ShaderConfigProvider();

	private ShaderConfigProvider() {
	}

	@Override
	public @Nullable ShaderEffect effectFor(final ItemStack stack, final ShaderRenderContext context) {
		return AbyssFallShaderConfig.get().get(context.itemId());
	}
}

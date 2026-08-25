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
 * Decides what, if anything, is drawn over an item stack at this moment.
 *
 * <h2>Why a decision per frame and not a fixed assignment</h2>
 *
 * <p>The configuration file answers "which items have an appearance". It cannot answer "what does this
 * item look like <em>right now</em>", and that is the question the mod is ultimately built around: as a
 * player's San falls, ordinary things are meant to start looking wrong, and which things and how wrong
 * are properties of the moment rather than of the item.
 *
 * <p>A provider is therefore asked on every frame that draws the item, and may return a different
 * effect each time — or none. The configured effects are themselves supplied by one such provider, so
 * the file is not a special case in the machinery but the lowest-priority participant in it.
 *
 * <h2>What a provider must not do</h2>
 *
 * <p>This runs during rendering, once per item drawn. It must be cheap, must not allocate per call
 * where it can avoid it, and must not touch the server. Returning one of a small number of cached
 * effect values is the intended shape; building a fresh record every frame would defeat the pipeline
 * cache, which is keyed on the effect.
 */
@FunctionalInterface
public interface ShaderEffectProvider {
	/**
	 * The effect to draw over {@code stack}, or {@code null} to leave the decision to lower-priority
	 * providers.
	 *
	 * @param stack the stack about to be drawn
	 * @param context where it is being drawn, in case an effect should only appear in some places
	 */
	@Nullable ShaderEffect effectFor(ItemStack stack, ShaderRenderContext context);
}

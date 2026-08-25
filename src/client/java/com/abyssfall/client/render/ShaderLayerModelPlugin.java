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

package com.abyssfall.client.render;

import net.minecraft.client.resources.model.cuboid.ItemTransforms;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

import com.abyssfall.AbyssFall;
import com.abyssfall.shadercore.AbyssFallShaderCore;

/**
 * Installs the shader layer on every item, leaving the decision of what to draw to the core.
 *
 * <h2>🔴 Why every item and not only the configured ones</h2>
 *
 * <p>Filtering here would fix the answer at bake time, and the point of the system is that the answer
 * changes: a provider reacting to San may decide at any moment that an ordinary stone pickaxe should look
 * wrong, and an item that was never in the configuration file would have no wrapper to draw through.
 *
 * <p>The wrapper is therefore unconditional and cheap: when nothing claims an item, {@code update} adds no
 * layer and the item renders exactly as it would have. The cost is one delegating call and one map lookup
 * per item drawn.
 *
 * <p>If nothing could ever claim anything — no providers at all — the wrapper is not installed, because then
 * the cost buys nothing.
 *
 * <h2>Why after baking rather than before</h2>
 *
 * <p>What gets wrapped is the finished {@code ItemModel} — the item complete with its texture and
 * transforms. Modifying it earlier would mean rebuilding that work; modifying it here means inheriting it
 * and adding a layer.
 *
 * <h2>Where the display transforms come from</h2>
 *
 * <p>The wrapper needs the item's own rotation, translation and scale so its layer can be posed the same way
 * the item is; see {@link ShaderLayerItemModel} for what goes wrong otherwise. They are read from the model
 * the item resolved to, through {@code ResolvedModel.getTopTransforms}, which walks the parent chain exactly
 * as vanilla does — so a {@code handheld} item inherits {@code handheld}'s values without this class knowing
 * what {@code handheld} is.
 */
public final class ShaderLayerModelPlugin {
	private ShaderLayerModelPlugin() {
	}

	/**
	 * Registers the model modifier. Call during client initialisation, after the core is initialised.
	 */
	public static void initialize() {
		if (!AbyssFallShaderCore.hasAnyProvider()) {
			AbyssFall.LOGGER.info("No shader effect providers; not installing the shader layer");
			return;
		}

		ModelLoadingPlugin.register(context -> context.modifyItemModelAfterBake().register((model, modelContext) ->
				new ShaderLayerItemModel(model, transformsFor(modelContext), modelContext.itemId())));

		AbyssFall.LOGGER.debug("Shader layer installed on all item models");
	}

	/**
	 * The display transforms of the model this item resolved to.
	 *
	 * <p>Derived from the item id by vanilla's own convention — {@code namespace:item/path} — rather than
	 * configured, because that is the model an item's client entry points at unless it was written to do
	 * otherwise. An item that does point elsewhere still gets a layer; only the posing falls back, and
	 * {@code NO_TRANSFORMS} is what a model without a {@code display} block would have given anyway.
	 *
	 * <p>Not logged on failure: this now runs for every item in the game, and an absent model is ordinary
	 * rather than notable.
	 */
	private static ItemTransforms transformsFor(final ModelModifier.AfterBakeItem.Context context) {
		try {
			return context.bakingContext()
					.blockModelBaker()
					.getModel(context.itemId().withPrefix("item/"))
					.getTopTransforms();
		} catch (RuntimeException exception) {
			return ItemTransforms.NO_TRANSFORMS;
		}
	}
}

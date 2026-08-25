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

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.abyssfall.shadercore.AbyssFallShaderCore;
import com.abyssfall.shadercore.ShaderEffect;
import com.abyssfall.shadercore.ShaderRenderContext;

/**
 * Wraps an item's ordinary model and adds one shader-drawn layer on top of it.
 *
 * <h2>🔴 The effect is chosen per frame, not fixed at bake time</h2>
 *
 * <p>This is what makes the system able to react. {@code update} runs every time the item is drawn, so it
 * asks {@link AbyssFallShaderCore} each time rather than holding an effect decided when the model was
 * baked. An item may therefore look different from one frame to the next — which is the whole point, since
 * the mod intends ordinary things to start looking wrong as a player's San falls.
 *
 * <p>A renderer is cached per effect rather than allocated per frame: the effects a provider returns are
 * expected to be a small set of stable values, and the special-model renderer is only a holder for one.
 *
 * <h2>Why wrap rather than replace</h2>
 *
 * <p>The item should still look like itself: its texture, its transforms, its inventory sizing all come
 * from the model vanilla baked, and there is no reason to reimplement any of it. The delegate is asked to
 * fill in the render state exactly as it always would, and only then is an extra layer appended — or not,
 * if nothing claims the item this frame.
 *
 * <h2>Why {@code setupSpecialModel} and not a quad list</h2>
 *
 * <p>Ordinary layers carry baked quads, and those are validated: an item model whose quads come from
 * outside the item or block atlas is rejected outright by
 * {@code CuboidItemModelWrapper.validateAtlasUsage}. A special model renderer is the route vanilla itself
 * uses for things that are not atlas quads — shields, tridents, chests — and it is handed a
 * {@code PoseStack} and a {@code SubmitNodeCollector}, which is precisely what submitting geometry under
 * our own render type requires. That check is not circumvented; it simply does not apply to this kind of
 * layer.
 *
 * <h2>🔴 The extra layer must be given the item's own transform</h2>
 *
 * <p>Each layer in a render state carries its own {@code ItemTransform}, and {@code submit} applies it per
 * layer. A fresh layer starts at {@code ItemTransform.NO_TRANSFORM}, so without this the item would be
 * posed by its model's display settings while the shader layer stayed unposed — visible as a quad floating
 * beside the item rather than on it, and worst in hand, where those settings rotate the most.
 *
 * <h2>The two calls that keep the GUI from freezing the first frame</h2>
 *
 * <p>Item models are cached in the GUI by an identity built from the render state. {@code setAnimated()}
 * marks this stack as one whose appearance changes over time, and {@code appendModelIdentityElement}
 * contributes something to that identity — here the effect itself, so that a stack whose effect changed
 * is not served the previous one from cache.
 */
public final class ShaderLayerItemModel implements ItemModel {
	private final ItemModel delegate;
	private final ItemTransforms transforms;
	private final Identifier itemId;

	/**
	 * One renderer per effect seen so far.
	 *
	 * <p>Keyed by value, like the pipeline cache, so the small set of effects a provider cycles through
	 * costs a handful of objects in total rather than one per frame.
	 */
	private final Map<ShaderEffect, ShaderLayerRenderer> renderers = new HashMap<>();

	public ShaderLayerItemModel(final ItemModel delegate, final ItemTransforms transforms,
			final Identifier itemId) {
		this.delegate = delegate;
		this.transforms = transforms;
		this.itemId = itemId;
	}

	@Override
	public void update(
			final ItemStackRenderState output,
			final ItemStack item,
			final ItemModelResolver resolver,
			final ItemDisplayContext displayContext,
			final @Nullable ClientLevel level,
			final @Nullable ItemOwner owner,
			final int seed) {
		// The item first, unchanged.
		this.delegate.update(output, item, resolver, displayContext, level, owner, seed);

		ShaderEffect effect = AbyssFallShaderCore.effectFor(item,
				new ShaderRenderContext(this.itemId, displayContext));

		if (effect == null) {
			// Nothing claims this item right now. The wrapper stays installed — a provider may claim it
			// on the very next frame — but this frame is exactly the ordinary item.
			return;
		}

		ItemStackRenderState.LayerRenderState layer = output.newLayer();
		layer.setItemTransform(this.transforms.getTransform(displayContext));
		layer.setupSpecialModel(
				this.renderers.computeIfAbsent(effect, ShaderLayerRenderer::new), null);

		// Marks the result as time-varying so the GUI does not serve a stale first frame forever, and
		// includes the effect in the identity so a change of effect is itself a cache miss.
		output.setAnimated();
		output.appendModelIdentityElement(effect);
	}
}

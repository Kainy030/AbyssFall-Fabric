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
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
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
import com.abyssfall.shadercore.ShaderQuad;
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
	 * The item's own geometry, converted once when the model was baked.
	 *
	 * <p>Held rather than fetched per frame because it is a property of the model: the same quads every frame
	 * until the model is rebaked, at which point this wrapper is rebuilt anyway. Fetching it per frame would
	 * also mean reading it back out of the render state, which is cleared and refilled each frame — a copy per
	 * item per frame, on the render path, for a value that never changes.
	 */
	private final List<ShaderQuad> itemGeometry;

	/**
	 * Which atlas holds this item's own texture, for effects that read it.
	 *
	 * <p>Part of what identifies a render type, since the setup binds it as a sampler.
	 */
	private final @Nullable Identifier atlas;

	/**
	 * One renderer per (effect, GUI-or-world) pair seen so far.
	 *
	 * <p>Keyed by value, like the pipeline cache, so the small set of effects a provider cycles through
	 * costs a handful of objects in total rather than one per frame.
	 *
	 * <p>The GUI-or-world distinction exists because the viewer state differs between the two: a GUI slot
	 * has no viewing angle and the field pushed to its far end, while a hand-held item follows the player's
	 * head. Without this split, the two would share a renderer and overwrite each other's viewer state,
	 * because both are set on the same object before either is submitted.
	 *
	 * <p>{@link Map#entry} serves as a cheap, comparable key without a dedicated record.
	 */
	private final Map<Map.Entry<ShaderEffect, Boolean>, ShaderLayerRenderer> renderers = new HashMap<>();

	public ShaderLayerItemModel(final ItemModel delegate, final ItemTransforms transforms,
			final Identifier itemId, final ShaderLayerModelPlugin.ItemGeometry itemGeometry) {
		this.delegate = delegate;
		this.transforms = transforms;
		this.itemId = itemId;
		this.itemGeometry = itemGeometry.quads();
		this.atlas = itemGeometry.atlas();
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

		boolean viewerRelative = isViewerRelative(displayContext);

		ShaderLayerRenderer renderer = this.renderers.computeIfAbsent(
				Map.entry(effect, viewerRelative),
				key -> new ShaderLayerRenderer(key.getKey(), this.itemGeometry, this.atlas));

		renderer.setViewerState(viewerStateFor(viewerRelative));
		layer.setupSpecialModel(renderer, null);

		// Marks the result as time-varying so the GUI does not serve a stale first frame forever, and
		// includes the effect in the identity so a change of effect is itself a cache miss.
		output.setAnimated();
		output.appendModelIdentityElement(effect);
	}

	/**
	 * Whether this display context has a meaningful relationship to where the player is looking.
	 *
	 * <p>The hand and head positions do: the item is held by someone whose facing the effect should follow.
	 * A GUI slot, an item frame and the ground do not — an item in an inventory is not being looked at from
	 * anywhere, and feeding an angle in makes every slot churn as the player turns around.
	 */
	private static boolean isViewerRelative(final ItemDisplayContext displayContext) {
		return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
				|| displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
				|| displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
				|| displayContext == ItemDisplayContext.HEAD;
	}

	/**
	 * The per-frame values for this frame: where the viewer is facing, and how far off the effect should sit.
	 *
	 * <p>Computed here rather than in the effect or the renderer because this is the only place that knows both
	 * the display context and that a client player exists. An effect stating what it wants done with a viewing
	 * angle is a different thing from an effect knowing how to find one.
	 *
	 * <h2>Why a GUI gets no angle and maximum depth</h2>
	 *
	 * <p>Pushing the depth to its far end is the trick the reference implementation used for inventories:
	 * whatever the effect draws ends up far away, so it reads as fine still detail rather than as motion.
	 *
	 * <p>Light is deliberately absent: vanilla supplies it per draw, correct for whatever is being drawn. See
	 * {@link ViewerState} and {@link ShaderLayerRenderer#submit}.
	 */
	private static ViewerState viewerStateFor(final boolean viewerRelative) {
		LocalPlayer player = Minecraft.getInstance().player;

		if (!viewerRelative || player == null) {
			// A GUI slot, an item frame, or the ground: no viewing angle, and the field pushed to its far end
			// so it reads as fine still grain rather than as churn. The camera position is the folded origin
			// too — an item in a slot is not being looked at from anywhere in the world.
			return new ViewerState(0.0F, 0.5F, 1.0F, 0.0F, 0.0F, 0.0F);
		}

		// Pitch runs -90..90 and is negated: vanilla counts downwards as positive, while an effect treating
		// this as a viewing direction wants the opposite. Biased to put level at the middle of the range.
		float pitchTurns = 0.5F - (player.getXRot() / 360.0F);

		// The camera's real world position, folded for the vertex stream. This is what a volumetric effect
		// (the abyss) needs that an infinitely-far sky does not: translation parallax. The eye position is the
		// rendered camera's, so it includes the head bob and the held-item sway — the near structures slide
		// past exactly as the head moves. 26.2 names these record-style: gameRenderer.mainCamera() and
		// camera.position() (a Vec3 with x()/y()/z()), verified against the decompiled sources.
		net.minecraft.client.Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		net.minecraft.world.phys.Vec3 eye = camera.position();
		float camX = ViewerState.foldPosition((float) eye.x());
		float camY = ViewerState.foldPosition((float) eye.y());
		float camZ = ViewerState.foldPosition((float) eye.z());

		// Depth 0.0: the field stays at world scale. Only the GUI pushes it away.
		return new ViewerState(ViewerState.degreesToTurns(player.getYRot()), pitchTurns, 0.0F,
				camX, camY, camZ);
	}
}

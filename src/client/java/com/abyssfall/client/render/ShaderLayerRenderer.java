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

import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.abyssfall.client.shader.AbyssFallPipelines;
import com.abyssfall.shadercore.ShaderEffect;
import com.abyssfall.shadercore.ShaderQuad;
import com.abyssfall.shadercore.ShaderVertex;

/**
 * Draws an effect's geometry under the render type built for that effect.
 *
 * <h2>The shape is not decided here</h2>
 *
 * <p>This class writes vertices; it does not choose where they are. The quads arrive already resolved by the
 * effect's own {@code ShaderGeometrySource}, so nothing here knows whether they trace an item's hull, a plane,
 * or something a future effect invents. That separation is the point: the first version of this renderer had a
 * flat quad and a {@code Z_PLANE} constant written into it, which meant the shape of every effect was a
 * property of the drawing code and could not be varied without editing it.
 *
 * <h2>Why the geometry is resolved once and held</h2>
 *
 * <p>Geometry follows the model, and the model changes only when it is rebaked. Resolving per frame would
 * repeat identical work sixty times a second and allocate a list each time, on the render path, for every item
 * on screen. So a renderer is built per (effect, model) pair and keeps its answer.
 *
 * <h2>Vertex layout is not free-form</h2>
 *
 * <p>The pipeline binds {@code DefaultVertexFormat.ENTITY}, whose six attributes must all be written in order
 * — position, colour, UV0, UV1, UV2, normal. The sequence below mirrors what vanilla's own
 * {@code submitCustomGeometry} callers do; leaving one out shifts every attribute after it.
 *
 * <p>None of them carries what its name suggests any more: see the {@code vertex} method for what each one
 * actually holds and why it ended up there.
 */
public final class ShaderLayerRenderer implements NoDataSpecialModelRenderer {
	/**
	 * Scale for packing a {@code 0..1} coordinate into a signed 16-bit attribute.
	 *
	 * <p>The signed 16-bit maximum. Shared with the shader, which divides by the same number — if one changes,
	 * both must.
	 */
	private static final int FIXED_POINT_SCALE = 32767;

	private final ShaderEffect effect;

	/**
	 * The per-frame values written into the colour channel, replaced each frame before submitting.
	 *
	 * <p>Mutable state on a renderer looks wrong and is deliberate. {@code NoDataSpecialModelRenderer#submit}
	 * takes no argument this could arrive through — the interface is fixed by vanilla — and the values are
	 * needed at vertex-writing time, several calls below where they are known. The alternative is a renderer
	 * allocated per frame per item, on the render path.
	 *
	 * <p>Safe because a renderer is only ever used from the render thread, and because the write and the read
	 * are one frame apart in the same call chain: {@code ShaderLayerItemModel#update} sets it, then submits.
	 */
	private ViewerState viewerState = ViewerState.IDENTITY;

	/**
	 * The surface to draw, resolved once at construction.
	 *
	 * <p>Empty is a legitimate result — a source may decline — and is handled by drawing nothing rather than
	 * by refusing to exist, so that an effect declining on one model does not complicate the caller.
	 */
	private final List<ShaderQuad> geometry;

	/**
	 * The atlas holding the item's own texture, bound so an effect can read what it is covering.
	 *
	 * <p>{@code null} when the item baked no geometry, in which case {@link #geometry} is empty too and nothing
	 * is ever submitted — so the render type is never asked for.
	 */
	private final @Nullable Identifier atlas;

	public ShaderLayerRenderer(final ShaderEffect effect, final List<ShaderQuad> itemGeometry,
			final @Nullable Identifier atlas) {
		this.effect = effect;
		this.geometry = atlas == null ? List.of() : effect.geometry().resolve(itemGeometry);
		this.atlas = atlas;
	}

	/**
	 * Sets the values to write into the colour channel on the next submission.
	 *
	 * <p>Called every frame, immediately before the layer is submitted. See {@link #viewerState} for why this is
	 * a setter rather than a parameter.
	 */
	public void setViewerState(final ViewerState state) {
		this.viewerState = state.clamped();
	}

	@Override
	public void submit(
			final PoseStack poseStack,
			final SubmitNodeCollector submitNodeCollector,
			final int lightCoords,
			final int overlayCoords,
			final boolean hasFoil,
			final int outlineColor) {
		if (this.geometry.isEmpty() || this.atlas == null) {
			return;
		}

		// Captured now rather than read inside the callback: the callback may run after this method returns, by
		// which time the field could belong to a later frame.
		ViewerState state = this.viewerState;

		// 🔴 The light comes from vanilla's own argument, not from the local player.
		//
		// This is the light at whatever is being drawn, computed exactly as it was for the item's ordinary
		// layers — correct for a dropped item, an item in a frame, or another player's weapon, none of which
		// are where the local player is standing. An earlier version read the local player's position instead,
		// which was right only for the one case where the two happen to coincide.
		int blockLight = LightCoordsUtil.block(lightCoords);
		int skyLight = LightCoordsUtil.sky(lightCoords);

		// Order 1 puts this after the item's ordinary layers, which are submitted at order 0.
		submitNodeCollector.order(1)
				.submitCustomGeometry(poseStack,
						AbyssFallPipelines.forEffect(this.effect, this.atlas), (pose, buffer) -> {
					for (ShaderQuad quad : this.geometry) {
						emit(buffer, pose, quad, state, blockLight, skyLight);
					}
				});
	}

	private static void emit(final VertexConsumer buffer, final PoseStack.Pose pose, final ShaderQuad quad,
			final ViewerState state, final int blockLight, final int skyLight) {
		vertex(buffer, pose, quad.vertex0(), quad, state, blockLight, skyLight);
		vertex(buffer, pose, quad.vertex1(), quad, state, blockLight, skyLight);
		vertex(buffer, pose, quad.vertex2(), quad, state, blockLight, skyLight);
		vertex(buffer, pose, quad.vertex3(), quad, state, blockLight, skyLight);
	}

	/**
	 * 🔴 Which vertex attribute carries what, and why each landed where it did.
	 *
	 * <p>{@code DefaultVertexFormat.ENTITY} has one floating-point coordinate pair — {@code UV0} — and two
	 * sixteen-bit integer pairs, {@code UV1} and {@code UV2}, meant for overlay and lightmap. Both integer pairs
	 * were already spoken for before this class touched them:
	 *
	 * <ul>
	 *   <li><strong>{@code UV0}</strong> carries the atlas coordinate, because that one must be exact. It
	 *       addresses a sprite packed among hundreds of others, and being off by a fraction reads a
	 *       neighbouring item's pixels.</li>
	 *   <li><strong>{@code UV1}</strong> carries the mask coordinate, quantised to 16-bit. Verified as
	 *       harmless: at {@code 1/32767} per step, the worst round-trip error is 0.00024 of a pixel even on a
	 *       16-pixel mask, and a 128-pixel mask still gets 256 steps per pixel.</li>
	 *   <li><strong>{@code UV2}</strong> carries the viewer's yaw and pitch, each quantised to 16-bit.</li>
	 * </ul>
	 *
	 * <p>🔴 <strong>The viewing angles were originally packed into the colour channel's bytes — one per angle —
	 * and that was too coarse.</strong> A byte gives 256 steps per full turn, which is 1.4° per step. That is
	 * invisible for a value that changes slowly, and catastrophic for one that tracks the player's turning: the
	 * starfield's sphere is driven directly by the yaw, so every 1.4° of head movement made every star jump one
	 * cell at once, reading as teleporting rather than gliding. UV2's sixteen bits give 32767 steps per turn —
	 * 0.011° per step, far finer than any mouse can produce.
	 *
	 * <p>{@code UV2} was free to take for the same reason the mask's UV1 was: this layer never used the
	 * lightmap, and the shader never read it. It was always written as {@code FULL_BRIGHT} by a predecessor of
	 * this class, and nothing sampled it. Same observation as the colour channel — a declared attribute that
	 * nobody uses is the natural home for a value with nowhere else to go.
	 *
	 * <p>{@code setUv1} is used directly rather than {@code setOverlay}, which packs a single int into the two
	 * halves and would not let the two coordinates be set independently. {@code setUv2} is the same shape and
	 * lets yaw and pitch be set independently.
	 */
	private static void vertex(
			final VertexConsumer buffer,
			final PoseStack.Pose pose,
			final ShaderVertex vertex,
			final ShaderQuad quad,
			final ViewerState state,
			final int blockLight,
			final int skyLight) {
		buffer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
				// Depth stays in the colour channel's red byte; the block and sky light levels sit in the
				// green and blue bytes; yaw and pitch moved to UV2. See the javadoc below for why the
				// viewing angles outgrew a byte.
				.setColor(toByte(state.depth()), toByte(blockLight / 15.0F),
						toByte(skyLight / 15.0F), 255)
				.setUv(vertex.atlasU(), vertex.atlasV())
				.setUv1(toFixedPoint(vertex.maskU()), toFixedPoint(vertex.maskV()))
				.setUv2(toFixedPoint(state.yawTurns()), toFixedPoint(state.pitchTurns()))
				.setNormal(pose, quad.normalX(), quad.normalY(), quad.normalZ());
	}

	/**
	 * A {@code 0..1} value as one of the colour channel's bytes.
	 *
	 * <p>The colour channel is per-draw-call, not per-pipeline, so it takes no part in what identifies a compiled
	 * program — a value that varied through a define would compile a pipeline per value, measured at over a
	 * thousand for one item. The colour's red byte is {@code depth}, a coarse near/far selector; the green and
	 * blue bytes carry the block and sky light levels, demoted from the lightmap attribute that UV2 now carries
	 * the viewing angles.
	 *
	 * <p>{@code RGBA8_UNORM}, so 256 steps per field.
	 */
	private static int toByte(final float value) {
		return Math.clamp(Math.round(value * 255.0F), 0, 255);
	}

	/**
	 * A {@code 0..1} coordinate as a signed 16-bit integer, which the shader divides back out.
	 *
	 * <p>Clamped because a geometry source is free to return coordinates outside {@code 0..1} and a value past
	 * the signed range would wrap to a negative — reading the far side of the mask rather than its edge.
	 */
	private static int toFixedPoint(final float coordinate) {
		return Math.clamp(Math.round(coordinate * FIXED_POINT_SCALE), 0, FIXED_POINT_SCALE);
	}

	/**
	 * Reports the geometry's corners so the GUI can size and centre the item correctly.
	 *
	 * <p>Without this the item would be laid out from the extents of its ordinary model alone, and geometry
	 * reaching past that outline would be clipped in inventory slots. Since this layer now traces the item
	 * itself, the extents are very nearly the item's own — but they are reported rather than assumed, because
	 * a source is free to return something larger.
	 */
	@Override
	public void getExtents(final Consumer<Vector3fc> output) {
		for (ShaderQuad quad : this.geometry) {
			for (int i = 0; i < 4; i++) {
				ShaderVertex vertex = quad.vertex(i);

				if (vertex != null) {
					output.accept(new Vector3f(vertex.x(), vertex.y(), vertex.z()));
				}
			}
		}
	}
}

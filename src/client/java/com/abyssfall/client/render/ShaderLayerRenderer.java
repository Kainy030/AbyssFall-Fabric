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

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.abyssfall.client.shader.AbyssFallPipelines;
import com.abyssfall.shadercore.ShaderEffect;

/**
 * Draws one quad over an item, using the render type built for that item's effect.
 *
 * <h2>Why a quad and not the item's own geometry</h2>
 *
 * <p>What the shader needs is a surface to cover, not a shape to follow: the mask decides which
 * fragments end up visible, so the geometry only has to be large enough to contain the item. One flat
 * quad is the least that can do that, and it makes the effect independent of how the item is
 * modelled.
 *
 * <p>Culling is off in the pipeline, so winding order cannot hide the result.
 *
 * <h2>🔴 The coordinate space is 0..1, not -0.5..0.5</h2>
 *
 * <p>Getting this wrong puts the quad half an item away from where it belongs. Item models are
 * authored in a 0..16 grid and {@code FaceBakery} divides every baked vertex by 16, so a finished item
 * occupies <strong>0.0 to 1.0</strong> on each axis with its centre at {@code (0.5, 0.5)} — not at the
 * origin. Flat items sit at {@code z = 7.5/16} through {@code 8.5/16}, which is why {@link #Z_PLANE}
 * is just above 0.5 rather than just above 0.
 *
 * <p>What makes this counter-intuitive is that {@code ItemTransform.apply} ends with
 * {@code translate(-0.5F, -0.5F, -0.5F)} on every path, including {@code NO_TRANSFORM}. That shift is
 * what finally centres the model, meaning it is applied <em>to</em> geometry still expressed in 0..1;
 * geometry written around the origin is shifted along with it and lands off to one side.
 *
 * <h2>Vertex layout is not free-form</h2>
 *
 * <p>The pipeline binds {@code DefaultVertexFormat.ENTITY}, whose six attributes must all be written
 * in order — position, colour, UV0, overlay (UV1), light (UV2), normal. The sequence below mirrors what
 * vanilla's own {@code submitCustomGeometry} callers do; leaving one out shifts every attribute after
 * it. Light and overlay are written because the format demands them, not because they matter: the
 * shader ignores both.
 */
public final class ShaderLayerRenderer implements NoDataSpecialModelRenderer {
	/**
	 * Lower bound of the quad, in the item model's own 0..1 space.
	 *
	 * <p>Slightly outside the model so the quad reaches just past the item's silhouette, which lets a
	 * mask paint right up to the edge without the quad clipping it.
	 */
	private static final float MIN = -0.05F;

	/**
	 * Upper bound of the quad. See {@link #MIN}.
	 */
	private static final float MAX = 1.05F;

	/**
	 * Depth of the quad, just in front of a flat item's front face.
	 *
	 * <p>Flat items are baked between {@code 7.5/16} and {@code 8.5/16}, so {@code 8.5/16} plus a hair
	 * clears the front face without drifting far enough to look detached. Z-fighting against a
	 * coplanar surface would read as the effect not working at all.
	 */
	private static final float Z_PLANE = 8.5F / 16.0F + 0.002F;

	private final ShaderEffect effect;

	public ShaderLayerRenderer(final ShaderEffect effect) {
		this.effect = effect;
	}

	@Override
	public void submit(
			final PoseStack poseStack,
			final SubmitNodeCollector submitNodeCollector,
			final int lightCoords,
			final int overlayCoords,
			final boolean hasFoil,
			final int outlineColor) {
		// Order 1 puts this after the item's ordinary layers, which are submitted at order 0.
		submitNodeCollector.order(1)
				.submitCustomGeometry(poseStack, AbyssFallPipelines.forEffect(this.effect), (pose, buffer) -> {
					vertex(buffer, pose, MIN, MIN, 0.0F, 1.0F);
					vertex(buffer, pose, MAX, MIN, 1.0F, 1.0F);
					vertex(buffer, pose, MAX, MAX, 1.0F, 0.0F);
					vertex(buffer, pose, MIN, MAX, 0.0F, 0.0F);
				});
	}
	private static void vertex(
			final VertexConsumer buffer,
			final PoseStack.Pose pose,
			final float x,
			final float y,
			final float u,
			final float v) {
		buffer.addVertex(pose, x, y, Z_PLANE)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, 0.0F, 0.0F, 1.0F);
	}

	/**
	 * Reports the quad's corners so the GUI can size and centre the item correctly.
	 *
	 * <p>Without this the item would be laid out from the extents of its ordinary model alone, and a
	 * quad reaching past that outline would be clipped in inventory slots.
	 */
	@Override
	public void getExtents(final Consumer<Vector3fc> output) {
		output.accept(new Vector3f(MIN, MIN, Z_PLANE));
		output.accept(new Vector3f(MAX, MIN, Z_PLANE));
		output.accept(new Vector3f(MAX, MAX, Z_PLANE));
		output.accept(new Vector3f(MIN, MAX, Z_PLANE));
	}
}

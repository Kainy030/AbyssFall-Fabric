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

package com.abyssfall.shadercore.geometry;

import java.util.ArrayList;
import java.util.List;

import com.abyssfall.shadercore.ShaderGeometrySource;
import com.abyssfall.shadercore.ShaderQuad;

/**
 * Follows the item's own surface — every face it has, pushed out by a hair.
 *
 * <h2>Why this replaced a single flat quad</h2>
 *
 * <p>🔴 <strong>Items are not flat.</strong> Vanilla bakes a generated item as an extruded sprite: a front
 * face at {@code z = 8.5/16}, a back face at {@code z = 7.5/16}, and a wall of side faces traced pixel by
 * pixel around the sprite's silhouette. Drawing an effect on one plane in front of the front face therefore
 * covered one of those three parts. From any angle other than straight on, the item read as a flat sheet with
 * an effect floating off it — a rendering fault rather than an appearance.
 *
 * <p>Following every face fixes that by construction, and it fixes it for shapes this class knows nothing
 * about: a 3D model item, a block item, an item with several texture layers. There is no assumption here
 * about what an item looks like, which is why the old {@code Z_PLANE} constant and its "assumes a flat item"
 * caveat are gone rather than adjusted.
 *
 * <p>⚠️ <strong>Unlike {@link ItemFacesGeometry}, this does not collapse a model's texture layers.</strong>
 * That is deliberate but it is a trade, not a free choice, so it is worth stating. Each layer has its own
 * silhouette and therefore its own wall of side faces, and a source whose job is to <em>coat the whole hull</em>
 * has a real claim to coating all of them. The cost is that the front and back faces, which every layer bakes at
 * identical coordinates, are drawn once per layer — and repeated {@code SRC_ALPHA} blending over one patch of
 * screen saturates the mask's faint values while leaving its bright ones untouched. See
 * {@code ShaderQuad#UNKNOWN_LAYER} for the measured shape of that.
 *
 * <p>It has not been changed because no shipped effect currently uses this source, so the trade has never been
 * looked at on screen. An effect that coats a multi-layer item and finds its mask edges hardening should filter
 * on {@code ShaderQuad#layer()} the way {@code ItemFacesGeometry} does.
 *
 * <h2>The offset, and why it is along the normal</h2>
 *
 * <p>Each face is moved outward along its own normal rather than along a fixed axis. A fixed axis would work
 * for the front face and be wrong for every side face — the walls face sideways, and pushing them towards the
 * camera would slide them along the item instead of off it.
 *
 * <p>The distance is deliberately tiny. Coplanar surfaces z-fight, which flickers and reads as broken; too
 * large a gap and the effect visibly floats. This value clears the depth buffer's precision at the scale
 * items are drawn without being noticeable.
 *
 * <h2>UVs are kept as the item had them</h2>
 *
 * <p>A face arrives carrying the texture coordinates that read the item's own sprite, and those are passed
 * through unchanged. The consequence is that a mask is read in the same layout as the item's texture, so mask
 * artwork lines up with item artwork pixel for pixel — which is what makes a mask authorable at all. An
 * effect wanting some other mapping is a different source, not a change here.
 */
public final class ItemHullGeometry implements ShaderGeometrySource {
	/**
	 * The only instance. Stateless, and a record-like value: two references compare equal, so effects using
	 * it share one pipeline as they should.
	 */
	public static final ItemHullGeometry INSTANCE = new ItemHullGeometry();

	/**
	 * How far each face is pushed along its normal, in model space where the whole item spans {@code 0..1}.
	 *
	 * <p>Small enough to be invisible at the scale an item is drawn, large enough to beat z-fighting against
	 * the face it sits on.
	 */
	private static final float OFFSET = 0.002F;

	private ItemHullGeometry() {
	}

	@Override
	public List<ShaderQuad> resolve(final List<ShaderQuad> itemGeometry) {
		List<ShaderQuad> result = new ArrayList<>(itemGeometry.size());

		for (ShaderQuad quad : itemGeometry) {
			// Along the face's own normal, so a side wall moves sideways off the item rather than towards the
			// camera. A degenerate face has a zero normal and therefore moves nowhere, which is correct by
			// omission: a face with no area draws nothing either way.
			result.add(quad.translated(
					quad.normalX() * OFFSET,
					quad.normalY() * OFFSET,
					quad.normalZ() * OFFSET));
		}

		return List.copyOf(result);
	}

	@Override
	public boolean equals(final Object other) {
		// All instances are interchangeable, and equality by class keeps the pipeline cache sharing across
		// effects that were deserialised separately.
		return other instanceof ItemHullGeometry;
	}

	@Override
	public int hashCode() {
		return ItemHullGeometry.class.hashCode();
	}

	@Override
	public String toString() {
		return "ItemHullGeometry";
	}
}

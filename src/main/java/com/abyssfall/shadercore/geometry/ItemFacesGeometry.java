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
 * The two flat faces of an item, and nothing else.
 *
 * <h2>🔴 Why the side walls are deliberately dropped</h2>
 *
 * <p>An effect that <em>replaces what an item is made of</em> — a starfield, rather than a glow over it — wants
 * one clean read per visible pixel. Following the whole hull gives it the opposite.
 *
 * <p>Vanilla bakes a generated item as a front face, a back face, and <strong>one side face per outline
 * pixel</strong> — measured at 106 for the blade this was written for, so 108 quads in total. Each of those
 * side quads carries the texture coordinates of the single item pixel it walls, which is correct for texturing
 * it and useless for reading a mask: the mask coordinate barely moves across the quad, so the whole wall takes
 * the colour of one mask pixel.
 *
 * <p>Drawn together with the front face, and with a depth bias lifting all of them, the result is a hundred
 * overlapping surfaces each sampling a different single pixel — visible as patchy, broken coverage that follows
 * the item's shape without matching its silhouette. Exactly what was observed.
 *
 * <p>Two faces sample the mask across its whole extent, which is what the mask was authored for.
 *
 * <h2>Why the back face is kept</h2>
 *
 * <p>Held items are seen from behind about half the time. The back face's winding is reversed, so it samples the
 * mask mirrored — correct, since the item's own texture is mirrored there too.
 *
 * <p>The two never fight, because they are 1/16 apart and only one faces the viewer at a time.
 *
 * <h2>What this costs</h2>
 *
 * <p>The item's 1/16 thickness gets no effect on it. At the size items are drawn that edge is one or two screen
 * pixels; {@link ItemHullGeometry} remains for an effect that genuinely wants to coat it.
 */
public final class ItemFacesGeometry implements ShaderGeometrySource {
	/**
	 * The only instance. Stateless, and compares by class so effects deserialised separately share a pipeline.
	 */
	public static final ItemFacesGeometry INSTANCE = new ItemFacesGeometry();

	/**
	 * How nearly axis-aligned a face's normal must be to count as a front or back face.
	 *
	 * <p>The two flat faces have normals of exactly ±Z, and the side walls point sideways — ±X or ±Y — so any
	 * threshold between them works. This one is generous enough to survive a model that tilts its faces slightly
	 * and strict enough to reject every wall.
	 */
	private static final float FLAT_FACE_THRESHOLD = 0.9F;

	private ItemFacesGeometry() {
	}

	@Override
	public List<ShaderQuad> resolve(final List<ShaderQuad> itemGeometry) {
		List<ShaderQuad> result = new ArrayList<>(2);

		for (ShaderQuad quad : itemGeometry) {
			if (Math.abs(quad.normalZ()) >= FLAT_FACE_THRESHOLD) {
				result.add(quad);
			}
		}

		// A model whose faces all point sideways gets nothing rather than a wrong guess. Nothing is then drawn
		// for it, which is the honest answer for a source that means "the flat faces".
		//
		// A block item is not that case: a cube's north and south faces are Z-facing and pass this test, so a
		// block item gets those two. Whether a sky drawn on two faces of a cube is desirable is a question for
		// whoever configures it, not for this source.
		return List.copyOf(result);
	}

	/**
	 * Coplanar: these are the item's own faces, returned unmoved.
	 *
	 * <p>Nothing is offset here, so the layer shares depth exactly with the surface it was taken from and needs
	 * a depth bias to win the comparison. See {@link ShaderGeometrySource#isCoplanar()}.
	 */
	@Override
	public boolean isCoplanar() {
		return true;
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof ItemFacesGeometry;
	}

	@Override
	public int hashCode() {
		return ItemFacesGeometry.class.hashCode();
	}

	@Override
	public String toString() {
		return "ItemFacesGeometry";
	}
}
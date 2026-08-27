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

import java.util.List;

/**
 * What surface an effect is drawn onto — deliberately not fixed.
 *
 * <h2>🔴 Why this is a seam and not a constant</h2>
 *
 * <p>The first version of this system drew every effect onto a single flat quad placed just in front of
 * an item. That was wrong, and wrong in a way worth recording, because the mistake is easy to repeat:
 * <strong>no item in Minecraft is flat.</strong> Vanilla's {@code ItemModelGenerator.bakeExtrudedSprite}
 * gives every generated item a 1/16 thickness — a front face, a back face, and a per-pixel wall of side
 * faces traced around the sprite's silhouette. An effect painted on one plane therefore covered a third
 * of the item and left the rest bare, which read as a rendering fault rather than as an effect.
 *
 * <p>The fix is not to hard-code the opposite either. A starfield of the kind researched for this mod does
 * not want to hug an item's hull at all — it wants a surface to project onto, and its whole appearance comes
 * from treating each fragment as a ray. If the renderer were rewritten to always follow item geometry, that
 * effect would need the renderer rewritten again.
 *
 * <p>So the shape an effect occupies is stated by the effect, resolved through this interface, and the
 * drawing code knows only "whatever geometry this source produced". Adding a kind of surface is adding an
 * implementation here; it is not a change to the renderer, to the pipelines, or to any existing effect.
 *
 * <h2>What an implementation receives</h2>
 *
 * <p>The item's own baked geometry, as the model produced it. An implementation is free to follow it, to
 * derive something from it, or to ignore it entirely and return a shape of its own.
 *
 * <p>Resolution happens once, when the item's model is baked — not per frame. Geometry is a property of the
 * model, and a model that has been rebaked produces a fresh resolution anyway. Implementations may therefore
 * do real work here without a per-frame cost.
 *
 * <h2>Implementations must compare by value</h2>
 *
 * <p>Like {@link ShaderColorSource}, a source takes part in identifying an effect, and an effect is what
 * keys the pipeline cache. A source that compared by identity would defeat the sharing that cache exists for.
 */
public interface ShaderGeometrySource {
	/**
	 * The surface to draw this effect on, derived from the item's own geometry.
	 *
	 * @param itemGeometry the item's baked quads, in the model's own {@code 0..1} space. Never empty for a
	 *                     real item, but an implementation should not assume a particular count or ordering:
	 *                     what a model bakes to is the model's business
	 * @return the quads to draw, in the same space. An empty list means this source declines to draw,
	 *         which is a legitimate answer and leaves the item exactly as vanilla rendered it
	 */
	List<ShaderQuad> resolve(List<ShaderQuad> itemGeometry);

	/**
	 * Whether the quads this source produces sit exactly on the item's own surface.
	 *
	 * <h2>🔴 Why the source answers this and not the effect</h2>
	 *
	 * <p>This decides how the depth test is set up: coplanar geometry needs a depth bias to win the
	 * comparison against the surface it shares, while geometry already lifted off the item wants the ordinary
	 * test and would be pushed too far by a bias on top.
	 *
	 * <p>Only the source knows the answer, because only the source decides where the vertices go. Asking the
	 * effect instead — as an earlier version did — created a pair of settings that had to be kept in agreement
	 * by hand, and getting them out of step means either z-fighting or nothing drawn at all. Here it cannot go
	 * out of step: the geometry and the statement about it come from the same object.
	 *
	 * <p>Defaults to {@code false}, matching a source that offsets its output, which is the safer of the two
	 * to get wrong: an unnecessary gap is visible, an unnecessary bias is not.
	 */
	default boolean isCoplanar() {
		return false;
	}
}

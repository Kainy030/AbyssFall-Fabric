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

/**
 * One corner of a {@link ShaderQuad}: where it is, and the two texture coordinates it reads by.
 *
 * <h2>🔴 Why there are two sets of texture coordinates</h2>
 *
 * <p>A shader layer reads two different textures, and they are not addressed the same way.
 *
 * <ul>
 *   <li>{@code maskU}/{@code maskV} — the effect's mask, a standalone texture bound on its own. It is read
 *       edge to edge, so these run {@code 0..1} across the item's own artwork. This is what makes a mask
 *       paintable by tracing the item.</li>
 *   <li>{@code atlasU}/{@code atlasV} — the item's own texture, which is <strong>not</strong> standalone: it
 *       is packed into a shared atlas alongside every other item. Reading it requires the coordinate of its
 *       sprite <em>within that sheet</em>. Handing it {@code 0..1} would read the whole atlas — every item in
 *       the game at once.</li>
 * </ul>
 *
 * <p>Both are carried because an effect may need either or both: a mask-driven effect needs the first, a
 * colour derived from the item's own pixels needs the second, and the two together are what let an effect
 * recolour an item rather than paint over it.
 *
 * <p>Positions are in the item model's own space, {@code 0..1} with its centre at {@code (0.5, 0.5, 0.5)}.
 *
 * @param x      position, in model space
 * @param maskU  coordinate into the effect's mask, {@code 0..1} over the item's artwork
 * @param atlasU coordinate into the item atlas, where the item's own sprite actually lives
 */
public record ShaderVertex(float x, float y, float z,
		float maskU, float maskV,
		float atlasU, float atlasV) {
}

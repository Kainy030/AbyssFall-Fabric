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

import org.jspecify.annotations.Nullable;

/**
 * One four-cornered surface for an effect to be drawn on.
 *
 * <h2>Why the system carries its own quad type</h2>
 *
 * <p>Vanilla's {@code BakedQuad} would have done the job, but it lives in the client source set while this
 * package does not, and it carries a great deal this system has no use for — an atlas layer, a render type, a
 * tint index, a light emission. A {@link ShaderGeometrySource} deciding what shape an effect occupies has no
 * business being handed a render type it must ignore, and an effect kind defined outside this mod should be
 * able to describe a surface without reaching into Minecraft's model internals.
 *
 * <p>So the geometry that crosses this seam is reduced to what a shader layer actually needs: four corners,
 * each knowing where it is and how to read both textures. See {@link ShaderVertex} for why there are two sets
 * of texture coordinates.
 *
 * @param vertex0 corners in winding order around the face
 * @param normalX the face's outward direction, derived from the winding rather than supplied
 */
public record ShaderQuad(ShaderVertex vertex0, ShaderVertex vertex1, ShaderVertex vertex2, ShaderVertex vertex3,
		float normalX, float normalY, float normalZ) {
	/**
	 * A quad from its four corners, with the normal computed from the winding.
	 *
	 * <p>The normal is derived rather than supplied because a surface's facing is a property of its corners,
	 * and asking a caller for both invites the two to disagree.
	 */
	public static ShaderQuad of(final ShaderVertex vertex0, final ShaderVertex vertex1,
			final ShaderVertex vertex2, final ShaderVertex vertex3) {
		// Cross product of two edges leaving the first corner. Degenerate quads (a side face collapsed to a
		// line, which the per-pixel walls can produce) yield a zero-length normal; that is left as it is
		// rather than substituted, since a surface with no area has no meaningful facing and nothing reads it.
		float edgeAx = vertex1.x() - vertex0.x();
		float edgeAy = vertex1.y() - vertex0.y();
		float edgeAz = vertex1.z() - vertex0.z();
		float edgeBx = vertex3.x() - vertex0.x();
		float edgeBy = vertex3.y() - vertex0.y();
		float edgeBz = vertex3.z() - vertex0.z();

		float nx = edgeAy * edgeBz - edgeAz * edgeBy;
		float ny = edgeAz * edgeBx - edgeAx * edgeBz;
		float nz = edgeAx * edgeBy - edgeAy * edgeBx;

		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

		if (length > 0.0F) {
			nx /= length;
			ny /= length;
			nz /= length;
		}

		return new ShaderQuad(vertex0, vertex1, vertex2, vertex3, nx, ny, nz);
	}

	/**
	 * The corner at the given index, {@code 0} to {@code 3}, or {@code null} if there is no such corner.
	 *
	 * <p>Offered so a source can walk the corners without four branches of its own.
	 */
	public @Nullable ShaderVertex vertex(final int index) {
		return switch (index) {
			case 0 -> this.vertex0;
			case 1 -> this.vertex1;
			case 2 -> this.vertex2;
			case 3 -> this.vertex3;
			default -> null;
		};
	}

	/**
	 * The same quad with every corner moved by the given amount, keeping both sets of texture coordinates.
	 *
	 * <p>Offered because pushing a face off the surface it sits on is what almost every geometry source needs
	 * to do, and doing it by hand means restating twenty-eight fields.
	 */
	public ShaderQuad translated(final float dx, final float dy, final float dz) {
		return new ShaderQuad(
				moved(this.vertex0, dx, dy, dz),
				moved(this.vertex1, dx, dy, dz),
				moved(this.vertex2, dx, dy, dz),
				moved(this.vertex3, dx, dy, dz),
				this.normalX, this.normalY, this.normalZ);
	}

	private static ShaderVertex moved(final ShaderVertex vertex, final float dx, final float dy, final float dz) {
		return new ShaderVertex(vertex.x() + dx, vertex.y() + dy, vertex.z() + dz,
				vertex.maskU(), vertex.maskV(), vertex.atlasU(), vertex.atlasV());
	}
}

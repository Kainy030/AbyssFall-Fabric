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

/**
 * Where the viewer is, this frame — packed into the vertex stream.
 *
 * <h2>🔴 Why this lives here and not in {@code shadercore}</h2>
 *
 * <p>It was briefly put there, and that was wrong. A {@code ShaderEffect} answers <em>what an item looks
 * like</em>: its density, its mask, its brightness. Two items configured identically are meant to be the same
 * effect and share one compiled pipeline, which is why an effect is a value and its identity is its fields.
 *
 * <p>A viewing angle is not that. It is the same for every effect on screen, it has nothing to do with what any
 * of them <em>is</em>, and it changes every frame. Putting it on an effect would make two identical swords
 * "different effects" because the player turned their head — and since the pipeline cache is keyed on the
 * effect, that is precisely the mistake that compiles a thousand pipelines.
 *
 * <p>So it is an agreement between <strong>this renderer and the shaders it draws</strong>, and nothing above
 * it needs to know it exists. An effect that ignores the vertex stream is unaffected; one that reads it says
 * so in its own GLSL.
 *
 * <h2>🔴 What is deliberately not here: the light level</h2>
 *
 * <p>Light was once carried in this record, read from the local player's position. That was wrong for every
 * item not in the local player's hand — a dropped item, an item frame, another player's weapon — because it
 * described where the <em>viewer</em> stood rather than where the <em>item</em> is.
 *
 * <p>Vanilla already answers that question: it hands {@code lightCoords} to
 * {@code SpecialModelRenderer#submit}, correct for whatever is being drawn and computed the same way as for
 * the item's own layers. It is unpacked there rather than travelling here, because this record is about the
 * viewer and light is about the item.
 *
 * <h2>Where each field travels</h2>
 *
 * <ul>
 *   <li><strong>yaw, pitch</strong> — {@code UV2}, the sixteen-bit integer pair that used to carry the
 *       lightmap. This layer never used the lightmap and no shader read it, so the pair was free. The
 *       precision matters: a byte gave 256 steps per full turn (1.4° per step), which made every star on the
 *       sphere jump in sync while turning. Sixteen bits give 32767 steps per turn, far finer than a mouse.</li>
 *   <li><strong>depth</strong> — the colour channel's red byte. A coarse near/far selector needs nowhere near
 *       256 levels.</li>
 * </ul>
 *
 * <p>Angles are stored as turns rather than radians, {@code 0..1} being the range a fixed-point attribute can
 * carry.
 *
 * @param yawTurns   the viewer's horizontal facing, in turns, {@code 0..1}
 * @param pitchTurns the viewer's vertical facing, biased so level sits at {@code 0.5}
 * @param depth      how far away whatever this drives should appear, {@code 0..1}
 */
public record ViewerState(float yawTurns, float pitchTurns, float depth) {
	/**
	 * Level, facing along positive Z, at the nearest depth.
	 *
	 * <p>Used where a viewpoint is not available or not meaningful — an item in an inventory slot is not being
	 * looked at from anywhere.
	 */
	public static final ViewerState IDENTITY = new ViewerState(0.0F, 0.5F, 0.0F);

	/**
	 * The same values with each field clamped into what the channel can carry.
	 *
	 * <p>Applied here rather than trusted from the caller: an angle computed from a player's rotation can
	 * legitimately land outside {@code 0..1}, and the byte it is packed into wraps rather than saturates — a
	 * value just past the top would read as the bottom, which shows up as the effect snapping round once per
	 * revolution.
	 */
	public ViewerState clamped() {
		return new ViewerState(
				Math.clamp(this.yawTurns, 0.0F, 1.0F),
				Math.clamp(this.pitchTurns, 0.0F, 1.0F),
				Math.clamp(this.depth, 0.0F, 1.0F));
	}

	/**
	 * An angle in degrees expressed as turns in {@code 0..1}, wrapping rather than clamping.
	 *
	 * <p>Wrapping is right for a heading: 370° and 10° are the same direction, and clamping would flatten
	 * everything past a full circle onto one value.
	 */
	public static float degreesToTurns(final float degrees) {
		float turns = degrees / 360.0F;

		return turns - (float) Math.floor(turns);
	}
}
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
 *   <li><strong>camX, camZ</strong> — {@code UV0}, the one floating-point pair, but only for effects whose
 *       {@link com.abyssfall.shadercore.ShaderEffect#usesViewerPosition()} is true (the abyss); such a shader
 *       does not read the item's
 *       own texture, so the pair is free for them. They carry the camera's world X and Z, folded into
 *       {@code 0..1} over {@link #POSITION_PERIOD} metres. Other effects leave UV0 as the atlas coordinate and
 *       these values are ignored.</li>
 *   <li><strong>camY</strong> — the colour channel's green and blue bytes (sixteen bits combined), folded the
 *       same way. Those bytes carry block/sky light for the other shaders; the abyss shader reads neither the
 *       world's light nor depth, so the three bytes it does not use carry the position instead.</li>
 * </ul>
 *
 * <p>Angles are stored as turns rather than radians, {@code 0..1} being the range a fixed-point attribute can
 * carry. The position is folded, not clamped: see {@link #foldPosition(float)}.
 *
 * @param yawTurns   the viewer's horizontal facing, in turns, {@code 0..1}
 * @param pitchTurns the viewer's vertical facing, biased so level sits at {@code 0.5}
 * @param depth      how far away whatever this drives should appear, {@code 0..1}
 * @param camX       the camera's world X, folded to {@code 0..1} over {@link #POSITION_PERIOD} metres
 * @param camY       the camera's world Y, folded the same way
 * @param camZ       the camera's world Z, folded the same way
 */
public record ViewerState(float yawTurns, float pitchTurns, float depth,
		float camX, float camY, float camZ) {
	/**
	 * The period, in metres, over which the camera position is folded into {@code 0..1} for the vertex stream.
	 *
	 * <p>No attribute can carry an unbounded world coordinate, and the procedural volume the abyss marches is
	 * periodic anyway: a ray is walked through a lattice of cells, and the lattice only needs the camera's
	 * position modulo a large distance, because a cell far enough back is identical (under the hash) to one a
	 * period ahead. The period therefore only has to be longer than what the eye could ever match up across —
	 * a thousand metres of walking is well past that — while keeping the folded value inside what a float pair
	 * or a pair of bytes can address.
	 *
	 * <p>This is a channel-packing constant, not a property of any effect: an effect states how large a cell
	 * is and how far a ray reaches; it never needs to know the position was folded.
	 */
	public static final float POSITION_PERIOD = 1024.0F;

	/**
	 * Level, facing along positive Z, at the nearest depth, with the camera treated as standing at the folded
	 * origin.
	 *
	 * <p>Used where a viewpoint is not available or not meaningful — an item in an inventory slot is not being
	 * looked at from anywhere.
	 */
	public static final ViewerState IDENTITY = new ViewerState(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F);

	/**
	 * The same values with each field in the range the channel it travels in can carry.
	 *
	 * <p>The angles are clamped: an angle computed from a player's rotation can land outside {@code 0..1}, and
	 * the byte it is packed into wraps rather than saturates — a value just past the top would read as the
	 * bottom, which shows up as the effect snapping round once per revolution. The position is folded rather
	 * than clamped by the caller ({@link #foldPosition(float)}), so it already sits in range here.
	 */
	public ViewerState clamped() {
		return new ViewerState(
				Math.clamp(this.yawTurns, 0.0F, 1.0F),
				Math.clamp(this.pitchTurns, 0.0F, 1.0F),
				Math.clamp(this.depth, 0.0F, 1.0F),
				Math.clamp(this.camX, 0.0F, 1.0F),
				Math.clamp(this.camY, 0.0F, 1.0F),
				Math.clamp(this.camZ, 0.0F, 1.0F));
	}

	/**
	 * Folds a world coordinate in metres into the {@code 0..1} range a vertex attribute can carry, wrapping
	 * every {@link #POSITION_PERIOD} metres.
	 *
	 * <p>Wrapping rather than clamping for the same reason a heading wraps rather than clamps: a position a
	 * period away is the same lattice cell, not a boundary to flatten onto.
	 */
	public static float foldPosition(final float metres) {
		float folded = metres / POSITION_PERIOD;

		return folded - (float) Math.floor(folded);
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
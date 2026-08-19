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

package com.abyssfall.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import io.netty.buffer.ByteBuf;

/**
 * A player's San (sanity) reading: how much they currently have, and how much they are
 * currently capable of holding.
 *
 * <p>Both halves live in a single immutable value on purpose. They are mutually constrained —
 * the current value can never exceed the maximum — so storing them separately would make it
 * possible to observe a state where that invariant is briefly broken, and the Data Attachment
 * API would have to sync two packets where one will do. Every factory and mutator here returns
 * a fresh, already-valid instance; the invariant is enforced in the canonical constructor so
 * that a broken state simply cannot be constructed, not even by deserialization.
 *
 * <p>This type deliberately exposes no notion of a "stage" or "level". San is a continuous
 * parameter: the entire {@code [0, 1]} span of {@link #ratio()} is meaningful, and any movement
 * within it — however small — is a real change. Deciding what a particular ratio means is the
 * business of whatever consumes the value, not of the value itself.
 *
 * @param current the present San reading, always within {@code [0, max]}
 * @param max     the present ceiling, always at least {@link #MIN_MAX}
 */
public record SanState(float current, float max) {
	/**
	 * The San every player starts a world with, and the default ceiling.
	 */
	public static final float DEFAULT_MAX = 100.0F;

	/**
	 * Hard floor for the ceiling. A maximum of zero would make {@link #ratio()} undefined and
	 * everything derived from it meaningless, so the ceiling is never allowed to reach it.
	 */
	public static final float MIN_MAX = 1.0F;

	/**
	 * Hard ceiling for the ceiling. Not a design statement about how high San may go, just a
	 * guard so a runaway grant or a corrupted save cannot produce infinities or NaN ratios.
	 */
	public static final float MAX_MAX = 10000.0F;

	/**
	 * The state a player has before anything at all has happened to them: full San at the
	 * default ceiling.
	 */
	public static final SanState INITIAL = new SanState(DEFAULT_MAX, DEFAULT_MAX);

	/**
	 * Used for persistence. Both fields are optional on read so that a save written by an
	 * earlier version, or one that only ever recorded a ceiling, still loads.
	 */
	public static final Codec<SanState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.optionalFieldOf("current", DEFAULT_MAX).forGetter(SanState::current),
			Codec.FLOAT.optionalFieldOf("max", DEFAULT_MAX).forGetter(SanState::max)
	).apply(instance, SanState::new));

	/**
	 * Used for server-to-client syncing. Eight bytes, sent only when the value actually
	 * changes.
	 */
	public static final StreamCodec<ByteBuf, SanState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, SanState::current,
			ByteBufCodecs.FLOAT, SanState::max,
			SanState::new
	);

	public SanState {
		// Sanitising here rather than at the call sites means the invariant holds for values
		// arriving from disk or from the network too, not just for ones the mod produced.
		max = sanitizeMax(max);
		current = sanitizeCurrent(current, max);
	}

	/**
	 * A full-San state at the given ceiling.
	 */
	public static SanState full(float max) {
		float sanitized = sanitizeMax(max);
		return new SanState(sanitized, sanitized);
	}

	/**
	 * How much San the player holds as a fraction of their own ceiling, in {@code [0, 1]}.
	 *
	 * <p>This — not the absolute value — is what the rest of the mod should judge a player by,
	 * since the ceiling itself moves as the game progresses. Treat it as a continuous knob: every
	 * point in the range is a distinct condition, and consumers are free to respond smoothly, to
	 * pick their own thresholds, or both.
	 */
	public float ratio() {
		return this.current / this.max;
	}

	/**
	 * The ratio expressed as a percentage in {@code [0, 100]}, for display and for rules that are
	 * more naturally written in percentage terms.
	 */
	public float percent() {
		return ratio() * 100.0F;
	}

	/**
	 * Whether the player is at their own ceiling.
	 */
	public boolean isFull() {
		return this.current >= this.max;
	}

	/**
	 * Whether the player has bottomed out.
	 */
	public boolean isEmpty() {
		return this.current <= 0.0F;
	}

	/**
	 * This state with the current reading set to an absolute value, clamped to the ceiling.
	 */
	public SanState withCurrent(float value) {
		return new SanState(value, this.max);
	}

	/**
	 * This state with {@code delta} added to the current reading. Negative deltas subtract.
	 */
	public SanState addCurrent(float delta) {
		return withCurrent(this.current + delta);
	}

	/**
	 * This state with a new ceiling.
	 *
	 * <p>Raising the ceiling deliberately does <em>not</em> hand out San: a player who becomes
	 * capable of holding more is not thereby made saner, they simply have room to grow. Lowering
	 * it below the current reading does clamp the reading down, because the alternative is an
	 * impossible state.
	 */
	public SanState withMax(float value) {
		return new SanState(this.current, value);
	}

	/**
	 * This state with {@code delta} added to the ceiling. Negative deltas shrink it.
	 */
	public SanState addMax(float delta) {
		return withMax(this.max + delta);
	}

	private static float sanitizeMax(float max) {
		if (!Float.isFinite(max)) {
			return DEFAULT_MAX;
		}

		return Mth.clamp(max, MIN_MAX, MAX_MAX);
	}

	private static float sanitizeCurrent(float current, float sanitizedMax) {
		if (!Float.isFinite(current)) {
			return sanitizedMax;
		}

		return Mth.clamp(current, 0.0F, sanitizedMax);
	}
}

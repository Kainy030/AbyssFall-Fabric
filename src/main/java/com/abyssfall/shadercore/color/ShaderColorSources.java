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

package com.abyssfall.shadercore.color;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import com.abyssfall.shadercore.ShaderColorSource;

/**
 * Serialisation for colour sources, chosen by a {@code "type"} field.
 *
 * <h2>🔴 Why this exists now and did not before</h2>
 *
 * <p>While {@link FixedColorSource} was the only implementation, {@code MaskedPulseEffect} read colour through
 * a single codec with an {@code xmap} that cast on the way out. That was sound only because the cast could
 * never fail: reading always produced a {@code FixedColorSource}, so writing one back always succeeded. It was
 * recorded at the time as temporary, with the note that <strong>a second implementation would require this
 * dispatch first</strong> — otherwise saving a configuration file holding any other source would throw a
 * {@code ClassCastException}.
 *
 * <p>{@link DerivedColorSource} is that second implementation, so this is that dispatch.
 *
 * <h2>Same shape as the effect dispatch, deliberately</h2>
 *
 * <p>Effects already select their codec by a {@code "type"} field, and colour sources now do it the same way,
 * for the same reason: a kind added later needs no change to the file format and no migration of files already
 * written. {@code partialDispatch} rather than {@code dispatch} because the latter's signature does not accept
 * a {@code DataResult}.
 *
 * <h2>Backwards compatibility</h2>
 *
 * <p>A {@code "color"} object written before this existed has no {@code "type"} field. Rather than treat that
 * as a broken file, an absent type reads as {@code fixed} — which is what such a file meant, since it was the
 * only kind that existed. Files already on disk therefore keep working untouched.
 */
public final class ShaderColorSources {
	/**
	 * The name written into the {@code "type"} field for each kind.
	 *
	 * <p>Kept next to the codec map so a kind cannot be registered in one and forgotten in the other.
	 */
	private static final String FIXED = "fixed";
	private static final String DERIVED = "derived";

	private ShaderColorSources() {
	}

	/**
	 * The codec for whichever kind a {@code "type"} field names.
	 */
	public static final Codec<ShaderColorSource> CODEC = Codec.STRING
			.partialDispatch("type",
					ShaderColorSources::typeNameOf,
					ShaderColorSources::codecFor);

	private static DataResult<String> typeNameOf(final ShaderColorSource source) {
		if (source instanceof FixedColorSource) {
			return DataResult.success(FIXED);
		}

		if (source instanceof DerivedColorSource) {
			return DataResult.success(DERIVED);
		}

		// A source defined outside this class cannot be written, because nothing here knows its name. Reported
		// rather than guessed: silently writing it as some other kind would corrupt the file quietly.
		return DataResult.error(() -> "Unregistered colour source: " + source.getClass().getName());
	}

	private static DataResult<MapCodec<? extends ShaderColorSource>> codecFor(final String typeName) {
		return switch (typeName) {
			case FIXED -> DataResult.success(FixedColorSource.CODEC.fieldOf("value"));
			case DERIVED -> DataResult.success(DerivedColorSource.CODEC.fieldOf("value"));
			default -> DataResult.error(() -> "Unknown colour source type: " + typeName);
		};
	}

	/**
	 * The dispatching codec, falling back to {@code fixed} when no {@code "type"} is present.
	 *
	 * <p>This is what an effect should use. See the class javadoc for why the fallback exists.
	 */
	public static final Codec<ShaderColorSource> LENIENT_CODEC = Codec.either(CODEC, FixedColorSource.CODEC)
			.xmap(either -> either.map(Function.identity(), source -> (ShaderColorSource) source),
					// Written through the dispatching side always, so a file this mod writes always carries a
					// type. Only reading tolerates its absence.
					com.mojang.datafixers.util.Either::left);
}

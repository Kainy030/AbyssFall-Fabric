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
 * <h2>Why the dispatch exists</h2>
 *
 * <p>Colour once read through a single codec with an {@code xmap} that cast on the way out. That was sound only
 * while one implementation existed: reading always produced the same type, so writing one back always
 * succeeded. A second kind made the cast able to fail, so the dispatch that had been noted as required went in
 * — otherwise saving a file holding any other source threw a {@code ClassCastException}.
 *
 * <h2>Same shape as the effect dispatch, deliberately</h2>
 *
 * <p>Effects select their codec by a {@code "type"} field and colour sources do it the same way, for the same
 * reason: a kind added later needs no change to the file format and no migration of files already written.
 * {@code partialDispatch} rather than {@code dispatch} because the latter's signature does not accept a
 * {@code DataResult}.
 *
 * <h2>An absent type reads as derived</h2>
 *
 * <p>A {@code "color"} object may have been written before this dispatch existed and so carry no
 * {@code "type"}. Such a file is read as {@code derived} rather than rejected. It will not mean quite what it
 * said — the kind it was written as was the red-and-blue debug source, which has since been deleted — but a
 * colour derived from the item's own texture is the sane reading of "some colour was configured here", and it
 * keeps an old file loading instead of failing the whole entry.
 */
public final class ShaderColorSources {
	/**
	 * The name written into the {@code "type"} field for each kind.
	 *
	 * <p>Kept next to the codec map so a kind cannot be registered in one and forgotten in the other.
	 */
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
		if (source instanceof DerivedColorSource) {
			return DataResult.success(DERIVED);
		}

		// A source defined outside this class cannot be written, because nothing here knows its name. Reported
		// rather than guessed: silently writing it as some other kind would corrupt the file quietly.
		return DataResult.error(() -> "Unregistered colour source: " + source.getClass().getName());
	}

	private static DataResult<MapCodec<? extends ShaderColorSource>> codecFor(final String typeName) {
		return switch (typeName) {
			case DERIVED -> DataResult.success(DerivedColorSource.CODEC.fieldOf("value"));
			default -> DataResult.error(() -> "Unknown colour source type: " + typeName);
		};
	}

	/**
	 * The dispatching codec, falling back to {@code derived} when no {@code "type"} is present.
	 *
	 * <p>This is what an effect should use. See the class javadoc for why the fallback exists.
	 *
	 * <p>⚠️ The fallback is lenient in the strong sense: {@code DerivedColorSource}'s fields are all optional,
	 * so <em>any</em> object without a {@code "type"} parses, including one carrying only fields it does not
	 * know. Those fields are dropped and the defaults used. That is deliberate — the alternative is failing
	 * the whole entry, which loses the effect as well as the colour — but it means an old file's colour is not
	 * preserved, only its presence. A file this mod writes always carries a type and is never read this way.
	 */
	public static final Codec<ShaderColorSource> LENIENT_CODEC = Codec.either(CODEC, DerivedColorSource.CODEC)
			.xmap(either -> either.map(Function.identity(), source -> (ShaderColorSource) source),
					// Written through the dispatching side always, so a file this mod writes always carries a
					// type. Only reading tolerates its absence.
					com.mojang.datafixers.util.Either::left);
}

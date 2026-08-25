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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Every registered kind of shader effect, and the codec that reads whichever one a file names.
 *
 * <h2>How the file stays open to new kinds</h2>
 *
 * <p>An entry in the configuration says which kind it is, and the rest of that entry is read by that
 * kind's own codec:
 *
 * <pre>{@code
 * { "type": "abyssfall:masked_pulse", "mask": "...", "sample_density": 0.1 }
 * }</pre>
 *
 * <p>{@link #CODEC} dispatches on that field, so a kind registered later is readable without this
 * class or the file format changing. This is the same shape vanilla uses for its own open-ended
 * lists — special model renderers, for instance — for the same reason.
 *
 * <h2>Registering</h2>
 *
 * <p>Call {@link #register} from a static initialiser or from mod setup, before configuration is
 * read. Registering afterwards is not an error, but a file naming that type will have already been
 * rejected once.
 *
 * <p>The registry is deliberately not frozen. A type may be added at any point, because the cost of
 * a late registration is one unreadable entry rather than a corrupt state — and leaving it open is
 * what allows an add-on to contribute a kind of effect without this mod knowing about it.
 */
public final class ShaderEffectTypes {
	private static final Map<Identifier, ShaderEffectType<?>> TYPES = new LinkedHashMap<>();

	/**
	 * Reads any registered kind, choosing by the entry's {@code type} field.
	 *
	 * <p>Resolved through a lookup rather than a captured snapshot, so a type registered after this
	 * codec was built is still found. A codec holding a copy of the map would silently reject the
	 * newer type.
	 */
	public static final Codec<ShaderEffect> CODEC = Identifier.CODEC
			.partialDispatch("type", ShaderEffectTypes::idOf, ShaderEffectTypes::codecOf);

	private ShaderEffectTypes() {
	}

	/**
	 * Adds a kind of effect, replacing any previously registered under the same id.
	 *
	 * <p>Replacement rather than refusal so that a pack or an add-on can substitute its own reading of
	 * a type it knows about; silently ignoring the second registration would be the harder failure to
	 * diagnose of the two.
	 *
	 * @return the type, so a caller can keep it in a constant
	 */
	public static <T extends ShaderEffect> ShaderEffectType<T> register(ShaderEffectType<T> type) {
		TYPES.put(type.id(), type);
		return type;
	}

	/**
	 * The type registered under {@code id}, or {@code null} if none is.
	 */
	public static @Nullable ShaderEffectType<?> get(Identifier id) {
		return TYPES.get(id);
	}

	/**
	 * Every registered type, in registration order.
	 */
	public static Collection<ShaderEffectType<?>> all() {
		return TYPES.values();
	}

	/**
	 * The codec for a type id, as the dispatch needs it.
	 *
	 * <p>An unknown id yields a failed result naming what was asked for and what is available, because
	 * a mistyped type is the most likely thing to go wrong in a hand-written file and the least
	 * self-evident once it has.
	 */
	private static DataResult<? extends MapCodec<? extends ShaderEffect>> codecOf(Identifier id) {
		ShaderEffectType<?> type = TYPES.get(id);

		if (type == null) {
			return DataResult.error(() -> "Unknown shader effect type '" + id
					+ "'; registered types are " + TYPES.keySet());
		}

		return DataResult.success(type.codec());
	}

	/**
	 * The type id a dispatching codec writes for an effect.
	 *
	 * <p>Exists so the write side and the read side cannot disagree about where the id lives.
	 */
	private static DataResult<Identifier> idOf(ShaderEffect effect) {
		return DataResult.success(effect.type().id());
	}
}

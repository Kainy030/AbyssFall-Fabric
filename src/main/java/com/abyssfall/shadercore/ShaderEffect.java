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

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;

import com.abyssfall.shadercore.geometry.ItemHullGeometry;

/**
 * One way of drawing over an item, and the values that particular way needs.
 *
 * <h2>Why this is an interface and not a record with more fields</h2>
 *
 * <p>The mod is meant to end up putting several unrelated appearances on items — a starfield, a
 * wrongness that grows as San falls, whatever a later idea asks for. Those do not share a set of
 * numbers: a scanline has a direction and a speed, a starfield has a depth and a density, and forcing
 * them into one record would give every effect every other effect's fields and leave most of them
 * meaningless.
 *
 * <p>So an effect is whatever its own type says it is. Each implementation is a record with only its
 * own values, carries its own {@link MapCodec}, and states which GLSL program draws it. Adding a kind
 * of effect is writing one record and one shader and registering the pair; nothing existing is
 * touched, and nothing here needs to know the new kind exists.
 *
 * <h2>Effects are values</h2>
 *
 * <p>Implementations must be records, or otherwise compare by value. Two items configured identically
 * are meant to share one compiled pipeline, and that sharing is keyed on the effect itself.
 *
 * @see ShaderEffectTypes for the registry, and for what registering costs
 */
public interface ShaderEffect {
	/**
	 * The type that produced this effect, which is also what serialises it back.
	 */
	ShaderEffectType<?> type();

	/**
	 * The mask texture, whose channels decide which fragments this effect may cover.
	 *
	 * <p>Every effect has one: an effect that covered an item edge to edge would have no way to leave
	 * the item recognisable. What the individual channels mean is the effect's own business.
	 */
	Identifier mask();

	/**
	 * What surface this effect is drawn on.
	 *
	 * <p>Defaults to following the item's own hull, which is what an effect meant to make an ordinary object
	 * look wrong almost always wants: the item keeps its shape and its thickness, and the effect covers all
	 * of it. See {@link ShaderGeometrySource} for why this is answerable per effect rather than fixed — an
	 * effect that projects rather than coats, a starfield being the case already in mind, has an entirely
	 * different answer.
	 */
	default ShaderGeometrySource geometry() {
		return ItemHullGeometry.INSTANCE;
	}

	/**
	 * Values to compile into the shader, as {@code #define NAME value} pairs.
	 *
	 * <p>Defines rather than uniforms because a uniform buffer has to be filled by driving a
	 * {@code RenderPass} by hand, which is not possible where these are drawn. The consequence is that
	 * two effects differing in any value here are different programs — which is why this map is also
	 * part of what identifies a pipeline.
	 *
	 * <p>Float rather than a formatted string: the pipeline builder takes {@code int} or {@code float}
	 * and nothing else, and letting it do the formatting avoids the classic GLSL trap where a value
	 * written as {@code 1} is read as an integer and turns a division into integer division.
	 */
	Map<String, Float> shaderDefines();

	/**
	 * Names of {@code #define} flags to set, for parts of the shader guarded by {@code #ifdef}.
	 *
	 * <p>Separate from {@link #shaderDefines()} because a flag's presence is the whole of its meaning,
	 * and a shader can leave a branch out of the compiled program entirely rather than testing a value
	 * at runtime.
	 */
	default java.util.Set<String> shaderFlags() {
		return java.util.Set.of();
	}
}

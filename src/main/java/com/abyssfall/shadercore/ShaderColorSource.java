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
import java.util.Set;

/**
 * Where an effect's colour comes from — deliberately left open.
 *
 * <h2>🔴 Why this exists as an interface with one trivial implementation</h2>
 *
 * <p>How colour should be decided is <strong>not yet designed</strong>. The candidates are genuinely
 * different: a value stated in the configuration file; a value a provider computes each frame from a
 * player's San; a hue rotation applied to the item's own texture; a colour a shader invents per fragment
 * as a starfield would. They do not have the same inputs, they do not have the same cost, and one of them
 * cannot be expressed with the current draw path at all.
 *
 * <p>Choosing now would mean choosing wrong, and the wrong choice is expensive: colour reaches the shader
 * through compile-time defines, so a decision made in the wrong place ends up wired through the pipeline
 * cache and the render types. This interface is the seam that keeps that from happening. The renderer and
 * the pipeline builder deal only in "whatever this source contributes"; nothing below this line knows how
 * a colour was arrived at.
 *
 * <p><strong>Do not add behaviour here to serve a particular scheme.</strong> When the colour system is
 * designed, it should be able to arrive as new implementations of this interface — and if it cannot, this
 * interface is what should change, not the renderer.
 *
 * <h2>What a source is allowed to be</h2>
 *
 * <p>Currently a source contributes preprocessor defines and flags, because that is how every value reaches
 * a shader on this path. A future source that needed a uniform, or a second texture, would need this
 * contract widened — which is a change to one file rather than to the drawing code.
 *
 * <p>Implementations must compare by value: they take part in the key that decides whether two effects
 * share a compiled pipeline.
 */
public interface ShaderColorSource {
	/**
	 * Values this source contributes to the shader, as {@code #define NAME value} pairs.
	 *
	 * <p>Names are the source's own business, but the shader that reads them has to agree, so in practice a
	 * kind of effect and the sources it accepts are written together.
	 */
	Map<String, Float> shaderDefines();

	/**
	 * Names of {@code #define} flags this source contributes, for branches guarded by {@code #ifdef}.
	 */
	default Set<String> shaderFlags() {
		return Set.of();
	}
}

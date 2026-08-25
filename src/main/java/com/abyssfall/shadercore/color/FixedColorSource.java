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

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.abyssfall.shadercore.ShaderColorSource;

/**
 * Two colours stated outright, which the shader sweeps between.
 *
 * <h2>⚠ A placeholder, not a decision</h2>
 *
 * <p>This is the simplest thing that satisfies {@link ShaderColorSource} and it exists so the rest of the
 * system can be finished and tested. <strong>It is not a statement that colour ought to be configured as
 * two constants</strong> — that question is open, and this class is expected to be joined or replaced by
 * others once it is settled.
 *
 * <p>Its limits are worth stating plainly, so nobody mistakes them for the system's limits:
 *
 * <ul>
 *   <li>Both values are compiled into the shader, so a colour that changed continuously would compile a
 *       new pipeline per value. Anything reacting to San frame by frame needs a different kind of source,
 *       not more fields here.</li>
 *   <li>Every fragment gets the same colour at any instant. A colour that varies across the item — a
 *       gradient, a starfield — cannot come from constants at all.</li>
 *   <li>Nothing is read from the item's own texture.</li>
 * </ul>
 *
 * @param colorA colour at one end of the sweep, as {@code 0xRRGGBB}
 * @param colorB colour at the other end
 */
public record FixedColorSource(int colorA, int colorB) implements ShaderColorSource {
	/**
	 * Red to blue. Chosen only because it is unmistakable on screen while the system is being built.
	 */
	public static final FixedColorSource DEFAULT = new FixedColorSource(0xFF0000, 0x0000FF);

	public static final Codec<FixedColorSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 0xFFFFFF).optionalFieldOf("color_a", DEFAULT.colorA())
					.forGetter(FixedColorSource::colorA),
			Codec.intRange(0, 0xFFFFFF).optionalFieldOf("color_b", DEFAULT.colorB())
					.forGetter(FixedColorSource::colorB)
	).apply(instance, FixedColorSource::new));

	/**
	 * A single colour, expressed as a sweep whose ends are the same.
	 *
	 * <p>Offered because "just this colour" is a reasonable thing to want and writing the same value twice
	 * reads like a mistake.
	 */
	public static FixedColorSource of(int color) {
		return new FixedColorSource(color, color);
	}

	@Override
	public Map<String, Float> shaderDefines() {
		return Map.of(
				"COLOR_A_R", red(this.colorA), "COLOR_A_G", green(this.colorA), "COLOR_A_B", blue(this.colorA),
				"COLOR_B_R", red(this.colorB), "COLOR_B_G", green(this.colorB), "COLOR_B_B", blue(this.colorB));
	}

	private static float red(int color) {
		return ((color >> 16) & 0xFF) / 255.0F;
	}

	private static float green(int color) {
		return ((color >> 8) & 0xFF) / 255.0F;
	}

	private static float blue(int color) {
		return (color & 0xFF) / 255.0F;
	}
}

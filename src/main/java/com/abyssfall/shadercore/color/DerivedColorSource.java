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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.abyssfall.shadercore.ShaderColorSource;

/**
 * Colour worked out from the item's own texture, rather than stated in advance.
 *
 * <h2>🔴 Why this matters more than it looks</h2>
 *
 * <p>{@link FixedColorSource} paints a colour <em>over</em> an item. That works, but it means every item that
 * should look wrong needs artwork saying exactly where and in what colour — a mask per item, hand-painted.
 * For a mod whose premise is that <em>ordinary things</em> start looking wrong as San falls, that does not
 * scale: the interesting case is a stone pickaxe the player has carried for hours, and nobody is going to
 * author a mask for every item in the game.
 *
 * <p>This source reads the pixel the effect is covering and derives a colour from it. The item supplies its
 * own detail, so one effect works on anything — and the result keeps the item's shading, which a painted
 * colour cannot.
 *
 * <p>It does not replace {@code FixedColorSource}. Stating a colour outright is still the right answer when an
 * effect is meant to look like itself rather than like the thing it covers.
 *
 * <h2>What it can and cannot do</h2>
 *
 * <p>Reading the item's texture requires its atlas coordinates, which is why {@code ShaderVertex} carries a
 * second pair. That was added for this; nothing else needed it.
 *
 * <p>Like every source on this path, the parameters here are compiled in, so a value that changed every frame
 * would compile a pipeline per value. The derivation itself is per-fragment and costs nothing extra — it is
 * the tunables that are fixed, not the result.
 *
 * @param derivation how the covered pixel becomes the pixel drawn
 * @param color      the colour the derivation works towards, as {@code 0xRRGGBB}. What it means depends on the
 *                   derivation: a hue to shift towards, a stain, a glow
 * @param strength   how far the derivation is taken, {@code 0} leaving the item untouched and {@code 1}
 *                   applying it in full. Anything in between is a blend with the item's own colour, which is
 *                   what makes an effect able to grow as San falls without changing kind
 */
public record DerivedColorSource(ColorDerivation derivation, int color, float strength)
		implements ShaderColorSource {
	/**
	 * A moderate hue shift towards a cold violet — the mod's own register, and visible on almost any item
	 * without obliterating it. A starting point, not a house style.
	 */
	public static final DerivedColorSource DEFAULT =
			new DerivedColorSource(ColorDerivation.TINTED, 0x6B4A73, 0.75F);

	public static final Codec<DerivedColorSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ColorDerivation.CODEC.optionalFieldOf("derivation", DEFAULT.derivation())
					.forGetter(DerivedColorSource::derivation),
			Codec.intRange(0, 0xFFFFFF).optionalFieldOf("color", DEFAULT.color())
					.forGetter(DerivedColorSource::color),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("strength", DEFAULT.strength())
					.forGetter(DerivedColorSource::strength)
	).apply(instance, DerivedColorSource::new));

	/**
	 * This derivation at full strength, in the mod's default colour.
	 */
	public static DerivedColorSource of(final ColorDerivation derivation) {
		return new DerivedColorSource(derivation, DEFAULT.color(), 1.0F);
	}

	@Override
	public Map<String, Float> shaderDefines() {
		Map<String, Float> defines = new HashMap<>();

		defines.put("DERIVE_R", ((this.color >> 16) & 0xFF) / 255.0F);
		defines.put("DERIVE_G", ((this.color >> 8) & 0xFF) / 255.0F);
		defines.put("DERIVE_B", (this.color & 0xFF) / 255.0F);
		defines.put("DERIVE_STRENGTH", this.strength);

		return Map.copyOf(defines);
	}

	@Override
	public Set<String> shaderFlags() {
		// Two flags: one saying the colour comes from the texture at all, one selecting the derivation. The
		// first is what the shader tests to decide whether to sample; keeping them separate means a future
		// source could reuse the sampling without inheriting these four derivations.
		return Set.of("COLOR_FROM_TEXTURE", this.derivation.shaderFlag());
	}
}

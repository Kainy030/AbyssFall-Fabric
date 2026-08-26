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

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * How a colour is derived from the pixel it is covering.
 *
 * <h2>Why these are one enum and not four sources</h2>
 *
 * <p>All four ask the same question of the same input — given the item's own colour at this fragment, what
 * should appear instead — and all four are answered by the same shader with one branch. Splitting them into
 * separate {@code ShaderColorSource} implementations would duplicate the sampling, the parameters and the
 * blending four times over to vary one expression.
 *
 * <p>They are an enum rather than a boolean pair because they are alternatives, not options: a fragment is
 * recoloured exactly one way. The distinctions matter to how the result reads, so each is described in terms
 * of what a player would see rather than what the arithmetic is.
 */
public enum ColorDerivation implements StringRepresentable {
	/**
	 * Keeps the item's light and shade, pushes its hue towards the effect's colour.
	 *
	 * <p>The item stays recognisably itself — every highlight and every shadow where the artist put it — but
	 * its colour is no longer its own. Of the four this is the one that reads as a familiar object gone wrong,
	 * rather than as an object replaced by something else.
	 */
	TINTED("tinted"),

	/**
	 * Drains the item's colour, then stains what remains.
	 *
	 * <p>Reads as something having been taken out of the object rather than added to it. Where {@link #TINTED}
	 * keeps the item's own palette and shifts it, this discards the palette first, so two differently coloured
	 * items end up looking alike.
	 */
	DRAINED("drained"),

	/**
	 * Inverts the item's brightness, keeping its shape legible.
	 *
	 * <p>What was lit becomes dark and what was dark becomes lit, so the object is still perfectly readable
	 * while being unmistakably wrong. Reads as a negative, or as the same object seen from somewhere it should
	 * not be visible from.
	 */
	INVERTED("inverted"),

	/**
	 * Leaves the item's colour alone and adds light over it.
	 *
	 * <p>The most restrained of the four: the object is exactly itself, merely glowing. Useful where an effect
	 * should register as something happening <em>to</em> an item without obscuring what the item is — and the
	 * only one of the four that cannot make an item harder to identify.
	 */
	GLOWING("glowing");

	public static final Codec<ColorDerivation> CODEC = StringRepresentable.fromEnum(ColorDerivation::values);

	private final String name;

	ColorDerivation(final String name) {
		this.name = name;
	}

	/**
	 * The {@code #define} flag this derivation sets, which is how the shader picks its branch.
	 *
	 * <p>A flag per derivation rather than a numeric value compared at runtime: the branches not taken are
	 * left out of the compiled program entirely, and a shader cannot be handed a derivation it does not know.
	 */
	public String shaderFlag() {
		return "DERIVE_" + this.name().toUpperCase(java.util.Locale.ROOT);
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}

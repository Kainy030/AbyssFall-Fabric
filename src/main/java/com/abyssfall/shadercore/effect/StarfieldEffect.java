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

package com.abyssfall.shadercore.effect;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import com.abyssfall.AbyssFall;
import com.abyssfall.shadercore.ShaderEffect;
import com.abyssfall.shadercore.ShaderEffectType;
import com.abyssfall.shadercore.ShaderGeometrySource;
import com.abyssfall.shadercore.geometry.ItemFacesGeometry;

/**
 * A depth of sky seen through the item's own outline.
 *
 * <h2>What this is, and where the approach came from</h2>
 *
 * <p>The stars are not artwork. Every fragment is treated as a ray leaving the viewer, rotated to face where
 * the viewer is facing, and mapped onto a sphere; the sphere is divided into a grid and a pseudo-random test
 * decides which cells hold a star. Sixteen such spheres are stacked, each turned about its own arbitrary axis,
 * and their contributions are added.
 *
 * <p>The consequence is the point: <strong>there is no texture to author and nothing to tile</strong>. Density
 * is a constant, the field never repeats, it has no seams, and it costs the same at any resolution. The
 * alternative — a painted starfield — needs an enormous seamless image and still repeats.
 *
 * <p>The technique is taken from Avaritia's cosmic shader, which solves exactly this problem and solves it
 * well. The arithmetic is that shader's, restated for this project's draw path; the differences are all forced
 * by 26.2 and are recorded in the fragment program next to the lines they affect.
 *
 * <h2>How this kind uses its mask</h2>
 *
 * <p>Every other effect so far covers part of an item and leaves the rest recognisable, which is what a mask is
 * for. A starfield does the opposite: it replaces what the item is made of.
 *
 * <p>The mask is nevertheless read, and read as the reference implementation reads it: its <strong>red
 * channel is the effect's opacity</strong>, so it decides both where the sky shows and how strongly. A red of
 * zero discards the fragment outright. That is the whole of the mask's role here — the green and blue channels
 * are unused, unlike {@code masked_pulse}, which assigns each channel a behaviour.
 *
 * <p>⚠️ Because the mask alone decides coverage, mask artwork may extend past the item's own outline, and the
 * sky will be drawn there. That is intended: the effect is meant to show through gaps in an item rather than
 * only over its solid pixels.
 *
 * @param mask       where the sky shows through, and how strongly. Red channel only
 * @param stars      the sprites a star is drawn from, in the atlas. Their coordinates are resolved when the
 *                   model is baked, not here: an effect names artwork, it does not know where the atlas packer
 *                   put it
 * @param layers     how many rotated spheres are stacked, more giving a deeper field for more cost
 * @param driftSpeed how fast the field slides past, in cells per Minecraft day
 * @param brightness overall multiplier on the result, for pulling the whole field up or down
 * @param density    multiplier on the star count. 1.0 is the reference's own {@code 10 of 101} cells; 2.0
 *                   doubles how many cells hold a star
 * @param debugSolid diagnostic paint, see {@link #DEFAULT_DEBUG_SOLID}
 */
public record StarfieldEffect(Identifier mask, List<Identifier> stars, int layers,
		float driftSpeed, float brightness, float density, boolean debugSolid) implements ShaderEffect {
	/**
	 * The ten sprites the reference implementation uses, in its own order.
	 *
	 * <p>🔴 <strong>The count matters.</strong> The reference tests {@code hash(cell) % 101 < spriteCount}, so
	 * <em>how many sprites there are</em> is what decides how full the sky is — ten in a hundred and one cells.
	 * <p>The order matters too, since the same hash picks which sprite a cell gets.
	 */
	public static final List<Identifier> DEFAULT_STARS = List.of(
			star(0), star(1), star(2), star(3), star(4), star(5), star(6), star(7), star(8), star(9));

	/**
	 * Upper bound on how many sprites a starfield may draw from.
	 *
	 * <p>Ten is what the shader has branches for. More would need the branch chain extended, and past a hundred
	 * and one every cell holds a star and the sky is solid.
	 */
	public static final int MAX_STARS = 10;

	private static Identifier star(final int index) {
		return Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "shader/cosmic_" + index);
	}

	/**
	 * Sixteen stacked spheres, as the reference uses.
	 *
	 * <p>The count is what produces the sense of depth: each layer is scaled differently, so nearer layers
	 * slide past faster than farther ones as the viewer turns. Fewer layers is cheaper and visibly flatter.
	 */
	public static final int DEFAULT_LAYERS = 16;

	/** Below this the field is a single flat sheet of stars with no parallax at all. */
	public static final int MIN_LAYERS = 1;

	/**
	 * An upper bound on the layer count.
	 *
	 * <p>Not a hardware limit — the loop is unrolled at compile time and each layer is a handful of
	 * instructions — but the returns stop long before this, and a value in the hundreds turns a per-fragment
	 * cost into something worth noticing on every item on screen.
	 */
	public static final int MAX_LAYERS = 64;

	/** Slow enough to read as motion rather than as scrolling. */
	public static final float DEFAULT_DRIFT_SPEED = 4.8F;

	public static final float DEFAULT_BRIGHTNESS = 1.0F;

	/**
	 * The reference's own density: {@code 10 of 101} cells hold a star, which is how full the reference's sky
	 * reads. A value of 2.0 doubles that, 0.5 halves it.
	 */
	public static final float DEFAULT_DENSITY = 1.0F;

	/**
	 * ⚠️ Diagnostic. Paints the masked area a flat magenta instead of drawing sky.
	 *
	 * <p>Exists because "the effect is too faint to be sure it is working" and "the effect is not working" look
	 * alike, and telling them apart by tuning numbers is guesswork. With this on, the answer is unambiguous: a
	 * magenta blade means the geometry, depth state, mask and blending are all correct and the faintness is the
	 * field's own arithmetic; an unchanged blade means the fault is upstream and tuning the field is pointless.
	 *
	 * <p>Set {@code "debug_solid": true} on the effect in {@code AbyssFallShader.json}. Not something to ship.
	 */
	public static final boolean DEFAULT_DEBUG_SOLID = false;

	/**
	 * The shader that draws this kind of effect. Both stages share the name, as vanilla's own do.
	 *
	 * <p>⚠️ The {@code core/} prefix is not decoration. {@code ShaderManager} builds a shader's id with
	 * {@code FileToIdConverter("shaders", ".vsh")}, which strips only {@code shaders/} and the extension — so
	 * the file at {@code assets/abyssfall/shaders/core/starfield.vsh} is known as
	 * {@code abyssfall:core/starfield}. Naming it without the prefix produces "Couldn't find source for VERTEX
	 * shader", the pipeline never compiles, and nothing is drawn.
	 */
	public static final Identifier SHADER =
			Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "core/starfield");

	public static final MapCodec<StarfieldEffect> MAP_CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					Identifier.CODEC.fieldOf("mask").forGetter(StarfieldEffect::mask),
					Identifier.CODEC.listOf(1, MAX_STARS).optionalFieldOf("stars", DEFAULT_STARS)
							.forGetter(StarfieldEffect::stars),
					Codec.intRange(MIN_LAYERS, MAX_LAYERS).optionalFieldOf("layers", DEFAULT_LAYERS)
							.forGetter(StarfieldEffect::layers),
					Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("drift_speed", DEFAULT_DRIFT_SPEED)
							.forGetter(StarfieldEffect::driftSpeed),
					Codec.floatRange(0.0F, 4.0F).optionalFieldOf("brightness", DEFAULT_BRIGHTNESS)
							.forGetter(StarfieldEffect::brightness),
					// 10.0 keeps a dense-but-not-solid sky: at STAR_COUNT = 10 the reference's 101-cell test
					// saturates at a density of 10.1, past which every cell holds a star.
					Codec.floatRange(0.0F, 10.0F).optionalFieldOf("density", DEFAULT_DENSITY)
							.forGetter(StarfieldEffect::density),
					Codec.BOOL.optionalFieldOf("debug_solid", DEFAULT_DEBUG_SOLID)
							.forGetter(StarfieldEffect::debugSolid)
			).apply(instance, StarfieldEffect::new));

	/**
	 * Registered in {@code AbyssFallShaderCore} rather than here, so that registration order is visible in one
	 * place.
	 */
	public static final ShaderEffectType<StarfieldEffect> TYPE = new ShaderEffectType<>(
			Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "starfield"), SHADER, MAP_CODEC,
			DEFAULT_STARS);

	/**
	 * A starfield with every value left at its default.
	 *
	 * @param mask where the sky shows through — its red channel is the opacity, so a mask with no red draws
	 *             nothing at all
	 */
	public static StarfieldEffect of(final Identifier mask) {
		return new StarfieldEffect(mask, DEFAULT_STARS, DEFAULT_LAYERS, DEFAULT_DRIFT_SPEED,
				DEFAULT_BRIGHTNESS, DEFAULT_DENSITY, DEFAULT_DEBUG_SOLID);
	}

	@Override
	public ShaderEffectType<?> type() {
		return TYPE;
	}

	/**
	 * The two flat faces of the item — see {@link ItemFacesGeometry} for why the side walls are dropped.
	 *
	 * <p>That source is coplanar with the item's own surface and says so itself, so the depth bias this effect
	 * needs follows from the geometry rather than from a second setting here.
	 */
	@Override
	public ShaderGeometrySource geometry() {
		return ItemFacesGeometry.INSTANCE;
	}

	@Override
	public Map<String, Float> shaderDefines() {
		// STAR_LAYERS is a define rather than a uniform for a reason beyond the usual one: the shader's main
		// loop runs over it, and a loop with a compile-time bound is unrolled. As a uniform it would be a real
		// loop with a real branch, evaluated per fragment.
		//
		// 🔴 The star sprites' atlas coordinates are NOT here, and cannot be: this record has no way to know
		// where the atlas packer put them, and the packing changes with the resource pack. They are contributed
		// by the renderer, which resolves each sprite when the model is baked and adds STAR_N_U0 and friends.
		// See AbyssFallPipelines.
		//
		// They are still compile-time constants, because a sprite's coordinates never change once the atlas is
		// stitched — 26.2 animates a sprite by drawing the current frame into its fixed rectangle, leaving its
		// UVs alone. That fact is what makes this whole approach possible; the reference implementation had to
		// re-upload them every frame because 1.12 moved the UVs instead.
		return Map.of(
				"STAR_LAYERS", (float) this.layers,
				"STAR_DRIFT_SPEED", this.driftSpeed,
				"STAR_BRIGHTNESS", this.brightness,
				"STAR_DENSITY", this.density);
	}

	@Override
	public List<Identifier> spriteDependencies() {
		return this.stars;
	}

	@Override
	public Set<String> shaderFlags() {
		// No colour source: the field invents a colour per star, which is the whole of what makes it a
		// starfield rather than a tint. A source contributing DERIVE_* here would have nothing to act on.
		return this.debugSolid ? Set.of("STARFIELD_DEBUG_SOLID") : Set.of();
	}
}
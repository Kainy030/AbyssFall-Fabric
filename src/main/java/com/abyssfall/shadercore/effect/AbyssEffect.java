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

import java.util.HashMap;
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
 * The Abyss effect — a window onto a vast, dark, unmeasurably deep space, seen through the item's own outline.
 *
 * <h2>🔴 This is not the cosmic sky, and shares nothing with it any more</h2>
 *
 * <p>This kind began as a file-for-file copy of the ported Avaritia starfield (rotated sphere shells, a gridded
 * sky, ten hand-drawn mote sprites). That algorithm answered a different question — an <em>infinitely far
 * sky</em>, whose only motion is the viewer turning. The abyss is meant as an <em>infinitely deep space</em>:
 * sparse, random, unrecognisable structures hanging in the dark at real distances, so that moving the camera
 * makes the near ones slide past while the far ones barely move. A sphere shell cannot do that; it is
 * motionless under translation, so the port's sixteen shells faked depth by each rotating on its own axis.
 *
 * <p>The program was therefore rewritten from scratch. What replaced the shells is a purely procedural volume:
 * the world is a lattice of cells, most empty; a ray is marched cell by cell out from the camera, and each
 * occupied cell holds a faint structure the ray may pass near. No artwork is sampled — the shapes are produced
 * by math alone — so this record carries no sprite list and the kind declares no sprites.
 *
 * <p><strong>The shader file contains only the geometry and the marching.</strong> Every number that says what
 * the abyss is — how big a cell is, how far a ray reaches, how many cells hold anything, how large, bright and
 * coloured the structures are, how strongly they breathe — is a field here, persisted in
 * {@code AbyssFallShader.json} and compiled in as a {@code #define}. Nothing about the look is welded into the
 * GLSL; that was the central mistake of the port it replaced.
 *
 * <h2>Depth comes from the camera's position, not from stacked layers</h2>
 *
 * <p>Turning the head rotates every ray; <em>translating</em> the camera is what produces parallax, and only a
 * real 3-D position makes near cells move against far ones. There is no camera position in the uniforms
 * available on this draw path (verified against 26.2's shader includes), so the renderer packs a folded camera
 * position into the vertex stream for this kind. See {@link #usesViewerPosition()} and the channel notes in
 * {@code ShaderLayerRenderer} and {@code abyss.vsh}.
 *
 * <h2>How this kind uses its mask</h2>
 *
 * <p>Its <strong>red channel is the effect's opacity</strong>, so it decides both where the window opens and how
 * strongly. A red of zero discards the fragment; inside the mask the shader paints its own near-black void — it
 * does not let the item's texture through. That is what makes the region read as a hole opening onto somewhere,
 * rather than as a picture laid over the item.
 */
public record AbyssEffect(Identifier mask, float cellSize, float maxDistance, float occupancy,
		int structuresPerCell, float structureRadius, float distanceFalloff, float domainEdge,
		int voidColor, int structureColor, float brightness,
		Haze haze, float stirAmplitude, float stirSpeed, boolean debugSolid) implements ShaderEffect {

	/**
	 * The volumetric haze settings, grouped as one codec slot.
	 *
	 * <p>This is a grouping for serialization rather than a separate concept: {@code RecordCodecBuilder.group}
	 * accepts at most sixteen fields, and the abyss has more tunables than that, so the four haze values live
	 * in their own {@code "haze": { … }} object in the configuration. They are spread back into the
	 * {@link #shaderDefines()} flat, so the shader sees no grouping.
	 *
	 * @param coverage   fraction of the noise field that carries haze, {@code 0..1}
	 * @param brightness how strongly a wisp lights the cells it crosses
	 * @param distance   depth (as a fraction of the ray's reach) over which haze builds toward its far tone
	 * @param scale      spatial frequency of the noise field, in cells; small makes broad wisps, large fine
	 */
	public record Haze(float coverage, float brightness, float distance, float scale) {
		static final Haze DEFAULT = new Haze(DEFAULT_HAZE_COVERAGE, DEFAULT_HAZE_BRIGHTNESS,
				DEFAULT_HAZE_DISTANCE, DEFAULT_HAZE_SCALE);

		static final MapCodec<Haze> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).optionalFieldOf("coverage", DEFAULT_HAZE_COVERAGE)
						.forGetter(Haze::coverage),
				Codec.floatRange(0.0F, 1.0F).optionalFieldOf("brightness", DEFAULT_HAZE_BRIGHTNESS)
						.forGetter(Haze::brightness),
				Codec.floatRange(0.05F, 2.0F).optionalFieldOf("distance", DEFAULT_HAZE_DISTANCE)
						.forGetter(Haze::distance),
				Codec.floatRange(0.1F, 8.0F).optionalFieldOf("scale", DEFAULT_HAZE_SCALE)
						.forGetter(Haze::scale)
		).apply(instance, Haze::new));
	}
	/** World metres across one lattice cell. The ruler of the volume; larger cells make sparser, slower shapes. */
	public static final float DEFAULT_CELL_SIZE = 4.0F;
	public static final float MIN_CELL_SIZE = 0.5F;
	public static final float MAX_CELL_SIZE = 32.0F;

	/** How far a ray reaches, in metres — the COMPUTE sphere. Cost is linear in the number of cells marched
	 *  (max_distance / cell_size), so after the wide-field test proved heavy on a held item the sphere was
	 *  pulled all the way in: 16m at the 4m cell size is 4 cells of march, an eighth of the original 32-cell
	 *  wide field. The visible content stops at the smaller view sphere ({@link #DEFAULT_DOMAIN_EDGE}, ~12m /
	 *  3 cells) and the 1-cell band between them stays black. This is around the smallest sphere that still
	 *  reads as a space: below it the march has too few cells to hold any parallax and the window collapses
	 *  into a layer of fog painted on the item. Note this is metres, not a 0..1 value — the codec floor is 8m,
	 *  which at this cell size leaves only 2 cells and is not a depth effect. */
	public static final float DEFAULT_MAX_DISTANCE = 16.0F;
	public static final float MIN_MAX_DISTANCE = 8.0F;
	public static final float MAX_MAX_DISTANCE = 256.0F;

	/** Occupancy threshold for a direction cell to hold a mote: the shader places a mote when the cell's hash
	 *  is UNDER this value, so LOW means dense and 1.0 means empty. Debug value 0.3 fills roughly seven cells
	 *  in ten so the layered field is unmistakably visible. */
	public static final float DEFAULT_OCCUPANCY = 0.3F;

	/** Motes placed per occupied direction cell. In the layered field a couple per cell is already dense; the
	 *  coverage is driven by {@link #DEFAULT_OCCUPANCY}. Debug value 2. */
	public static final int DEFAULT_STRUCTURES_PER_CELL = 2;
	public static final int MIN_STRUCTURES_PER_CELL = 1;
	public static final int MAX_STRUCTURES_PER_CELL = 4;

	/** Angular half-size of one structure, as a fraction of a cell. This is the glow's screen resolution: the
	 *  falloff spans this radius around a point, so a large value renders soft, fat smudges and a small one
	 *  renders sharp, pin-like motes. Pulled down from 0.18 once the user described the lights as blobs — at
	 *  0.07 each point is roughly two pixels across at the middle of the small sphere, reading as a crisp
	 *  distant light rather than a blurred glow. Smaller still approaches single-pixel points.
	 *  At 0.01 the glow is effectively a single hard pixel: the falloff spans well under one pixel, so the
	 *  point is either lit or not, a true pin-prick (and the soft glow that once read as a blob is gone). */
	public static final float DEFAULT_STRUCTURE_RADIUS = 0.01F;

	/** How strongly structure brightness fades with depth; higher values swallow the far field into the void.
	 *  Tuned for the small sphere: this is exp(-falloff) by the reach, so with only 8 cells of march a value
	 *  of 2.2 would drive points to invisible long before the 6-cell view radius. Scaled back so a point is
	 *  still a faint trace (~0.16) at the reach and then the shared domain-edge fade takes it to zero — motes
	 *  still loom out of the dark as you close on them, without going black a third of the way in. */
	public static final float DEFAULT_DISTANCE_FALLOFF = 1.8F;

	/**
	 * The VIEW sphere as a fraction of the COMPUTE sphere — MC's "render distance is not view distance". The
	 * march evaluates all the way out to the compute radius ({@link #DEFAULT_MAX_DISTANCE}), but every content
	 * contribution (structures and haze alike) fades to EXACTLY ZERO by this inner radius, leaving the whole
	 * band between the two spheres pure black. The abyss is observed 360° with no ground or sky to hide behind,
	 * so the compute limit must always sit outside the visible region: keeping a black margin around it means a
	 * moving camera never sees content materialise at the edge. With the compute sphere at 32m (8 cells) this
	 * is set to 0.75 so the view sphere is 24m (6 cells) and the pure-black margin is the remaining 2 cells —
	 * a two-cell buffer the edge fade and any sub-cell jitter can never let a half-computed shape leak through.
	 */
	public static final float DEFAULT_DOMAIN_EDGE = 0.75F;

	/**
	 * The darkest the void ever gets, in its own channels. Not pure black: the window should read as a
	 * dark, living volume rather than as an untextured hole.
	 */
	public static final int DEFAULT_VOID_COLOR = 0x12101A;

	/** The cold, muted tint of the structures and the haze. Purple-grey, so the depth reads violet rather
	 *  than blue or neutral. */
	public static final int DEFAULT_STRUCTURE_COLOR = 0x8E82B8;

	public static final float DEFAULT_BRIGHTNESS = 1.0F;

	/**
	 * Fraction of cells that carry a wisp of haze. The haze is what gives the void its purple-grey-black
	 * depth — faint, per-cell volumetric murk the ray accumulates as it travels — rather than a flat colour.
	 * Higher fills more of the volume; the abyss should keep long stretches of near-black between wisps.
	 */
	public static final float DEFAULT_HAZE_COVERAGE = 0.42F;

	/** How strongly a wisp of haze lights each march step. A per-SAMPLE factor: the haze march takes
	 *  (2 * maxCells + 1) steps, so its accumulated front-to-back alpha depends on both per-step strength and
	 *  how many steps fit in the sphere. The sphere was deliberately shrunk to 8 cells to cut cost (17 steps
	 *  versus the wide field's 65), so this is scaled back up in proportion to keep the murk visually equally
	 *  dense across the small sphere rather than reading thin and empty. DEBUG: pushed bright so the haze is
	 *  unmistakably visible before tuning back to murk. */
	public static final float DEFAULT_HAZE_BRIGHTNESS = 0.55F;

	/** Depth (as a fraction of the ray's reach) over which the haze fades IN right in front of the camera, then
	 *  stays uniform until the shared domain-edge fade takes over. Kept small: a large value made the haze ramp
	 *  up through the mid-field, concentrating it into a bright spherical band that read as a moving chunk
	 *  boundary as the camera advanced. A near-only fade-in plus the far edge fade leaves the haze evenly
	 *  distributed across the whole depth. */
	public static final float DEFAULT_HAZE_DISTANCE = 0.08F;

	/**
	 * Spatial frequency of the haze field, in noise cells per lattice cell. Smaller makes the wisps large and
	 * broad, larger breaks them into fine grain. The haze is a continuous 3-D value-noise field rather than a
	 * per-cell choice, so this scales how big the soft patches read, never their hard edges — there are none.
	 */
	public static final float DEFAULT_HAZE_SCALE = 1.0F;

	/**
	 * How far a structure drifts from where its cell anchors it, as a fraction of a cell.
	 *
	 * <p>Deliberately tiny. The requested motion is "did that just move?" — a slow, sub-perceptual wander — not
	 * visible travel. Periodic (see {@code stirSpeed}) so the daily clock reset cannot be seen.
	 */
	public static final float DEFAULT_STIR_AMPLITUDE = 0.06F;

	/** Turns of the stir cycle per Minecraft day. Slow: even one whole turn a day is already imperceptible. */
	public static final float DEFAULT_STIR_SPEED = 0.5F;

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
	 * the file at {@code assets/abyssfall/shaders/core/abyss.vsh} is known as
	 * {@code abyssfall:core/abyss}. Naming it without the prefix produces "Couldn't find source for VERTEX
	 * shader", the pipeline never compiles, and nothing is drawn.
	 */
	public static final Identifier SHADER =
			Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "core/abyss");

	public static final MapCodec<AbyssEffect> MAP_CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					Identifier.CODEC.fieldOf("mask").forGetter(AbyssEffect::mask),
					Codec.floatRange(MIN_CELL_SIZE, MAX_CELL_SIZE)
							.optionalFieldOf("cell_size", DEFAULT_CELL_SIZE).forGetter(AbyssEffect::cellSize),
					Codec.floatRange(MIN_MAX_DISTANCE, MAX_MAX_DISTANCE)
							.optionalFieldOf("max_distance", DEFAULT_MAX_DISTANCE).forGetter(AbyssEffect::maxDistance),
					Codec.floatRange(0.0F, 1.0F).optionalFieldOf("occupancy", DEFAULT_OCCUPANCY)
							.forGetter(AbyssEffect::occupancy),
					Codec.intRange(MIN_STRUCTURES_PER_CELL, MAX_STRUCTURES_PER_CELL)
							.optionalFieldOf("structures_per_cell", DEFAULT_STRUCTURES_PER_CELL)
							.forGetter(AbyssEffect::structuresPerCell),
					Codec.floatRange(0.01F, 0.9F).optionalFieldOf("structure_radius", DEFAULT_STRUCTURE_RADIUS)
							.forGetter(AbyssEffect::structureRadius),
					Codec.floatRange(0.0F, 8.0F).optionalFieldOf("distance_falloff", DEFAULT_DISTANCE_FALLOFF)
							.forGetter(AbyssEffect::distanceFalloff),
					Codec.floatRange(0.05F, 0.99F).optionalFieldOf("domain_edge", DEFAULT_DOMAIN_EDGE)
							.forGetter(AbyssEffect::domainEdge),
					Codec.intRange(0, 0xFFFFFF).optionalFieldOf("void_color", DEFAULT_VOID_COLOR)
							.forGetter(AbyssEffect::voidColor),
					Codec.intRange(0, 0xFFFFFF).optionalFieldOf("structure_color", DEFAULT_STRUCTURE_COLOR)
							.forGetter(AbyssEffect::structureColor),
					Codec.floatRange(0.0F, 4.0F).optionalFieldOf("brightness", DEFAULT_BRIGHTNESS)
							.forGetter(AbyssEffect::brightness),
					// MapCodec has no optionalFieldOf(name, default); lift it to a Codec first, which does.
					// The partialDispatch caveat from ShaderColorSources does not apply here — this is an
					// ordinary group member, not a type dispatch.
					Haze.CODEC.codec().optionalFieldOf("haze", Haze.DEFAULT)
							.forGetter(AbyssEffect::haze),
					Codec.floatRange(0.0F, 0.5F).optionalFieldOf("stir_amplitude", DEFAULT_STIR_AMPLITUDE)
							.forGetter(AbyssEffect::stirAmplitude),
					Codec.floatRange(0.0F, 20.0F).optionalFieldOf("stir_speed", DEFAULT_STIR_SPEED)
							.forGetter(AbyssEffect::stirSpeed),
					Codec.BOOL.optionalFieldOf("debug_solid", DEFAULT_DEBUG_SOLID)
							.forGetter(AbyssEffect::debugSolid)
			).apply(instance, AbyssEffect::new));

	/**
	 * Registered in {@code AbyssFallShaderCore} rather than here, so that registration order is visible in one
	 * place. No sprites: the abyss is generated, not drawn from artwork.
	 */
	public static final ShaderEffectType<AbyssEffect> TYPE = new ShaderEffectType<>(
			Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "abysseffect"), SHADER, MAP_CODEC);

	/**
	 * An abyss effect with every value left at its default.
	 *
	 * @param mask where the window opens — its red channel is the opacity, so a mask with no red draws nothing
	 */
	public static AbyssEffect of(final Identifier mask) {
		return new AbyssEffect(mask, DEFAULT_CELL_SIZE, DEFAULT_MAX_DISTANCE, DEFAULT_OCCUPANCY,
				DEFAULT_STRUCTURES_PER_CELL, DEFAULT_STRUCTURE_RADIUS, DEFAULT_DISTANCE_FALLOFF, DEFAULT_DOMAIN_EDGE,
				DEFAULT_VOID_COLOR, DEFAULT_STRUCTURE_COLOR, DEFAULT_BRIGHTNESS,
				Haze.DEFAULT, DEFAULT_STIR_AMPLITUDE, DEFAULT_STIR_SPEED, DEFAULT_DEBUG_SOLID);
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

	/**
	 * The abyss is a space the viewer moves through, so the renderer must pack the camera's world position into
	 * the vertices it feeds this kind. This is the switch that tells it to; other effects leave the channel
	 * contents alone.
	 */
	@Override
	public boolean usesViewerPosition() {
		return true;
	}

	@Override
	public Map<String, Float> shaderDefines() {
		// Everything the shader is is stated here; the GLSL holds only the geometry and the marching. Distances
		// are compiled in metres and converted to cell units inside the shader, so changing the cell size alone
		// rescales the whole volume without any other define moving.
		Map<String, Float> defines = new HashMap<>();

		defines.put("ABYSS_CELL_SIZE", this.cellSize);
		defines.put("ABYSS_MAX_CELLS", this.maxDistance / this.cellSize);
		defines.put("ABYSS_OCCUPANCY", this.occupancy);
		defines.put("ABYSS_STRUCTURES_PER_CELL", (float) this.structuresPerCell);
		defines.put("ABYSS_STRUCTURE_RADIUS", this.structureRadius);
		defines.put("ABYSS_HAZE_COVERAGE", this.haze.coverage());
		defines.put("ABYSS_HAZE_BRIGHTNESS", this.haze.brightness());
		defines.put("ABYSS_HAZE_SCALE", this.haze.scale());
		defines.put("ABYSS_STIR_AMPLITUDE", this.stirAmplitude);
		defines.put("ABYSS_STIR_SPEED", this.stirSpeed);
		defines.put("ABYSS_BRIGHTNESS", this.brightness);

		// Layered direction-field constants (the cheap-parallax sky model, see abyss.fsh). DEBUG values pushed
		// to be unmistakably visible — dense layers, strong parallax, bright haze — so the layering, parallax
		// and content can be judged before tuning back down. Not yet exposed in the codec.
		defines.put("ABYSS_LAYERS", 12.0F);
		defines.put("ABYSS_PARALLAX", 0.05F);
		defines.put("ABYSS_PARALLAX_FALLOFF", 2.0F);
		defines.put("ABYSS_LAYER_FREQ_NEAR", 6.0F);
		defines.put("ABYSS_LAYER_FREQ_FAR", 28.0F);
		defines.put("ABYSS_HAZE_DEPTH_SHALLOW", 0.35F);

		defines.put("ABYSS_VOID_R", ((this.voidColor >> 16) & 0xFF) / 255.0F);
		defines.put("ABYSS_VOID_G", ((this.voidColor >> 8) & 0xFF) / 255.0F);
		defines.put("ABYSS_VOID_B", (this.voidColor & 0xFF) / 255.0F);
		defines.put("ABYSS_STRUCT_R", ((this.structureColor >> 16) & 0xFF) / 255.0F);
		defines.put("ABYSS_STRUCT_G", ((this.structureColor >> 8) & 0xFF) / 255.0F);
		defines.put("ABYSS_STRUCT_B", (this.structureColor & 0xFF) / 255.0F);

		return Map.copyOf(defines);
	}

	@Override
	public Set<String> shaderFlags() {
		// Diagnostic only: paints the masked window flat so the whole volume can be bypassed while checking
		// geometry, depth state, mask and blending.
		return this.debugSolid ? Set.of("ABYSS_DEBUG_SOLID") : Set.of();
	}
}
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

package com.abyssfall.client.shader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import com.abyssfall.AbyssFall;
import com.abyssfall.client.mixin.RenderTypeInvoker;
import com.abyssfall.client.render.ShaderSpriteAtlas;
import com.abyssfall.shadercore.ShaderEffect;

/**
 * Turns any {@link ShaderEffect} into a render type, and keeps the ones already built.
 *
 * <h2>Nothing here knows what kinds of effect exist</h2>
 *
 * <p>An effect states its own shader and its own defines; this class only assembles them. A kind of
 * effect added later needs no change here — which is the point, since the alternative is a switch that
 * has to learn about every new appearance.
 *
 * <h2>One pipeline per distinct effect value</h2>
 *
 * <p>Because the tunables are compiled in as defines, two effects differing in any value are genuinely
 * different programs, while two configured identically are the same one. The cache is keyed on the
 * effect itself, which as a record compares by value, so that sharing happens without anyone arranging
 * it.
 *
 * <p>Defines rather than a uniform block because filling a uniform buffer means driving a
 * {@code RenderPass} by hand, and that is not possible from inside {@code submitCustomGeometry} where
 * these are used.
 *
 * <h2>No registration is needed, and none is possible</h2>
 *
 * <p>{@code RenderPipelines.register} is private, but nothing here wants it: pipelines compile on
 * demand, the first draw reaching {@code GpuDevice.getOrCompilePipeline}. Being outside vanilla's static
 * list has one consequence worth knowing — {@code ShaderManager.apply} only pre-compiles
 * {@code getStaticPipelines()} and only reports failures for those. <strong>A failure here is
 * silent</strong>: nothing draws and no exception says why, so an effect that never appears is as likely
 * to be a shader that did not compile as a mask painted wrong.
 *
 * <h2>The bind group layouts are not decoration</h2>
 *
 * <p>Each corresponds to a uniform block the shader imports, and the two sets have to match or the
 * program will not link: {@code GLOBALS} for {@code GameTime}, {@code MATRICES_PROJECTION} for the
 * matrices, {@code SAMPLER0_SAMPLER1_SAMPLER2} for the item texture, the mask and the lightmap.
 *
 * <p>⚠️ The set is the same for every effect, which means an effect shader must declare all three samplers
 * even when it reads only two — the layout and the program have to agree. That is a small cost paid to keep
 * this method from having to know which effects want a lightmap; the alternative is a second layout and a
 * decision here about which effects get it, which is exactly the sort of knowledge this class avoids.
 */
public final class AbyssFallPipelines {
	/**
	 * Effects already built, keyed by value and by the atlas they read.
	 *
	 * <p>The atlas is part of the key because it is bound into the render setup: the same effect over a block
	 * item and over an ordinary item are two render types, since they sample different sheets.
	 *
	 * <p>Not synchronised: rendering happens on one thread, and the worst case if that stopped being true
	 * is a second identical pipeline rather than a corrupt one.
	 */
	private static final Map<CacheKey, RenderType> CACHE = new HashMap<>();

	/**
	 * What distinguishes one built render type from another.
	 */
	private record CacheKey(ShaderEffect effect, Identifier atlas) {
	}

	/**
	 * Depth bias for a layer drawn on the item's own surface.
	 *
	 * <p>Positive because the bias is applied in window-space depth, and in 26.2's reversed depth range
	 * a larger value is nearer. Positive pushes the fragment towards the viewer, which is what makes it
	 * win a {@code GREATER_THAN_OR_EQUAL} comparison against the surface it shares.
	 *
	 * <p>🔴 <strong>An earlier version of this code had them negative</strong>, reasoning that the
	 * reversed range required the sign to be flipped. That was wrong: {@code glPolygonOffset} operates
	 * in window space (the depth buffer), not in NDC, and the sign convention in window space is the
	 * same regardless of the range direction — positive is nearer.
	 *
	 * <p>The magnitudes are the reference implementation's own {@code polygonOffset(-1.0F, -10.0F)},
	 * which were written for a forward depth range. In 26.2's reversed range the same depth offset
	 * in window space is achieved by the same magnitude and a positive sign.
	 */
	private static final float COPLANAR_DEPTH_BIAS_SCALE = 1.0F;
	private static final float COPLANAR_DEPTH_BIAS_CONSTANT = 10.0F;

	private AbyssFallPipelines() {
	}

	/**
	 * The render type for an effect over an item on the given atlas, building it on first use.
	 */
	public static RenderType forEffect(final ShaderEffect effect, final Identifier atlas) {
		return CACHE.computeIfAbsent(new CacheKey(effect, atlas), key -> {
			// Logged because a pipeline failing to compile is silent — ShaderManager only reports failures for
			// vanilla's static list. Seeing this line means the draw path was reached and a render type was
			// built; not seeing it means nothing ever asked for one, which is a different problem entirely.
			AbyssFall.LOGGER.debug("Building render type for {} on atlas {}", key.effect().type().id(),
					key.atlas());

			return create(key);
		});
	}

	/**
	 * Drops every built render type, so the next draw rebuilds from current state.
	 *
	 * <p>Called from {@code ShaderLayerModelPlugin} on every resource reload. A built render type holds a
	 * pipeline whose sprite coordinates were compiled in as constants, and a setup holding texture handles —
	 * both stale once the atlas is restitched.
	 */
	public static void clear() {
		CACHE.clear();
	}

	private static RenderType create(final CacheKey key) {
		ShaderEffect effect = key.effect();
		Identifier shader = effect.type().shader();

		RenderPipeline.Builder builder = RenderPipeline.builder()
				.withLocation(pipelineId(effect))
				.withVertexShader(shader)
				.withFragmentShader(shader)
				.withBindGroupLayout(BindGroupLayouts.GLOBALS)
				.withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
				// Sampler2 is the lightmap, bound by the render setup's useLightmap() — the fragment
				// stage of an effect that wants the item's environment samples it directly, exactly
				// as the reference implementation looked its lightlevel up in the lightmap pixels.
				.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2)
				.withVertexBinding(0, DefaultVertexFormat.ENTITY)
				.withPrimitiveTopology(PrimitiveTopology.QUADS)
				// The depth test depends on whether the effect's geometry sits on the item's own surface or
				// slightly in front of it; the geometry source is what knows, so it is what is asked.
				.withDepthStencilState(depthStateFor(effect))
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				.withCull(false);

		// Whatever the effect says it needs, verbatim. This class does not interpret the names.
		// Values go through the float overload so GLSL always sees a float literal.
		effect.shaderDefines().forEach((name, value) -> builder.withShaderDefine(name, value.floatValue()));
		effect.shaderFlags().forEach(builder::withShaderDefine);

		// Where the effect's own artwork ended up in the atlas. Contributed here rather than by the effect
		// because only the renderer has been told — an effect names sprites, it does not know the packing.
		//
		// These are constants despite describing a texture that animates: 26.2 animates a sprite by drawing the
		// current frame into the sprite's fixed rectangle, so the coordinates are stitched once and the pixels
		// behind them are what change. See ShaderSpriteAtlas.
		addSpriteDefines(builder, effect);

		return RenderTypeInvoker.abyssfall$create(
				AbyssFall.MOD_ID + ":" + effect.type().id().getPath() + "/" + effect.mask(),
				RenderSetup.builder(builder.build())
						// 🔴 Sampler0 is the ITEM's own texture, not the mask. It was bound to the mask and
						// left unread while nothing needed it; a colour derived from what the effect is
						// covering needs the real thing. It is an atlas, so the vertices carry atlas
						// coordinates for it — see ShaderVertex.
						//
						// LINEAR would be wrong here for the same reason it is wrong for the mask: item art is
						// pixel art, and interpolating it invents colours the artist never used, which a
						// derivation would then faithfully amplify.
						.withTexture("Sampler0", key.atlas(),
								() -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
						// Sampler1 stays the mask. NEAREST because the mask is read as data, not as a picture:
						// linear filtering would blend one channel into another along their shared edges and
						// invent pixels that belong to neither effect.
						.withTexture("Sampler1", effect.mask(),
								() -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
						// Sampler2 is the lightmap texture, bound by vanilla because this setup asks for it.
						// LINEAR is what vanilla's own shaders use for the lightmap — it is a gradient, not
						// pixel art, so interpolating between adjacent light levels is the correct read.
						.useLightmap()
						.createRenderSetup());
	}

	/**
	 * Adds one set of {@code SPRITE_n_*} defines per sprite the effect depends on.
	 *
	 * <h2>🔴 The names are generic on purpose</h2>
	 *
	 * <p>They were once {@code STAR_COUNT} and {@code STAR_0_U0}, which put one effect kind's vocabulary into
	 * the class that serves every kind. Any effect drawing from named artwork wants this mechanism, and a
	 * second one would either inherit a name about stars or need this method taught about it. Neither is
	 * acceptable in a class whose whole point is not knowing what kinds exist.
	 *
	 * <p>The naming is positional — {@code SPRITE_0_U0} through {@code SPRITE_n_V1} — because a shader indexing
	 * into an array of constants is what replaces the uniform array a per-frame upload would need. A shader
	 * reading these declares its own accessor and fills it from them.
	 *
	 * <p>An unresolved sprite is skipped with a warning rather than substituted: a zero-sized region would
	 * sample one pixel of whatever is packed at the origin, which looks like a bug in the effect rather than a
	 * missing texture.
	 */
	private static void addSpriteDefines(final RenderPipeline.Builder builder, final ShaderEffect effect) {
		List<Identifier> sprites = effect.spriteDependencies();

		if (sprites.isEmpty()) {
			return;
		}

		// 🔴 The int overload, not the float one. The shader tests this with #if to compile in exactly the
		// branches that have data, and the preprocessor cannot compare "10.0" — a float define here makes every
		// #if fail and every sprite vanish, with no compile error to say why.
		builder.withShaderDefine("SPRITE_COUNT", sprites.size());

		for (int i = 0; i < sprites.size(); i++) {
			Identifier spriteId = sprites.get(i);
			ShaderSpriteAtlas.SpriteBounds bounds = ShaderSpriteAtlas.get(spriteId);

			if (bounds == null) {
				AbyssFall.LOGGER.warn("Shader sprite {} was never resolved; effect {} will be missing one",
						spriteId, effect.type().id());
				continue;
			}

			String prefix = "SPRITE_" + i + "_";

			builder.withShaderDefine(prefix + "U0", bounds.u0());
			builder.withShaderDefine(prefix + "V0", bounds.v0());
			builder.withShaderDefine(prefix + "U1", bounds.u1());
			builder.withShaderDefine(prefix + "V1", bounds.v1());
		}
	}

	/**
	 * The depth state for an effect drawn on or near the item's own surface.
	 *
	 * <p>🔴 Coplanar geometry uses a depth bias rather than a geometric offset. The bias nudges the layer
	 * towards the viewer in <em>depth space</em> rather than in model space, so it wins the comparison at every
	 * scale and from every angle, and no geometry has to be moved.
	 *
	 * <p>Depth is not written. There is nothing to record that the item has not already recorded, and writing
	 * would let this layer occlude something legitimately at the same depth.
	 *
	 * <p>Non-coplanar geometry uses the default depth test — it is already offset and needs no bias.
	 */
	private static DepthStencilState depthStateFor(final ShaderEffect effect) {
		if (!effect.geometry().isCoplanar()) {
			return DepthStencilState.DEFAULT;
		}

		return new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false,
				COPLANAR_DEPTH_BIAS_SCALE, COPLANAR_DEPTH_BIAS_CONSTANT);
	}

	/**
	 * A pipeline location unique to this effect's compiled program.
	 *
	 * <p>Two effects differing only in a define are different programs and must not share a location, or
	 * the device's pipeline cache hands the second one the first one's compilation. The mask path alone is
	 * not enough, since one mask may be used at several settings — so the whole effect is hashed, which
	 * also keeps the result inside what an {@code Identifier} accepts as a path.
	 */
	private static Identifier pipelineId(ShaderEffect effect) {
		return Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID,
				"pipeline/" + effect.type().id().getPath() + "_" + Integer.toHexString(effect.hashCode()));
	}
}
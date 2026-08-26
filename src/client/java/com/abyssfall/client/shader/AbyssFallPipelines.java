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
import java.util.Map;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import com.abyssfall.AbyssFall;
import com.abyssfall.client.mixin.RenderTypeInvoker;
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
 * matrices, {@code SAMPLER0_SAMPLER1} for the item texture and the mask. Every effect shader is expected
 * to import exactly this set; an effect needing something else would need this method to grow an option
 * rather than to be worked around.
 */
public final class AbyssFallPipelines {
	/**
	 * Effects already built, keyed by value.
	 *
	 * <p>Not synchronised: rendering happens on one thread, and the worst case if that stopped being true
	 * is a second identical pipeline rather than a corrupt one.
	 */
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

	private AbyssFallPipelines() {
	}

	/**
	 * The render type for an effect over an item on the given atlas, building it on first use.
	 */
	public static RenderType forEffect(final ShaderEffect effect, final Identifier atlas) {
		return CACHE.computeIfAbsent(new CacheKey(effect, atlas), AbyssFallPipelines::create);
	}

	/**
	 * Drops every built render type, so the next draw rebuilds from current state.
	 *
	 * <p>Exists for a resource reload, after which the textures a render setup resolved are stale.
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
				.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1)
				.withVertexBinding(0, DefaultVertexFormat.ENTITY)
				.withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withDepthStencilState(DepthStencilState.DEFAULT)
				.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				.withCull(false);

		// Whatever the effect says it needs, verbatim. This class does not interpret the names.
		// Values go through the float overload so GLSL always sees a float literal.
		effect.shaderDefines().forEach((name, value) -> builder.withShaderDefine(name, value.floatValue()));
		effect.shaderFlags().forEach(builder::withShaderDefine);

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
						.createRenderSetup());
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

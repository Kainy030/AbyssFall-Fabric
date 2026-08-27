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

package com.abyssfall.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3fc;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

import com.abyssfall.AbyssFall;
import com.abyssfall.client.shader.AbyssFallPipelines;
import com.abyssfall.shadercore.AbyssFallShaderConfig;
import com.abyssfall.shadercore.AbyssFallShaderCore;
import com.abyssfall.shadercore.ShaderEffect;
import com.abyssfall.shadercore.ShaderEffectType;
import com.abyssfall.shadercore.ShaderEffectTypes;
import com.abyssfall.shadercore.ShaderQuad;
import com.abyssfall.shadercore.ShaderVertex;

/**
 * Installs the shader layer on every item, leaving the decision of what to draw to the core.
 *
 * <h2>🔴 Why every item and not only the configured ones</h2>
 *
 * <p>Filtering here would fix the answer at bake time, and the point of the system is that the answer
 * changes: a provider reacting to San may decide at any moment that an ordinary stone pickaxe should look
 * wrong, and an item that was never in the configuration file would have no wrapper to draw through.
 *
 * <p>The wrapper is therefore unconditional and cheap: when nothing claims an item, {@code update} adds no
 * layer and the item renders exactly as it would have. The cost is one delegating call and one map lookup
 * per item drawn.
 *
 * <p>If nothing could ever claim anything — no providers at all — the wrapper is not installed, because then
 * the cost buys nothing.
 *
 * <h2>Why after baking rather than before</h2>
 *
 * <p>What gets wrapped is the finished {@code ItemModel} — the item complete with its texture and
 * transforms. Modifying it earlier would mean rebuilding that work; modifying it here means inheriting it
 * and adding a layer.
 *
 * <h2>Where the display transforms come from</h2>
 *
 * <p>The wrapper needs the item's own rotation, translation and scale so its layer can be posed the same way
 * the item is; see {@link ShaderLayerItemModel} for what goes wrong otherwise. They are read from the model
 * the item resolved to, through {@code ResolvedModel.getTopTransforms}, which walks the parent chain exactly
 * as vanilla does — so a {@code handheld} item inherits {@code handheld}'s values without this class knowing
 * what {@code handheld} is.
 */
public final class ShaderLayerModelPlugin {
	private ShaderLayerModelPlugin() {
	}

	/**
	 * Registers the model modifier. Call during client initialisation, after the core is initialised.
	 */
	public static void initialize() {
		if (!AbyssFallShaderCore.hasAnyProvider()) {
			AbyssFall.LOGGER.info("No shader effect providers; not installing the shader layer");
			return;
		}

		ModelLoadingPlugin.register(context -> {
			// 🔴 This body re-runs on every resource reload, which is what makes it the right place to
			// discard cached state.
			//
			// Fabric's plugin manager calls preparePlugins from the model manager's reload listener and
			// then initialises every registered plugin again — so this is reached once per reload, before
			// anything is baked. Two things must not survive a reload:
			//
			//   - resolved sprite coordinates, because the atlas is stitched afresh and the same sprite may
			//     land somewhere else;
			//   - built render types, because their pipelines compiled those coordinates in as constants and
			//     their setups hold textures that are about to be replaced.
			//
			// Neither failure is loud. Stale coordinates draw the wrong artwork; a stale texture handle draws
			// nothing. Both look like the effect being broken rather than like a reload having happened.
			ShaderSpriteAtlas.clear();
			AbyssFallPipelines.clear();

			context.modifyItemModelAfterBake().register((model, modelContext) ->
					new ShaderLayerItemModel(model, transformsFor(modelContext), modelContext.itemId(),
							geometryFor(modelContext)));
		});

		AbyssFall.LOGGER.debug("Shader layer installed on all item models");
	}

	/**
	 * An item's geometry together with the atlas its texture lives in.
	 *
	 * <p>The two travel as a pair because a shader layer needs both and they come from the same place: the
	 * quads say where to draw, and the atlas says which sheet their coordinates address. Deriving the atlas
	 * later from the quads would work but would repeat the search, and passing them separately invites one to
	 * be updated without the other.
	 *
	 * @param atlas {@code null} when the item baked no geometry, since there is then no sprite to ask and
	 *              nothing to draw. Deliberately not defaulted to the item atlas: {@code TextureAtlas}'s
	 *              location constants are deprecated in 26.2, and inventing a plausible answer for a case
	 *              that never draws would be the only reason to touch them
	 */
	public record ItemGeometry(List<ShaderQuad> quads, @Nullable Identifier atlas) {
		/** Nothing to draw, and therefore no atlas to name. */
		public static final ItemGeometry EMPTY = new ItemGeometry(List.of(), null);
	}

	/**
	 * The item's own baked geometry, reduced to what a shader layer needs.
	 *
	 * <p>🔴 <strong>This is where "an item is not flat" is honoured.</strong> The collection returned by
	 * {@code bakeTopGeometry} contains every face the model produced — for a generated item that is a front
	 * face, a back face, and a per-pixel wall of side faces around the sprite's silhouette. Taking all of them
	 * is what lets an effect cover the item's whole surface instead of one plane of it.
	 *
	 * <p>Baked here rather than read from the render state each frame: geometry belongs to the model, and this
	 * runs once per model rather than once per item per frame.
	 *
	 * <p>Resolved with {@code BlockModelRotation.IDENTITY} because display transforms are applied per layer at
	 * submit time, from {@code ItemTransforms} — baking a rotation in as well would apply it twice.
	 *
	 * <p>Not logged on failure: this runs for every item in the game, and a model without conventional geometry
	 * is ordinary rather than notable. An empty result simply means nothing is drawn for that item.
	 */
	private static ItemGeometry geometryFor(final ModelModifier.AfterBakeItem.Context context) {
		try {
			ModelBaker baker = context.bakingContext().blockModelBaker();
			ResolvedModel resolved = baker.getModel(context.itemId().withPrefix("item/"));

			// Resolve the sprites every registered effect names, while a baker is in hand. Done here because
			// this is the only point at which the atlas is stitched and a baker exists; see ShaderSpriteAtlas
			// for why the answers stay valid afterwards.
			resolveEffectSprites(baker, resolved);

			QuadCollection baked = resolved.bakeTopGeometry(
					resolved.getTopTextureSlots(), baker, BlockModelRotation.IDENTITY);

			List<BakedQuad> quads = baked.getAll();
			List<ShaderQuad> result = new ArrayList<>(quads.size());

			for (BakedQuad quad : quads) {
				result.add(convert(quad));
			}

			return new ItemGeometry(List.copyOf(result), atlasOf(baked));
		} catch (RuntimeException exception) {
			return ItemGeometry.EMPTY;
		}
	}

	/**
	 * Resolves the atlas sprites every registered kind of effect may draw from.
	 *
	 * <h2>🔴 Why registered kinds and not the configuration file</h2>
	 *
	 * <p>Driven from the registry because the configuration file is only one of the places an effect can come
	 * from, and the least interesting one. A provider may hand out any registered kind on any frame — that is
	 * the whole point of {@code ShaderEffectProvider} — and such an effect was never named in a file. Resolving
	 * only what the file mentions is the same mistake as installing the layer only on configured items:
	 * it fixes at bake time an answer that is meant to be decided per frame.
	 *
	 * <p>The consequence of getting it wrong is silent. An unresolved sprite means its {@code SPRITE_n_*}
	 * defines are missing, the shader fails to compile, and a custom pipeline's compilation failure is not
	 * reported anywhere — the effect simply never appears.
	 *
	 * <p>Instances still contribute their own names on top, since an instance may draw from artwork its kind
	 * did not anticipate.
	 *
	 * <p>Runs per item model, which repeats the work — but resolving is a map lookup after the first time, and
	 * the alternative is another hook whose only job is to run once.
	 */
	private static void resolveEffectSprites(final ModelBaker baker, final ResolvedModel resolved) {
		for (ShaderEffectType<?> type : ShaderEffectTypes.all()) {
			for (Identifier spriteId : type.sprites()) {
				ShaderSpriteAtlas.resolve(baker, spriteId, resolved);
			}
		}

		for (ShaderEffect effect : AbyssFallShaderConfig.get().effects().values()) {
			// The mask, unconditionally: every effect has one, and it is a sprite like any other since it
			// stopped being bound as a standalone texture. See ShaderEffect#mask for why that changed.
			ShaderSpriteAtlas.resolve(baker, effect.mask(), resolved);

			for (Identifier spriteId : effect.spriteDependencies()) {
				ShaderSpriteAtlas.resolve(baker, spriteId, resolved);
			}
		}
	}

	/**
	 * One of vanilla's baked quads, as the geometry this system passes around.
	 *
	 * <h2>🔴 Atlas coordinates are converted back to sprite-local ones</h2>
	 *
	 * <p>A baked quad's UVs address the item atlas — a position within one large sheet of every item texture.
	 * Both coordinates are kept: the atlas one for reading the item's own artwork, and a sprite-local
	 * {@code 0..1} one derived from it.
	 *
	 * <p>The local pair is what a mask is read with. It has to exist separately because a mask's artwork is
	 * authored against the item's own texture — pixel {@code (3, 7)} of the mask means pixel {@code (3, 7)} of
	 * the item — and that correspondence is only expressible in coordinates local to the sprite. The useful
	 * consequence is that a mask can be painted by tracing the item.
	 *
	 * <p>⚠️ The mask is itself an atlas sprite now (see {@code ShaderEffect#mask}), so the shader maps this
	 * local pair into the mask's own rectangle before sampling. That mapping belongs to the shader, which knows
	 * the rectangle as a define; nothing here needs to.
	 */
	private static ShaderQuad convert(final BakedQuad quad) {
		TextureAtlasSprite sprite = quad.materialInfo().sprite();

		float spriteWidthInAtlas = sprite.getU1() - sprite.getU0();
		float spriteHeightInAtlas = sprite.getV1() - sprite.getV0();

		// A zero-sized sprite would make this a division by zero. It should not happen for a real texture, but
		// the fallback keeps a broken resource pack from producing infinities in a vertex buffer.
		float uScale = spriteWidthInAtlas != 0.0F ? 1.0F / spriteWidthInAtlas : 0.0F;
		float vScale = spriteHeightInAtlas != 0.0F ? 1.0F / spriteHeightInAtlas : 0.0F;

		return ShaderQuad.of(
				vertexOf(quad.position0(), quad.packedUV0(), sprite, uScale, vScale),
				vertexOf(quad.position1(), quad.packedUV1(), sprite, uScale, vScale),
				vertexOf(quad.position2(), quad.packedUV2(), sprite, uScale, vScale),
				vertexOf(quad.position3(), quad.packedUV3(), sprite, uScale, vScale));
	}

	/**
	 * One corner, carrying both the atlas coordinate it arrived with and the sprite-local one derived from it.
	 */
	private static ShaderVertex vertexOf(final Vector3fc position, final long packedUV,
			final TextureAtlasSprite sprite, final float uScale, final float vScale) {
		float atlasU = UVPair.unpackU(packedUV);
		float atlasV = UVPair.unpackV(packedUV);

		return new ShaderVertex(position.x(), position.y(), position.z(),
				(atlasU - sprite.getU0()) * uScale,
				(atlasV - sprite.getV0()) * vScale,
				atlasU, atlasV);
	}

	/**
	 * Which atlas an item's own texture lives in, so the effect's shader can sample it.
	 *
	 * <p>Read from the geometry rather than assumed: most items are on the item atlas, but a block item's
	 * quads carry block-atlas sprites, and binding the wrong sheet reads the wrong artwork entirely. Asking
	 * the sprite also avoids {@code TextureAtlas}'s location constants, which 26.2 deprecates.
	 *
	 * <p>Taken from the first quad. A model mixing atlases across its faces would be misrepresented here, but
	 * a render type binds one texture per draw, so that case cannot be served by a single layer regardless.
	 */
	private static @Nullable Identifier atlasOf(final QuadCollection geometry) {
		List<BakedQuad> quads = geometry.getAll();

		return quads.isEmpty() ? null : quads.getFirst().materialInfo().sprite().atlasLocation();
	}

	/**
	 * The display transforms of the model this item resolved to.
	 *
	 * <p>Derived from the item id by vanilla's own convention — {@code namespace:item/path} — rather than
	 * configured, because that is the model an item's client entry points at unless it was written to do
	 * otherwise. An item that does point elsewhere still gets a layer; only the posing falls back, and
	 * {@code NO_TRANSFORMS} is what a model without a {@code display} block would have given anyway.
	 *
	 * <p>Not logged on failure: this now runs for every item in the game, and an absent model is ordinary
	 * rather than notable.
	 */
	private static ItemTransforms transformsFor(final ModelModifier.AfterBakeItem.Context context) {
		try {
			return context.bakingContext()
					.blockModelBaker()
					.getModel(context.itemId().withPrefix("item/"))
					.getTopTransforms();
		} catch (RuntimeException exception) {
			return ItemTransforms.NO_TRANSFORMS;
		}
	}
}

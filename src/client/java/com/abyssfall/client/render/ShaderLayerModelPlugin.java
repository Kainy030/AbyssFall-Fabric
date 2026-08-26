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
import com.abyssfall.shadercore.AbyssFallShaderCore;
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

		ModelLoadingPlugin.register(context -> context.modifyItemModelAfterBake().register((model, modelContext) ->
				new ShaderLayerItemModel(model, transformsFor(modelContext), modelContext.itemId(),
						geometryFor(modelContext))));

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
	 * One of vanilla's baked quads, as the geometry this system passes around.
	 *
	 * <h2>🔴 Atlas coordinates are converted back to sprite-local ones</h2>
	 *
	 * <p>A baked quad's UVs address the item atlas — a position within one large sheet of every item texture.
	 * A mask is not in that atlas; it is a standalone texture bound directly, and reads {@code 0..1} across its
	 * own width. Handing atlas coordinates to a mask sampler would read whatever else happened to be packed at
	 * those coordinates, which is to say a different item's artwork.
	 *
	 * <p>So each UV is mapped back through its own sprite's extent, giving the position within the item's own
	 * texture. The consequence is the useful one: mask artwork lines up with item artwork pixel for pixel, so a
	 * mask can be painted by tracing the item.
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

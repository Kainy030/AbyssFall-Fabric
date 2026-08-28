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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;

import com.abyssfall.shadercore.geometry.ItemHullGeometry;

/**
 * One way of drawing over an item, and the values that particular way needs.
 *
 * <h2>Why this is an interface and not a record with more fields</h2>
 *
 * <p>The mod is meant to end up putting several unrelated appearances on items — a starfield, a
 * wrongness that grows as San falls, whatever a later idea asks for. Those do not share a set of
 * numbers: a scanline has a direction and a speed, a starfield has a depth and a density, and forcing
 * them into one record would give every effect every other effect's fields and leave most of them
 * meaningless.
 *
 * <p>So an effect is whatever its own type says it is. Each implementation is a record with only its
 * own values, carries its own {@link MapCodec}, and states which GLSL program draws it. Adding a kind
 * of effect is writing one record and one shader and registering the pair; nothing existing is
 * touched, and nothing here needs to know the new kind exists.
 *
 * <h2>Effects are values</h2>
 *
 * <p>Implementations must be records, or otherwise compare by value. Two items configured identically
 * are meant to share one compiled pipeline, and that sharing is keyed on the effect itself.
 *
 * @see ShaderEffectTypes for the registry, and for what registering costs
 */
public interface ShaderEffect {
	/**
	 * The type that produced this effect, which is also what serialises it back.
	 */
	ShaderEffectType<?> type();

	/**
	 * The mask sprite, whose channels decide which fragments this effect may cover.
	 *
	 * <p>Every effect has one: an effect that covered an item edge to edge would have no way to leave
	 * the item recognisable. What the individual channels mean is the effect's own business.
	 *
	 * <h2>🔴 This is an atlas sprite name, not a texture path</h2>
	 *
	 * <p>So {@code abyssfall:item/my_mask} rather than {@code abyssfall:textures/item/my_mask.png} — the same
	 * form {@link #spriteDependencies()} uses, and for the same reason.
	 *
	 * <p>It was a standalone texture path once, and that quietly cost the mask its animation. In 26.2 only
	 * {@code TextureAtlas} implements {@code TickableTexture}, so a mask bound as its own texture sits on frame
	 * zero forever no matter what its {@code .mcmeta} says — the file is never even read. A mask in the atlas
	 * animates, because the atlas ticks and vanilla blits the current frame into the sprite's fixed rectangle.
	 *
	 * <p>Nothing had to be added to make this possible: vanilla's own {@code items.json} already stitches
	 * {@code item/}, and same-named atlas definitions across packs are <em>concatenated</em> rather than
	 * overridden ({@code SpriteSourceList.load} walks the whole resource stack). A mask under
	 * {@code textures/item/} was therefore in the atlas the entire time; the old path form simply declined to
	 * look for it there.
	 *
	 * <p>⚠️ Because the sprite's place in the atlas depends on the resource pack, <strong>the coordinates are
	 * not part of this name and must not be</strong>. The renderer resolves them at bake time and contributes
	 * them as defines, exactly as it does for {@link #spriteDependencies()}.
	 */
	Identifier mask();

	/**
	 * What surface this effect is drawn on.
	 *
	 * <p>Defaults to following the item's own hull, which is what an effect meant to make an ordinary object
	 * look wrong almost always wants: the item keeps its shape and its thickness, and the effect covers all
	 * of it. See {@link ShaderGeometrySource} for why this is answerable per effect rather than fixed — an
	 * effect that projects rather than coats, a starfield being the case already in mind, has an entirely
	 * different answer.
	 */
	default ShaderGeometrySource geometry() {
		return ItemHullGeometry.INSTANCE;
	}

	/**
	 * Names of atlas sprites this effect needs resolved before it can be drawn.
	 *
	 * <p>Defaults to whatever its {@linkplain #type() kind} declares, which is the right answer for almost
	 * every effect: artwork belongs to a kind of appearance rather than to one instance of it, and stating it
	 * on the kind is what lets a provider hand out an instance nobody configured. See
	 * {@link ShaderEffectType} for why that matters.
	 *
	 * <p>An instance may still narrow or extend the list — a starfield configured to draw from three sprites
	 * rather than ten, say. <strong>The effect names sprites; it does not know where they are.</strong> Where
	 * the atlas stitcher put them depends on the resource pack, so a coordinate has no place in an effect's
	 * identity or in a configuration file — the renderer contributes those as additional defines.
	 *
	 * <p>⚠️ The {@linkplain #mask() mask} is <em>not</em> listed here and must not be. It is resolved
	 * unconditionally by the renderer, since every effect has one and the shader reads it through its own
	 * {@code MASK_*} defines rather than through the positional {@code SPRITE_n_*} chain. Listing it would give
	 * it a second, meaningless index.
	 *
	 * <p>This is deliberately a list of names rather than anything richer. An effect stating a dependency is not
	 * the same as an effect knowing how texture atlases work.
	 */
	default List<Identifier> spriteDependencies() {
		return this.type().sprites();
	}

	/**
	 * Values to compile into the shader, as {@code #define NAME value} pairs.
	 *
	 * <p>Defines rather than uniforms because a uniform buffer has to be filled by driving a
	 * {@code RenderPass} by hand, which is not possible where these are drawn. The consequence is that
	 * two effects differing in any value here are different programs — which is why this map is also
	 * part of what identifies a pipeline.
	 *
	 * <p>Float rather than a formatted string: the pipeline builder takes {@code int} or {@code float}
	 * and nothing else, and letting it do the formatting avoids the classic GLSL trap where a value
	 * written as {@code 1} is read as an integer and turns a division into integer division.
	 */
	Map<String, Float> shaderDefines();

	/**
	 * Names of {@code #define} flags to set, for parts of the shader guarded by {@code #ifdef}.
	 *
	 * <p>Separate from {@link #shaderDefines()} because a flag's presence is the whole of its meaning,
	 * and a shader can leave a branch out of the compiled program entirely rather than testing a value
	 * at runtime.
	 */
	default Set<String> shaderFlags() {
		return Set.of();
	}

	/**
	 * Whether this kind needs the camera's world position packed into the vertices it draws, not just the
	 * viewing angles.
	 *
	 * <p>Most effects do not. The two are different quantities: the angles ({@code yaw}/{@code pitch}) rotate
	 * every ray and describe where the viewer is <em>looking</em>; the position describes where the viewer
	 * <em>is</em>. An appearance painted onto the item — a glow, a pulse, a sky projected infinitely far away —
	 * reads correctly from the angles alone, because translating the camera changes nothing about an
	 * infinitely distant surface. An appearance that occupies a real 3-D volume, by contrast, only gains
	 * parallax from translation, and without a position the near parts cannot slide past the far parts.
	 *
	 * <p>Effects that answer {@code true} receive, in their vertex stream, a folded camera position (see
	 * {@code ViewerState}); effects that answer {@code false} leave those channels holding whatever they
	 * always did, and their shaders must not read the position. Default is {@code false} so existing kinds are
	 * untouched.
	 */
	default boolean usesViewerPosition() {
		return false;
	}
}

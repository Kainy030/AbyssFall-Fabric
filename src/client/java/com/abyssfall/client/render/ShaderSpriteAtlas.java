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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import com.abyssfall.AbyssFall;

/**
 * Where in the atlas a named sprite ended up.
 *
 * <h2>Why this exists at all</h2>
 *
 * <p>An effect can name artwork — "draw stars from these ten sprites" — but it cannot know where the atlas
 * stitcher put them, and it must not: the packing depends on the resource pack, and a value that changes with
 * the resource pack has no business being in a configuration file or in an effect's identity.
 *
 * <p>So the effect names sprites and this resolves them, once, while models are being baked. That is the only
 * moment both facts are available: the atlas has been stitched, and a {@code ModelBaker} is in hand.
 *
 * <h2>🔴 Why the answers can be treated as constants</h2>
 *
 * <p>A sprite's coordinates never change once the atlas is stitched — not even for an animated sprite. 26.2
 * animates by <em>drawing the current frame into the sprite's fixed rectangle</em>: every unique frame becomes
 * its own small GPU texture at load, and each tick blits the current one to the same place, using a transform
 * built from the sprite's own {@code x}/{@code y}. The sprite's {@code u0}/{@code v0}/{@code u1}/{@code v1} are
 * {@code final} fields, assigned once.
 *
 * <p>That single fact is what makes this whole approach possible. The reference implementation this effect is
 * ported from had to re-upload its sprite coordinates as a uniform <strong>every frame</strong>, because 1.12
 * animated a sprite by moving its UVs instead. On this draw path a per-frame uniform is impossible — so had
 * 26.2 kept that behaviour, the effect could not have been ported at all.
 *
 * <p>⇒ Coordinates resolved here go into the shader as compile-time constants, and animation still works,
 * because the pixels behind those constants are what change.
 *
 * <h2>Lifetime</h2>
 *
 * <p>Cleared when models are rebaked, since a new atlas may pack the same sprites elsewhere. Anything holding a
 * pipeline built from the old values has to be dropped at the same time — see {@code AbyssFallPipelines.clear}.
 */
public final class ShaderSpriteAtlas {
	/**
	 * Sprite id to its place in the atlas.
	 *
	 * <p>🔴 Concurrent because baking is parallel. The log shows two dozen {@code Worker-Main} threads resolving
	 * these at once — a plain {@code HashMap} here is a data race, and the failure mode of a racing
	 * {@code HashMap} is a corrupted table rather than a lost entry.
	 *
	 * <p>Resolving the same sprite on several threads is harmless: the answer is a pure function of the stitched
	 * atlas, so whichever write lands is the same value.
	 */
	private static final Map<Identifier, SpriteBounds> BOUNDS = new ConcurrentHashMap<>();

	private ShaderSpriteAtlas() {
	}

	/**
	 * A sprite's extent within its atlas, in the {@code 0..1} coordinates a sampler takes.
	 *
	 * @param atlas which sheet it is on, since an effect reading it must bind that sheet
	 */
	public record SpriteBounds(Identifier atlas, float u0, float v0, float u1, float v1) {
	}

	/**
	 * Resolves a sprite and remembers where it is, returning {@code null} if it could not be resolved.
	 *
	 * <p>Called during baking. Resolving the same sprite twice is harmless and cheap — the baker interns its
	 * materials — so this does not attempt to avoid it.
	 */
	public static @Nullable SpriteBounds resolve(final ModelBaker baker, final Identifier spriteId,
			final ModelDebugName debugName) {
		SpriteBounds known = BOUNDS.get(spriteId);

		if (known != null) {
			return known;
		}

		TextureAtlasSprite sprite = baker.materials().get(new Material(spriteId), debugName).sprite();

		// A missing sprite resolves to the "missing texture" checkerboard rather than to null, so the way to
		// detect a typo is that the name we asked for is not the name we got back.
		if (!spriteId.equals(sprite.contents().name())) {
			AbyssFall.LOGGER.warn("Shader sprite {} is missing; got {} instead", spriteId,
					sprite.contents().name());
			return null;
		}

		SpriteBounds bounds = new SpriteBounds(sprite.atlasLocation(),
				sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());

		BOUNDS.put(spriteId, bounds);

		AbyssFall.LOGGER.debug("Shader sprite {} resolved to {} at u {}..{} v {}..{}",
				spriteId, bounds.atlas(), bounds.u0(), bounds.u1(), bounds.v0(), bounds.v1());

		return bounds;
	}

	/**
	 * Where a sprite resolved to earlier, or {@code null} if it was never resolved.
	 */
	public static @Nullable SpriteBounds get(final Identifier spriteId) {
		return BOUNDS.get(spriteId);
	}

	/**
	 * Forgets every resolved sprite. Call when the atlas is about to be rebuilt.
	 */
	public static void clear() {
		BOUNDS.clear();
	}
}
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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.abyssfall.AbyssFall;
import com.abyssfall.shadercore.effect.MaskedPulseEffect;
import com.abyssfall.shadercore.effect.StarfieldEffect;

/**
 * The second core system: what items look like, as opposed to what the world does.
 *
 * <p>{@code AbyssFallCoreSystem} owns San, which is the rule the mod is built on. This owns the other
 * half of that idea — the part the player sees. Its job is to answer one question, on demand, for any
 * item being drawn: <em>is there something over this, and what</em>. Everything else here exists to
 * keep that question answerable by parties that do not know about each other.
 *
 * <h2>🔴 This is not the Final Death Omen's renderer</h2>
 *
 * <p>The blade was the first consumer and is still the only one shipped, but nothing in this package
 * mentions it. An item gains an appearance by a provider claiming it, and the configuration file is
 * itself only a provider. Do not add item-specific behaviour here; add a provider, or a kind of effect.
 *
 * <h2>Two axes of extension, deliberately separate</h2>
 *
 * <ul>
 *   <li><strong>Kinds of effect</strong> — {@link ShaderEffectTypes}. A new appearance means a new
 *       record and a new GLSL program. Nothing existing changes.</li>
 *   <li><strong>Sources of decision</strong> — {@link #addProvider}. A new reason for an item to look
 *       different means a new provider. It may return any registered kind.</li>
 * </ul>
 *
 * <p>Keeping them apart is what makes "as San falls, ordinary things look wrong" a provider rather than
 * a rewrite: such a provider decides <em>when</em> and <em>how much</em>, and reuses whatever kinds of
 * effect already exist to say <em>what</em>.
 *
 * <h2>Priority is registration order, reversed</h2>
 *
 * <p>The most recently added provider is asked first, and the first non-null answer wins. Later
 * registrations therefore override earlier ones, which puts the configuration file — registered during
 * setup, before anything reactive — at the bottom where a file of static preferences belongs.
 *
 * <p>No provider can see what another returned. A provider that wants to defer returns {@code null}.
 */
public final class AbyssFallShaderCore {
	/**
	 * Providers in registration order; consulted from the end.
	 *
	 * <p>An {@code ArrayList} walked backwards by index rather than anything fancier: this is on the
	 * render path, called once per item drawn, and an iterator per call is an allocation per item.
	 */
	private static final List<ShaderEffectProvider> PROVIDERS = new ArrayList<>();

	private AbyssFallShaderCore() {
	}

	/**
	 * Registers the built-in effect kinds and the configuration-backed provider.
	 *
	 * <p>Must run before the configuration file is read, since reading it needs the types to exist.
	 */
	public static void initialize() {
		ShaderEffectTypes.register(MaskedPulseEffect.TYPE);
		ShaderEffectTypes.register(StarfieldEffect.TYPE);

		// Lowest priority, registered first: a file of stated preferences should lose to anything that
		// is reacting to what is happening in the game.
		addProvider(ShaderConfigProvider.INSTANCE);

		AbyssFall.LOGGER.debug("Shader core ready with {} effect type(s)",
				ShaderEffectTypes.all().size());
	}

	/**
	 * Adds a provider, giving it priority over every provider already registered.
	 */
	public static void addProvider(ShaderEffectProvider provider) {
		PROVIDERS.add(provider);
	}

	/**
	 * The effect to draw over a stack right now, or {@code null} for none.
	 *
	 * <p>Called once per item per frame. Providers are asked from the most recently registered backwards
	 * and the first non-null answer is returned.
	 */
	public static @Nullable ShaderEffect effectFor(ItemStack stack, ShaderRenderContext context) {
		for (int i = PROVIDERS.size() - 1; i >= 0; i--) {
			ShaderEffect effect = PROVIDERS.get(i).effectFor(stack, context);

			if (effect != null) {
				return effect;
			}
		}

		return null;
	}

	/**
	 * Whether anything at all could produce an effect.
	 *
	 * <p>Consulted once, when deciding whether to install the renderer. A provider added later still
	 * works — this only avoids the cost of wrapping every item model in a build that will never draw
	 * anything.
	 */
	public static boolean hasAnyProvider() {
		return !PROVIDERS.isEmpty();
	}
}

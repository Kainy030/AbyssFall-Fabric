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

package com.abyssfall.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import com.abyssfall.AbyssFall;

/**
 * Registry holder for the mod's mob effects.
 */
public final class AbyssFallEffects {
	public static final Holder<MobEffect> ABYSS_EXPLORER = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			AbyssFall.id("abyss_explorer"),
			new AbyssExplorerEffect()
	);

	public static final Holder<MobEffect> SAN_BREAKDOWN = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			AbyssFall.id("san_breakdown"),
			new SanBreakdownEffect()
	);

	public static final Holder<MobEffect> SAN_SPIRITED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			AbyssFall.id("san_spirited"),
			new SanSpiritedEffect()
	);

	private AbyssFallEffects() {
	}

	public static void initialize() {
		// Registration happens in the static initialiser above; this call exists so the
		// class is loaded from the mod initializer.
	}
}

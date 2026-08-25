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

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import com.abyssfall.AbyssFall;
import com.abyssfall.shadercore.effect.MaskedPulseEffect;

/**
 * The contents of {@code AbyssFallShader.json}: which items have a stated appearance.
 *
 * <h2>Keyed by item id, open as to kind</h2>
 *
 * <p>Each entry names its own {@code type}, and the rest of the entry is read by that type's codec. A
 * kind of effect added later is usable in this file without the file format changing.
 *
 * <p>An id rather than a model or texture name, because the id is the one name for an item that cannot
 * be shared or renamed out from under the entry. Any namespace is accepted — nothing in the renderer
 * requires the item to be one of this mod's.
 *
 * <p><strong>An unknown id is not an error.</strong> Entries for items that do not exist are never
 * consulted, which is what lets one file serve a setup where some add-on is absent.
 *
 * @param effects item id to the effect stated for it
 */
public record ShaderConfigData(Map<Identifier, ShaderEffect> effects) {
	/**
	 * A fresh install: the blade, with everything at its defaults.
	 *
	 * <p>Written out as an ordinary entry rather than special-cased, so the shipped file is an example
	 * of the format as much as it is a setting.
	 */
	public static final ShaderConfigData DEFAULT = new ShaderConfigData(Map.of(
			Identifier.fromNamespaceAndPath(AbyssFall.MOD_ID, "final_death_omen"),
			MaskedPulseEffect.of(Identifier.fromNamespaceAndPath(
					AbyssFall.MOD_ID, "textures/item/final_death_omen_mask.png"))));

	public static final Codec<ShaderConfigData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Identifier.CODEC, ShaderEffectTypes.CODEC)
					.optionalFieldOf("effects", Map.of())
					.forGetter(ShaderConfigData::effects)
	).apply(instance, ShaderConfigData::new));

	/**
	 * The effect stated for an item, or {@code null} if none is.
	 */
	public @Nullable ShaderEffect get(Identifier itemId) {
		return this.effects.get(itemId);
	}

	/**
	 * Whether the file states nothing at all, which is a legitimate way to turn it off.
	 */
	public boolean isEmpty() {
		return this.effects.isEmpty();
	}
}

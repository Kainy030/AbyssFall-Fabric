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

import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;

/**
 * A kind of shader effect: how to read one from configuration, and which program draws it.
 *
 * <p>One instance per kind, registered once in {@link ShaderEffectTypes}. The {@code id} is what
 * appears as {@code "type"} in the configuration file, and the {@code shader} is the name under
 * {@code assets/<namespace>/shaders/core} of the vertex and fragment pair — both stages share the
 * name, as vanilla's own do.
 *
 * @param id     name used in configuration, and the key this type is registered under
 * @param shader shader program that draws effects of this kind
 * @param codec  reads and writes the values this kind needs
 * @param <T>    the effect record this type produces
 */
public record ShaderEffectType<T extends ShaderEffect>(Identifier id, Identifier shader,
		MapCodec<T> codec) {
}

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

package com.abyssfall.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Opens {@code RenderType.create} so this mod can build a render type around its own shader.
 *
 * <h2>Why this cannot be done without a mixin</h2>
 *
 * <p>Everything else on the path is public and was verified as such: {@code RenderPipeline.builder()}
 * accepts an {@code Identifier} for the vertex and fragment shader, so a shader shipped under our
 * own namespace can be named; {@code RenderSetup.builder(RenderPipeline)} and every method on the
 * returned builder are public; {@code SubmitNodeCollector.submitCustomGeometry} takes an arbitrary
 * render type. The single closed link is the step in the middle — turning a finished
 * {@code RenderSetup} into a {@code RenderType}:
 *
 * <pre>{@code static RenderType create(String name, RenderSetup state)}</pre>
 *
 * <p>It is package-private and static, and the whole {@code RenderTypes} catalogue of public
 * factories only ever produces vanilla's own pipelines. There is no public way to pass a pipeline
 * of ours through it. Fabric API does not fill the gap either: the {@code api} packages of
 * {@code fabric-rendering-v1} and {@code fabric-renderer-api-v1} were both searched, and the only
 * pipeline-related type there is {@code FabricRenderPipeline}, which just adds a GUI draw-mode
 * flag and cannot construct a render type.
 *
 * <p>The alternative was declaring a class in {@code net.minecraft.client.renderer.rendertype} to
 * borrow package access. That works, but it puts one of our files inside a vanilla package, and a
 * three-line invoker is a smaller intrusion than that.
 *
 * <h2>Verified against</h2>
 *
 * <p><strong>Minecraft 26.2.</strong> The target descriptor was read from the class file itself:
 *
 * <pre>{@code (Ljava/lang/String;Lnet/minecraft/client/renderer/rendertype/RenderSetup;)Lnet/minecraft/client/renderer/rendertype/RenderType;}</pre>
 *
 * <p>There is exactly one {@code RenderType} class in the jar, so the target is unambiguous. The
 * mixin config sets {@code injectors.defaultRequire = 1}: should this method be renamed or made
 * public in a later version, the launch fails immediately rather than leaving a render type that
 * silently never draws.
 */
@Mixin(RenderType.class)
public interface RenderTypeInvoker {
	/**
	 * Calls vanilla's own factory; no behaviour is added or changed.
	 *
	 * @param name  label used in {@code toString} and in profiler output
	 * @param state the finished pipeline and texture configuration
	 * @return the render type vanilla would have built for the same arguments
	 */
	@Invoker("create")
	static RenderType abyssfall$create(String name, RenderSetup state) {
		throw new AssertionError("Replaced by the mixin processor");
	}
}

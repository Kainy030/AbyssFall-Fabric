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

package com.abyssfall.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abyssfall.item.AbyssFallItemTags;

/**
 * Lets an Abyssdium tool dig where the game says digging is impossible.
 *
 * <h2>The gate this lifts, and why here</h2>
 *
 * <p>A block with a destroy speed of −1 — bedrock, barriers, command blocks, portal frames —
 * is not merely hard to mine, it is <em>refused</em>: {@code getDestroyProgress} returns zero
 * for it before the tool is ever consulted. That one line is the whole of the "unmineable"
 * mechanic, and everything routes through it: the client's crack animation and the server's
 * break logic ({@code MultiPlayerGameMode}, {@code ServerPlayerGameMode}) both call this one
 * method, so this one injection covers both sides.
 *
 * <p>A mixin because there is no earlier seam: Fabric's interaction events
 * ({@code fabric-events-interaction-v0}, whose API package was checked class by class) offer
 * callbacks before and after a break, but nothing that can answer "how fast may this block
 * be dug" — the question is never asked of anyone but this method.
 *
 * <h2>What this deliberately does not do</h2>
 *
 * <p>It changes speed, and nothing else, and only for the forgings that dig: the question is
 * answered by {@link AbyssFallItemTags#digsFromAbyss} — an Abyssdium forging whose class
 * digs by vanilla's own tags, which no sword joins, the Final Death Omen included, because
 * a sword is not a mining tool. Whether a break drops
 * anything stays where vanilla keeps it: blocks with no loot table drop nothing, and every
 * refused block in vanilla has none — bedrock's own drop exists because
 * {@code AbyssFallBedrockDrops} adds it, by event, not by injection. The speed also still
 * comes from the tool itself: the material's {@code MAX_VALUE} speed lands through mining
 * rules, which digging tools have and swords do not.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockDestroyProgressMixin {
	/**
	 * The hardness a refused block is treated as while an Abyssdium digging tool is asking:
	 * obsidian's 50. How long the dig then takes is the tool's own business — with the
	 * material's {@code MAX_VALUE} speed behind a mining rule, the answer is instant.
	 */
	private static final float UNMINEABLE_STANDIN_HARDNESS = 50.0F;

	/**
	 * Recomputes the progress for a refused block when the digger holds Abyssdium, using the
	 * stand-in hardness and vanilla's own correct-tool modifier. Every other block, and every
	 * other tool, is left exactly as vanilla wrote it.
	 */
	@Inject(method = "getDestroyProgress(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F", at = @At("HEAD"), cancellable = true)
	private void abyssfall$abyssdiumDigsTheRefused(BlockState state, Player player, BlockGetter level,
			BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (state.getDestroySpeed(level, pos) != -1.0F
				|| !AbyssFallItemTags.digsFromAbyss(player.getMainHandItem())) {
			return;
		}

		int modifier = player.hasCorrectToolForDrops(state) ? 30 : 100;
		cir.setReturnValue(player.getDestroySpeed(state) / UNMINEABLE_STANDIN_HARDNESS / modifier);
	}
}

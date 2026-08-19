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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.WitherRoseBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.abyssfall.block.AbyssDirtBlock;

/**
 * Allows wither roses to be planted on abyss dirt.
 *
 * <p>A mixin is genuinely required here. {@code WitherRoseBlock#mayPlaceOn} widens the
 * inherited check with three hard-coded block comparisons:
 *
 * <pre>{@code
 * super.mayPlaceOn(...) || state.is(Blocks.NETHERRACK)
 *                       || state.is(Blocks.SOUL_SAND)
 *                       || state.is(Blocks.SOUL_SOIL)
 * }</pre>
 *
 * <p>There is no tag or registry behind those comparisons, so a modded block cannot opt in
 * through data. The inherited check does use {@code BlockTags.DIRT}, but joining that tag
 * would additionally let every other plant, sugar cane and melon stem grow on the block and
 * would let moss replace it, which is far broader than intended.
 *
 * <p>Injecting at {@code RETURN} and only ever flipping {@code false} to {@code true} keeps
 * the change additive: vanilla placements behave exactly as before, and other mods touching
 * the same method still see their own results honoured.
 */
@Mixin(WitherRoseBlock.class)
public class WitherRoseBlockMixin {
	@Inject(method = "mayPlaceOn", at = @At("RETURN"), cancellable = true)
	private void abyssfall$allowAbyssDirt(BlockState state, BlockGetter level, BlockPos pos,
			CallbackInfoReturnable<Boolean> callback) {
		if (!callback.getReturnValueZ() && state.getBlock() instanceof AbyssDirtBlock) {
			callback.setReturnValue(true);
		}
	}
}

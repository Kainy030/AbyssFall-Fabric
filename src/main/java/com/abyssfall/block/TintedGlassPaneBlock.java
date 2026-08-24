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

package com.abyssfall.block;

import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tinted Glass Pane — the pane vanilla never made.
 *
 * <p>Vanilla has tinted glass as a full block and a pane for every other kind of glass, but no
 * tinted pane. This is that missing piece, and it is deliberately nothing more: the same shape and
 * connection behaviour as any other pane, the same texture as tinted glass, and the same refusal to
 * let light through.
 *
 * <h2>Why it extends {@link IronBarsBlock}</h2>
 *
 * <p>{@code IronBarsBlock} is what vanilla's own {@code GLASS_PANE} is, and it carries everything a
 * pane needs: the four connection properties, the cross-shaped collision, connecting to its own kind
 * and to walls, and the culling rules that stop adjacent panes drawing a seam between them.
 * Reimplementing any of that would only be a chance to get it subtly wrong.
 *
 * <p>A subclass is required rather than a bare {@code IronBarsBlock}, for two reasons. Its
 * constructor is {@code protected}, so it cannot be referenced as {@code IronBarsBlock::new} from
 * outside its package at all; and the light behaviour below has to be overridden somewhere.
 *
 * <h2>How the light blocking works</h2>
 *
 * <p>Copied from {@code TintedGlassBlock}, which is the only place in vanilla that does this, and
 * which does it with exactly these two methods:
 *
 * <ul>
 *   <li>{@link #propagatesSkylightDown} returning {@code false} stops daylight passing straight
 *       down through the block, which is what makes tinted glass unable to light a room from above.
 *   <li>{@link #getLightDampening} returning {@code 15} costs light the full amount it can lose in
 *       one step, so no block light gets through either.
 * </ul>
 *
 * <p><strong>Unverified:</strong> whether a pane — which occupies a two pixel slice rather than a
 * full cube — is treated by the lighting engine exactly as the full block is. The two methods are
 * declared on {@code BlockBehaviour} and so apply to any block, but vanilla has no thin block that
 * dampens light, so there is no precedent to compare against and this has not been tested in game.
 * If it turns out light still leaks through, the fix belongs here rather than anywhere else.
 */
public class TintedGlassPaneBlock extends IronBarsBlock {
	public TintedGlassPaneBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return false;
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 15;
	}
}

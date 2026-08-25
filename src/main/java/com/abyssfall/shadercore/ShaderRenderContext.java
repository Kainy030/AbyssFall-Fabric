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

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Where and for whom an item is being drawn.
 *
 * <p>Passed to every {@link ShaderEffectProvider} so a decision can depend on the situation rather than
 * only on the stack. An effect that would be distracting in an inventory but right in the hand, or one
 * that should only appear on the item its owner is holding, needs this to say so.
 *
 * <p>The viewer's San is not on this record. A provider that wants it reads it from the client's own
 * player, which is the only San a client can legitimately know; putting it here would suggest the
 * renderer knows whose item it is drawing, and in an inventory or on the ground it does not.
 *
 * @param itemId  the item being drawn
 * @param display where it is being drawn — in a GUI, in a hand, on the ground, in a frame
 */
public record ShaderRenderContext(Identifier itemId, ItemDisplayContext display) {
	/**
	 * Whether this is one of the two first-person hand positions.
	 *
	 * <p>Offered because "only while I am holding it" is the distinction most likely to be wanted, and
	 * because getting it wrong by forgetting the left hand is easy.
	 */
	public boolean isFirstPersonHand() {
		return this.display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				|| this.display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
	}

	/**
	 * Whether this is an inventory slot or another flat, framed presentation.
	 */
	public boolean isGui() {
		return this.display == ItemDisplayContext.GUI;
	}
}

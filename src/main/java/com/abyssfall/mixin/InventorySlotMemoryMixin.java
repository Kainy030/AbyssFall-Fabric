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

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abyssfall.itemframework.SlotMemoryAccess;

/**
 * A one-entry memory of what the inventory last gave up: the slot {@code removeItem}
 * served, and the very object it returned.
 *
 * <p>Every drop that leaves the inventory goes through {@code removeItem(int, int)} first —
 * the Q key reaches it through {@code removeFromSelected}, a container throw through
 * {@code Slot.remove} — and the removed object is then handed to {@code Player.drop}. By
 * remembering the pair, {@code ServerPlayerDropMixin} can tell, at drop time and without
 * guessing, which slot the dropped stack just left. The match is by reference, never by
 * item equality: the answer decides which slot gets <em>overwritten</em> on the void's
 * return, and a wrong answer would destroy an innocent occupant.
 *
 * <p>The memory is not cleared by unrelated inventory work — crafting and rearranging also
 * remove — because it is only ever read back under an identity match with a stack that is
 * actually being dropped. A stale entry can never match, and the next {@code removeItem}
 * overwrites it.
 */
@Mixin(Inventory.class)
public abstract class InventorySlotMemoryMixin implements SlotMemoryAccess {
	@Unique
	private int abyssfall$lastRemovedSlot = -1;

	@Unique
	private ItemStack abyssfall$lastRemovedStack = ItemStack.EMPTY;

	@Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
	private void abyssfall$rememberRemoval(int slot, int count, CallbackInfoReturnable<ItemStack> cir) {
		this.abyssfall$lastRemovedSlot = slot;
		this.abyssfall$lastRemovedStack = cir.getReturnValue();
	}

	@Override
	public int abyssfall$consumeSlotIfMatches(ItemStack dropped) {
		if (this.abyssfall$lastRemovedStack != dropped) {
			return -1;
		}

		int slot = this.abyssfall$lastRemovedSlot;
		this.abyssfall$lastRemovedSlot = -1;
		this.abyssfall$lastRemovedStack = ItemStack.EMPTY;

		return slot;
	}
}

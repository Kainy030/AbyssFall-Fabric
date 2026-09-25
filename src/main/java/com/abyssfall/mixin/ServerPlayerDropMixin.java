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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abyssfall.itemframework.NeverDestroyed;
import com.abyssfall.itemframework.SlotMemoryAccess;
import com.abyssfall.itemframework.SourceSlotAccess;

/**
 * Marks a stack holding {@code NeverDestroyed} with the slot it left, the moment it
 * becomes an item entity.
 *
 * <p>Every server-side way a player lets go of a stack converges on
 * {@code ServerPlayer.drop(ItemStack, boolean, boolean)}: the Q key, a container throw,
 * {@code /give} overflow, death's {@code dropAll}. One injection therefore covers them
 * all. The slot is resolved in two steps, each exact, with a safe fallthrough:
 *
 * <ol>
 *   <li>the inventory's removal memory ({@code InventorySlotMemoryMixin}), consumed only
 *       when the dropped stack is the very object the last {@code removeItem} returned —
 *       the Q key and container throws;</li>
 *   <li>a reference scan of the inventory for the dropped object — {@code dropAll}, which
 *       drops the slot's own stack before clearing it, so at this instant the slot still
 *       holds the answer.</li>
 * </ol>
 *
 * <p>If neither answers, nothing is written: the slot stays unknown and the void's return
 * will use vanilla's slot finding rather than risk overwriting the wrong occupant.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDropMixin {
	@Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("RETURN"))
	private void abyssfall$markSourceSlot(ItemStack stack, boolean randomly, boolean thrownFromHand,
			CallbackInfoReturnable<ItemEntity> cir) {
		ItemEntity entity = cir.getReturnValue();

		if (entity == null || !NeverDestroyed.INSTANCE.has(entity.getItem())) {
			return;
		}

		Inventory inventory = ((ServerPlayer)(Object)this).getInventory();
		int slot = ((SlotMemoryAccess)inventory).abyssfall$consumeSlotIfMatches(entity.getItem());

		if (slot < 0) {
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (inventory.getItem(i) == entity.getItem()) {
					slot = i;
					break;
				}
			}
		}

		if (slot >= 0) {
			((SourceSlotAccess)entity).abyssfall$setSourceSlot(slot);
		}
	}
}

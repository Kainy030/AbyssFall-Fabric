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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.abyssfall.itemmechanismruntime.NeverDestroyed;
import com.abyssfall.itemmechanismruntime.SourceSlotAccess;

/**
 * The two removals that are not damage, and what happens instead of them.
 *
 * <p>{@code Entity.kill} — what {@code /kill @e[type=item]} reaches — is a plain
 * {@code remove(KILLED)} in 26.2, not a damage call, so no amount of damage immunity
 * stops it; and the void's {@code onBelowWorld} is a bare {@code discard()}. Both are
 * intercepted here for an item entity whose stack holds {@code NeverDestroyed} — the
 * mechanic lives in the item framework, ownership is granted outside it — the first by
 * refusal, the second by the abyss handing the stack back to whoever dropped it.
 *
 * <h2>The return</h2>
 *
 * <p>The stack goes back to the dropper — the entity's {@code thrower}, which
 * {@code Player.drop} sets on everything a player throws. Placement follows a short
 * order of preference, detection code rather than configuration: <b>an empty slot
 * anywhere in the inventory wins first</b> — vanilla's own placement is used and no
 * occupant is disturbed. Only with a full inventory does the original-slot rule engage:
 * the slot the stack left from ({@link SourceSlotAccess}) is reclaimed and its occupant
 * destroyed, which is the stated rule, not an accident to be defended against — unless
 * the occupant is the <em>same protected kind</em>, in which case the return merges
 * through vanilla's add instead, because destroying a protected stack to make room for
 * another would betray the whole point. When no slot is known, vanilla's
 * {@code Inventory.add} picks one, and if even that cannot take the stack — a full
 * inventory with nothing mergeable — the return is declined and vanilla keeps its prey,
 * the same policy as a missing owner.
 *
 * <p>If the dropper cannot be found — offline, or never a player — the return has no
 * address and the void keeps its prey: the injection deliberately does not cancel, so the
 * behaviour stays vanilla's rather than inventing a third answer. A protected stack
 * whose owner is away is the one way it can still be lost.
 *
 * <p>After a successful return the entity is discarded: the stack now lives in the
 * inventory, and leaving the entity under the world would re-trigger this every tick and
 * duplicate the stack.
 */
@Mixin(Entity.class)
public abstract class EntityUndyingMixin {
	@Inject(method = "kill", at = @At("HEAD"), cancellable = true)
	private void abyssfall$neverDestroyedSurvivesKill(ServerLevel level, CallbackInfo ci) {
		if ((Object)this instanceof ItemEntity itemEntity
				&& NeverDestroyed.INSTANCE.has(itemEntity.getItem())) {
			ci.cancel();
		}
	}

	@Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
	private void abyssfall$abyssHandsBack(CallbackInfo ci) {
		if (!((Object)this instanceof ItemEntity itemEntity)
				|| !NeverDestroyed.INSTANCE.has(itemEntity.getItem())) {
			return;
		}

		if (!(itemEntity.level() instanceof ServerLevel)) {
			// The client must not delete its copy; the server is the side that decides.
			ci.cancel();
			return;
		}

		if (!(itemEntity.getOwner() instanceof Player player)) {
			// No address to return to — see the class comment. Vanilla swallows it.
			return;
		}

		Inventory inventory = player.getInventory();
		ItemStack toReturn = itemEntity.getItem().copy();
		int slot = ((SourceSlotAccess)itemEntity).abyssfall$getSourceSlot();

		// Detection code: any empty slot in the inventory means the return costs nothing —
		// vanilla's placement is used and the original slot, occupant included, is left
		// alone. Only a full inventory brings the original-slot rule (and its overwrite)
		// back into play.
		boolean hasEmptySlot = false;

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).isEmpty()) {
				hasEmptySlot = true;
				break;
			}
		}

		boolean returned;

		if (hasEmptySlot) {
			returned = inventory.add(toReturn);
		} else if (slot >= 0 && slot < inventory.getContainerSize()) {
			ItemStack occupant = inventory.getItem(slot);

			if (!occupant.isEmpty() && ItemStack.isSameItemSameComponents(occupant, toReturn)) {
				returned = inventory.add(toReturn);
			} else {
				inventory.setItem(slot, toReturn);
				returned = true;
			}
		} else {
			returned = inventory.add(toReturn);
		}

		if (!returned) {
			// The return found no address after all (a full inventory that cannot take the
			// stack and no original slot to claim). Same policy as a missing owner: do not
			// invent a third answer — vanilla keeps its prey.
			return;
		}

		ci.cancel();
		itemEntity.discard();
	}
}

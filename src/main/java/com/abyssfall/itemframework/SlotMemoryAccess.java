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

package com.abyssfall.itemframework;

import net.minecraft.world.item.ItemStack;

/**
 * Duck interface for the scratch memory {@code InventorySlotMemoryMixin} adds to the
 * player {@code Inventory}: which slot the most recent {@code removeItem} came from, so
 * that a drop immediately following it can be attributed to that slot.
 *
 * <p>Lives in the framework package rather than beside the mixins for the same reason as
 * {@link SourceSlotAccess}: a mixin config's package accepts mixin classes only.
 */
public interface SlotMemoryAccess {
	/**
	 * The remembered slot, consumed, if the dropped stack is the very object the last
	 * {@code removeItem} returned; otherwise −1 and the memory is left alone. Identity is
	 * demanded rather than item equality: a wrong slot must never be reported, because the
	 * answer is used to overwrite whatever occupies it.
	 */
	int abyssfall$consumeSlotIfMatches(ItemStack dropped);
}

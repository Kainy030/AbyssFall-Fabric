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

package com.abyssfall.itemmechanismruntime;

/**
 * Duck interface for the field {@code ItemEntityUndyingMixin} adds to {@code ItemEntity}:
 * the inventory slot the stack occupied before it was dropped, so that the void can hand
 * it back to the exact place it left. −1 means "unknown" — the return then falls back to
 * vanilla's own slot finding rather than guessing.
 *
 * <p>Lives in the framework package rather than beside the mixins: a mixin config owns
 * its package outright and rejects any non-mixin class found there, so anything the
 * mixins share with the rest of the mod must live somewhere else.
 */
public interface SourceSlotAccess {
	int abyssfall$getSourceSlot();

	void abyssfall$setSourceSlot(int slot);
}

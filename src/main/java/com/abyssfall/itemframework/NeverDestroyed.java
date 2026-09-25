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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;

/**
 * NeverDestroyed (不毁) — the mechanic of not being destroyable, in any of the ways an
 * item stack can cease to exist while it lies in the world as an item entity.
 *
 * <p>Four deaths are answered, each by its own hook (all in {@code com.abyssfall.mixin}):
 *
 * <ul>
 *   <li><b>Damage of every kind</b> — lava, fire, cactus, whatever else asks.
 *       {@code ItemStackUndyingMixin} answers {@code ItemStack.canBeHurtBy} with an
 *       unqualified "no", so no damage source present or future can touch the
 *       entity.</li>
 *   <li><b>Explosions</b> — which reach the entity through a gate ahead of the damage
 *       question. {@code ItemEntityUndyingMixin} answers {@code ignoreExplosion} with
 *       an unqualified "yes", exempting the stack from the whole blast: no damage, and
 *       no knockback to fling it somewhere worse.</li>
 *   <li><b>{@code /kill}</b> — in 26.2 a plain {@code remove(KILLED)}, not a damage
 *       call, so immunity cannot stop it. {@code EntityUndyingMixin} refuses the
 *       removal itself.</li>
 *   <li><b>The five-minute despawn</b> — {@code ItemEntityUndyingMixin} applies
 *       vanilla's own {@code setUnlimitedLifetime()}, freezing the countdown and
 *       persisting that with the entity.</li>
 *   <li><b>The void</b> — not refused but answered: {@code EntityUndyingMixin} hands
 *       the stack back to whoever dropped it. An empty slot anywhere in the inventory
 *       is preferred; only a full inventory reclaims the original slot (the memory
 *       chain: {@code InventorySlotMemoryMixin} → {@code ServerPlayerDropMixin} →
 *       {@code ItemEntityUndyingMixin}, persisted with the entity), destroying its
 *       occupant — unless the occupant is the same stack, in which case vanilla's
 *       merge is used and nothing is destroyed. A stack whose dropper cannot be found,
 *       or whose inventory cannot take it back, keeps vanilla's behaviour and is lost;
 *       that is the one way out.</li>
 * </ul>
 *
 * <p>Two comforts come with it, both in {@code ItemEntityUndyingMixin}: the stack does
 * not catch fire visually, and it rests on lava as though the lava were ground — no
 * sinking, no bobbing, no jitter.
 *
 * <p>Which stacks hold this mechanic is not said here and never will be: eligibility
 * arrives through {@link #grant} from the composition root outside this package, and
 * this class must not come to name any particular item — the framework does not exist
 * because of anything that uses it.
 */
public final class NeverDestroyed implements ItemMechanic {
	/**
	 * The single instance enumerated in {@link ItemMechanics}.
	 */
	public static final NeverDestroyed INSTANCE = new NeverDestroyed();

	/**
	 * Every eligibility ever granted. Read on every query, written only during mod
	 * initialisation — copy-on-write keeps the gameplay-side reads free of locking
	 * without giving the two phases a chance to meet on a half-written list.
	 */
	private final List<Predicate<ItemStack>> grants = new CopyOnWriteArrayList<>();

	private NeverDestroyed() {
	}

	@Override
	public void grant(Predicate<ItemStack> eligibility) {
		this.grants.add(eligibility);
	}

	@Override
	public boolean has(ItemStack stack) {
		for (Predicate<ItemStack> eligibility : this.grants) {
			if (eligibility.test(stack)) {
				return true;
			}
		}

		return false;
	}
}

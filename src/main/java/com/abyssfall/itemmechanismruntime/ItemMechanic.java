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

import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;

/**
 * One item mechanic: a named behaviour that can hold for an item stack.
 *
 * <p>A mechanic knows two things and nothing else: how to be <b>granted</b> — an
 * eligibility predicate, stated from outside this package — and how to answer
 * {@link #has(ItemStack)}. It never knows <em>why</em> it was granted. Whether the
 * predicate reads a tag, an item, or the phase of the moon is the caller's business;
 * that is the rule that keeps this package reusable. Mechanics are defined here,
 * ownership is decided elsewhere, and this package must never come to exist because of
 * any particular item that uses it.
 *
 * <p>The behaviour itself is not in this interface. A mechanic's engine is a set of
 * hooks elsewhere (mixins, events) that ask {@link #has(ItemStack)} and act on the
 * answer; the mechanic's own javadoc names its engine.
 */
public interface ItemMechanic {
	/**
	 * Grants this mechanic to whatever the predicate accepts. Called during mod
	 * initialisation, before anything in the world can ask {@link #has}.
	 */
	void grant(Predicate<ItemStack> eligibility);

	/**
	 * Whether this stack holds the mechanic.
	 */
	boolean has(ItemStack stack);
}

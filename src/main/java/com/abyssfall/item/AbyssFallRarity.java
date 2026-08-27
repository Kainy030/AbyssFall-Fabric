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

package com.abyssfall.item;

import java.util.Map;
import java.util.IdentityHashMap;

import net.minecraft.world.item.Item;

import org.jspecify.annotations.Nullable;

/**
 * Rarities beyond the four vanilla provides: Abyssal and Infinity.
 *
 * <h2>🔴 Why this is not an addition to {@code Rarity}</h2>
 *
 * <p>Vanilla's {@code Rarity} is a plain {@code enum} with four constants and a {@code private}
 * constructor, so <strong>it cannot be extended</strong> — not by this mod and not by any other. Its
 * values are also wire and disk format: {@code STREAM_CODEC} sends the ordinal and {@code CODEC}
 * reads the name, so even if a value could be added, a client or a save that did not know it would
 * misread every stack.
 *
 * <p>So this is a separate idea that sits <em>beside</em> {@code Rarity} rather than inside it. An
 * item still declares a vanilla rarity — that is what colours it for anyone without this mod, and
 * what everything reading the stack already understands — and may additionally be listed here.
 *
 * <h2>What it currently does, and what it deliberately does not</h2>
 *
 * <p>Only the item's <strong>name colour</strong>. Nothing else. There is no drop-rate meaning, no
 * tooltip line, no sorting, no loot behaviour. That is the whole of this round's scope and the
 * absence is intentional, not unfinished: a rarity that changes a colour is honest about being a
 * label, and inventing mechanics for it before there is a use would be guessing at design.
 *
 * <h2>How the colour reaches the screen</h2>
 *
 * <p>26.2 builds a name's colour in {@code ItemStack.getStyledHoverName()} as
 * {@code Component.empty().append(getHoverName()).withStyle(getRarity().color())}, and that
 * component is line zero of {@code getTooltipLines()}. Since it is a line of the tooltip like any
 * other, the client can replace it through {@code ItemTooltipCallback} — no mixin, and nothing here
 * needs to know how drawing works.
 *
 * <p>⚠️ <strong>That is a client-side, tooltip-only substitution.</strong> The name is drawn from
 * the vanilla rarity's colour everywhere the callback does not reach — the held-item popup above the
 * hotbar, and any other mod's own rendering. So the vanilla rarity an item declares alongside this
 * one is not a formality: it is what the item looks like whenever the tooltip is not open, and it
 * should be the closest fixed colour to what this rarity is trying to say.
 *
 * <p>This class lives in {@code src/main} because items are registered there and an item has to be
 * able to declare its rarity. It knows nothing about rendering; the client half reads it.
 */
public enum AbyssFallRarity {
	/**
	 * Abyssal — the name drifts through greys, a wave travelling along it one character at a time.
	 *
	 * <p>Deliberately dark rather than bright. Every vanilla rarity gets lighter and more saturated as
	 * it gets rarer, which leaves nowhere above {@code EPIC} to go without shouting. Going the other
	 * way says something different: an Abyssal item is not a prize, and a name that is hard to read is
	 * the point.
	 *
	 * <p>Falls back to {@code DARK_GRAY}, the closest fixed colour to the middle of its range, so the
	 * name still reads as Abyssal where the animation cannot run.
	 */
	ABYSSAL,

	/**
	 * Infinity — a fixed red, vanilla's {@code §c}.
	 *
	 * <p>Fixed, not animated, and that contrast with {@link #ABYSSAL} is the design: the Abyss is
	 * something that moves and cannot be pinned down, and Infinity simply is. A red above
	 * {@code EPIC}'s light purple also reads as "past the end of the scale" in the way players
	 * already expect from every game that has ever done this.
	 */
	INFINITY;

	/**
	 * Which items carry which rarity.
	 *
	 * <p>A side table rather than a field on the item, because {@code Item} is vanilla's class and
	 * this is our idea. The alternative — a data component — would put the rarity on every stack,
	 * send it over the network and write it to disk, which is a great deal of machinery for something
	 * that currently only tints a name and never varies between two stacks of the same item.
	 *
	 * <p>An {@code IdentityHashMap} because items are singletons compared by identity, and because
	 * this is read once per tooltip line on the render path.
	 */
	private static final Map<Item, AbyssFallRarity> ASSIGNMENTS = new IdentityHashMap<>();

	/**
	 * Declares an item's rarity. Call during registration.
	 *
	 * <p>Returns the item so a registration line can wrap this around it without a second statement.
	 */
	public static <T extends Item> T assign(T item, AbyssFallRarity rarity) {
		ASSIGNMENTS.put(item, rarity);

		return item;
	}

	/**
	 * The rarity declared for an item, or {@code null} if it has none.
	 *
	 * <p>{@code null} rather than an {@code Optional} or a {@code COMMON}-like default: the
	 * overwhelming majority of items have no answer here, and the caller's job in that case is to
	 * leave the stack completely alone rather than to apply a neutral value.
	 */
	public static @Nullable AbyssFallRarity of(Item item) {
		return ASSIGNMENTS.get(item);
	}

	/**
	 * Whether anything at all has been declared, so the client can skip installing its callback.
	 */
	public static boolean hasAny() {
		return !ASSIGNMENTS.isEmpty();
	}

	/**
	 * Whether this rarity's colour changes over time.
	 *
	 * <p>Asked by anything that has to decide whether it needs a clock. An animated rarity cannot be
	 * expressed as a single {@code ChatFormatting}, so a caller that only has room for one fixed colour
	 * knows from this that it is choosing an approximation.
	 */
	public boolean isAnimated() {
		return this == ABYSSAL;
	}
}

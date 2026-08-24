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

package com.abyssfall.client.tooltip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import com.abyssfall.item.AbyssFallItems;

/**
 * Sends a slow wave of grey along the word naming the Abyss, wherever it appears in a tooltip.
 *
 * <p>The word is drawn one character at a time, each a fixed step further through the same cycle
 * than the one before it, so the light appears to travel along it rather than the whole word
 * brightening at once. Two characters in Chinese, five in English; the translation decides and
 * nothing here needs to know which is loaded.
 *
 * <h2>Why the colour is applied here and not in the component</h2>
 *
 * <p>A {@code Component} carries one fixed colour. Anything that changes over time therefore has
 * to be recoloured by whoever is about to draw it, and a tooltip is rebuilt from the stack every
 * time the game draws one — {@code getTooltipLines} runs per frame while the cursor rests on an
 * item. Producing a differently coloured line at that point is enough to animate it, with no
 * per-frame state to keep and nothing to reset when the tooltip goes away.
 *
 * <p>The item's own component still carries a static grey, so a screenshot, a dedicated server or
 * anything else reading the stack without this callback sees a sensible colour rather than a
 * placeholder. This only replaces it while a client is looking.
 *
 * <h2>🔴 Why this rebuilds components instead of restyling them</h2>
 *
 * <p>Two hard-won reasons, both of which produced visible bugs when this was written the obvious
 * way — calling {@code setStyle} on the segment found by walking the tree. <b>Do not go back to
 * that.</b>
 *
 * <p>First, it does not work. {@code MutableComponent} caches its own rendered form in
 * {@code visualOrderText} and only rebuilds it when the <em>language</em> changes, so a style
 * written after that cache is warm never reaches the screen: the word stayed the colour it
 * happened to have on the first frame it was drawn.
 *
 * <p>Second, and worse, it corrupted text belonging to something else. {@code Component#copy}
 * copies the sibling <em>list</em> but not the siblings, so a copied line still points at the very
 * same child instances. The creative screen adds a tab's name to every tooltip as
 * {@code tab.getDisplayName().copy()}, and this mod's tab name is built from two shared translated
 * halves — restyling in place reached through the copy and repainted those halves, which is why
 * "深渊" and "浮现" flickered on every item in the game rather than only on this one.
 *
 * <p>Rebuilding sidesteps both: nothing that already exists is touched, so no cache can be stale
 * and no shared instance can be damaged.
 *
 * <h2>Why it matches on the translation key</h2>
 *
 * <p>The line is assembled in {@code AbyssFallItems} from four segments, only one of which is
 * ours. Recolouring by position would break the moment anything is inserted, and recolouring by
 * rendered text would need a list of every translation. The key is the one part that is the same
 * in every language and cannot drift out of step with the item.
 */
public final class AbyssFallTooltips {
	/**
	 * How long one full cycle takes, in milliseconds.
	 *
	 * <p>Three and a half seconds: quick enough that the travel along the word is plainly a
	 * movement rather than something you have to wait to notice, still slow enough that it never
	 * reads as a flicker. The point is that the word does not settle.
	 */
	private static final float CYCLE_MILLIS = 3500.0F;

	/**
	 * How far the wave is offset between one character and the next, as a fraction of a cycle.
	 *
	 * <p>An eighth, which spreads a two-character word across a quarter of the cycle and a
	 * five-letter one across just over half. Small enough that the whole word still reads as one
	 * thing moving rather than as letters blinking independently, large enough that the direction
	 * of travel is unmistakable.
	 *
	 * <p>Negative, so the wave runs left to right: an earlier character reaches a given point in
	 * the cycle <em>before</em> a later one, which is what makes it look like the light is passing
	 * along the word rather than crawling backwards through it.
	 */
	private static final float PHASE_STEP_PER_CHARACTER = -0.2F;

	/**
	 * Darkest point of the cycle: near-black, but never pure black, which would read as a hole in
	 * the tooltip rather than as text.
	 */
	private static final int DARKEST = 0x1E1E1E;

	/**
	 * Lightest point of the cycle. Deliberately below vanilla's {@code GRAY} — the range stays in
	 * the greys the item name and lore already occupy, so the word never brightens into something
	 * that competes with them.
	 */
	private static final int LIGHTEST = 0x767676;

	private AbyssFallTooltips() {
	}

	public static void initialize() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			float phase = currentPhase();

			// Indexed rather than a for-each: a matching line is replaced outright, and mutating
			// the list while iterating it would not be allowed.
			for (int i = 0; i < lines.size(); i++) {
				Component rebuilt = recolored(lines.get(i), phase);

				if (rebuilt != null) {
					lines.set(i, rebuilt);
				}
			}
		});
	}

	/**
	 * Where in the cycle this frame sits, in {@code [0, 1)}.
	 *
	 * <p>Uses {@link Util#getMillis()} rather than a tick count so the drift continues while the
	 * game is paused — a tooltip is very often being read with the game paused, and a colour that
	 * freezes exactly when someone stops to look at it would be a strange thing to have built.
	 */
	private static float currentPhase() {
		return Util.getMillis() % (long)CYCLE_MILLIS / CYCLE_MILLIS;
	}

	/**
	 * The grey at a given point in the cycle, easing between {@link #DARKEST} and
	 * {@link #LIGHTEST}.
	 *
	 * <p>Driven by a cosine so the two ends are dwelt on and the turn is smooth. A sawtooth would
	 * snap back to the start of the range once per cycle, which reads as a glitch rather than as
	 * something breathing. Cosine is also why the phase needs no wrapping: it is periodic, so an
	 * offset past one or below zero lands exactly where wrapping would have put it.
	 */
	private static int colorAt(float phase) {
		float eased = (1.0F - Mth.cos(phase * Mth.TWO_PI)) * 0.5F;

		return ARGB.color(
				255,
				Mth.lerpInt(eased, ARGB.red(DARKEST), ARGB.red(LIGHTEST)),
				Mth.lerpInt(eased, ARGB.green(DARKEST), ARGB.green(LIGHTEST)),
				Mth.lerpInt(eased, ARGB.blue(DARKEST), ARGB.blue(LIGHTEST)));
	}

	/**
	 * A copy of {@code component} with every Abyss word within it replaced by a wave, or
	 * {@code null} if it contains none.
	 *
	 * <p>Returning {@code null} for the overwhelmingly common case means an untouched line stays the
	 * object it already was, so nothing is allocated for the tooltips of the thousands of items that
	 * have nothing to do with this.
	 *
	 * <p>Recurses because the word is a sibling of the line rather than the line itself. Children
	 * are rebuilt from their own contents and styles, never restyled, so no component that existed
	 * before this call is altered — see the class comment for what went wrong when they were.
	 */
	private static @Nullable Component recolored(Component component, float phase) {
		if (component.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().equals(AbyssFallItems.ABYSS_WORD_KEY)) {
			return wave(translatable.getKey(), component.getStyle(), phase);
		}

		List<Component> siblings = component.getSiblings();
		List<Component> rebuiltSiblings = null;

		for (int i = 0; i < siblings.size(); i++) {
			Component rebuilt = recolored(siblings.get(i), phase);

			if (rebuilt == null) {
				continue;
			}

			// Copied lazily, so a subtree with nothing of ours in it allocates nothing at all.
			if (rebuiltSiblings == null) {
				rebuiltSiblings = new ArrayList<>(siblings);
			}

			rebuiltSiblings.set(i, rebuilt);
		}

		if (rebuiltSiblings == null) {
			return null;
		}

		MutableComponent result = MutableComponent.create(component.getContents())
				.setStyle(component.getStyle());

		for (Component sibling : rebuiltSiblings) {
			result.append(sibling);
		}

		return result;
	}

	/**
	 * The translated word, one component per character, each a step further through the cycle than
	 * the last.
	 *
	 * <p>Split per character because a component carries a single colour, so a gradient across a
	 * word can only be a run of components. Two for {@code 深渊}, five for {@code Abyss} — the
	 * translation decides, and nothing here needs to know which language is loaded.
	 *
	 * <p>Resolved through {@link Language} rather than by rendering the component, because the
	 * characters have to be known before they can be coloured individually. The word takes no
	 * arguments, so a plain lookup is the whole of what resolving it means.
	 *
	 * <p>Iterated by code point, not by {@code char}, so a character outside the basic plane stays
	 * one character instead of being split down the middle into two halves of a surrogate pair.
	 * Neither current translation contains one, but a future language or a stylised glyph could.
	 *
	 * @param key   the word's translation key
	 * @param style the style the word already had; the wave replaces its colour and keeps the rest,
	 *              so bold or italic survive if the line ever gains any
	 * @param phase where the first character sits in the cycle
	 */
	private static MutableComponent wave(String key, Style style, float phase) {
		String word = Language.getInstance().getOrDefault(key);
		MutableComponent result = Component.empty().setStyle(style);
		int index = 0;

		for (int offset = 0; offset < word.length(); ) {
			int codePoint = word.codePointAt(offset);
			float characterPhase = phase + index * PHASE_STEP_PER_CHARACTER;

			result.append(Component.literal(Character.toString(codePoint))
					.setStyle(style.withColor(colorAt(characterPhase))));

			offset += Character.charCount(codePoint);
			index++;
		}

		return result;
	}
}

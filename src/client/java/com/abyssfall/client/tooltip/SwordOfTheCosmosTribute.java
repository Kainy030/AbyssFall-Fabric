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

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * The Sword of the Cosmos's tribute, as tooltip lines.
 *
 * <h2>What this is</h2>
 *
 * <p>Fifteen lines of the author's own words about Avaritia's Infinity Sword, the mod that made him a
 * programmer. It is not lore text for an item; it is a dedication that happens to live on one.
 *
 * <p>🔴 <strong>The wording is not this file's to change.</strong> Every line comes from the language
 * files verbatim — this class decides only how they are laid out and coloured. If a line reads oddly,
 * that is a question for the author, not a thing to quietly fix.
 *
 * <h2>How it is laid out</h2>
 *
 * <p>Collapsed to a single hint line until Shift is held. Twenty-odd lines is taller than most screens,
 * and a dedication that buries the item every time the cursor drifts across it is a dedication nobody
 * finishes reading.
 *
 * <p>Held open, it is set as an inscription rather than a paragraph: blank lines group it into
 * movements, and colour marks what kind of statement each line is rather than decorating it. Reading
 * down the tooltip should feel like the piece is being spoken, with pauses.
 *
 * <ul>
 *   <li><strong>Hint</strong> — dim and italic, an instruction rather than part of the piece.</li>
 *   <li><strong>Narration</strong> — grey, vanilla's own colour for descriptive text, so the story does
 *       not compete with the item above it.</li>
 *   <li><strong>Quotations</strong> — italic and dimmer, set apart as remembered speech.</li>
 *   <li><strong>Turns</strong> — the two sentences the piece pivots on are the only lines given the
 *       sword's own colour.</li>
 *   <li><strong>The Avaritia epigraph</strong> — indented and attributed, as the author wrote it.</li>
 *   <li><strong>The closing line</strong> — in the sword's colour and bold, because it is the point the
 *       rest of it arrives at.</li>
 * </ul>
 *
 * <h2>Why the text is not styled in the language file</h2>
 *
 * <p>Section codes in a translation string do work, but they would put layout decisions inside the
 * translatable text: a translator would have to preserve {@code §7} and {@code §o} to keep the
 * appearance, and any of them going missing would silently change it. Keeping the strings plain means
 * the only thing a translation carries is words.
 */
final class SwordOfTheCosmosTribute {
	/** Base key of the tribute lines; each line is this plus its number, from one. */
	private static final String KEY_PREFIX = "item.abyssfall.fake_infinity_sword.tribute.";

	/** The one line shown while the inscription is collapsed. */
	private static final String HINT_KEY = KEY_PREFIX + "hint";

	/** How many lines the tribute has. Must match the language files. */
	private static final int LINE_COUNT = 15;

	/**
	 * The collapsed hint: dim, italic, and quiet.
	 *
	 * <p>Dimmer than the narration on purpose. It is an instruction rather than part of the piece, so it
	 * should be legible and then ignorable — the same register vanilla uses for its own "hold Shift"
	 * style prompts.
	 */
	private static final Style HINT = Style.EMPTY
			.withColor(ChatFormatting.DARK_GRAY)
			.withItalic(true);

	/**
	 * Narration: vanilla's tooltip grey.
	 *
	 * <p>{@code GRAY} rather than anything of ours, because this is prose being read and vanilla has
	 * already decided what prose on a tooltip looks like.
	 */
	private static final Style NARRATION = Style.EMPTY.withColor(ChatFormatting.GRAY);

	/**
	 * Remembered speech: dimmer than the narration, and italic.
	 *
	 * <p>Italic is what sets a quotation apart at this size — the quotation marks are already in the
	 * text, and a second, louder signal would fight them.
	 */
	private static final Style QUOTATION = Style.EMPTY
			.withColor(ChatFormatting.DARK_GRAY)
			.withItalic(true);

	/**
	 * The two turns: the sword's own red.
	 *
	 * <p>The same {@code §c} the item's name uses, so the emphasis reads as the sword speaking rather
	 * than as arbitrary highlighting.
	 */
	private static final Style TURN = Style.EMPTY.withColor(ChatFormatting.RED);

	/** The closing line: the same red, bold. */
	private static final Style CLOSING = Style.EMPTY
			.withColor(ChatFormatting.RED)
			.withBold(true);

	/**
	 * The Avaritia epigraph's indent.
	 *
	 * <p>Two leading spaces stand in for a block quote's margin: a tooltip has none, so the indent has
	 * to be part of the line.
	 */
	private static final String EPIGRAPH_INDENT = "  ";

	private SwordOfTheCosmosTribute() {
	}

	/**
	 * The tribute, ready to be appended to a tooltip: one hint line, or the whole inscription while
	 * Shift is held.
	 *
	 * <p>Collapsed by default because twenty-odd lines is taller than most screens and would bury the
	 * item itself every time the cursor passed over it. The piece is worth reading, but it should be
	 * read when someone chooses to.
	 *
	 * <p>{@code Minecraft.hasShiftDown()} asks the window for the physical key state rather than
	 * remembering anything, so there is nothing to keep in sync and nothing to reset — pressing or
	 * releasing Shift takes effect on the next frame the tooltip is built, which is every frame it is
	 * open. Vanilla itself reads the same method while a tooltip is up ({@code ExtendedView}).
	 *
	 * <p>Built fresh on each call rather than cached. A cached list would be shared between every
	 * tooltip that shows it, and this class is in no position to promise nobody downstream will style
	 * what it is handed — the very mistake this mod already made once with the creative tab's title
	 * (see {@code AbyssFallTooltips}). A handful of components while a tooltip is open costs nothing
	 * worth protecting.
	 */
	static List<Component> lines() {
		if (!Minecraft.getInstance().hasShiftDown()) {
			return List.of(Component.empty(),
					Component.translatable(HINT_KEY).withStyle(HINT));
		}

		List<Component> lines = new ArrayList<>(LINE_COUNT + 8);

		lines.add(Component.empty());
		lines.add(line(1, NARRATION));
		lines.add(Component.empty());
		lines.add(line(2, QUOTATION));
		lines.add(Component.empty());
		lines.add(line(3, NARRATION));
		lines.add(line(4, NARRATION));
		lines.add(line(5, NARRATION));
		lines.add(line(6, NARRATION));
		lines.add(Component.empty());

		// The first turn: the piece stops explaining and states what was done.
		lines.add(line(7, TURN));
		lines.add(Component.empty());
		lines.add(line(8, NARRATION));

		// The second turn answers the sentence before it, so it follows with no pause.
		lines.add(line(9, TURN));
		lines.add(Component.empty());
		lines.add(line(10, QUOTATION));
		lines.add(Component.empty());

		// Avaritia's own words, indented and attributed.
		lines.add(indented(11, QUOTATION));
		lines.add(indented(12, QUOTATION));
		lines.add(Component.empty());

		lines.add(line(13, NARRATION));
		lines.add(line(14, NARRATION));
		lines.add(Component.empty());
		lines.add(line(15, CLOSING));

		return lines;
	}

	private static Component line(int number, Style style) {
		return Component.translatable(KEY_PREFIX + number).withStyle(style);
	}

	/**
	 * A line with the epigraph indent in front of it.
	 *
	 * <p>The style is applied to the whole thing including the indent, which is harmless — spaces carry
	 * no colour — and keeps the line a single styled unit.
	 */
	private static Component indented(int number, Style style) {
		return Component.literal(EPIGRAPH_INDENT)
				.append(Component.translatable(KEY_PREFIX + number))
				.withStyle(style);
	}
}

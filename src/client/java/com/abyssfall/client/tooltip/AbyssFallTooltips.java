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
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import com.abyssfall.item.AbyssFallItems;
import com.abyssfall.item.AbyssFallRarity;

/**
 * Sends a slow wave along the word standing in for a weapon's damage figure, wherever one appears in a
 * tooltip.
 *
 * <p>The word is drawn one character at a time, each a fixed step further through the same cycle
 * than the one before it, so the light appears to travel along it rather than the whole word
 * brightening at once. Two characters in Chinese, five in English; the translation decides and
 * nothing here needs to know which is loaded.
 *
 * <h2>Two words, one animation, different palettes</h2>
 *
 * <p>The Abyss word travels through greys, staying within the range the item name and lore already
 * occupy. The Infinity word travels through hues instead — the same wave, the same cycle, the same
 * per-character step, read off a colour wheel rather than a grey ramp. The timing is shared
 * deliberately: they are the same gesture, and only the palette says which weapon is being described.
 *
 * <p>Which palette a word gets is decided by its translation key, so the two are told apart by the
 * one property that is stable across languages and cannot drift away from the item.
 *
 * <h2>Item names, for the mod's own rarities</h2>
 *
 * <p>This also colours an item's name for items carrying an {@link AbyssFallRarity}: Abyssal names get
 * the same travelling grey wave, over a wider range and at their own pace, and Infinity names get a
 * fixed red. See {@code AbyssFallRarity} for why the mod's rarities are a table beside vanilla's enum
 * rather than additions to it.
 *
 * <p>The colouring reaches both places a name is drawn — tooltips through this class's own callback, and
 * the popup above the hotbar through {@code HudSelectedItemNameMixin}, which calls
 * {@link #rarityName} so the two never disagree. The vanilla rarity an item declares is still what
 * shows anywhere neither of those covers.
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
	 * How long one full cycle of the grey wave takes, in milliseconds.
	 *
	 * <p>Three and a half seconds: quick enough that the travel along the word is plainly a
	 * movement rather than something you have to wait to notice, still slow enough that it never
	 * reads as a flicker. The point is that the word does not settle.
	 *
	 * <p>⚠️ The hue word has its own period — see {@link #RAINBOW_CYCLE_MILLIS}. This value was
	 * settled on for the Abyss word and is left alone.
	 */
	private static final float GREY_CYCLE_MILLIS = 3500.0F;

	/**
	 * 🔴 <strong>How fast the rainbow runs.</strong> One full trip round the hue wheel, in
	 * milliseconds. Lower is faster.
	 *
	 * <p>Separate from {@link #GREY_CYCLE_MILLIS} because the two animations do not want the same
	 * pace. The grey wave is a slow breath through a narrow band of greys, and hurrying it turns it
	 * into a flicker. The rainbow travels the whole wheel, so the same period spends far longer on
	 * each visible step and reads as sluggish. Sharing one constant would mean neither could be set
	 * without spoiling the other.
	 *
	 * <p>Rough guide, at the current {@link #HUE_STEP_PER_CHARACTER}:
	 *
	 * <ul>
	 *   <li>{@code 3500} — the grey wave's period. Visibly slow for hues; this is what it was.</li>
	 *   <li>{@code 1500} — brisk, the colours clearly flowing along the word.</li>
	 *   <li>{@code 500} — fast, a live chase light. <strong>Current value, set by Kainy after seeing
	 *       it in game.</strong></li>
	 *   <li>{@code 200} and below — reads as strobing rather than as motion.</li>
	 * </ul>
	 */
	private static final float RAINBOW_CYCLE_MILLIS = 500.0F;

	/**
	 * How far the grey wave is offset between one character and the next, as a fraction of a cycle.
	 *
	 * <p>A fifth, which spreads the two-character Chinese word across two fifths of the cycle and the
	 * five-letter English one across four fifths. Small enough that the whole word still reads as one
	 * thing moving rather than as letters blinking independently, large enough that the direction
	 * of travel is unmistakable.
	 *
	 * <p>Negative, so the wave runs left to right: an earlier character reaches a given point in
	 * the cycle <em>before</em> a later one, which is what makes it look like the light is passing
	 * along the word rather than crawling backwards through it.
	 *
	 * <p>⚠️ This is the <em>grey</em> word's step and is not shared with the hue word — see
	 * {@link #HUE_STEP_PER_CHARACTER}. It is safe for this one to exceed a full cycle, because the
	 * cosine driving {@link #colorAt} folds; hue does not.
	 */
	private static final float PHASE_STEP_PER_CHARACTER = -0.2F;

	/**
	 * How far the hue is offset between one character and the next, for the Infinity word.
	 *
	 * <p>🔴 <strong>Deliberately not {@link #PHASE_STEP_PER_CHARACTER}, and this is not an
	 * inconsistency.</strong> A cosine folds, so the grey wave can span more than a full cycle and
	 * nobody can tell: a phase and that phase plus one turn produce the same grey, which simply reads
	 * as the wave having passed. Hue does not fold — it wraps onto itself, so a character a full turn
	 * behind another wears its <em>exact</em> colour, and the word grows a visibly repeated band.
	 *
	 * <p>Measured, not guessed. At {@code -0.2} the eight letters of {@code Infinity} span 1.4 turns
	 * and letters one and six come out byte-identical (RGB distance 0). A tenth of a turn spans 0.7,
	 * so the word runs red through magenta, violet, blue, cyan, green without ever repeating, and the
	 * closest pair of letters is still 91 apart in RGB. Two-character Chinese stays legible too —
	 * red and magenta are plainly different.
	 */
	private static final float HUE_STEP_PER_CHARACTER = -0.1F;

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

	/**
	 * Saturation and value for the Infinity word's hues.
	 *
	 * <p>Full saturation, because a rainbow that is not saturated is a set of pastels and reads as a
	 * mistake rather than as a choice. Value slightly below one so the brightest hues do not glare
	 * against the tooltip's dark background the way pure {@code 1.0} yellow does.
	 */
	private static final float RAINBOW_SATURATION = 0.8F;

	private static final float RAINBOW_VALUE = 1.0F;

	/**
	 * 🔴 How fast an Abyssal item's name drifts, in milliseconds per cycle. Lower is faster.
	 *
	 * <p>Its own constant rather than {@link #GREY_CYCLE_MILLIS}, deliberately, even though the two
	 * currently hold the same number. They are different things that happen to agree: one paces a word
	 * inside an attribute line, the other paces an item's name. Sharing the constant would mean the
	 * next person to tune either one silently retunes the other.
	 */
	private static final float RARITY_CYCLE_MILLIS = 3500.0F;

	/**
	 * How far the Abyssal name's wave is offset between one character and the next.
	 *
	 * <p>Smaller than the attribute word's step because item names are far longer — {@code 寰宇支配之剑}
	 * is six characters and an English name can be twenty. At the attribute word's fifth of a cycle a
	 * long name would wrap several times over and break into unrelated bands rather than reading as one
	 * wave passing along it.
	 *
	 * <p>Negative for the same reason as everywhere else here: the light travels left to right.
	 */
	private static final float RARITY_STEP_PER_CHARACTER = -0.045F;

	/**
	 * Darkest point of an Abyssal name's cycle.
	 *
	 * <p>Darker than {@link #DARKEST}, which is used inside an attribute line where the text has to
	 * stay comfortably readable. A name may go further: it is the largest text on the tooltip and the
	 * one a player is already looking for, so it can afford to nearly disappear. Still not pure black,
	 * which would read as a gap rather than as a name.
	 */
	private static final int RARITY_DARKEST = 0x1F1F1F;

	/**
	 * Lightest point of an Abyssal name's cycle.
	 *
	 * <p>Brighter than {@link #LIGHTEST} — that value is capped below vanilla's {@code GRAY} so the
	 * Abyss word never competes with the name beside it, whereas this <em>is</em> the name and has
	 * nothing to defer to. It stays a grey, though: the point of the Abyssal rarity is that it is the
	 * one label on the scale that does not brighten into a colour.
	 */
	private static final int RARITY_LIGHTEST = 0xB4B4B4;

	/**
	 * The fixed colour of an Infinity item's name — vanilla's {@code §c}.
	 *
	 * <p>Taken from {@code ChatFormatting} rather than written as a literal, so it is the same red
	 * players know from {@code §c} even if vanilla ever adjusts its palette.
	 */
	private static final ChatFormatting INFINITY_NAME_COLOR = ChatFormatting.RED;

	private AbyssFallTooltips() {
	}

	public static void initialize() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			// Read once per tooltip rather than once per word: two words on the same tooltip should be
			// describing the same instant, and the two periods are applied to this one reading.
			long now = nameClock();

			// Line zero is the item's name, and only line zero — vanilla builds it as
			// getStyledHoverName() and prepends it before anything else contributes. Handled before the
			// loop rather than inside it so nothing has to test an index once per line.
			if (!lines.isEmpty()) {
				Component renamed = rarityName(stack, lines.getFirst(), now);

				if (renamed != null) {
					lines.set(0, renamed);
				}
			}

			// Indexed rather than a for-each: a matching line is replaced outright, and mutating
			// the list while iterating it would not be allowed.
			for (int i = 0; i < lines.size(); i++) {
				Component rebuilt = recolored(lines.get(i), now);

				if (rebuilt != null) {
					lines.set(i, rebuilt);
				}
			}

			// Appended last, after the recolouring pass, for two reasons: the tribute's own styling is
			// already final and has no words the pass would match, and appending first would mean
			// walking twenty-odd extra lines on every tooltip in the game.
			if (stack.is(AbyssFallItems.FAKE_INFINITY_SWORD)) {
				lines.addAll(SwordOfTheCosmosTribute.lines());
			}
		});
	}
	/**
	 * The item's name, coloured for its {@link AbyssFallRarity}, or {@code null} if it has none.
	 *
	 * <p>🔴 <strong>Public because two unrelated places draw an item's name</strong>: the tooltip, and
	 * the popup above the hotbar when the held item changes. Vanilla builds both the same way — an
	 * empty root with the name appended as a sibling and {@code rarity.color()} applied to the root —
	 * so one implementation serves both, and the animation stays in step between them because both ask
	 * the same clock.
	 *
	 * <p>🔴 <strong>Rebuilt, never restyled.</strong> The name component handed in is not ours to write
	 * to — the same two reasons as everywhere else in this class, and the creative screen's shared
	 * tab-name components make the second one real rather than theoretical. See the class comment.
	 *
	 * <p>The colour vanilla already applied is replaced rather than merged, since it came from the
	 * item's <em>vanilla</em> rarity and this is the whole point of the substitution. Italic survives,
	 * though: vanilla adds it to mark a renamed stack, and dropping it would hide that.
	 *
	 * @param stack     the stack whose name is being drawn
	 * @param name      the name as vanilla resolved it
	 * @param nowMillis the clock, so an animated rarity can advance
	 */
	public static @Nullable Component rarityName(ItemStack stack, Component name, long nowMillis) {
		AbyssFallRarity rarity = AbyssFallRarity.of(stack.getItem());

		if (rarity == null) {
			return null;
		}

		boolean renamed = stack.has(DataComponents.CUSTOM_NAME);

		return switch (rarity) {
			case ABYSSAL -> nameWave(name.getString(), renamed,
					phaseOf(nowMillis, RARITY_CYCLE_MILLIS));
			case INFINITY -> Component.literal(name.getString())
					.withStyle(nameStyle(INFINITY_NAME_COLOR, renamed));
		};
	}

	/**
	 * The clock the name animation reads, so every place drawing a name agrees on the moment.
	 */
	public static long nameClock() {
		return Util.getMillis();
	}

	/**
	 * A style for a name of one fixed colour.
	 */
	private static Style nameStyle(ChatFormatting color, boolean italic) {
		Style style = Style.EMPTY.withColor(color);

		return italic ? style.withItalic(true) : style;
	}

	/**
	 * Plain text split one character per component, each a step further through the grey cycle.
	 *
	 * <p>Separate from {@link #wave} because that one resolves a translation key: the word it colours is
	 * a single translatable segment, so it has to look the text up. A name is already resolved — it may
	 * be a translation, a custom name, or something another mod built — so the string is taken as given.
	 *
	 * <p>⚠️ Reading the name through {@code getString()} flattens it, so a name that was itself several
	 * differently styled pieces loses those distinctions. Accepted for now: an Abyss name is meant to be
	 * one wave from end to end, and there is no agreed way to combine that with styling arriving from
	 * elsewhere. Worth revisiting if a name ever needs to keep internal styling.
	 */
	private static MutableComponent nameWave(String text, boolean italic, float phase) {
		MutableComponent result = Component.empty();
		int index = 0;

		if (italic) {
			result.withStyle(ChatFormatting.ITALIC);
		}

		for (int offset = 0; offset < text.length(); ) {
			int codePoint = text.codePointAt(offset);
			float characterPhase = phase + index * RARITY_STEP_PER_CHARACTER;

			result.append(Component.literal(Character.toString(codePoint))
					.withStyle(Style.EMPTY.withColor(rarityGreyAt(characterPhase))));

			offset += Character.charCount(codePoint);
			index++;
		}

		return result;
	}

	/**
	 * The grey at a point in an Abyss name's cycle.
	 *
	 * <p>The same cosine easing {@link #colorAt} uses, over a wider range — see {@link #RARITY_DARKEST}
	 * and {@link #RARITY_LIGHTEST} for why a name may go further than a word inside an attribute line.
	 */
	private static int rarityGreyAt(float phase) {
		float eased = (1.0F - Mth.cos(phase * Mth.TWO_PI)) * 0.5F;

		return ARGB.color(
				255,
				Mth.lerpInt(eased, ARGB.red(RARITY_DARKEST), ARGB.red(RARITY_LIGHTEST)),
				Mth.lerpInt(eased, ARGB.green(RARITY_DARKEST), ARGB.green(RARITY_LIGHTEST)),
				Mth.lerpInt(eased, ARGB.blue(RARITY_DARKEST), ARGB.blue(RARITY_LIGHTEST)));
	}



	/**
	 * Where in a cycle of the given length this frame sits, in {@code [0, 1)}.
	 *
	 * <p>Uses {@link Util#getMillis()} rather than a tick count so the drift continues while the
	 * game is paused — a tooltip is very often being read with the game paused, and a colour that
	 * freezes exactly when someone stops to look at it would be a strange thing to have built.
	 *
	 * <p>Takes the period as an argument rather than reading one constant, because the two words run
	 * at different speeds. Both are computed from the same clock reading per tooltip, so the two
	 * animations stay in step with each other even though they are not in step with the same cycle.
	 *
	 * @param nowMillis     the clock, read once per tooltip so every word on it agrees on the moment
	 * @param cycleMillis   how long one full cycle of this word's animation takes
	 */
	private static float phaseOf(long nowMillis, float cycleMillis) {
		return nowMillis % (long)cycleMillis / cycleMillis;
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
	 * The hue at a given point in the cycle, at full saturation.
	 *
	 * <p>The counterpart to {@link #colorAt}: same phase, same cycle, same per-character step, but the
	 * phase drives hue rather than lightness. Nothing else about the animation differs — which is the
	 * point, since both words are the same kind of thing and only their palette distinguishes them.
	 *
	 * <p>Linear in the phase rather than eased, unlike the grey wave. A cosine there makes the word
	 * dwell at each end of a range and turn back; hue has no ends to dwell at, it wraps, and easing it
	 * would make the rainbow crawl and then rush for no reason a viewer could see.
	 *
	 * <p>🔴 The phase must be wrapped into {@code [0, 1)} before it reaches
	 * {@code Mth.hsvToArgb}, and this is not defensive. That method's first line is
	 * {@code (int)(hue * 6.0F) % 6}, which for a negative hue yields a negative branch index, falls
	 * through its switch and <strong>throws</strong>. Negative phases are the normal case here:
	 * {@link #HUE_STEP_PER_CHARACTER} is negative, so the last letter of {@code Infinity} sits at
	 * the incoming phase minus seven tenths. {@code Mth.positiveModulo} is what makes the wrap right
	 * for both signs — a plain {@code %} keeps the sign of its left operand and would not help.
	 */
	private static int hueAt(float phase) {
		return ARGB.opaque(Mth.hsvToRgb(
				Mth.positiveModulo(phase, 1.0F), RAINBOW_SATURATION, RAINBOW_VALUE));
	}

	/**
	 * A copy of {@code component} with every animated word within it replaced by a wave, or
	 * {@code null} if it contains none.
	 *
	 * <p>Returning {@code null} for the overwhelmingly common case means an untouched line stays the
	 * object it already was, so nothing is allocated for the tooltips of the thousands of items that
	 * have nothing to do with this.
	 *
	 * <p>Recurses because the word is a sibling of the line rather than the line itself. Children
	 * are rebuilt from their own contents and styles, never restyled, so no component that existed
	 * before this call is altered — see the class comment for what went wrong when they were.
	 *
	 * <p>Carries the clock rather than a phase, because each word converts it with its own period.
	 *
	 * @param nowMillis the clock, read once for the whole tooltip
	 */
	private static @Nullable Component recolored(Component component, long nowMillis) {
		if (component.getContents() instanceof TranslatableContents translatable) {
			String key = translatable.getKey();

			// Which word it is decides which palette it gets, and how fast it runs. Matching on the key
			// rather than on the item means a line carrying either word animates wherever it appears,
			// including on a stack this mod never built.
			if (key.equals(AbyssFallItems.ABYSS_WORD_KEY)) {
				return wave(key, component.getStyle(),
						phaseOf(nowMillis, GREY_CYCLE_MILLIS),
						PHASE_STEP_PER_CHARACTER, AbyssFallTooltips::colorAt);
			}

			if (key.equals(AbyssFallItems.INFINITY_WORD_KEY)) {
				return wave(key, component.getStyle(),
						phaseOf(nowMillis, RAINBOW_CYCLE_MILLIS),
						HUE_STEP_PER_CHARACTER, AbyssFallTooltips::hueAt);
			}
		}

		List<Component> siblings = component.getSiblings();
		List<Component> rebuiltSiblings = null;

		for (int i = 0; i < siblings.size(); i++) {
			Component rebuilt = recolored(siblings.get(i), nowMillis);

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
	 * @param step  how far each character is offset from the one before it. A parameter rather than a
	 *              constant because the two palettes need different values — see
	 *              {@link #HUE_STEP_PER_CHARACTER} for why a step that suits greys ruins hues
	 * @param palette what a phase looks like — greys for the Abyss, hues for Infinity. Passed in
	 *                rather than branched on inside, so the splitting, the stepping and the style
	 *                handling stay one piece of code with one behaviour
	 */
	private static MutableComponent wave(String key, Style style, float phase, float step,
			FloatUnaryOperator palette) {
		String word = Language.getInstance().getOrDefault(key);
		MutableComponent result = Component.empty().setStyle(style);
		int index = 0;

		for (int offset = 0; offset < word.length(); ) {
			int codePoint = word.codePointAt(offset);
			float characterPhase = phase + index * step;

			result.append(Component.literal(Character.toString(codePoint))
					.setStyle(style.withColor(palette.applyAsInt(characterPhase))));

			offset += Character.charCount(codePoint);
			index++;
		}

		return result;
	}

	/**
	 * A phase to a packed colour.
	 *
	 * <p>Declared here rather than reusing a JDK functional interface because the only fitting one is
	 * {@code DoubleToIntFunction}, and everything in this file is {@code float}: routing through
	 * {@code double} would widen and narrow once per character for nothing.
	 */
	@FunctionalInterface
	private interface FloatUnaryOperator {
		int applyAsInt(float value);
	}
}

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

package com.abyssfall.client.hud;

import java.util.Locale;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;

import com.abyssfall.config.AbyssFallConfig;
import com.abyssfall.core.AbyssFallCoreSystem;
import com.abyssfall.core.SanHudModeState;
import com.abyssfall.core.SanState;

/**
 * Draws the player's San as a bar with the reading written across it.
 *
 * <h2>One of two readouts</h2>
 *
 * <p>This is the precise reading; {@link SanIconHudElement} is the ambient one. Which of the two is
 * drawn is chosen by the player with the Cognition Lens and dispatched by
 * {@link SanHudDispatchElement}, so only ever one of them is on screen at a time. They differ in
 * kind on purpose and should not be folded together: this one gives the exact figure, the other is
 * deliberately coarse.
 *
 * <p>Both react to the reading the same way — the same tremor at low San, the same shudder on a
 * loss, the same wave on a gain, the same fade at full — so that switching between them changes
 * what you read, not how the mod behaves. The one thing that cannot be shared is the highlight:
 * the icon row swaps in brighter artwork, while this bar lightens its own fill colour, because a
 * filled rectangle has no texture to swap.
 *
 * <h2>Where the number comes from</h2>
 *
 * <p>Read from the attachment on the client's own player, which holds the value the server last
 * pushed. The attachment is registered with {@code syncWith(targetOnly())}, so what is drawn is
 * the server's authoritative reading for this player and nothing the client worked out for
 * itself. That is the same value the San Counter reports; the counter reads it server-side
 * because a debug tool must not trust a mirror, whereas a HUD necessarily draws from the synced
 * copy every frame and has no reason to distrust it.
 *
 * <h2>Why the bar is drawn rather than blitted</h2>
 *
 * <p>Filled rectangles and a string, with no texture of its own. This is deliberately the plain
 * version asked for: a placeholder built from solid colours can be restyled or replaced outright
 * without leaving an unused sprite behind. It also makes the highlight possible without new art —
 * see {@link #HIGHLIGHT_STRENGTH}.
 *
 * <h2>Visibility and the fade</h2>
 *
 * <p>Hidden while San sits at or above the configured threshold, shown continuously below it,
 * and faded out over {@link #FADE_OUT_MILLIS} once it climbs back above. The fade is measured in
 * real time rather than in ticks because it is a purely visual flourish, and should look the
 * same however the server happens to be ticking.
 */
public final class SanBarHudElement implements HudElement {
	/**
	 * Width of the bar, matched to the vanilla hunger row: ten 8-pixel icons plus the last one's
	 * extra pixel. Fixed rather than sized to the label, so the bar reads as belonging to the
	 * row of hunger icons directly beneath it and never reaches across the middle of the screen
	 * into the health side.
	 */
	public static final int BAR_WIDTH = 81;

	/**
	 * Height of the bar, matched to the 10-pixel row that vanilla allots each status bar. Sizing
	 * it to the row rather than to the label is what lets the bar sit flush against the hunger
	 * icons: its bottom edge lands exactly on their top edge, with no gap and no overlap.
	 */
	public static final int BAR_HEIGHT = 10;

	/**
	 * Vertical space this element claims, reported to {@link HudStatusBarHeightRegistry}.
	 *
	 * <p>Exactly the bar's height, so the element takes up one vanilla status row and everything
	 * above it moves by that same amount — the spacing between our bar and the hunger row below
	 * ends up identical to the spacing vanilla uses between its own bars.
	 */
	public static final int OCCUPIED_HEIGHT = BAR_HEIGHT;

	/**
	 * How long the bar takes to disappear once San is no longer low enough to warrant showing
	 * it. Long enough to read as a fade rather than as a flicker.
	 */
	private static final long FADE_OUT_MILLIS = 1000L;

	private static final int BORDER_COLOR = 0xFF12080F;

	private static final int BACKGROUND_COLOR = 0xFF2A1B2E;

	/**
	 * Fill colour at full San: the muted violet the mod uses for the abyss elsewhere.
	 */
	private static final int FILL_HIGH = 0xFF9B6BC9;

	/**
	 * Fill colour at zero San. The fill is interpolated between this and {@link #FILL_HIGH} so
	 * that the colour carries the reading as well as the length does, which suits a value whose
	 * whole design is that every point in the range is its own condition.
	 */
	private static final int FILL_LOW = 0xFF7A1030;

	private static final int TEXT_COLOR = 0xFFF0E6F0;

	/**
	 * Below this ratio the bar starts to tremble, matching {@link SanIconHudElement}. Vanilla
	 * shakes a starving hunger row below the same kind of line.
	 */
	private static final float JITTER_BELOW_RATIO = 0.3F;

	/**
	 * Stand-in for vanilla's food level in the jitter arithmetic, so the period this bar shakes at
	 * is the one the icon row shakes at. Twenty half-steps, the same as ten icons of two halves.
	 */
	private static final int TOTAL_HALVES = 20;

	/**
	 * How long the drop shudder lasts, in ticks. Same as the icon row.
	 */
	private static final int SHUDDER_TICKS = 4;

	/**
	 * How far the bar is lifted while the restore pulse runs, in pixels.
	 *
	 * <p>The icon row sends a wave across ten separate icons, lifting each in turn. A bar is one
	 * shape and has nothing to send a wave <em>across</em>, so the equivalent gesture is to lift the
	 * whole bar for the duration the wave would have taken. Same pixel distance, same length, one
	 * movement instead of ten.
	 */
	private static final int PULSE_LIFT = 2;

	/**
	 * How long a restore pulse lasts, in ticks. Matched to the icon row's sweep so a gain reads as
	 * the same length of event whichever readout is showing.
	 */
	private static final int PULSE_TICKS = SanIconHudElement.ICON_COUNT * 2;

	/**
	 * How far towards white the fill is pushed during a highlight, in {@code [0, 1]}.
	 *
	 * <h2>Why this bar can do with a colour what the icon row needs a second texture for</h2>
	 *
	 * <p>The icon row is blitted, and {@code blitSprite}'s tint is <em>multiplied</em> into the
	 * texture — it can only darken, and the row already passes white, so there is nothing above it
	 * to move towards. That is why {@code SanIconHudElement} ships brightened copies of its sprites.
	 *
	 * <p>This bar has no texture at all: {@code fill} takes the colour outright, so the highlight is
	 * simply a lighter colour computed on the spot. No new art, and the lift is continuous rather
	 * than a two-state swap — though it is still used as an on/off blink, so that the signal reads
	 * the same way in both readouts.
	 *
	 * <p>0.45 rather than 1.0 deliberately. Going the whole way to white loses the violet entirely
	 * and reads as the bar glitching rather than as the bar reacting; a little under half keeps the
	 * hue recognisable while being unmistakably brighter — the same relationship vanilla's
	 * {@code heart/full_blinking} has to {@code heart/full}.
	 */
	private static final float HIGHLIGHT_STRENGTH = 0.45F;

	/**
	 * Flash pacing, identical to {@link SanIconHudElement}: reaching full San blinks four times
	 * quickly, any other gain blinks once slowly.
	 */
	private static final int FULL_FLASH_BLINK_TICKS = 2;

	private static final int FULL_FLASH_BLINKS = 4;

	private static final int GAIN_FLASH_BLINK_TICKS = 5;

	private static final int GAIN_FLASH_BLINKS = 1;

	/**
	 * Source of the jitter offsets. Per instance, matching {@link SanIconHudElement}: the offsets are
	 * decorative and nothing depends on the sequence being reproducible.
	 */
	private final RandomSource random = RandomSource.create();

	/**
	 * The last synced reading this element saw, used to notice that it moved. {@code null} until the
	 * first frame, which is what stops a player logging in at low San from being shown a loss they
	 * did not just suffer.
	 */
	private SanState lastSeen;

	/**
	 * Tick up to which the bar shudders after a loss.
	 */
	private int shudderUntilTick;

	/**
	 * Tick the current restore pulse started on, or zero when none is running.
	 */
	private int pulseStartedTick;

	/**
	 * Tick the current flash started on, or zero when none is running.
	 */
	private int flashStartedTick;

	/**
	 * Length of each on/off phase of the running flash, in ticks. Held rather than derived for the
	 * reason given on {@code SanIconHudElement.flashBlinkTicks}: San is already at its ceiling for
	 * the whole of a full-San flash, so the two flashes cannot be told apart mid-flash.
	 */
	private int flashBlinkTicks;

	/**
	 * Total length of the running flash, in ticks.
	 */
	private int flashTicks;

	/**
	 * When the bar last had a reason to be visible, from {@link Util#getMillis()}. Zero means it
	 * has not been shown yet, which is what keeps a fade from playing on the first frame of a
	 * session.
	 */
	private long lastShownAt;

	@Override
	public void render(GuiGraphics context, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;

		if (player == null) {
			return;
		}

		SanState state = AbyssFallCoreSystem.get(player);

		// Watching for a change here, before anything decides whether to draw, so a loss that
		// happens while the bar is hidden still arms the shudder for the moment it appears.
		noteChange(state, player.tickCount);

		// Noting the moment is the one piece of state this class keeps, and it is updated here
		// rather than in alphaFor so that asking how opaque the bar is — which the layout does
		// every frame, through occupiedHeight — cannot itself keep the bar alive.
		if (AbyssFallConfig.hud().shouldShow(state.percent())) {
			this.lastShownAt = Util.getMillis();
		}

		float alpha = alphaFor(state, Util.getMillis());

		if (alpha <= 0.0F) {
			return;
		}

		// The position is asked for rather than worked out. getHeight returns the top Y of this
		// element's row: vanilla's 39 plus the height of every status bar below it, other mods'
		// included. We draw exactly at that Y, so the bar sits flush on top of the hunger row
		// with the same spacing vanilla uses between its own status bars.
		int top = context.guiHeight()
				- HudStatusBarHeightRegistry.getHeight(AbyssFallSanHud.SAN_BAR_ID);

		String reading = describe(state);

		// Right-aligned with the hunger row, which ends 91 pixels right of centre, so the bar
		// sits squarely over the hunger icons rather than straddling the middle of the screen.
		int left = context.guiWidth() / 2 + 91 - BAR_WIDTH;

		// The player's own tick count drives every animation, the same clock vanilla's hunger row
		// shakes on. Taken from the player rather than kept locally so the movement stays in step
		// with the game's ticking instead of with the frame rate.
		draw(context, minecraft.font, state, left, top, reading, alpha, player.tickCount);
	}

	/**
	 * Notices that the synced reading moved, and arms the matching reaction.
	 *
	 * <p>Identical in behaviour to {@code SanIconHudElement.noteChange}: a fall arms the shudder, a
	 * rise starts the pulse, and reaching the ceiling flashes harder than an ordinary gain. Kept as
	 * its own copy rather than shared, because the two elements hold their animation state
	 * separately — which is what lets a player switch readouts without either one losing its place.
	 */
	private void noteChange(SanState state, int tickCount) {
		SanState previous = this.lastSeen;
		this.lastSeen = state;

		// First frame: adopt the reading without reacting to it. A player logging in with low San
		// has not just lost any.
		if (previous == null || previous.equals(state)) {
			return;
		}

		if (state.current() < previous.current()) {
			this.shudderUntilTick = tickCount + SHUDDER_TICKS;
		} else if (state.current() > previous.current()) {
			this.pulseStartedTick = tickCount;

			if (state.isFull() && !previous.isFull()) {
				armFlash(tickCount, FULL_FLASH_BLINK_TICKS, FULL_FLASH_BLINKS);
			} else {
				armFlash(tickCount, GAIN_FLASH_BLINK_TICKS, GAIN_FLASH_BLINKS);
			}
		}
	}

	/**
	 * Starts a flash of the given pace and length from this tick. Restarts rather than extends, for
	 * the reason given on {@code SanIconHudElement.armFlash}.
	 */
	private void armFlash(int tickCount, int blinkTicks, int blinks) {
		this.flashStartedTick = tickCount;
		this.flashBlinkTicks = blinkTicks;
		this.flashTicks = blinkTicks * blinks * 2;
	}

	/**
	 * The vertical space the bar is claiming right now, for the status bar layout.
	 *
	 * <p>Zero whenever the bar is not being drawn, so a player at full San costs the layout
	 * nothing at all and everything above the hunger row sits exactly where vanilla would put
	 * it. The moment San drops the space is claimed again.
	 *
	 * <p>While fading, the height shrinks along with the opacity rather than holding full size
	 * until the last frame. Holding it would make everything above snap down by the bar's whole
	 * height the instant the fade finished; easing it out means the neighbours drift back into
	 * place at the same rate the bar disappears.
	 *
	 * <p>Free of side effects on purpose. The layout asks this every frame, and it must not be
	 * able to keep the bar alive by asking — only {@link #render} notes that the bar had a reason
	 * to be seen.
	 */
	public int occupiedHeight() {
		float alpha = currentAlpha();

		if (alpha <= 0.0F) {
			return 0;
		}

		return Math.max(1, Math.round(OCCUPIED_HEIGHT * alpha));
	}

	/**
	 * The opacity implied by the client player's current San, or zero when there is no player to
	 * read.
	 */
	private float currentAlpha() {
		Player player = Minecraft.getInstance().player;

		if (player == null) {
			return 0.0F;
		}

		return alphaFor(AbyssFallCoreSystem.get(player), Util.getMillis());
	}

	/**
	 * How opaque the bar should be, in {@code [0, 1]}.
	 *
	 * <p>Fully opaque while the reading is low enough to warrant showing, then easing to nothing
	 * over {@link #FADE_OUT_MILLIS} from the moment it stopped being.
	 *
	 * <p>A mode switch overrides that: for {@link SanHudModeState#REVEAL_MILLIS} the bar is held
	 * fully opaque whatever the reading is, and the fade is measured from the end of that window
	 * rather than from whenever San last warranted showing. Without it, switching readouts at full
	 * San would appear to do nothing — the very moment a player is looking is the moment the row is
	 * normally hidden.
	 */
	private float alphaFor(SanState state, long now) {
		if (AbyssFallConfig.hud().shouldShow(state.percent())) {
			return 1.0F;
		}

		// The later of the two: the last time San itself warranted showing, and the end of the
		// post-switch reveal. Taking the later one is what lets a switch extend a fade that was
		// already running instead of being ignored because the row had recently been visible.
		long from = Math.max(this.lastShownAt, SanHudModeState.revealEndsAt());

		if (from == 0L) {
			return 0.0F;
		}

		if (now < from) {
			// Inside the reveal window.
			return 1.0F;
		}

		long elapsed = now - from;

		if (elapsed >= FADE_OUT_MILLIS) {
			return 0.0F;
		}

		return 1.0F - (float) elapsed / FADE_OUT_MILLIS;
	}

	/**
	 * Draws the bar: border, background, fill, then the label.
	 *
	 * <h2>The jitter</h2>
	 *
	 * <p>Below {@link #JITTER_BELOW_RATIO} the bar is nudged a pixel at intervals that shorten as San
	 * runs out, the same trick vanilla plays with a starving hunger row and the same one
	 * {@link SanIconHudElement} uses. The one difference is that the icon row rolls an offset per
	 * icon so the row scatters, whereas a bar is a single shape and takes one offset for the whole of
	 * it — scattering would mean tearing the bar apart.
	 *
	 * <h2>Reacting to a change</h2>
	 *
	 * <ul>
	 *   <li><strong>a loss</strong> shudders the bar for {@link #SHUDDER_TICKS}, reusing the jitter
	 *       offset so the two cannot double up;</li>
	 *   <li><strong>a gain</strong> lifts the bar by {@link #PULSE_LIFT} for {@link #PULSE_TICKS} and
	 *       blinks it once;</li>
	 *   <li><strong>reaching full San</strong> blinks four times, faster.</li>
	 * </ul>
	 *
	 * <p>The blink lightens the fill towards white rather than swapping artwork, which is the only
	 * part of this that differs in kind from the icon row. See {@link #HIGHLIGHT_STRENGTH}.
	 */
	private void draw(GuiGraphics context, Font font, SanState state, int left, int top,
			String reading, float alpha, int tickCount) {
		int period = jitterPeriod(state);
		boolean shuddering = tickCount < this.shudderUntilTick;

		// One offset for the whole bar, unlike the icon row's per-icon roll: a bar nudged in pieces
		// would tear rather than tremble. A loss shudders regardless of how much is left; the
		// tremor fires on its own schedule.
		if (shuddering || (period > 0 && tickCount % period == 0)) {
			top += this.random.nextInt(3) - 1;
		}

		// Lifted for the length of the pulse. The icon row sends a wave across ten icons; a single
		// shape has nothing to send a wave across, so it rises as one.
		if (pulsingAt(tickCount)) {
			top -= PULSE_LIFT;
		}

		int right = left + BAR_WIDTH;
		int bottom = top + BAR_HEIGHT;

		// Border first, then the interior inset by a pixel, so the outline is not drawn over.
		context.fill(left, top, right, bottom, ARGB.multiplyAlpha(BORDER_COLOR, alpha));
		context.fill(left + 1, top + 1, right - 1, bottom - 1,
				ARGB.multiplyAlpha(BACKGROUND_COLOR, alpha));

		float ratio = Mth.clamp(state.ratio(), 0.0F, 1.0F);
		int trackWidth = BAR_WIDTH - 2;

		// Any San at all keeps at least one pixel of fill. Rounding the last sliver away would
		// draw "almost empty" and "empty" identically, and the difference between those two is
		// exactly what the reading exists to show.
		int fillWidth = ratio > 0.0F ? Math.max(1, Math.round(trackWidth * ratio)) : 0;

		if (fillWidth > 0) {
			int fill = fillColor(ratio);

			if (flashingAt(tickCount)) {
				fill = highlight(fill);
			}

			context.fill(left + 1, top + 1, left + 1 + fillWidth, bottom - 1,
					ARGB.multiplyAlpha(fill, alpha));
		}

		// Vanilla's font is 9 pixels tall against a 10 pixel row, so the single spare pixel goes
		// above the glyphs. Centring honestly rather than nudging down by a fixed amount keeps
		// the label inside the border whatever the row height is changed to later.
		int textX = left + BAR_WIDTH / 2 - font.width(reading) / 2;
		int textY = top + Math.max(0, (BAR_HEIGHT - font.lineHeight) / 2);

		context.drawString(font, reading, textX, textY, ARGB.multiplyAlpha(TEXT_COLOR, alpha));
	}

	/**
	 * Lightens a colour towards white by {@link #HIGHLIGHT_STRENGTH}, leaving its alpha alone.
	 *
	 * <p>Interpolated per channel in plain sRGB, matching {@link #fillColor(float)} — going through
	 * linear light would brighten the darker end disproportionately and make the highlight's
	 * strength depend on how much San the player has, which is not what a signal should do.
	 */
	private static int highlight(int color) {
		return ARGB.color(
				ARGB.alpha(color),
				Mth.lerpInt(HIGHLIGHT_STRENGTH, ARGB.red(color), 255),
				Mth.lerpInt(HIGHLIGHT_STRENGTH, ARGB.green(color), 255),
				Mth.lerpInt(HIGHLIGHT_STRENGTH, ARGB.blue(color), 255));
	}

	/**
	 * Whether the fill should be drawn highlighted this tick.
	 *
	 * <p>Blinks on and off every {@link #flashBlinkTicks} for {@link #flashTicks}, starting bright so
	 * the signal begins the instant San moves. Clears itself once done, or if the clock has gone
	 * backwards — which happens on a world change, since {@code tickCount} restarts from zero.
	 */
	private boolean flashingAt(int tickCount) {
		if (this.flashStartedTick == 0) {
			return false;
		}

		int elapsed = tickCount - this.flashStartedTick;

		if (elapsed < 0 || elapsed >= this.flashTicks) {
			this.flashStartedTick = 0;
			return false;
		}

		// Even phases bright, odd phases normal.
		return (elapsed / this.flashBlinkTicks) % 2 == 0;
	}

	/**
	 * Whether the restore pulse is currently running, and the bar therefore lifted.
	 *
	 * <p>Derived from the clock rather than stepped frame by frame, so the lift lasts the same length
	 * of time however fast the game is drawing and a paused game holds it where it was.
	 */
	private boolean pulsingAt(int tickCount) {
		if (this.pulseStartedTick == 0) {
			return false;
		}

		int elapsed = tickCount - this.pulseStartedTick;

		if (elapsed < 0 || elapsed >= PULSE_TICKS) {
			this.pulseStartedTick = 0;
			return false;
		}

		return true;
	}

	/**
	 * How many ticks apart the jitter should fire, or zero when the bar should sit still.
	 *
	 * <p>The same arithmetic {@link SanIconHudElement} uses, so both readouts tremble at the same
	 * pace: modelled on vanilla's {@code foodLevel * 3 + 1} but driven by the ratio, since San's
	 * ceiling moves and an absolute figure would mean different things to different players.
	 */
	private static int jitterPeriod(SanState state) {
		float ratio = Mth.clamp(state.ratio(), 0.0F, 1.0F);

		if (ratio > JITTER_BELOW_RATIO) {
			return 0;
		}

		return Math.round(ratio * TOTAL_HALVES) * 3 + 1;
	}

	/**
	 * The fill colour for a ratio, interpolated from {@link #FILL_LOW} to {@link #FILL_HIGH}.
	 *
	 * <p>Interpolated channel by channel rather than through a colour utility because the ones
	 * vanilla offers either work on packed sprite colours or go through linear light, and a flat
	 * sRGB blend is both what reads correctly here and what makes the midpoint predictable.
	 */
	private static int fillColor(float ratio) {
		return ARGB.color(
				255,
				Mth.lerpInt(ratio, ARGB.red(FILL_LOW), ARGB.red(FILL_HIGH)),
				Mth.lerpInt(ratio, ARGB.green(FILL_LOW), ARGB.green(FILL_HIGH)),
				Mth.lerpInt(ratio, ARGB.blue(FILL_LOW), ARGB.blue(FILL_HIGH)));
	}

	/**
	 * Formats the reading as {@code San: 90.00%}.
	 *
	 * <p>Only the current reading is shown, as a percentage of the ceiling, because the ceiling
	 * is always {@code 100%} by definition of what a percentage is — writing it out on both sides
	 * of a slash would be redundant. Two decimal places match {@code /san} and the San Counter,
	 * and keep sub-percent movement visible — which matters for a value whose whole design is
	 * that every point in the range is its own condition.
	 */
	private static String describe(SanState state) {
		return String.format(Locale.ROOT, "San: %.2f%%", state.percent());
	}
}

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

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;

import com.abyssfall.AbyssFall;
import com.abyssfall.config.AbyssFallConfig;
import com.abyssfall.core.AbyssFallCoreSystem;
import com.abyssfall.core.SanHudModeState;
import com.abyssfall.core.SanState;

/**
 * Draws the player's own San as a row of ten icons above the hotbar, the way vanilla draws
 * hunger.
 *
 * <h2>Why icons rather than the bar</h2>
 *
 * <p>This is the HUD the player normally sees. It reads at a glance and in the visual language
 * the status bar area already speaks, which is what a permanently visible readout needs; the
 * exact figure is not something the ambient HUD should be spending space on.
 *
 * <p>{@link SanBarHudElement} — the violet bar with the percentage written across it — is
 * deliberately kept and is <em>not</em> registered here. It is the detailed reading, intended for
 * an item that shows it on demand. Keeping both means the ambient display and the precise one can
 * differ in kind rather than one being a degraded version of the other.
 *
 * <h2>Why ten icons for a continuous value</h2>
 *
 * <p>San is continuous and the core is careful never to bucket it, so quantising to twenty halves
 * is a decision made <em>here</em>, for this display only, and nowhere else. Nothing else in the
 * mod learns about it: the ratio is read raw and rounded at the point of drawing. A player who
 * wants the real number has {@code /san}, the San Counter, and — eventually — the bar.
 *
 * <p>The rounding is deliberately generous at the bottom. Any San above zero keeps at least one
 * half icon lit, because rounding the last sliver away would draw "almost gone" and "gone"
 * identically, and that difference is the whole point of the reading.
 *
 * <h2>Where the number comes from</h2>
 *
 * <p>Read from the attachment on the client's own player, which holds the value the server last
 * pushed. The attachment is registered with {@code syncWith(targetOnly())}, so what is drawn is
 * the server's authoritative reading for this player.
 *
 * <h2>Visibility and the fade</h2>
 *
 * <p>Hidden while San sits at or above the configured threshold, shown continuously below it, and
 * faded out over {@link #FADE_OUT_MILLIS} once it climbs back above. Measured in real time rather
 * than ticks because it is a purely visual flourish and should look the same however the server
 * happens to be ticking.
 *
 * <h2>The tremor</h2>
 *
 * <p>Once San falls past {@link #JITTER_BELOW_RATIO} the icons start twitching, faster the less is
 * left, exactly as vanilla's hunger row shakes when a starving player has no saturation. That is
 * copied from {@code Gui.renderFood} rather than invented, so a player who has ever starved
 * already knows what it means.
 *
 * <h2>Reacting to change</h2>
 *
 * <p>The tremor says how bad things are; these two say that something just happened.
 *
 * <p><strong>Losing San</strong> shudders the whole row briefly — a flinch at the moment of loss,
 * distinct from the sustained tremor of simply being low.
 *
 * <p><strong>Gaining San</strong> sends a wave along the row from left to right, lifting each icon
 * two pixels as it passes, and blinks the row once through a brightened copy of the artwork. The
 * wave is vanilla's own regeneration effect, taken from {@code Gui.renderHearts}: position only,
 * no recolouring. The blink is an addition.
 *
 * <p><strong>Reaching full San</strong> blinks the same way but faster and four times over, to say
 * the reading is back at its ceiling rather than merely rising. It replaces the ordinary gain
 * blink rather than stacking with it — topping a player up is one event, so it gets one signal.
 * Vanilla has no equivalent; this exists because full San is precisely the moment the row is about
 * to vanish, and without it recovering would be marked only by the row quietly disappearing.
 *
 * <p>Note that the blink needs its own sprites: see {@link #FULL_FLASH_SPRITE} for why brightening
 * a sprite through {@code blitSprite}'s tint is not possible.
 *
 * <p>Both are noticed by watching the synced value change rather than by subscribing to
 * {@link com.abyssfall.core.SanChangedCallback}: what should be animated is the moment this client
 * <em>sees</em> the number move, which is not necessarily the moment the server moved it.
 */
public final class SanIconHudElement implements HudElement {
	/**
	 * The three sprites, in {@code assets/abyssfall/textures/gui/sprites/hud/}.
	 *
	 * <p>Sprite identifiers, not texture paths: the {@code textures/gui/sprites} prefix and the
	 * {@code .png} suffix are supplied by the GUI atlas, exactly as they are for vanilla's own
	 * {@code hud/food_empty} and friends.
	 */
	private static final Identifier EMPTY_SPRITE = AbyssFall.id("hud/san_empty");

	private static final Identifier HALF_SPRITE = AbyssFall.id("hud/san_half");

	private static final Identifier FULL_SPRITE = AbyssFall.id("hud/san_full");

	/**
	 * Brightened copies of the two lit sprites, shown while the full-San flash runs.
	 *
	 * <h2>Why a second pair of sprites rather than a brighter tint</h2>
	 *
	 * <p>Because a tint cannot brighten anything. The colour passed to {@code blitSprite} is
	 * <em>multiplied</em> into the texture, so it can only ever darken; and the tint this element
	 * normally passes is {@link ARGB#white(float)}, whose colour channels are already {@code 0xFF}.
	 * There is nothing above white to move towards.
	 *
	 * <p>Vanilla solves it the same way and it is worth knowing where to look: hearts have a
	 * {@code hud/heart/full_blinking} sprite next to {@code hud/heart/full}, the same artwork with
	 * its colours lifted ({@code 0xFF1313} becomes {@code 0xFFA1A1}). The blink is a texture swap,
	 * not a shader trick.
	 */
	private static final Identifier FULL_FLASH_SPRITE = AbyssFall.id("hud/san_full_blinking");

	private static final Identifier HALF_FLASH_SPRITE = AbyssFall.id("hud/san_half_blinking");

	/**
	 * Number of icons in the row, matching the hunger row so the two read as one block.
	 */
	public static final int ICON_COUNT = 10;

	/**
	 * Size each icon is drawn at. Nine pixels is what vanilla blits its status icons at, and what
	 * the sprite files themselves are.
	 */
	public static final int ICON_SIZE = 9;

	/**
	 * Horizontal step between icons. Eight, so each icon overlaps the previous one by a pixel —
	 * vanilla's own spacing, which is what makes a row of nine-pixel icons span 81 pixels rather
	 * than 90.
	 */
	public static final int ICON_STRIDE = 8;

	/**
	 * Total width of the row: nine strides plus the last icon's full width.
	 */
	public static final int ROW_WIDTH = ICON_STRIDE * (ICON_COUNT - 1) + ICON_SIZE;

	/**
	 * Vertical space this element claims, reported to {@link HudStatusBarHeightRegistry}.
	 *
	 * <p>Ten rather than nine: vanilla allots each status bar a ten-pixel row, so claiming the
	 * same makes the gap between this row and hunger identical to the gaps vanilla leaves between
	 * its own rows.
	 */
	public static final int OCCUPIED_HEIGHT = 10;

	/**
	 * Total number of halves the row can show: two per icon.
	 */
	private static final int TOTAL_HALVES = ICON_COUNT * 2;

	/**
	 * The San ratio at or below which the icons start to jitter.
	 *
	 * <p>A fifth, matching how far vanilla lets hunger fall before its own row starts shaking:
	 * saturation is gone well before the bar is, and from that point on every icon twitches. The
	 * threshold is a property of this display and of nothing else — the core still knows nothing
	 * about ratios that matter.
	 */
	private static final float JITTER_BELOW_RATIO = 0.2F;

	/**
	 * Source of the jitter offsets.
	 *
	 * <p>Unseeded, and shared by every icon in the row: the offsets are meant to be uncorrelated,
	 * so each icon jumping independently is the desired result rather than a defect. Nothing here
	 * needs to be reproducible, and nothing but rendering reads it, so an instance field on a
	 * client-only element is as far as this state ever travels.
	 */
	private final RandomSource random = RandomSource.create();

	/**
	 * How long the row takes to disappear once San is no longer low enough to warrant showing it.
	 * Long enough to read as a fade rather than as a flicker.
	 */
	private static final long FADE_OUT_MILLIS = 1000L;

	/**
	 * When the row last had a reason to be visible, from {@link Util#getMillis()}. Zero means it
	 * has not been shown yet, which is what keeps a fade from playing on the first frame of a
	 * session.
	 */
	private long lastShownAt;

	/**
	 * The reading this element last saw, used to notice a change without being told about one.
	 *
	 * <p>Polled rather than driven by {@link com.abyssfall.core.SanChangedCallback}. That event is
	 * fired on the server for authoritative changes; what this element must react to is the moment
	 * the <em>client</em> learns the value moved, which is when the attachment sync lands. Watching
	 * the synced value is what makes the reaction match what the player is looking at, and it also
	 * keeps this a pure renderer with no cross-side subscription to unregister.
	 *
	 * <p>{@code null} until the first frame, so arriving in a world with less than full San does
	 * not fire a reaction for San the player never saw drop.
	 */
	private SanState lastSeen;

	/**
	 * Tick at which the drop shudder ends, on the player's own clock. Zero when not shuddering.
	 *
	 * <p>A deadline rather than a countdown, so it does not matter how many times a frame this is
	 * consulted — the shudder lasts a fixed span of game time either way.
	 */
	private int shudderUntilTick;

	/**
	 * Tick at which the current restore pulse started, or zero when no pulse is running.
	 *
	 * <p>The pulse's position along the row is derived from how far past this tick the clock is,
	 * which is what makes the wave travel at a fixed speed regardless of frame rate.
	 */
	private int pulseStartedTick;

	/**
	 * Tick at which the current flash started, or zero when the row is not flashing.
	 */
	private int flashStartedTick;

	/**
	 * How long each on/off phase of the running flash lasts, in ticks.
	 *
	 * <p>Held as state rather than derived, because which flash is running is decided when it is
	 * armed and must not be re-derived while it plays: San is already at its ceiling for the whole
	 * of a full-San flash, so asking "is the player full?" mid-flash cannot distinguish the two.
	 */
	private int flashBlinkTicks;

	/**
	 * Total length of the running flash, in ticks.
	 */
	private int flashTicks;

	/**
	 * How long the drop shudder lasts, in ticks.
	 *
	 * <p>Short on purpose: it marks the instant San was lost, and is not meant to be confused with
	 * the sustained tremor of being at low San. Long enough to register at 20 ticks a second,
	 * short enough that repeated small losses read as repeated flinches rather than as one
	 * continuous shake.
	 */
	private static final int SHUDDER_TICKS = 4;

	/**
	 * How many ticks each icon of the restore pulse stays raised, and therefore how fast the wave
	 * crosses the row.
	 *
	 * <p>Two ticks per icon puts the whole sweep at twenty ticks — one second for the wave to
	 * cross ten icons, which is close to the cadence vanilla's regeneration wave runs at and slow
	 * enough to be followed by eye.
	 */
	private static final int PULSE_TICKS_PER_ICON = 2;

	/**
	 * Total length of one restore pulse, in ticks.
	 */
	private static final int PULSE_TICKS = ICON_COUNT * PULSE_TICKS_PER_ICON;

	/**
	 * How far the icon under the restore pulse is lifted, in pixels.
	 *
	 * <p>Two, which is exactly what vanilla lifts a regenerating heart by — see
	 * {@code Gui.renderHearts}, where the heart the wave is currently on has its Y reduced by 2.
	 * The lift is the whole of the pulse: vanilla does not recolour the heart, and neither does
	 * this.
	 */
	private static final int PULSE_LIFT = 2;

	/**
	 * How many ticks each on/off phase of a flash lasts, and how many times it blinks.
	 *
	 * <p>Two flashes exist, and they differ only in these numbers:
	 *
	 * <ul>
	 *   <li><strong>reaching full San</strong> — quick and insistent, four blinks. It marks an
	 *       arrival, and it is the last thing the row does before fading out, so it has to register
	 *       in the moment;</li>
	 *   <li><strong>any other gain</strong> — one slow blink, a single acknowledgement that
	 *       something was given back. Deliberately calmer, because partial restores may come in
	 *       quick succession and four fast blinks each time would turn the row into a strobe.</li>
	 * </ul>
	 *
	 * <p>The pace is what separates them rather than the brightness: both use the same brightened
	 * artwork, since it is a texture swap and there is no dimmer to turn.
	 *
	 * <p>A flash blinks rather than holding steady because the bright version is a fixed second
	 * texture — brightness cannot be eased down over time the way an alpha could, so alternation is
	 * the only shape available.
	 */
	private static final int FULL_FLASH_BLINK_TICKS = 2;

	private static final int FULL_FLASH_BLINKS = 4;

	private static final int GAIN_FLASH_BLINK_TICKS = 5;

	private static final int GAIN_FLASH_BLINKS = 1;

	@Override
	public void render(GuiGraphics context, DeltaTracker tickCounter) {
		Player player = Minecraft.getInstance().player;

		if (player == null) {
			return;
		}

		SanState state = AbyssFallCoreSystem.get(player);

		// Watching for a change here, before anything decides whether to draw, so a loss that
		// happens while the row is hidden still arms the shudder for the moment it appears.
		noteChange(state, player.tickCount);

		// Noting the moment is the one piece of state this class keeps, and it is updated here
		// rather than in alphaFor so that asking how tall the row is — which the layout does
		// every frame, through occupiedHeight — cannot itself keep the row alive.
		if (AbyssFallConfig.hud().shouldShow(state.percent())) {
			this.lastShownAt = Util.getMillis();
		}

		float alpha = alphaFor(state, Util.getMillis());

		if (alpha <= 0.0F) {
			return;
		}

		// The position is asked for rather than worked out. getHeight returns the top Y of this
		// element's row: vanilla's own offset plus the height of every status bar below it, other
		// mods' included.
		int top = context.guiHeight()
				- HudStatusBarHeightRegistry.getHeight(AbyssFallSanHud.SAN_BAR_ID);

		// Right-aligned with the hunger row, which ends 91 pixels right of centre.
		int left = context.guiWidth() / 2 + 91 - ROW_WIDTH;

		// The player's own tick count drives the jitter, the same clock vanilla's hunger row
		// shakes on. Taken from the player rather than kept locally so the twitch stays in step
		// with the game's ticking instead of with the frame rate: at 200fps a per-frame counter
		// would shake far faster than vanilla does, and pausing would not stop it.
		draw(context, state, left, top, alpha, player.tickCount);
	}

	/**
	 * Notices that the synced reading moved, and arms the matching reaction.
	 *
	 * <p>A fall arms the shudder; a rise starts the restore pulse. Compared on the whole state
	 * rather than on the ratio, so a change to the ceiling alone is seen too — a shrinking ceiling
	 * takes San with it and ought to flinch like any other loss.
	 *
	 * <p>Restarting rather than extending: each fresh loss re-arms the shudder from this tick, and
	 * each fresh gain restarts the pulse from the left. Extending would let a long drain blur into
	 * the sustained low-San tremor, and letting a pulse run to completion before honouring the
	 * next gain would make rapid restores look like they were being ignored.
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

			// Every gain flashes; reaching the ceiling flashes faster and more insistently. The
			// full-San case is checked first and wins outright rather than the two being layered,
			// since a restore that tops the player up is one event and should read as one signal.
			// Guarded on the previous state not already being full, so this fires on the
			// transition and not on a gain a player at full San might somehow receive.
			if (state.isFull() && !previous.isFull()) {
				armFlash(tickCount, FULL_FLASH_BLINK_TICKS, FULL_FLASH_BLINKS);
			} else {
				armFlash(tickCount, GAIN_FLASH_BLINK_TICKS, GAIN_FLASH_BLINKS);
			}
		}
	}

	/**
	 * Starts a flash of the given pace and length from this tick.
	 *
	 * <p>Restarts rather than extends: a fresh gain arriving mid-flash begins its own, so a slow
	 * gain flash is cut short by the full-San flash that follows it rather than the two queueing
	 * up. The alternative — letting the first finish — would delay the more important signal.
	 *
	 * <p>{@code blinks} counts bright phases, and a blink is a bright phase followed by a dark one,
	 * so the total runs {@code blinks * 2} phases. The last dark phase is included on purpose: it
	 * is what makes the final blink read as a blink rather than as the row simply going bright and
	 * staying there until the flash expires.
	 */
	private void armFlash(int tickCount, int blinkTicks, int blinks) {
		this.flashStartedTick = tickCount;
		this.flashBlinkTicks = blinkTicks;
		this.flashTicks = blinkTicks * blinks * 2;
	}

	/**
	 * The vertical space the row is claiming right now, for the status bar layout.
	 *
	 * <p>Zero whenever the row is not being drawn, so a player at full San costs the layout
	 * nothing at all and everything above the hunger row sits exactly where vanilla would put it.
	 *
	 * <p>While fading, the height shrinks along with the opacity rather than holding full size
	 * until the last frame. Holding it would make everything above snap down by the row's whole
	 * height the instant the fade finished; easing it out means the neighbours drift back into
	 * place at the same rate the row disappears.
	 *
	 * <p>Free of side effects on purpose. The layout asks this every frame, and it must not be
	 * able to keep the row alive by asking — only {@link #render} notes that the row had a reason
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
	 * How opaque the row should be, in {@code [0, 1]}.
	 *
	 * <p>Fully opaque while the reading is low enough to warrant showing, then easing to nothing
	 * over {@link #FADE_OUT_MILLIS} from the moment it stopped being.
	 *
	 * <p>A mode switch overrides that: for {@link SanHudModeState#REVEAL_MILLIS} the row is held
	 * fully opaque whatever the reading is, and the fade is measured from the end of that window
	 * rather than from whenever San last warranted showing. Without it, switching readouts at full
	 * San would appear to do nothing — the very moment a player is looking is the moment the row is
	 * normally hidden. Both readouts do this identically, so a switch reveals whichever one is being
	 * switched to.
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
	 * Draws the row: every slot's empty sprite first, then the lit ones over the top.
	 *
	 * <p>Two passes rather than one, following vanilla, because the lit sprites have transparent
	 * gaps that the empty sprite underneath shows through — that is what makes the half sprite
	 * read as half a lit icon rather than as a shape cut off in mid-air.
	 *
	 * <p>The fade is applied as a tint rather than by any global alpha state: the seven-argument
	 * {@code blitSprite} takes a packed ARGB colour, and {@link ARGB#white(float)} turns an
	 * opacity into exactly that — white at the requested alpha, which multiplies the sprite's own
	 * colours by nothing and its alpha by the fade.
	 *
	 * <h2>The jitter</h2>
	 *
	 * <p>Below {@link #JITTER_BELOW_RATIO} each icon is nudged a pixel up or down at intervals
	 * that shorten as San runs out, which is exactly the trick vanilla plays with a starving
	 * hunger row: {@code Gui.renderFood} offsets each icon by {@code random.nextInt(3) - 1}
	 * whenever {@code tickCount % (foodLevel * 3 + 1) == 0}.
	 *
	 * <p>Two details of that formula are worth copying rather than inventing. The period is driven
	 * by how much is <em>left</em>, so the shake accelerates on its own as the reading falls and
	 * becomes continuous at zero — no separate "critical" state is needed. And the offset is rolled
	 * per icon inside the loop, so the row scatters instead of hopping in unison, which is what
	 * makes it read as a tremor rather than as a moving bar.
	 *
	 * <p>Both sprites of a slot take the same offset, or the lit sprite would tear away from the
	 * empty one it is drawn over.
	 *
	 * <h2>Reacting to a change</h2>
	 *
	 * <p>Two further reactions ride on top of the tremor, both driven by
	 * {@link #noteChange(SanState, int)} rather than by the reading itself:
	 *
	 * <ul>
	 *   <li><strong>a loss</strong> shudders the whole row for {@link #SHUDDER_TICKS}, using the
	 *       same per-icon offset the tremor does. A row that is already trembling simply keeps
	 *       trembling — the two cannot double up, because an icon can only be one pixel out of
	 *       place at a time;</li>
	 *   <li><strong>a gain</strong> sends a wave from left to right, lifting each icon by
	 *       {@link #PULSE_LIFT} as it passes, which is what vanilla does for a regenerating
	 *       player, and blinks the row once through its brightened sprites;</li>
	 *   <li><strong>reaching full San</strong> blinks the same sprites four times, faster.</li>
	 * </ul>
	 */
	private void draw(GuiGraphics context, SanState state, int left, int top, float alpha,
			int tickCount) {
		int halves = halvesFor(state);
		int period = jitterPeriod(state);
		boolean shuddering = tickCount < this.shudderUntilTick;
		int pulseIcon = pulseIconAt(tickCount);
		boolean flashing = flashingAt(tickCount);
		int tint = ARGB.white(alpha);

		// Swapped wholesale rather than tinted: the bright versions are separate artwork, because
		// a multiplied tint cannot make anything brighter than the texture already is.
		Identifier full = flashing ? FULL_FLASH_SPRITE : FULL_SPRITE;
		Identifier half = flashing ? HALF_FLASH_SPRITE : HALF_SPRITE;

		for (int i = 0; i < ICON_COUNT; i++) {
			int x = left + i * ICON_STRIDE;
			int y = top;

			// Rolled per icon, so the row scatters rather than hopping as one block. A loss
			// shudders regardless of how much is left; the tremor fires on its own schedule.
			if (shuddering || (period > 0 && tickCount % period == 0)) {
				y += this.random.nextInt(3) - 1;
			}

			// The wave lifts exactly one icon at a time, so it reads as something travelling
			// along the row. Position only — vanilla does not recolour the heart it lifts.
			if (i == pulseIcon) {
				y -= PULSE_LIFT;
			}

			context.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY_SPRITE, x, y,
					ICON_SIZE, ICON_SIZE, tint);

			int filled = halves - i * 2;

			if (filled >= 2) {
				context.blitSprite(RenderPipelines.GUI_TEXTURED, full, x, y,
						ICON_SIZE, ICON_SIZE, tint);
			} else if (filled == 1) {
				context.blitSprite(RenderPipelines.GUI_TEXTURED, half, x, y,
						ICON_SIZE, ICON_SIZE, tint);
			}
		}
	}

	/**
	 * Whether the row should be drawn with its brightened sprites this tick.
	 *
	 * <p>Blinks on and off every {@link #flashBlinkTicks} for {@link #flashTicks}, starting on the
	 * bright phase so the signal begins the instant San moves.
	 *
	 * <p>Clears the flash — and returns {@code false} — once it has run its course, or if the clock
	 * has gone backwards, which happens on a world change since {@code tickCount} restarts from
	 * zero.
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
	 * Which icon the restore pulse is currently over, or {@code -1} when no pulse is running.
	 *
	 * <p>Derived from the clock rather than stepped frame by frame, so the wave crosses the row at
	 * the same speed however fast the game is drawing, and a paused game leaves it where it was.
	 */
	private int pulseIconAt(int tickCount) {
		if (this.pulseStartedTick == 0) {
			return -1;
		}

		int elapsed = tickCount - this.pulseStartedTick;

		// Past the end, or the clock went backwards — which happens on a world change, since
		// tickCount restarts. Either way the pulse is over.
		if (elapsed < 0 || elapsed >= PULSE_TICKS) {
			this.pulseStartedTick = 0;
			return -1;
		}

		return elapsed / PULSE_TICKS_PER_ICON;
	}

	/**
	 * How many ticks apart the jitter should fire, or zero when the row should sit still.
	 *
	 * <p>Modelled on vanilla's {@code foodLevel * 3 + 1}, but driven by the ratio rather than by
	 * an absolute reading, since San's ceiling moves and an absolute figure would mean different
	 * things to different players. The remaining halves stand in for vanilla's food level, which
	 * keeps the arithmetic — and the feel — the same: a comfortable stutter as the shaking starts,
	 * every tick once the reading bottoms out.
	 */
	private static int jitterPeriod(SanState state) {
		float ratio = Mth.clamp(state.ratio(), 0.0F, 1.0F);

		if (ratio > JITTER_BELOW_RATIO) {
			return 0;
		}

		return Math.round(ratio * TOTAL_HALVES) * 3 + 1;
	}

	/**
	 * How many half icons to light for a given state, in {@code [0, 20]}.
	 *
	 * <p>Rounded to nearest so the row tracks the reading as closely as twenty steps allow, with
	 * one exception: any San at all keeps a half icon lit rather than rounding down to an empty
	 * row. "Almost gone" and "gone" must not draw identically.
	 */
	private static int halvesFor(SanState state) {
		float ratio = Mth.clamp(state.ratio(), 0.0F, 1.0F);

		if (ratio <= 0.0F) {
			return 0;
		}

		return Math.max(1, Math.round(ratio * TOTAL_HALVES));
	}
}

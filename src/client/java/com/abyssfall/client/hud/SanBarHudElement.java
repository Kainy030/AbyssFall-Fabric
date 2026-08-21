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
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;

import com.abyssfall.config.AbyssFallConfig;
import com.abyssfall.core.AbyssFallCoreSystem;
import com.abyssfall.core.SanState;

/**
 * Draws the player's own San as a bar above the hotbar, with the reading written across it.
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
 * version asked for: the mod has no art for a San bar yet, and a placeholder built from solid
 * colours can be restyled or replaced outright without leaving an unused sprite behind.
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

		draw(context, minecraft.font, state, left, top, reading, alpha);
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
	 */
	private float alphaFor(SanState state, long now) {
		if (AbyssFallConfig.hud().shouldShow(state.percent())) {
			return 1.0F;
		}

		if (this.lastShownAt == 0L) {
			return 0.0F;
		}

		long elapsed = now - this.lastShownAt;

		if (elapsed >= FADE_OUT_MILLIS) {
			return 0.0F;
		}

		return 1.0F - (float) elapsed / FADE_OUT_MILLIS;
	}

	private static void draw(GuiGraphics context, Font font, SanState state, int left, int top,
			String reading, float alpha) {
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
			context.fill(left + 1, top + 1, left + 1 + fillWidth, bottom - 1,
					ARGB.multiplyAlpha(fillColor(ratio), alpha));
		}

		// Vanilla's font is 9 pixels tall against a 10 pixel row, so the single spare pixel goes
		// above the glyphs. Centring honestly rather than nudging down by a fixed amount keeps
		// the label inside the border whatever the row height is changed to later.
		int textX = left + BAR_WIDTH / 2 - font.width(reading) / 2;
		int textY = top + Math.max(0, (BAR_HEIGHT - font.lineHeight) / 2);

		context.drawString(font, reading, textX, textY, ARGB.multiplyAlpha(TEXT_COLOR, alpha));
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

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

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.abyssfall.core.AbyssFallCoreSystem;
import com.abyssfall.core.SanState;

/**
 * San Counter — a developer tool that reports the holder's own San reading on demand.
 *
 * <p>Right-clicking asks the server for the value and shows it above the health and hunger
 * bars as {@code current / max}. This exists because the San system has no HUD yet and is
 * not going to get one soon, but authoring and testing still need a way to watch the number
 * move without opening the chat and typing {@code /san}.
 *
 * <h2>Why the reading is taken on the server</h2>
 *
 * <p>{@link #use} runs on both sides, and the client's copy of the attachment is only ever a
 * mirror of what the server last pushed. Reading it client-side would report whatever the
 * client happens to believe, which is precisely the thing a debug tool must not do. So the
 * client half of the call does nothing but report the swing, and the value is read from the
 * authoritative state and delivered as an overlay message.
 *
 * <h2>Why there is no timer here</h2>
 *
 * <p>The three second display and its fade are vanilla behaviour, not something this class
 * arranges. {@code ServerPlayer.displayClientMessage(component, true)} sends a system chat
 * packet flagged as an overlay, which the client hands to {@code Gui.setOverlayMessage}; that
 * sets its countdown to 60 ticks and the renderer fades the text out over the final 20 of
 * them. Pressing again simply resets the countdown, so repeated use extends the display
 * rather than stacking anything up.
 */
public class SanCounterItem extends Item {
	/**
	 * Translation key for the readout. Takes the current reading and the ceiling as
	 * arguments, in that order.
	 */
	public static final String READOUT_KEY = "item.abyssfall.san_counter.readout";

	public SanCounterItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			SanState state = AbyssFallCoreSystem.get(serverPlayer);

			// The 'true' is what makes this an overlay message rather than a chat line, and
			// with it comes the three second lifetime and the fade.
			serverPlayer.displayClientMessage(describe(state), true);
		}

		// Reported as a client-side swing so the hand animation plays the moment the button
		// is pressed, without waiting for the round trip that produces the message.
		return InteractionResult.SUCCESS;
	}

	/**
	 * Formats a San state as {@code current / max}, matching the two decimal places
	 * {@code /san} reports so that the two tools can be read against each other. Two
	 * decimals also keep sub-percent movement visible, which is the point of a continuous
	 * value.
	 */
	private static Component describe(SanState state) {
		return Component.translatable(READOUT_KEY,
				format(state.current()),
				format(state.max()));
	}

	private static String format(float value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}

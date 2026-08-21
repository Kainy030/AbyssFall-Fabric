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

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.abyssfall.core.SanHudMode;
import com.abyssfall.core.SanHudModeState;

/**
 * Cognition Lens — switches the San readout between the icon row and the percentage bar.
 *
 * <p>Built from the San Counter, and deliberately not an extension of it. The two share a shape —
 * a single-stack item whose right click reports something above the hotbar — but they belong to
 * different layers of the design: the counter is developer tooling that prints the raw float and is
 * gated behind {@code developer.dev_tools}, whereas the lens is player-facing content that only
 * ever changes how an already-visible reading is drawn. Keeping them separate is what lets the
 * counter stay out of a released build while the lens ships.
 *
 * <h2>Why this runs on the client and not the server</h2>
 *
 * <p>The opposite of the San Counter, and for a matching reason. The counter reads a value the
 * server owns, so it does its work server-side. The lens changes nothing the server owns: which
 * readout is drawn is a fact about one screen, so the switch happens on the client that pressed the
 * button and never leaves it. Sending it to the server would mean two players sharing a world could
 * not read their San differently, and would make a display preference into save data.
 *
 * <p>This is also why {@link #use} tests for the <em>logical</em> client rather than for a
 * {@code ServerPlayer}. On a single-player world both sides run in one process, so without that
 * test the switch would fire twice and cancel itself out.
 */
public class SanLensItem extends Item {
	/**
	 * Translation key for the message shown on switching. Takes the name of the mode now in
	 * effect as its one argument.
	 */
	public static final String SWITCHED_KEY = "item.abyssfall.san_lens.switched";

	public SanLensItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		// Client only, and only once: use() is called on both logical sides, and this changes
		// client state, so the server half must not touch it.
		if (level.isClientSide()) {
			SanHudMode mode = SanHudModeState.toggle();

			// The 'true' makes this an overlay message above the hotbar rather than a chat line,
			// and brings vanilla's three second lifetime and fade with it. Sent locally rather
			// than through the server, since the client already knows what happened.
			player.displayClientMessage(describe(mode), true);
		}

		// SUCCESS on both sides so the hand animation plays immediately.
		return InteractionResult.SUCCESS;
	}

	/**
	 * Names the mode that is now in effect.
	 */
	private static Component describe(SanHudMode mode) {
		return Component.translatable(SWITCHED_KEY, Component.translatable(mode.translationKey()));
	}
}

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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * HeldItemCensus (手持普查) — a server-wide count of how many players currently hold a
 * matching stack, recounted every tick and reported only when it moves.
 *
 * <p>Like every mechanic in this package, a census knows nothing about <em>what</em> it
 * watches: the eligibility predicate arrives from outside, and this class must never come
 * to name any particular item. A feature that wants the number registers one here and keeps
 * the meaning of the number to itself.
 *
 * <h2>Derived, never accumulated</h2>
 *
 * <p>The count is rebuilt from zero every single tick by looking at every player's hands.
 * Nothing is incremented when a stack appears or decremented when one leaves, so the number
 * cannot drift away from reality: a blade that is sheathed, dropped, or carried offline by
 * a disconnect is simply absent from the next recount. A derived figure has no memory to
 * keep honest — the same reason the San core stores a value rather than a history.
 *
 * <h2>People, not stacks</h2>
 *
 * <p>A player holding a matching stack in each hand still counts once: the census answers
 * "how many players", and both hands belong to the same person.
 *
 * <h2>One tick hook for all censuses</h2>
 *
 * <p>The first registration installs the single server-tick listener every census shares,
 * so there is nothing for the composition root to initialise and no ordering to get wrong.
 */
public final class HeldItemCensus {
	/**
	 * Notified that a census's count changed. Fired on the server thread, after the new
	 * count has been stored.
	 *
	 * @param server   the server the count belongs to
	 * @param previous the count before this tick
	 * @param current  the count now — including zero when the last matching stack leaves
	 *                 every hand
	 */
	@FunctionalInterface
	public interface CountChangedListener {
		void onCountChanged(MinecraftServer server, int previous, int current);
	}

	/**
	 * Every registered census. Read every server tick, written only during mod
	 * initialisation — copy-on-write keeps the two phases apart, same as the mechanic
	 * grants in {@link NeverDestroyed}.
	 */
	private static final List<HeldItemCensus> REGISTERED = new CopyOnWriteArrayList<>();

	private static boolean tickHookInstalled;

	private final Predicate<ItemStack> eligibility;
	private final CountChangedListener listener;

	private int count;

	private HeldItemCensus(Predicate<ItemStack> eligibility, CountChangedListener listener) {
		this.eligibility = eligibility;
		this.listener = listener;
	}

	/**
	 * Starts watching hands for stacks matching {@code eligibility}. The returned census
	 * answers the current count at any time — for example when a player joins and needs
	 * the truth pushed to them.
	 */
	public static HeldItemCensus register(Predicate<ItemStack> eligibility,
			CountChangedListener listener) {
		installTickHookOnce();

		HeldItemCensus census = new HeldItemCensus(eligibility, listener);
		REGISTERED.add(census);
		return census;
	}

	/**
	 * How many players currently hold a matching stack.
	 */
	public int count() {
		return this.count;
	}

	private static void installTickHookOnce() {
		if (!tickHookInstalled) {
			tickHookInstalled = true;
			ServerTickEvents.END_SERVER_TICK.register(HeldItemCensus::onEndTick);
		}
	}

	private static void onEndTick(MinecraftServer server) {
		if (REGISTERED.isEmpty()) {
			return;
		}

		List<ServerPlayer> players = server.getPlayerList().getPlayers();

		for (HeldItemCensus census : REGISTERED) {
			int count = 0;

			for (ServerPlayer player : players) {
				if (census.isHeldBy(player)) {
					count++;
				}
			}

			if (count != census.count) {
				int previous = census.count;
				census.count = count;
				census.listener.onCountChanged(server, previous, count);
			}
		}
	}

	private boolean isHeldBy(ServerPlayer player) {
		return this.eligibility.test(player.getMainHandItem())
				|| this.eligibility.test(player.getOffhandItem());
	}
}

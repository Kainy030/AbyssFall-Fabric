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

package com.abyssfall.core;

import java.util.function.UnaryOperator;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import com.abyssfall.AbyssFall;

/**
 * The San (sanity) system — the value the whole mod is built to move.
 *
 * <p>Every player carries a San reading and a personal ceiling for it. The reading is authored
 * exclusively on the server: nothing on the client is trusted to change it, and the client only
 * ever receives the resulting value so that it can eventually render the world differently for
 * a player who is coming apart. That rendering is not implemented yet; this class exists to make
 * the number exist, persist, be reachable, and announce its own changes.
 *
 * <h2>San is continuous, not staged</h2>
 *
 * <p>There are no tiers here, and there deliberately never will be at this level. The core's whole
 * responsibility is to report {@code current}, {@code max} and their {@code ratio} accurately, and
 * to fire {@link SanChangedCallback} whenever any of them move. Every point in the {@code [0, 1]}
 * ratio span is a distinct condition, so a drop of a tenth of a percent is as much a real event as
 * a collapse to zero, and consumers are free to respond to it. Where the interesting boundaries lie
 * — if a given feature even has boundaries rather than scaling smoothly — is a decision for that
 * feature, made against the raw ratio. Baking thresholds into the core would force every consumer
 * to share one opinion about what San means.
 *
 * <h2>Why an attachment rather than a mixin</h2>
 *
 * <p>Fabric's Data Attachment API already does the three hard parts: it serialises the value
 * into the player's own save data, it survives death and respawn, and it pushes changes to the
 * owning client automatically. Injecting into {@code Player} to add a field would mean writing
 * save/load hooks, a respawn copy hook and a sync packet by hand, and would collide with any
 * other mod doing the same. There is nothing here that the API cannot express, so there is no
 * case for a mixin.
 *
 * <h2>Reading and writing</h2>
 *
 * <p>Read with {@link #get(Player)} — safe on either side, and safe before the value has ever
 * been written. Write with {@link #set}, {@link #modify}, or the {@code add*} helpers, all of
 * which require a {@link ServerPlayer} precisely because writes are a server concern.
 */
public final class AbyssFallCoreSystem {
	/**
	 * Registry name of the San attachment. Also the key the value is stored under in the
	 * player's save data, so changing it would orphan existing saves.
	 */
	public static final String SAN_ATTACHMENT_NAME = "core_system_san";

	/**
	 * The San attachment itself.
	 *
	 * <p>Configured to:
	 * <ul>
	 *   <li>{@code initializer} — hand out {@link SanState#INITIAL} the first time a player is
	 *       asked about their San, which is what gives every player 100.00F on first entering a
	 *       world without needing a join hook to seed it;</li>
	 *   <li>{@code persistent} — survive a restart;</li>
	 *   <li>{@code copyOnDeath} — survive dying. San is a record of what a player has been
	 *       through, and respawning does not undo that;</li>
	 *   <li>{@code syncWith(targetOnly)} — reach the owning client and nobody else. Other
	 *       players have no business knowing this number, and the future rendering changes are
	 *       first-person by nature.</li>
	 * </ul>
	 */
	public static final AttachmentType<SanState> SAN = AttachmentRegistry.create(
			AbyssFall.id(SAN_ATTACHMENT_NAME),
			builder -> builder
					.initializer(() -> SanState.INITIAL)
					.persistent(SanState.CODEC)
					.copyOnDeath()
					.syncWith(SanState.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	private AbyssFallCoreSystem() {
	}

	public static void initialize() {
		// Touching the attachment on join forces the initializer to run and be written, so the
		// value is present in save data and pushed to the client from the very first tick
		// instead of only once something happens to change it. Reads alone would otherwise
		// leave a brand new player with no stored attachment at all.
		ServerPlayerEvents.JOIN.register(player -> {
			SanState state = player.getAttachedOrCreate(SAN);
			AbyssFall.LOGGER.debug("San for {}: {}/{} ({}%)",
					player.getGameProfile().name(), state.current(), state.max(), state.percent());
		});
	}

	/**
	 * The player's current San state, never {@code null}.
	 *
	 * <p>Falls back to {@link SanState#INITIAL} rather than initialising, so this is safe to
	 * call from the client and from contexts where writing would be wrong.
	 */
	public static SanState get(Player player) {
		return player.getAttachedOrElse(SAN, SanState.INITIAL);
	}

	/**
	 * Shorthand for {@code get(player).current()}.
	 */
	public static float getCurrent(Player player) {
		return get(player).current();
	}

	/**
	 * Shorthand for {@code get(player).max()}.
	 */
	public static float getMax(Player player) {
		return get(player).max();
	}

	/**
	 * Shorthand for {@code get(player).ratio()} — the continuous {@code [0, 1]} parameter that
	 * anything reacting to San should be written against.
	 */
	public static float getRatio(Player player) {
		return get(player).ratio();
	}

	/**
	 * Shorthand for {@code get(player).percent()}.
	 */
	public static float getPercent(Player player) {
		return get(player).percent();
	}

	/**
	 * Replaces the player's whole San state and fires {@link SanChangedCallback}.
	 *
	 * <p>This is the single funnel every write goes through, which is what lets the event be
	 * dispatched from one place. The attachment API's own {@code onAttachedSet} is per-target and
	 * would have to be subscribed separately for each player, so routing through here is both
	 * simpler and the only point that sees every change.
	 *
	 * @return the stored state, which may differ from {@code state} if it needed clamping
	 */
	public static SanState set(ServerPlayer player, SanState state) {
		SanState previous = get(player);
		player.setAttached(SAN, state);
		SanState stored = get(player);

		SanChangedCallback.EVENT.invoker()
				.onSanChanged(new SanChangedCallback.Change(player, previous, stored));

		return stored;
	}

	/**
	 * Applies an arbitrary transformation to the player's San state.
	 *
	 * <p>This is the general primitive the other mutators are built from; reach for it when a
	 * change depends on the existing value in a way the helpers do not cover.
	 *
	 * @return the resulting state
	 */
	public static SanState modify(ServerPlayer player, UnaryOperator<SanState> operator) {
		return set(player, operator.apply(get(player)));
	}

	/**
	 * Moves the player's San by {@code delta}. Positive restores, negative erodes.
	 *
	 * @return the resulting state
	 */
	public static SanState addCurrent(ServerPlayer player, float delta) {
		return modify(player, state -> state.addCurrent(delta));
	}

	/**
	 * Sets the player's San to an absolute value, clamped to their ceiling.
	 *
	 * @return the resulting state
	 */
	public static SanState setCurrent(ServerPlayer player, float value) {
		return modify(player, state -> state.withCurrent(value));
	}

	/**
	 * Moves the player's ceiling by {@code delta}. Positive expands what they can hold,
	 * negative shrinks it — and shrinking it below their present reading drags the reading down
	 * with it.
	 *
	 * @return the resulting state
	 */
	public static SanState addMax(ServerPlayer player, float delta) {
		return modify(player, state -> state.addMax(delta));
	}

	/**
	 * Sets the player's ceiling to an absolute value.
	 *
	 * @return the resulting state
	 */
	public static SanState setMax(ServerPlayer player, float value) {
		return modify(player, state -> state.withMax(value));
	}

	/**
	 * Restores the player to their own ceiling, whatever it currently is.
	 *
	 * @return the resulting state
	 */
	public static SanState restore(ServerPlayer player) {
		return modify(player, state -> state.withCurrent(state.max()));
	}

	/**
	 * Returns the player to a completely untouched state: full San at the default ceiling.
	 *
	 * @return the resulting state
	 */
	public static SanState reset(ServerPlayer player) {
		return set(player, SanState.INITIAL);
	}
}

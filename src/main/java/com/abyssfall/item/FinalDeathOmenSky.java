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

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.abyssfall.AbyssFall;
import com.abyssfall.itemmechanismruntime.HeldItemCensus;

/**
 * The Final Death Omen's omen made literal: the more hands on the server hold the blade,
 * the darker the sky over <em>every</em> player. One or two drawn blades hold the gloom at
 * a Wither's own (1.0); beyond two, each further blade deepens it by an equal share, until
 * five bring the deepest dark (2.3).
 *
 * <h2>Two variables, no states</h2>
 *
 * <p>The mechanic is two live numbers, not a ladder of named stages. The first is the
 * wielder count — a {@link HeldItemCensus}, recounted from zero every tick so it cannot
 * stick. The second is the darkness the client shows: 0.0 while no blade is drawn, a
 * constant {@link #CONSTANT_DARKNESS} while at most {@link #CONSTANT_DARKNESS_COUNT} are,
 * then a linear climb reaching {@link #FULL_DARKNESS} at {@link #FULL_DARKNESS_COUNT},
 * eased once per client tick. Any count is legal — six blades count six — and only the
 * darkness stops deepening at five.
 *
 * <h2>Why the server decides</h2>
 *
 * <p>Who holds the blade is a fact about the world's players, and only the server knows it
 * for all of them at once. The omen is meant to be witnessed — the sky
 * over everyone dims, not just over the wielders — so the count crosses the wire to every
 * client whenever it changes, and to each player individually when they join. One integer,
 * a handful of times per play session.
 *
 * <h2>The Wither is vanilla's business</h2>
 *
 * <p>A real Wither darkens the sky through its own boss-event path, capped at the same 1.0
 * a lone blade commands here. The client only ever raises vanilla's number toward the
 * darkness the count asks for and never lowers it, so the Wither keeps exactly the sky
 * vanilla gives it: this mechanic does not count it, amplify it, or answer for it.
 *
 * <h2>What counts as holding</h2>
 *
 * <p>Either hand, one count per person — the census's own rule; see {@link HeldItemCensus}.
 *
 * <h2>Where the effect happens</h2>
 *
 * <p>Not here. The client keeps the latest count in {@code DeathOmenSkyState} and eases its
 * own darkness from it; {@code GameRendererMixin} tops vanilla's boss-overlay darkening up
 * to that value, so the visual is vanilla's own Wither gloom at whatever depth the count
 * commands. This class only decides the number, and says so.
 */
public final class FinalDeathOmenSky {
	/**
	 * How many counts it takes to bring the sky to its deepest dark. The count itself is
	 * never capped — only the darkness it maps to.
	 */
	public static final int FULL_DARKNESS_COUNT = 5;

	/**
	 * The darkness at {@link #FULL_DARKNESS_COUNT} counts and beyond: 2.3 on the
	 * boss-overlay scale, where 1.0 is a Wither's gloom — an oppressive red dusk with the
	 * greens and blues all but gone, deliberately short of true black.
	 */
	public static final float FULL_DARKNESS = 2.3F;

	/**
	 * How many drawn blades the darkness holds steady for: one or two wielders command
	 * exactly {@link #CONSTANT_DARKNESS}, and only the third starts the climb toward
	 * {@link #FULL_DARKNESS}.
	 */
	public static final int CONSTANT_DARKNESS_COUNT = 2;

	/**
	 * The darkness while at most {@link #CONSTANT_DARKNESS_COUNT} blades are drawn: 1.0 on
	 * the boss-overlay scale, a Wither's own gloom — a lone blade is already as ominous as
	 * the boss.
	 */
	public static final float CONSTANT_DARKNESS = 1.0F;

	/**
	 * The wielder count, kept by the framework's census; part of the number pushed to
	 * clients.
	 */
	private static HeldItemCensus holders;

	/**
	 * The last number put on the wire, so the broadcast goes out once per change rather
	 * than once per tick. A value left over from a previous world corrects itself on the
	 * new server's first recount — the census is derived, not accumulated.
	 */
	private static int lastBroadcast;

	private FinalDeathOmenSky() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.STREAM_CODEC);

		holders = HeldItemCensus.register(
				stack -> stack.is(AbyssFallItems.FINAL_DEATH_OMEN),
				(server, previous, current) -> broadcastIfChanged(server));

		// A player arriving mid-darkness must see the same sky as everyone else, so the
		// current count is pushed to them before their client has a chance to ask.
		ServerPlayerEvents.JOIN.register(player ->
				ServerPlayNetworking.send(player, new Payload(holders.count())));
	}

	private static void broadcastIfChanged(MinecraftServer server) {
		int count = holders.count();

		if (count == lastBroadcast) {
			return;
		}

		lastBroadcast = count;
		Payload payload = new Payload(count);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	/**
	 * The one message this mechanic ever sends: how many blades are currently drawn.
	 * Broadcast to everyone when the count changes, and sent to each player individually
	 * when they join.
	 *
	 * <p>Registered for the clientbound play phase from the common initializer, so both
	 * physical sides register the type: the server to encode, the client to decode.
	 */
	public record Payload(int count) implements CustomPacketPayload {
		public static final Type<Payload> TYPE = new Type<>(AbyssFall.id("final_death_omen_sky"));

		public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Payload::count, Payload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}

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

import java.util.Locale;
import java.util.function.Function;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.abyssfall.AbyssFall;
import com.abyssfall.config.AbyssFallConfig;

/**
 * {@code /san} — the operator-facing window onto the San system.
 *
 * <p>This is a test and authoring tool, not player-facing content. Two things follow from that.
 *
 * <p>First, it is registered only when {@code developer.dev_command} is enabled in the config,
 * which it is not by default. A distributed build therefore has no {@code /san} at all — not a
 * hidden one, not a permission-denied one, simply none.
 *
 * <p>Second, the whole tree — reads included — sits behind {@link Commands#LEVEL_ADMINS}, one
 * step above the {@code LEVEL_GAMEMASTERS} that {@code /effect} and {@code /give} use. An earlier
 * version let a bare {@code /san} through unprivileged on the reasoning that your own reading is
 * yours to know. That reasoning no longer holds: the design intends players to learn their San as
 * a percentage through in-game means, never as the underlying float, so a command that prints the
 * float is a debug facility rather than an entitlement.
 */
public final class AbyssFallSanCommand {
	private AbyssFallSanCommand() {
	}

	/**
	 * Registers {@code /san}, if the configuration allows it.
	 *
	 * <p>Reads {@link AbyssFallConfig}, so the configuration must already be loaded.
	 */
	public static void initialize() {
		if (!AbyssFallConfig.isDevCommandEnabled()) {
			AbyssFall.LOGGER.info("Developer commands disabled; /san is not registered");
			return;
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(build()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("san")
				// One check on the root rather than one per branch: every branch wants the same
				// permission, and Brigadier will not descend into a node the caller fails.
				.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
				// Bare /san reports the caller's own San.
				.executes(context -> reportSelf(context.getSource()))
				.then(Commands.literal("query")
						.then(Commands.argument("target", EntityArgument.player())
								.executes(context -> report(context.getSource(),
										EntityArgument.getPlayer(context, "target")))))
				.then(Commands.literal("set")
						.then(Commands.argument("target", EntityArgument.player())
								.then(Commands.argument("value", FloatArgumentType.floatArg(0.0F))
										.executes(context -> apply(context, player ->
												AbyssFallCoreSystem.setCurrent(player,
														FloatArgumentType.getFloat(context, "value")))))))
				.then(Commands.literal("add")
						.then(Commands.argument("target", EntityArgument.player())
								.then(Commands.argument("delta", FloatArgumentType.floatArg())
										.executes(context -> apply(context, player ->
												AbyssFallCoreSystem.addCurrent(player,
														FloatArgumentType.getFloat(context, "delta")))))))
				.then(Commands.literal("max")
						.then(Commands.literal("set")
								.then(Commands.argument("target", EntityArgument.player())
										.then(Commands.argument("value", FloatArgumentType.floatArg(
														SanState.MIN_MAX, SanState.MAX_MAX))
												.executes(context -> apply(context, player ->
														AbyssFallCoreSystem.setMax(player,
																FloatArgumentType.getFloat(context, "value")))))))
						.then(Commands.literal("add")
								.then(Commands.argument("target", EntityArgument.player())
										.then(Commands.argument("delta", FloatArgumentType.floatArg())
												.executes(context -> apply(context, player ->
														AbyssFallCoreSystem.addMax(player,
																FloatArgumentType.getFloat(context, "delta"))))))))
				.then(Commands.literal("restore")
						.then(Commands.argument("target", EntityArgument.player())
								.executes(context -> apply(context, AbyssFallCoreSystem::restore))))
				.then(Commands.literal("reset")
						.then(Commands.argument("target", EntityArgument.player())
								.executes(context -> apply(context, AbyssFallCoreSystem::reset))));
	}

	/**
	 * Runs a mutation against the {@code target} argument and reports the result.
	 */
	private static int apply(CommandContext<CommandSourceStack> context,
			Function<ServerPlayer, SanState> mutator) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(context, "target");
		SanState state = mutator.apply(target);

		context.getSource().sendSuccess(() -> describe(target, state), true);

		// Brigadier result codes are integers, so the reading is reported rounded here. The
		// exact float is in the message itself.
		return (int) state.current();
	}

	private static int reportSelf(CommandSourceStack source) throws CommandSyntaxException {
		return report(source, source.getPlayerOrException());
	}

	private static int report(CommandSourceStack source, ServerPlayer target) {
		SanState state = AbyssFallCoreSystem.get(target);

		// Not broadcast to other operators: reading a value changes nothing.
		source.sendSuccess(() -> describe(target, state), false);

		return (int) state.current();
	}

	private static Component describe(ServerPlayer target, SanState state) {
		return Component.literal(String.format(Locale.ROOT,
				"%s: San %.2f / %.2f (%.2f%%)",
				target.getGameProfile().name(),
				state.current(),
				state.max(),
				state.percent()));
	}
}

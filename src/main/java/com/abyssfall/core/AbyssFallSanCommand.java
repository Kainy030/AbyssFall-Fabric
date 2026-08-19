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

/**
 * {@code /san} — the operator-facing window onto the San system.
 *
 * <p>With no HUD yet, this is the only way to see and steer the value in game, so it exists
 * mainly as a test and authoring tool rather than as player-facing content. Every mutating
 * branch is gated behind the permission vanilla uses for {@code /effect} and friends; a bare
 * {@code /san} reports your own reading and needs no privilege, since it tells you nothing you
 * are not entitled to know about yourself.
 */
public final class AbyssFallSanCommand {
	private AbyssFallSanCommand() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(build()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("san")
				// Bare /san reports the caller's own San.
				.executes(context -> reportSelf(context.getSource()))
				.then(Commands.literal("query")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("target", EntityArgument.player())
								.executes(context -> report(context.getSource(),
										EntityArgument.getPlayer(context, "target")))))
				.then(Commands.literal("set")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("target", EntityArgument.player())
								.then(Commands.argument("value", FloatArgumentType.floatArg(0.0F))
										.executes(context -> apply(context, player ->
												AbyssFallCoreSystem.setCurrent(player,
														FloatArgumentType.getFloat(context, "value")))))))
				.then(Commands.literal("add")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("target", EntityArgument.player())
								.then(Commands.argument("delta", FloatArgumentType.floatArg())
										.executes(context -> apply(context, player ->
												AbyssFallCoreSystem.addCurrent(player,
														FloatArgumentType.getFloat(context, "delta")))))))
				.then(Commands.literal("max")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("target", EntityArgument.player())
								.executes(context -> apply(context, AbyssFallCoreSystem::restore))))
				.then(Commands.literal("reset")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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

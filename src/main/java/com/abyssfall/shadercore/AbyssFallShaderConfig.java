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

package com.abyssfall.shadercore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.StrictJsonParser;

import net.fabricmc.loader.api.FabricLoader;

import com.abyssfall.AbyssFall;

/**
 * Reads and holds {@code AbyssFallShader.json}.
 *
 * <p>Deliberately the same shape as {@code AbyssFallConfig}: the same failure cases handled the same
 * ways, the same timestamped backup for a file that cannot be understood, the same refusal to throw. The
 * two are separate files because they answer to different things — that one is gameplay, this one is
 * appearance — but a player who has learned how one behaves has learned how both do.
 *
 * <p>Must be loaded after {@link AbyssFallShaderCore#initialize()}, since parsing an entry needs its
 * effect type to be registered.
 */
public final class AbyssFallShaderConfig {
	private static final String FILE_NAME = "AbyssFallShader.json";

	/**
	 * Dashes rather than colons throughout: a colon is not a legal filename character on Windows, where
	 * the rename would fail and leave the broken file neither preserved nor replaced.
	 */
	private static final DateTimeFormatter BACKUP_TIMESTAMP =
			DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static ShaderConfigData data = ShaderConfigData.DEFAULT;

	private AbyssFallShaderConfig() {
	}

	/**
	 * The loaded configuration. Usable before {@link #load()} — it holds the defaults until then.
	 */
	public static ShaderConfigData get() {
		return data;
	}

	/**
	 * Reads the file, creating it with the defaults if it is not there.
	 *
	 * <p>Never throws: a bad file costs some appearances, which is not a reason to stop the game from
	 * starting.
	 */
	public static void load() {
		Path path = path();

		if (!Files.exists(path)) {
			data = ShaderConfigData.DEFAULT;
			AbyssFall.LOGGER.info("No shader configuration at {}; writing defaults", path);
			save();
			return;
		}

		ShaderConfigData parsed = read(path);

		if (parsed == null) {
			// The file exists but nothing in it is salvageable. Set it aside under a timestamped name
			// and write a fresh one: an author who has broken their file needs a working one to edit
			// and needs their original kept, because whatever they meant to write is in it.
			data = ShaderConfigData.DEFAULT;
			backup(path);
			save();
			return;
		}

		data = parsed;
		AbyssFall.LOGGER.info("Loaded {} shader effect(s) from {}", parsed.effects().size(), path);
	}

	/**
	 * Writes the current configuration out, creating the config directory if needed.
	 */
	public static void save() {
		Path path = path();

		JsonElement json = ShaderConfigData.CODEC.encodeStart(JsonOps.INSTANCE, data)
				.resultOrPartial(error ->
						AbyssFall.LOGGER.error("Could not encode shader configuration: {}", error))
				.orElse(null);

		if (json == null) {
			// Encoding a record built from its own codec should not be able to fail, so this means the
			// codec and the record have drifted apart. Writing nothing is safer than writing something
			// half-formed over a file the author may have edited.
			return;
		}

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
				writer.write('\n');
			}

			AbyssFall.LOGGER.info("Wrote shader configuration to {}", path);
		} catch (IOException exception) {
			AbyssFall.LOGGER.error("Could not write {}; continuing with the current settings",
					path, exception);
		}
	}

	private static void backup(Path path) {
		String stamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
		Path target = path.resolveSibling(path.getFileName() + ".broken-" + stamp);

		// Two launches within the same second would otherwise have the second backup overwrite the
		// first, which defeats the point of keeping it.
		for (int attempt = 2; Files.exists(target); attempt++) {
			target = path.resolveSibling(path.getFileName() + ".broken-" + stamp + "-" + attempt);
		}

		try {
			Files.move(path, target);
			AbyssFall.LOGGER.error("Could not understand {}; it has been moved to {} and replaced with "
					+ "default settings", path, target.getFileName());
		} catch (IOException exception) {
			AbyssFall.LOGGER.error("Could not understand {} and could not move it aside either; using "
					+ "default settings without touching the file", path, exception);
		}
	}

	/**
	 * Parses the file at {@code path}.
	 *
	 * <p>A file whose JSON does not parse yields {@code null}: nothing in it is salvageable, and the
	 * caller sets it aside. A file that parses but holds one unusable entry also yields {@code null}
	 * rather than silently dropping that entry — an author who mistyped a type or a mask path should be
	 * told rather than left wondering why one item looks wrong.
	 *
	 * @return the parsed configuration, or {@code null} if the file could not be understood
	 */
	private static ShaderConfigData read(Path path) {
		JsonElement json;

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			json = StrictJsonParser.parse(reader);
		} catch (IOException | JsonParseException exception) {
			AbyssFall.LOGGER.error("Could not read {}", path, exception);
			return null;
		}

		return ShaderConfigData.CODEC.parse(JsonOps.INSTANCE, json)
				.resultOrPartial(error -> AbyssFall.LOGGER.error("Could not parse {}: {}", path, error))
				.orElse(null);
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}
}

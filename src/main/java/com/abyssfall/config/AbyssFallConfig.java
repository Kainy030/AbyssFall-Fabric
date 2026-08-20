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

package com.abyssfall.config;

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
 * Loads and saves {@code config/abyssfall.json}.
 *
 * <p>Read exactly once, before anything is registered. That ordering is not incidental: some
 * settings decide whether content enters the registries at all, and a registry cannot be
 * amended after the fact, so the file has to be consulted before registration begins rather
 * than whenever something first asks.
 *
 * <h2>Why JSON, and why a codec</h2>
 *
 * <p>The mod is expected to accumulate a lot of configuration, much of it structured — per
 * dimension values, lists of affected loot tables, and so on. A flat {@code key=value} file
 * expresses that only by convention, whereas JSON expresses it directly and needs no extra
 * dependency: Gson and DataFixerUpper are already on the classpath, and the project already
 * describes its data with codecs. Vanilla persists its own debug profile the same way.
 *
 * <p>Describing the file with a {@link com.mojang.serialization.Codec} rather than hand-written
 * parsing buys two things worth having. Every field is declared optional with a default, so a
 * file written by an older version keeps loading and no migration step is ever needed. And the
 * settings arrive as immutable records, so nothing downstream can quietly mutate them.
 *
 * <h2>Failure handling</h2>
 *
 * <p>A missing file is written out with the defaults, so a first launch leaves behind something
 * to edit. A file that cannot be read or parsed is reported and the defaults used for the
 * session, without touching the file: refusing to start over a malformed setting would be a
 * worse outcome, and overwriting it would destroy whatever the author was in the middle of
 * writing.
 */
public final class AbyssFallConfig {
	private static final String FILE_NAME = "abyssfall.json";

	/**
	 * Timestamp appended to a set-aside broken file. Sortable, and free of any character that a
	 * filesystem objects to.
	 */
	private static final DateTimeFormatter BACKUP_TIMESTAMP =
			DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	/**
	 * Pretty printing is deliberate. This file is meant to be opened and edited by hand, so
	 * readability matters more than compactness.
	 */
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static AbyssFallConfigData data = AbyssFallConfigData.DEFAULT;

	private AbyssFallConfig() {
	}

	/**
	 * The current configuration. Never {@code null}: before {@link #load()} runs, and after any
	 * failure to read the file, this reports the defaults.
	 */
	public static AbyssFallConfigData get() {
		return data;
	}

	/**
	 * Whether the developer creative tab and its contents should be registered.
	 *
	 * <p>A shortcut for the one setting that currently has a consumer. Accessors like this are
	 * worth adding only for settings read from several places; anything else should go through
	 * {@link #get()} rather than growing this class a method per field.
	 */
	public static boolean isDevInventoryEnabled() {
		return data.developer().devInventory();
	}

	/**
	 * The loot settings: which tables the flower appears in, and how often.
	 */
	public static LootSettings loot() {
		return data.loot();
	}

	/**
	 * The visual settings: how busy and how loud the mod's effects are.
	 */
	public static VisualSettings visuals() {
		return data.visuals();
	}

	/**
	 * Reads the configuration file, creating it with the defaults if it is not there.
	 *
	 * <p>Must be called before any conditional registration happens. Never throws: any problem
	 * is logged and the defaults are used, because a bad config file is not a reason to stop
	 * the game from starting.
	 */
	public static void load() {
		Path path = path();

		if (!Files.exists(path)) {
			data = AbyssFallConfigData.DEFAULT;
			AbyssFall.LOGGER.info("No configuration file at {}; writing defaults", path);
			save();
			return;
		}

		AbyssFallConfigData parsed = read(path);

		if (parsed == null) {
			// The file exists but could not be understood at all. Set it aside under a
			// timestamped name and put a fresh, valid one in its place: a player who has broken
			// their config needs a working file to edit, and needs their original preserved so
			// that whatever they meant to write is not simply lost.
			data = AbyssFallConfigData.DEFAULT;
			backup(path);
			save();
			return;
		}

		data = parsed;

		AbyssFall.LOGGER.info("Loaded configuration from {}", path);
	}

	/**
	 * Renames an unreadable configuration file out of the way.
	 *
	 * <p>The timestamp uses dashes rather than colons throughout because a colon is not a legal
	 * filename character on Windows, where the rename would fail outright and leave the broken
	 * file neither preserved nor replaced.
	 */
	private static void backup(Path path) {
		String stamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
		Path target = path.resolveSibling(path.getFileName() + ".broken-" + stamp);

		// Two launches within the same second would otherwise have the second backup overwrite
		// the first, which defeats the point of keeping it.
		for (int attempt = 2; Files.exists(target); attempt++) {
			target = path.resolveSibling(path.getFileName() + ".broken-" + stamp + "-" + attempt);
		}

		try {
			Files.move(path, target);
			AbyssFall.LOGGER.error("Could not understand {}; it has been moved to {} and "
					+ "replaced with default settings", path, target.getFileName());
		} catch (IOException exception) {
			AbyssFall.LOGGER.error("Could not understand {} and could not move it aside either; "
					+ "using default settings without touching the file", path, exception);
		}
	}

	/**
	 * Writes the current configuration out, creating the config directory if needed.
	 *
	 * <p>Failure is logged rather than thrown: being unable to save is no reason to stop the
	 * game, and the values held in memory remain perfectly usable for this session.
	 */
	public static void save() {
		Path path = path();

		JsonElement json = AbyssFallConfigData.CODEC.encodeStart(JsonOps.INSTANCE, data)
				.resultOrPartial(error ->
						AbyssFall.LOGGER.error("Could not encode configuration: {}", error))
				.orElse(null);

		if (json == null) {
			// Encoding a record built from its own codec should not be able to fail, so this
			// means the codec and the record have drifted apart. Writing nothing is safer than
			// writing something half-formed over a file the author may have edited.
			return;
		}

		try {
			Files.createDirectories(path.getParent());

			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(json, writer);
				writer.write('\n');
			}

			AbyssFall.LOGGER.info("Wrote configuration to {}", path);
		} catch (IOException exception) {
			AbyssFall.LOGGER.error("Could not write {}; continuing with the current settings",
					path, exception);
		}
	}

	/**
	 * Parses the file at {@code path}.
	 *
	 * <p>Distinguishes two very different failures. A file whose JSON does not parse, or which
	 * cannot be read at all, yields {@code null}: nothing in it is salvageable, and the caller
	 * sets it aside. A file that parses but holds an unusable value somewhere is <em>not</em> a
	 * failure — the offending block falls back to its own defaults, every other setting is
	 * honoured, and the file is left exactly as the author wrote it. Replacing a file over one
	 * bad field would throw away all the fields that were fine.
	 *
	 * @return the parsed configuration, or {@code null} if the file could not be understood
	 */
	private static AbyssFallConfigData read(Path path) {
		JsonElement json;

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			json = StrictJsonParser.parse(reader);
		} catch (IOException | JsonParseException exception) {
			AbyssFall.LOGGER.error("Could not read {}", path, exception);
			return null;
		}

		return AbyssFallConfigData.CODEC.parse(JsonOps.INSTANCE, json)
				.resultOrPartial(error -> AbyssFall.LOGGER.error(
						"Could not parse {}: {}", path, error))
				.orElse(null);
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}
}

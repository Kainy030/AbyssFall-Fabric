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

package com.abyssfall;

import java.nio.file.Path;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abyssfall.block.AbyssFallBlocks;
import com.abyssfall.block.AbyssFallBoneMealHandler;
import com.abyssfall.config.AbyssFallConfig;
import com.abyssfall.core.AbyssFallCoreSystem;
import com.abyssfall.core.AbyssFallSanCommand;
import com.abyssfall.effect.AbyssFallEffects;
import com.abyssfall.item.AbyssFallDevInventory;
import com.abyssfall.item.AbyssFallItemGroups;
import com.abyssfall.item.AbyssFallItems;
import com.abyssfall.loot.AbyssFallLootTables;
import com.abyssfall.shadercore.AbyssFallShaderConfig;
import com.abyssfall.shadercore.AbyssFallShaderCore;

public class AbyssFall implements ModInitializer {
	public static final String MOD_ID = "abyssfall";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// The configuration decides whether some content is registered at all, and registries
		// cannot be amended once the game is running, so it has to be read before anything
		// else happens.
		AbyssFallConfig.load();

		// The shader core comes before its own configuration is read: an entry in that file names an
		// effect type, and the type has to be registered before the entry can be understood.
		AbyssFallShaderCore.initialize();
		AbyssFallShaderConfig.load();

		// The San system comes first: it is the value the rest of the mod is meant to move, and
		// registering its attachment before anything else can consult it keeps that ordering
		// honest.
		AbyssFallCoreSystem.initialize();
		AbyssFallSanCommand.initialize();

		AbyssFallEffects.initialize();
		AbyssFallItems.initialize();
		AbyssFallBlocks.initialize();
		AbyssFallItemGroups.initialize();
		AbyssFallLootTables.initialize();
		AbyssFallBoneMealHandler.initialize();

		// Last, and conditional: it registers nothing at all unless the config allows it.
		AbyssFallDevInventory.initialize();

		LOGGER.info("AbyssFall initialized!");
		LOGGER.info("深渊浮现已加载完毕！");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/**
	 * A file inside this mod's own configuration directory, {@code config/abyssfall/}.
	 *
	 * <h2>🔴 Every configuration file of this mod goes through here</h2>
	 *
	 * <p>Not {@code config/} directly. The mod is expected to accumulate a good many separate
	 * configuration files — gameplay, appearance, and whatever later systems need their own — and
	 * scattering them across the shared {@code config/} directory alongside every other mod's files
	 * makes them progressively harder to find, back up, or move between installations. One directory
	 * named after the mod keeps all of them together no matter how many there turn out to be.
	 *
	 * <p>⚠️ The directory is <strong>not</strong> created here. A caller that only reads has no business
	 * creating anything, and every writer here already calls {@code Files.createDirectories} on the
	 * parent before writing — which now creates this directory as a side effect, since it is that parent.
	 *
	 * <p>⚠️ <strong>Changing this path orphans existing files.</strong> A player's settings live at the
	 * old location and are not migrated; the mod would silently start from defaults and their edits would
	 * be lost without any error. Same hazard as renaming a configuration key.
	 *
	 * @param fileName the file's own name, such as {@code abyssfall.json}
	 */
	public static Path configPath(String fileName) {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve(fileName);
	}
}

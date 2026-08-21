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

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import com.abyssfall.AbyssFall;
import com.abyssfall.block.AbyssFallBlocks;

/**
 * The mod's creative mode tab.
 *
 * <p>The display name is built in code rather than pulled from a language file, because
 * it mixes two colours within a single title: the leading half is dark grey and the
 * trailing half is grey, both bold. A plain translation string cannot express per-segment
 * styling, so the localised halves are looked up individually and then styled and
 * concatenated here.
 */
public final class AbyssFallItemGroups {
	/**
	 * Translation key for the dark grey, leading half of the tab title
	 * ("深渊" / "Abyss").
	 */
	public static final String TITLE_HEAD_KEY = "itemGroup.abyssfall.head";

	/**
	 * Translation key for the grey, trailing half of the tab title
	 * ("浮现" / "Fall").
	 */
	public static final String TITLE_TAIL_KEY = "itemGroup.abyssfall.tail";

	public static final ResourceKey<CreativeModeTab> ABYSSFALL_TAB_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, AbyssFall.id("abyssfall"));

	public static final CreativeModeTab ABYSSFALL_TAB = FabricItemGroup.builder()
			.title(buildTitle())
			.icon(() -> new ItemStack(AbyssFallItems.ABYSS_FLOWER))
			.build();

	private AbyssFallItemGroups() {
	}

	/**
	 * Builds the two-tone bold title: dark grey "深渊" followed by grey "浮现".
	 *
	 * <p>Both halves are siblings of a deliberately empty root component. The creative
	 * inventory screen labels a hovered item with the owning tab's name via
	 * {@code getDisplayName().copy().withStyle(ChatFormatting.BLUE)}, which replaces the
	 * style of the <em>root</em> component only. Sibling styles win over an inherited one
	 * (see {@code Style.applyTo}, where the child's own non-null fields take precedence),
	 * so keeping the root empty means the blue lands on nothing visible and the tooltip
	 * keeps the intended grey tones.
	 */
	private static MutableComponent buildTitle() {
		return Component.empty()
				.append(Component.translatable(TITLE_HEAD_KEY)
						.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
				.append(Component.translatable(TITLE_TAIL_KEY)
						.withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ABYSSFALL_TAB_KEY, ABYSSFALL_TAB);

		ItemGroupEvents.modifyEntriesEvent(ABYSSFALL_TAB_KEY)
				.register(entries -> {
					entries.accept(AbyssFallItems.ABYSS_FLOWER);
					entries.accept(AbyssFallItems.SAN_LENS);
					entries.accept(AbyssFallBlocks.ABYSS_DIRT);
				});
	}
}

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

import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import com.abyssfall.AbyssFall;
import com.abyssfall.config.AbyssFallConfig;

/**
 * The developer creative tab and the tooling that lives in it.
 *
 * <p>Everything here is registered only when {@code developer.dev_inventory} is enabled in the
 * config — which it is not by default — and that is why none of it is held in a
 * {@code static final} field the way the mod's ordinary content is. A static field would be
 * initialised — and therefore registered — the moment the class was touched, leaving no room for
 * the switch to have any effect. So the items are created inside {@link #initialize()} and
 * exposed through accessors that report their absence honestly.
 *
 * <h2>What turning the switch off actually does</h2>
 *
 * <p>The content is not hidden, it is never registered. A distributed build with the switch off
 * behaves as though this class did not exist. The cost of that strictness is that a world which
 * already contains one of these items will drop it as unknown content when loaded without the
 * switch — acceptable for developer tooling, and the honest consequence of the item genuinely
 * not existing.
 */
public final class AbyssFallDevInventory {
	/**
	 * Translation key for the dark grey, leading third of the tab title
	 * ("深渊" / "Abyss").
	 */
	public static final String TITLE_HEAD_KEY = "itemGroup.abyssfall.head";

	/**
	 * Translation key for the grey, middle third of the tab title ("浮现" / "Fall").
	 */
	public static final String TITLE_TAIL_KEY = "itemGroup.abyssfall.tail";

	/**
	 * Translation key for the blood red, trailing third of the tab title
	 * ("开发者物品栏" / "Dev Inventory").
	 */
	public static final String TITLE_DEV_KEY = "itemGroup.abyssfall.dev";

	public static final ResourceKey<CreativeModeTab> DEV_TAB_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB, AbyssFall.id("abyssfall_dev_inventory"));

	/**
	 * Blood red, for the developer segment of the title. Chosen over
	 * {@link ChatFormatting#DARK_RED} because the tab title is drawn against the creative
	 * screen's light background, where the vanilla dark red reads as brown.
	 */
	private static final TextColor BLOOD_RED = TextColor.fromRgb(0xB01030);

	private static Item sanCounter;

	private static CreativeModeTab devTab;

	private AbyssFallDevInventory() {
	}

	/**
	 * Registers the developer tab and its contents, if the configuration allows it.
	 *
	 * <p>Reads {@link AbyssFallConfig}, so the configuration must already be loaded.
	 */
	public static void initialize() {
		if (!AbyssFallConfig.isDevInventoryEnabled()) {
			AbyssFall.LOGGER.info(
					"Developer inventory disabled; its tab and items are not registered");
			return;
		}

		sanCounter = register("san_counter", SanCounterItem::new,
				new Item.Properties().stacksTo(1));

		devTab = FabricItemGroup.builder()
				.title(buildTitle())
				.icon(() -> new ItemStack(sanCounter))
				.build();

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DEV_TAB_KEY, devTab);

		ItemGroupEvents.modifyEntriesEvent(DEV_TAB_KEY)
				.register(entries -> entries.accept(sanCounter));

		AbyssFall.LOGGER.info("Developer inventory registered");
	}

	/**
	 * Whether the developer content was registered this session. Equivalent to asking the
	 * configuration, but phrased as a fact about the registries rather than about a setting.
	 */
	public static boolean isRegistered() {
		return devTab != null;
	}

	/**
	 * The San Counter, or {@code null} if the developer inventory is disabled.
	 *
	 * <p>Nullable on purpose. The alternative — handing out a stand-in — would let a caller
	 * believe an item exists when it does not, and everything that reaches for this is
	 * developer tooling that can reasonably be asked to check.
	 */
	public static Item getSanCounter() {
		return sanCounter;
	}

	/**
	 * Builds the three-tone title: bold dark grey "深渊", bold grey "浮现", then bold italic
	 * blood red "开发者物品栏".
	 *
	 * <p>The empty root component matters and is not an oversight. The creative inventory
	 * screen labels a hovered item with the owning tab's name via
	 * {@code getDisplayName().copy().withStyle(ChatFormatting.BLUE)}, which replaces the style
	 * of the <em>root</em> component only. A sibling's own style wins over an inherited one, so
	 * keeping the root empty means the blue lands on nothing visible and all three segments keep
	 * their intended appearance. {@code AbyssFallItemGroups} does the same thing for the same
	 * reason.
	 *
	 * <p>The first two segments reuse the main tab's translation keys rather than duplicating
	 * the words, so the mod's name is spelled one way in one place.
	 */
	private static MutableComponent buildTitle() {
		return Component.empty()
				.append(Component.translatable(TITLE_HEAD_KEY)
						.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
				.append(Component.translatable(TITLE_TAIL_KEY)
						.withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD))
				.append(Component.translatable(TITLE_DEV_KEY)
						.withStyle(Style.EMPTY
								.withColor(BLOOD_RED)
								.withBold(true)
								.withItalic(true)));
	}

	/**
	 * Mirrors {@code AbyssFallItems.register}. Kept separate rather than shared because these
	 * registrations are conditional and run from {@link #initialize()}, whereas the ordinary
	 * items register from a static initialiser.
	 */
	private static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory,
			Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, AbyssFall.id(name));

		T item = itemFactory.apply(properties.setId(itemKey));

		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return item;
	}
}

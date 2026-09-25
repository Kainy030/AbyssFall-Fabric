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

import net.minecraft.world.item.ToolMaterial;

import com.abyssfall.block.AbyssFallBlockTags;

/**
 * Tool materials owned by the mod.
 */
public final class AbyssFallToolMaterials {
	/**
	 * Abyssdium — the element past the end of vanilla's scale, stated as a set of refusals.
	 *
	 * <p>Each component of the record, and what it means here:
	 *
	 * <ul>
	 *   <li><b>Harvest tier: nothing resists it.</b> {@code incorrectBlocksForDrops} is the
	 *       slot where a material names the blocks that refuse it; Abyssdium's tag ships
	 *       empty ({@link AbyssFallBlockTags#INCORRECT_FOR_ABYSSDIUM_TOOL}), so no block is
	 *       ever marked as denying this material. The remaining "unmineable" class — bedrock
	 *       and its kin, destroy speed −1 — is not a tier question at all and is answered by
	 *       {@code BlockDestroyProgressMixin}, not here, and only for the forgings in
	 *       {@link AbyssFallItemTags#DIG_FROM_ABYSS}.</li>
	 *   <li><b>Durability: none.</b> Declared {@code 0} because the element knows no wear.
	 *       🔴 The number still flows into {@code MAX_DAMAGE} through {@code sword(...)} or
	 *       {@code applyToolProperties}, and a {@code MAX_DAMAGE} of zero without
	 *       {@code UNBREAKABLE} reads as <em>already broken</em> — {@code isBroken()} is true
	 *       from the start and the item would shatter on its first use. <b>Every Abyssdium
	 *       product must carry {@code DataComponents.UNBREAKABLE}</b>; see the Final Death
	 *       Omen.</li>
	 *   <li><b>Speed: {@link Float#MAX_VALUE}.</b> Feeds the mining rules of whatever tools
	 *       are forged from the element later. Irrelevant to the sword, whose rules are the
	 *       fixed sword set.</li>
	 *   <li><b>Attack damage bonus: 5.0, deliberately untouched.</b> The stated direction —
	 *       recorded here, not implemented — is a dynamic multiplier: the bonus would scale
	 *       with the target, varying by mod and by entity type. Until that is designed the
	 *       value stays a flat step above netherite's four, and on the Final Death Omen it is
	 *       dead anyway, since the blade replaces its attributes wholesale.</li>
	 *   <li><b>Enchantability: 1, a dead letter.</b> Zero is not even expressible:
	 *       {@code Enchantable}'s constructor rejects non-positive values outright
	 *       ("Enchantment value must be positive" — verified, it has crashed the game once
	 *       already), so the record carries the smallest legal value instead. The value never
	 *       survives onto a product, because {@code ItemStack.isEnchantable()} gates on the
	 *       <em>presence</em> of the {@code ENCHANTABLE} component, never its value:
	 *       <b>every Abyssdium product must strip the component outright</b>
	 *       ({@code component(ENCHANTABLE, null)}), as the Final Death Omen does — the
	 *       material recognizes none of vanilla's enchantments.</li>
	 *   <li><b>Repair: nothing.</b> What cannot wear needs no repair material, so
	 *       {@link AbyssFallItemTags#ABYSSDIUM_TOOL_MATERIALS} ships empty. The record still
	 *       requires a key, and an empty datapack tag is safe at registration time:
	 *       {@code repairable(TagKey)} resolves it through
	 *       {@code MappedRegistry.getOrCreateTagForRegistration}, which creates the named tag
	 *       on the spot and lets datapack loading fill — or here, keep empty — it later.</li>
	 * </ul>
	 */
	public static final ToolMaterial ABYSSDIUM = new ToolMaterial(
			AbyssFallBlockTags.INCORRECT_FOR_ABYSSDIUM_TOOL, 0, Float.MAX_VALUE, 5.0F, 1,
			AbyssFallItemTags.ABYSSDIUM_TOOL_MATERIALS);

	private AbyssFallToolMaterials() {
	}
}

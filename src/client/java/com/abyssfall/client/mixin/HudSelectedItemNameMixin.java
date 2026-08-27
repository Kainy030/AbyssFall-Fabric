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

package com.abyssfall.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.abyssfall.client.tooltip.AbyssFallTooltips;

/**
 * Colours the held-item popup above the hotbar for the mod's own rarities.
 *
 * <h2>Why a mixin is needed here, when the tooltip did not need one</h2>
 *
 * <p>An item's name is drawn in two unrelated places, and only one of them is extensible. A tooltip is
 * assembled into a {@code List<Component>} that {@code ItemTooltipCallback} hands over for editing, so
 * the tooltip half of this feature is a plain Fabric event with no injection at all.
 *
 * <p>The popup is not. {@code Hud.extractSelectedItemName} is {@code private}, builds its component in
 * a local variable, reads two {@code private} fields ({@code toolHighlightTimer},
 * {@code lastToolHighlight}), and hands the finished text straight to {@code textWithBackdrop}. Nothing
 * is exposed at any point. Fabric API has no event for it either — {@code fabric-rendering-v1} lets a
 * mod add or replace whole HUD elements, but this text belongs to a vanilla element, and replacing that
 * element would mean reimplementing vanilla's timer, fade and placement to change one colour.
 *
 * <h2>Why {@code getHoverName} is the target, rather than the method or the draw call</h2>
 *
 * <p>Read from the bytecode of {@code extractSelectedItemName} in 26.2:
 *
 * <pre>{@code
 * 17: invokestatic  Component.empty()
 * 24: invokevirtual ItemStack.getHoverName()      <- wrapped here
 * 27: invokevirtual MutableComponent.append(...)
 * 34: invokevirtual ItemStack.getRarity()
 * 37: invokevirtual Rarity.color()
 * 40: invokevirtual MutableComponent.withStyle(...)
 * }</pre>
 *
 * <p>The name becomes a <strong>sibling</strong> of an empty root, and {@code rarity.color()} is applied
 * to the <strong>root</strong>. A sibling's own colour wins over one inherited from its parent
 * ({@code Style.applyTo} keeps the child's non-null fields), so returning an already-coloured component
 * from {@code getHoverName} is enough — the vanilla rarity colour lands on the empty root and is never
 * seen. <strong>Nothing about vanilla's logic has to be cancelled, replaced or recomputed.</strong>
 *
 * <p>This is the same mechanism the creative tab's two-tone title has relied on since 1.21.11, verified
 * there and reused here rather than discovered again.
 *
 * <p>Wrapping the call rather than injecting into the method also means the timer, the fade, the width
 * measurement and the placement are all still vanilla's, and a rarity of ours is the only thing that
 * changes anything.
 *
 * <h2>Verified against</h2>
 *
 * <p><strong>Minecraft 26.2.</strong> {@code extractSelectedItemName(GuiGraphicsExtractor)} and both
 * private fields were confirmed with {@code javap} on the 26.2 client jar, and the instruction order
 * above was read from that same disassembly.
 *
 * <p>⚠️ {@code getHoverName} is called in a great many places across the game; this only affects the
 * <em>one</em> call inside this <em>one</em> method, because {@code @WrapOperation} targets a call site
 * rather than a method. The mixin config sets {@code injectors.defaultRequire = 1}, so if this call ever
 * disappears the game fails to launch instead of quietly losing the colour.
 */
@Mixin(Hud.class)
public class HudSelectedItemNameMixin {
	/**
	 * Substitutes a rarity-coloured name for the popup, or leaves vanilla's alone.
	 *
	 * <p>{@code original.call(stack)} is always made: the substitution needs the resolved name as its
	 * input, and calling it keeps any other mod wrapping the same site in the chain.
	 */
	@WrapOperation(
			method = "extractSelectedItemName",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
	private Component abyssfall$colorNameByRarity(ItemStack stack, Operation<Component> original) {
		Component vanilla = original.call(stack);
		Component ours = AbyssFallTooltips.rarityName(stack, vanilla, AbyssFallTooltips.nameClock());

		return ours != null ? ours : vanilla;
	}
}

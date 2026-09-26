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

package com.abyssfall.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abyssfall.itemmechanismruntime.NeverDestroyed;

/**
 * What the abyss watches over, no harm may reach: the damage half of the undying rule.
 * The mechanic itself is {@code NeverDestroyed} in the item framework; which stacks hold
 * it is granted outside the framework — this class only enforces the answer.
 *
 * <p>An item entity dies to lava, fire, cactus, explosions and their kin through exactly
 * one question — {@code ItemEntity.hurtServer} (and its client shadow) asks
 * {@code ItemStack.canBeHurtBy} before any damage is applied. That method is vanilla's own
 * seam for "this item cannot be hurt by this source", the same one netherite's
 * {@code DAMAGE_RESISTANT} rides. Answering it with an unqualified "no" for a stack that
 * holds the mechanic covers every damage source at once, present and future, rather than
 * a list of damage types that would silently age. As a free consequence
 * {@code ItemEntity.fireImmune()}, which consults the same method, also answers true, so
 * such a stack does not even catch fire visually.
 *
 * <p>The only other caller is {@code LivingEntity}'s equipment-damage path, where the
 * answer is already short-circuited by {@code isDamageableItem()} for every item
 * currently holding the mechanic — they are unbreakable or non-damageable — so nothing
 * else changes.
 *
 * <p>A mixin because there is no event for this: Fabric offers no hook into whether an item
 * may be hurt, and the alternative — a damage-type tag on {@code DAMAGE_RESISTANT} — can
 * only name sources that exist today.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackUndyingMixin {
	@Inject(method = "canBeHurtBy", at = @At("HEAD"), cancellable = true)
	private void abyssfall$neverDestroyedCannotBeHurt(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if (NeverDestroyed.INSTANCE.has((ItemStack)(Object)this)) {
			cir.setReturnValue(false);
		}
	}
}

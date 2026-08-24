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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;

import com.abyssfall.item.AbyssFallItems;
import com.abyssfall.item.FinalDeathOmen;

/**
 * Hands an attack made with the Final Death Omen to the weapon's own resolution, and leaves
 * every other attack in the game exactly as it found it.
 *
 * <h2>Why this cannot be an item hook or a Fabric event</h2>
 *
 * <p>The weapon's premise is that no existing verdict about whether an entity can be damaged is
 * trusted. Every extension point the game offers sits inside the verdict it would have to
 * override. {@code Item.hurtEnemy} runs after {@code hurtServer} has returned, so a target that
 * refused the hit never reaches it. {@code ServerLivingEntityEvents.ALLOW_DAMAGE} is itself
 * injected into {@code hurtServer}, so it is one of the opinions being stepped around rather
 * than a way to step around them. A {@code damage_type} component only changes which source a
 * blow carries, and the blow still has to survive the pipeline.
 *
 * <p>{@code Player#attack} is the last point that is unambiguously before all of it. Wrapping
 * it means the weapon's resolution runs in place of the vanilla method, and every mitigation
 * downstream is not overruled but simply never reached: the fifty-odd {@code hurtServer}
 * overrides, the Wither's invulnerability window, the dragon's phase damage reduction, the
 * PvP toggle, invulnerability flags, creative mode, armour, resistance, absorption, shields
 * and totems.
 *
 * <h2>Why {@code @WrapMethod} rather than a cancellable {@code @Inject}</h2>
 *
 * <p>Both can stop the vanilla body from running, but wrapping says what is meant. The vanilla
 * method becomes a value this class may or may not call, which makes "the weapon replaces the
 * attack" a structural fact rather than the effect of setting a cancellation flag. It also
 * keeps the ordinary path honest: anything that is not this weapon reaches
 * {@code original.call} untouched, including other mods' injections into the method body,
 * because those live inside the operation being invoked.
 *
 * <p>Unlike {@code @Redirect} or {@code @Overwrite}, wrapping composes. Another mod wrapping
 * the same method nests with this one instead of conflicting with it, so the choice costs
 * nothing in compatibility. The priority is nevertheless pinned as high as it goes, on the
 * principle that this weapon should be the outermost voice if there is ever a contest — though
 * with the wrap already sitting outside the method body, that is belt and braces rather than
 * what makes it work.
 *
 * <h2>Server side only</h2>
 *
 * <p>{@code Player#attack} runs on both sides: the client calls it from
 * {@code MultiPlayerGameMode#attack} right after sending the attack packet, and the server
 * calls it from the packet handler. In single player both are the same process. Killing on the
 * client would write a death into a copy of the world the server has not agreed to, leaving an
 * entity that is dead locally and alive authoritatively, so the client is handed straight back
 * to vanilla — which also lets it play the swing animation exactly as it always did.
 */
@Mixin(value = Player.class, priority = Integer.MAX_VALUE)
public abstract class PlayerAttackMixin {
	@WrapMethod(method = "attack")
	private void abyssfall$resolveDeathOmen(Entity target, Operation<Void> original) {
		Player self = (Player)(Object)this;

		// Authoritative side only; see the class comment. Anything else is vanilla's business.
		if (!(self.level() instanceof ServerLevel level)
				|| !self.getWeaponItem().is(AbyssFallItems.FINAL_DEATH_OMEN)) {
			original.call(target);
			return;
		}

		// original is deliberately never called: the vanilla attack, and everything any mod has
		// injected into it, is what this weapon exists to bypass.
		FinalDeathOmen.strike(level, self, target);
	}
}

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

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.abyssfall.itemframework.NeverDestroyed;
import com.abyssfall.itemframework.SourceSlotAccess;

/**
 * The undying rule as it applies to the item entity itself: it does not expire, and it
 * remembers where it came from.
 *
 * <p><b>Lifetime.</b> Vanilla gives a dropped item five minutes before {@code tick}
 * discards it. The same method already carries the exemption — an age of −32768
 * ({@code ItemEntity}'s private {@code INFINITE_LIFETIME}) freezes the countdown — and
 * exposes it as {@code setUnlimitedLifetime()}, so all this injection does is apply
 * vanilla's own answer to any stack holding the mechanic it meets. Doing it in
 * {@code tick} rather than at spawn covers every way such a stack can come into being as
 * an item entity — thrown by a player, spilled from a broken chest, summoned, dropped by
 * a dispenser — with no enumeration of creation paths to keep in step. The marker itself
 * is persisted in the entity's {@code Age} field, so it stays immortal across a save
 * reload even before its first tick back.
 *
 * <p><b>Explosions.</b> An explosion reaches an item entity through a gate of its own,
 * ahead of any damage question: {@code ServerExplosion.hurtEntities} first asks
 * {@code entity.ignoreExplosion(this)}, and only the unignored are damaged, pushed and
 * notified. Answering that gate with an unqualified "yes, ignored" for a protected stack
 * exempts it from the whole blast — no damage, and no knockback either, so it cannot be
 * flung somewhere the void (or a player's patience) would finish the job. The damage
 * half was already covered by {@code ItemStackUndyingMixin}; this gate is what made
 * explosions need their own answer.
 *
 * <p><b>Lava.</b> Vanilla's in-lava drift ({@code setUnderLavaMovement}) is a slow sink:
 * the buoyancy term is so weak that a stack falling from any height takes minutes to
 * come back up. A protected stack instead <em>rests on the surface as though the lava
 * were ground</em>: it rises toward a fixed submersion band, capped per tick by the
 * distance remaining so it can never overshoot, and once there its vertical velocity is
 * exactly zero. Because the stack stays submerged past the depth at which vanilla's
 * tick would apply gravity, gravity never runs — no sinking, no bobbing, no jitter.
 * The figures (band, rise speed, friction) are first-pass values awaiting playtesting,
 * all marked in the method below.
 *
 * <p><b>Source slot.</b> The inventory slot the stack occupied before it was dropped,
 * written by {@code ServerPlayerDropMixin} and read by {@code EntityUndyingMixin} when the
 * void hands the stack back. Persisted as a plain int beside vanilla's own fields, so a
 * chunk unload does not erase the way home. −1 means unknown; the return then falls back
 * to vanilla's slot finding.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityUndyingMixin implements SourceSlotAccess {
	/**
	 * Vanilla's {@code INFINITE_LIFETIME}, private in {@code ItemEntity}; the age that keeps
	 * the despawn countdown frozen. Verified against the tick source it is compared in.
	 */
	@Unique
	private static final int ABYSSFALL_INFINITE_LIFETIME = -32768;

	@Unique
	private int abyssfall$sourceSlot = -1;

	@Override
	public int abyssfall$getSourceSlot() {
		return this.abyssfall$sourceSlot;
	}

	@Override
	public void abyssfall$setSourceSlot(int slot) {
		this.abyssfall$sourceSlot = slot;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void abyssfall$neverDestroyedDoesNotExpire(CallbackInfo ci) {
		ItemEntity self = (ItemEntity)(Object)this;

		if (self.getAge() != ABYSSFALL_INFINITE_LIFETIME
				&& NeverDestroyed.INSTANCE.has(self.getItem())) {
			self.setUnlimitedLifetime();
		}
	}

	/**
	 * How deep the underside of a resting stack sits below the lava's surface: 0.15.
	 *
	 * <p>Must stay above vanilla's 0.1 in-lava threshold — the tick only routes here while
	 * submersion exceeds that value, and dipping below it would hand every other tick back
	 * to gravity, which is exactly the jitter this replaces. First-pass value.
	 */
	@Unique
	private static final double ABYSSFALL_LAVA_SURFACE_BAND = 0.15;

	/**
	 * The fastest a protected stack rises toward the band: +0.08 m/tick. First-pass value.
	 */
	@Unique
	private static final double ABYSSFALL_LAVA_RISE_SPEED = 0.08;

	/**
	 * Horizontal drag while resting on lava: ×0.6 per tick, the same order as ground
	 * friction — the stack settles where it landed instead of skating. First-pass value.
	 */
	@Unique
	private static final double ABYSSFALL_LAVA_FRICTION = 0.6;

	@Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
	private void abyssfall$neverDestroyedIgnoresExplosions(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
		if (NeverDestroyed.INSTANCE.has(((ItemEntity)(Object)this).getItem())) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "setUnderLavaMovement", at = @At("HEAD"), cancellable = true)
	private void abyssfall$neverDestroyedRestsOnLava(CallbackInfo ci) {
		ItemEntity self = (ItemEntity)(Object)this;

		if (!NeverDestroyed.INSTANCE.has(self.getItem())) {
			return;
		}

		ci.cancel();

		// getFluidHeight is the depth of the stack's underside below the lava's surface.
		// While any depth remains above the band the stack rises — capped per tick by the
		// distance left, so it cannot overshoot; once the band is reached the vertical
		// velocity is exactly zero. Because the stack stays submerged past the point where
		// vanilla's tick would apply gravity, gravity never runs: it holds there, on the
		// surface, as though the lava were ground.
		double depth = self.getFluidHeight(FluidTags.LAVA);
		double y = 0.0;

		if (depth > ABYSSFALL_LAVA_SURFACE_BAND) {
			y = Math.min(ABYSSFALL_LAVA_RISE_SPEED, depth - ABYSSFALL_LAVA_SURFACE_BAND);
		}

		Vec3 movement = self.getDeltaMovement();
		self.setDeltaMovement(movement.x * ABYSSFALL_LAVA_FRICTION, y, movement.z * ABYSSFALL_LAVA_FRICTION);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
	private void abyssfall$saveSourceSlot(ValueOutput output, CallbackInfo ci) {
		output.putInt("AbyssFallSourceSlot", this.abyssfall$sourceSlot);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
	private void abyssfall$loadSourceSlot(ValueInput input, CallbackInfo ci) {
		this.abyssfall$sourceSlot = input.getIntOr("AbyssFallSourceSlot", -1);
	}
}

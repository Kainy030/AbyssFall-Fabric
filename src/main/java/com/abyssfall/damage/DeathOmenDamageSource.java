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

package com.abyssfall.damage;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * A Death Omen blow, which words its own obituary.
 *
 * <p>Exists only to vary the death message. Vanilla derives the message key from the damage
 * type's {@code message_id} and offers no way to have more than one, so a type wanting three
 * phrasings has to say so itself: {@link #getLocalizedDeathMessage} is the single point every
 * death message passes through, and overriding it here is enough. The alternative would have
 * been another mixin, on {@code CombatTracker} or on the death path, to intercept a message
 * this class can simply author.
 *
 * <p>The variant is drawn when the source is built rather than when the message is read.
 * {@code getLocalizedDeathMessage} is called more than once per death — the player's own combat
 * packet, the broadcast to everyone else, and the log line for named entities all ask for it —
 * and choosing on each call would let one kill be described three different ways.
 *
 * <p>The messages take the victim's name and nothing else. Vanilla's ordinary attack messages
 * name the killer and their weapon; these do not, because the weapon's own framing is that the
 * death was pronounced rather than inflicted, and naming a culprit invites the reader to think
 * a fight took place.
 */
public class DeathOmenDamageSource extends DamageSource {
	private final int variant;

	/**
	 * @param type     the Death Omen damage type
	 * @param attacker the entity credited with the kill, or {@code null}
	 * @param variant  a one-based death message index within
	 *                 {@link AbyssFallDamageTypes#DEATH_MESSAGE_VARIANTS}
	 */
	public DeathOmenDamageSource(Holder<DamageType> type, Entity attacker, int variant) {
		super(type, attacker);
		this.variant = variant;
	}

	@Override
	public Component getLocalizedDeathMessage(LivingEntity victim) {
		return Component.translatable(
				AbyssFallDamageTypes.deathMessageKey(this.variant), victim.getDisplayName());
	}
}

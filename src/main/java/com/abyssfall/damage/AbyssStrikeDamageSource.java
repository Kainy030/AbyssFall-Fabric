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

import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * An Abyssal Strike blow from something other than the Final Death Omen — a modpack member
 * of {@code abyss_striking}. Mechanically it is the Omen's own blow: same damage type,
 * same verdict, same four steps in {@code FinalDeathOmen.strike}. Only the obituary
 * differs, by design: the Omen's death messages are the blade's own brand, and a weapon
 * that merely shares the trigger has no business wearing it.
 *
 * <p>The message is one entry drawn from the configurable {@code death_message_1..N} pool
 * when the source is built — a {@link String#format} pattern where {@code %1$s} is the
 * victim and {@code %2$s} the striker. The draw is made once and kept, because the message
 * is asked for more than once per death and one kill must be described one way. Written as
 * a literal rather than a translation: the config is the customization point, and every
 * client reads the same words.
 */
public class AbyssStrikeDamageSource extends DamageSource {
	/**
	 * The message pattern this blow was dealt, drawn from the config pool at build time.
	 */
	private final String message;

	/**
	 * @param type     the shared Death Omen damage type
	 * @param attacker the entity credited with the kill, or {@code null}
	 * @param message  the drawn message pattern: {@code %1$s} victim, {@code %2$s} striker
	 */
	public AbyssStrikeDamageSource(Holder<DamageType> type, Entity attacker, String message) {
		super(type, attacker);
		this.message = message;
	}

	@Override
	public Component getLocalizedDeathMessage(LivingEntity victim) {
		Component striker = this.getEntity() == null ? Component.empty() : this.getEntity().getDisplayName();

		return Component.literal(String.format(Locale.ROOT,
				this.message, victim.getDisplayName(), striker));
	}
}

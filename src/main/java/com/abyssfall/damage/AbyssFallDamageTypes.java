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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

import com.abyssfall.AbyssFall;

/**
 * The Death Omen damage type — what the Final Death Omen kills things with.
 *
 * <p>Only the key lives in code. The type itself is a data file
 * ({@code data/abyssfall/damage_type/death_omen.json}) because damage types are registry
 * content loaded from datapacks, and a registry entry cannot be conjured from Java at runtime.
 * The accompanying tag files put it in every {@code bypasses_*} tag vanilla offers.
 *
 * <p>Those tags are, strictly speaking, redundant for the weapon as implemented: the mixin
 * never enters {@code hurtServer}, so no mitigation step is reached to be bypassed in the
 * first place. They are declared anyway for two reasons. The type should mean the same thing
 * wherever it turns up — if anything ever deals this damage through the ordinary pipeline, it
 * must not be reduced by armour on the way — and a datapack author reading the tags should be
 * able to see what the damage claims about itself without reading the mixin.
 *
 * <p>The three death messages are picked at random per kill, which is why
 * {@code death.attack.death_omen} has {@code .1} / {@code .2} / {@code .3} variants rather
 * than the single key vanilla would generate from {@code msgId}. Vanilla derives its message
 * key from the type's {@code message_id} and offers no way to vary it, so the choice is made
 * where the kill is (see the mixin) and this class only names the keys.
 */
public final class AbyssFallDamageTypes {
	/**
	 * Registry key of the Death Omen damage type. Matches the data file's path, and is what the
	 * weapon looks up to build its damage source.
	 */
	public static final ResourceKey<DamageType> DEATH_OMEN =
			ResourceKey.create(Registries.DAMAGE_TYPE, AbyssFall.id("death_omen"));

	/**
	 * How many death message variants exist. Kept beside the keys so adding a fourth line means
	 * touching one number and one language file rather than hunting for the bound in the mixin.
	 */
	public static final int DEATH_MESSAGE_VARIANTS = 3;

	/**
	 * Translation key of one death message variant.
	 *
	 * @param variant a one-based index; callers should stay within
	 *                {@link #DEATH_MESSAGE_VARIANTS}
	 */
	public static String deathMessageKey(int variant) {
		return "death.attack.death_omen." + variant;
	}

	private AbyssFallDamageTypes() {
	}

	/**
	 * Builds a Death Omen damage source attributed to {@code attacker}, with one of the death
	 * messages already chosen.
	 *
	 * <p>Resolved from the level's registries on every call rather than cached in a static
	 * field: damage types are datapack content, so the holder is only valid for as long as the
	 * registries that produced it, and a value captured at mod init would outlive a
	 * {@code /reload} and point at a stale entry.
	 *
	 * @param level    the level whose registries hold the damage type
	 * @param attacker the entity to credit with the kill, or {@code null} for an unattributed one
	 */
	public static DamageSource create(ServerLevel level, Entity attacker) {
		Holder<DamageType> type =
				level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DEATH_OMEN);

		return new DeathOmenDamageSource(
				type, attacker, level.getRandom().nextInt(DEATH_MESSAGE_VARIANTS) + 1);
	}
}

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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;

import com.abyssfall.damage.AbyssFallDamageTypes;

/**
 * What happens when the Final Death Omen connects.
 *
 * <p>Invoked from {@code PlayerAttackMixin} in place of the vanilla attack, on the server only.
 * By the time this runs the decision has already been made — the swing landed, the weapon is
 * ours — so nothing here asks whether the target may be hurt. That question is the one the
 * weapon declines to ask.
 *
 * <h2>The order of the steps matters</h2>
 *
 * <p>Each of the four steps exists because skipping it loses something that the ordinary damage
 * pipeline would have provided, and the pipeline is not running:
 *
 * <ol>
 *   <li><b>Credit the player.</b> {@code lastHurtByPlayerMemoryTime} is what tells
 *       {@code dropAllDeathLoot} that a player was responsible, and it is normally set from
 *       inside {@code hurtServer}. Without it the kill counts as unattributed: experience is
 *       withheld and loot tables take their non-player branch, so a great many mobs would drop
 *       little or nothing.</li>
 *   <li><b>Record the blow.</b> The death message is read from the last entry in the victim's
 *       {@code CombatTracker}, and entries are only ever added by {@code recordDamage} inside
 *       {@code actuallyHurt}. With no entry the tracker falls back to {@code death.attack.generic}
 *       — "so-and-so died" — and the weapon's own wording is lost. The damage recorded is the
 *       target's health at this instant, so the figure in the statistics is the whole of what was
 *       taken.</li>
 *   <li><b>Empty the target.</b> Setting health to zero directly, rather than subtracting
 *       damage, because subtraction is the step where absorption and every other mitigation
 *       would ordinarily get a say.</li>
 *   <li><b>Run the death.</b> {@code die} is what drops the loot, awards the experience, updates
 *       the scoreboard and sends the message. It is idempotent — it begins by checking
 *       {@code !isRemoved() && !dead} — so calling it here is safe even in the event that
 *       something else already did.</li>
 * </ol>
 *
 * <p>Non-living entities have no health to empty and no death to run, so they are removed
 * through {@code Entity#kill} instead. Boats, minecarts, armour stands and end crystals all
 * override it with their own idea of being destroyed, and calling it rather than removing them
 * outright lets each keep that.
 */
public final class FinalDeathOmen {
	/**
	 * How long the victim remembers being killed by a player, in ticks.
	 *
	 * <p>The same hundred ticks vanilla uses in {@code resolvePlayerResponsibleForDamage}. The
	 * value only has to outlast the death it is set during, but matching vanilla means anything
	 * else reading the field sees what it would normally see.
	 */
	private static final int PLAYER_KILL_CREDIT_TICKS = 100;

	private FinalDeathOmen() {
	}

	/**
	 * Pronounces {@code target} dead.
	 *
	 * @param level    the server level the attack happened in
	 * @param attacker the player wielding the weapon
	 * @param target   whatever was struck; need not be a {@link LivingEntity}
	 */
	public static void strike(ServerLevel level, Player attacker, Entity target) {
		// A dragon's hitboxes are separate entities that hold no health of their own. Vanilla's own
		// spear code redirects to the parent the same way, so striking a wing is striking the
		// dragon and the quarter-damage reduction applied to non-head parts never enters into it.
		Entity struck = target instanceof EnderDragonPart part ? part.parentMob : target;

		DamageSource source = AbyssFallDamageTypes.create(level, attacker);

		if (struck instanceof LivingEntity victim) {
			// Step 1: without this the kill is not a player's and the drops go with it.
			victim.setLastHurtByPlayer(attacker, PLAYER_KILL_CREDIT_TICKS);

			// Step 2: the figure the weapon claims is the whole of what the target had left.
			float dealt = victim.getHealth();
			victim.getCombatTracker().recordDamage(source, dealt);

			// Step 3.
			victim.setHealth(0.0F);

			// Step 4. Idempotent, so a second death here would be a no-op rather than double loot.
			victim.die(source);
		} else {
			struck.kill(level);
		}
	}
}
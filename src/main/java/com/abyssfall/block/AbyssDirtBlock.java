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

package com.abyssfall.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.abyssfall.config.AbyssFallConfig;
import com.abyssfall.config.VisualSettings;
import com.abyssfall.item.AbyssFallItems;

/**
 * Abyss Dirt — behaves exactly like vanilla dirt, except that a wither rose planted on top
 * of it can be forced to bloom.
 *
 * <p>Applying bone meal consumes the rose and leaves a single Flower of the Abyss where it
 * stood. The block implements {@link BonemealableBlock} so that bone meal applied directly
 * to the dirt works; bone meal applied to the rose itself is routed here by
 * {@code AbyssFallBoneMealHandler}, because vanilla's {@code FlowerBlock} is not
 * bonemealable and would otherwise swallow the interaction.
 */
public class AbyssDirtBlock extends Block implements BonemealableBlock {
	public AbyssDirtBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	/**
	 * Whether the given position holds abyss dirt with a wither rose directly above it.
	 */
	public static boolean hasHarvestableRose(LevelReader level, BlockPos dirtPos) {
		return level.getBlockState(dirtPos).getBlock() instanceof AbyssDirtBlock
				&& level.getBlockState(dirtPos.above()).is(Blocks.WITHER_ROSE);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return hasHarvestableRose(level, pos);
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		// The bloom is a deliberate, reliable interaction rather than a gamble, so this
		// never fails once a rose is present.
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		bloom(level, pos);
	}

	/**
	 * Consumes the wither rose above {@code dirtPos} and yields a Flower of the Abyss.
	 *
	 * @return whether a rose was actually consumed
	 */
	public static boolean bloom(ServerLevel level, BlockPos dirtPos) {
		BlockPos rosePos = dirtPos.above();

		if (!level.getBlockState(rosePos).is(Blocks.WITHER_ROSE)) {
			return false;
		}

		// Destroying the rose with drops disabled and then dropping the flower ourselves
		// means the player never gets the rose back — it is consumed by the bloom.
		level.destroyBlock(rosePos, false);
		Block.popResource(level, rosePos, new ItemStack(AbyssFallItems.ABYSS_FLOWER));

		playBloomEffects(level, rosePos);

		return true;
	}

	/**
	 * The visual and audible payoff of the bloom.
	 *
	 * <p>Deliberately built from soul-flavoured vanilla particles rather than the cheerful
	 * green {@code HAPPY_VILLAGER} burst bone meal normally produces: the rose is being
	 * consumed, not fertilised. Three layers are stacked so the effect reads well both up
	 * close and from a distance.
	 *
	 * <p>Particle counts and sound volumes are scaled by the configured multipliers, so the
	 * whole display can be toned down or switched off without changing what it is made of. The
	 * pitches are not configurable: the low-then-high pairing is what makes the moment read as
	 * "something was given, something arrived" rather than a generic pop.
	 */
	private static void playBloomEffects(ServerLevel level, BlockPos rosePos) {
		VisualSettings visuals = AbyssFallConfig.visuals();

		double x = rosePos.getX() + 0.5;
		double y = rosePos.getY() + 0.4;
		double z = rosePos.getZ() + 0.5;

		if (visuals.hasParticles()) {
			// Souls drifting upward out of the spent rose.
			level.sendParticles(ParticleTypes.SOUL, x, y, z,
					visuals.scaleParticles(12), 0.22, 0.30, 0.22, 0.02);

			// A short-lived dark bloom hugging the ground, hinting at what came through.
			level.sendParticles(ParticleTypes.SCULK_SOUL, x, y + 0.1, z,
					visuals.scaleParticles(8), 0.28, 0.12, 0.28, 0.01);

			// Downward-pulling motes: the abyss reclaiming the flower's price.
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 0.6, z,
					visuals.scaleParticles(20), 0.30, 0.20, 0.30, 0.05);

			// Smoke where the rose stood, so the destruction itself is legible.
			level.sendParticles(ParticleTypes.SMOKE, x, y, z,
					visuals.scaleParticles(6), 0.18, 0.10, 0.18, 0.01);
		}

		if (visuals.hasSounds()) {
			// A soul escaping, layered under the sculk catalyst's bloom chime. The two together
			// land as "something was given, something arrived" rather than a generic pop.
			level.playSound(null, rosePos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS,
					visuals.scaleVolume(0.7F), 0.6F);
			level.playSound(null, rosePos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS,
					visuals.scaleVolume(0.5F), 1.4F);
		}
	}

	@Override
	public BonemealableBlock.Type getType() {
		// Particles should appear where the rose was, i.e. above the dirt.
		return BonemealableBlock.Type.NEIGHBOR_SPREADER;
	}
}

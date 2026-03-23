package com.mysteryofthecrash.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SpaceshipCoreBlock extends Block {

    public SpaceshipCoreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.15f) {
            double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double py = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0, 0.02, 0);
        }
        if (random.nextFloat() < 0.08f) {
            double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5);
            double py = pos.getY() + 0.8;
            double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz,
                    (random.nextDouble() - 0.5) * 0.05, 0.04, (random.nextDouble() - 0.5) * 0.05);
        }
    }
}

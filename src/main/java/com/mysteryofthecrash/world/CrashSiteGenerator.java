package com.mysteryofthecrash.world;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.registry.ModBlocks;
import com.mysteryofthecrash.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

public class CrashSiteGenerator {

    public static void generate(ServerLevel level) {
        AlienWorldData data = AlienWorldData.get(level);
        if (data.isCrashSiteGenerated()) return;

        Random random = new Random(level.getSeed() + 42L);

        BlockPos centre = findLandPosition(level, random);
        MysteryOfTheCrash.LOGGER.info("[CrashSite] Land position selected: {}", centre);

        MysteryOfTheCrash.LOGGER.info("[CrashSite] Generating crash site at {}", centre);

        buildCrater(level, centre);
        buildDebris(level, centre, random);
        buildCore(level, centre);
        spawnAlien(level, centre, data);

        data.setCrashSitePos(centre);
        data.setCrashSiteGenerated(true);
        data.setDirty();

        MysteryOfTheCrash.LOGGER.info("[CrashSite] Done.");
    }

    private static BlockPos findLandPosition(ServerLevel level, Random random) {
        BlockPos best = null;

        for (int attempt = 0; attempt < 20; attempt++) {
            int x = random.nextInt(400) - 200;
            int z = random.nextInt(400) - 200;

            level.getChunk(x >> 4, z >> 4);

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

            y = Math.max(level.getMinBuildHeight() + 5, Math.min(y, level.getMaxBuildHeight() - 10));

            BlockPos pos = new BlockPos(x, y, z);

            if (!level.getFluidState(pos).isEmpty()) {
                best = pos;
                continue;
            }

            if (y <= level.getSeaLevel()) {
                best = pos;
                continue;
            }

            MysteryOfTheCrash.LOGGER.info("[CrashSite] Land found at attempt {}: {}", attempt, pos);
            return pos;
        }

        MysteryOfTheCrash.LOGGER.warn("[CrashSite] Could not find dry land after 20 attempts; using {}", best);
        return best != null ? best : new BlockPos(0, 65, 0);
    }

    private static void buildCrater(ServerLevel level, BlockPos centre) {
        BlockState coarseDirt = Blocks.COARSE_DIRT.defaultBlockState();
        BlockState gravel     = Blocks.GRAVEL.defaultBlockState();

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 4.5) continue;

                BlockPos pos = centre.offset(dx, 0, dz);
                level.setBlock(pos, dist < 2.5 ? gravel : coarseDirt, 3);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.above(2), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void buildDebris(ServerLevel level, BlockPos centre, Random random) {
        BlockState debris = ModBlocks.ALIEN_DEBRIS.get().defaultBlockState();

        int[][] offsets = {
            {-3, -3}, {3, -3}, {-3, 3}, {3, 3},
            {0, -4},  {0, 4},  {-4, 0}, {4, 0}
        };

        for (int[] offset : offsets) {
            int dx = offset[0] + random.nextInt(3) - 1;
            int dz = offset[1] + random.nextInt(3) - 1;
            BlockPos pos = centre.offset(dx, 0, dz);
            level.setBlock(pos, debris, 3);
            if (random.nextBoolean()) {
                level.setBlock(pos.above(), debris, 3);
            }
        }
    }

    private static void buildCore(ServerLevel level, BlockPos centre) {
        BlockState core = ModBlocks.SPACESHIP_CORE.get().defaultBlockState();

        int[][] coreOffsets = {
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };
        for (int[] o : coreOffsets) {
            level.setBlock(centre.offset(o[0], 0, o[1]), core, 3);
        }

        level.setBlock(centre.above(), core, 3);
        level.setBlock(centre.above(2), core, 3);
    }

    private static void spawnAlien(ServerLevel level, BlockPos centre, AlienWorldData data) {
        AlienEntity alien = ModEntities.ALIEN.get().create(level);
        if (alien == null) {
            MysteryOfTheCrash.LOGGER.error("[CrashSite] Failed to create AlienEntity!");
            return;
        }

        alien.setCrashSitePos(centre);

        int safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, centre.getX(), centre.getZ());
        alien.moveTo(centre.getX() + 0.5, safeY, centre.getZ() + 0.5, 0f, 0f);

        alien.finalizeSpawn(level,
                level.getCurrentDifficultyAt(centre),
                net.minecraft.world.entity.MobSpawnType.EVENT,
                null);

        level.addFreshEntity(alien);

        data.setAlienUUID(alien.getUUID());
        data.setRespawnPos(centre);

        MysteryOfTheCrash.LOGGER.info("[CrashSite] Alien spawned with UUID {}", alien.getUUID());
    }
}

package com.mysteryofthecrash.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BlockUtil {

    private BlockUtil() {}

    public static int getSafeY(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return pos.getY() + 2;
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    }

    public static int getSafeYBlocking(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return pos.getY() + 3;
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
    }
}

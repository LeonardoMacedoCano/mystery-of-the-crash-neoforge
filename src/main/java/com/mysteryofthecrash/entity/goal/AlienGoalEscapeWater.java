package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

public class AlienGoalEscapeWater extends Goal {

    private final AlienEntity alien;
    private BlockPos escapeTarget = null;

    public AlienGoalEscapeWater(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return alien.isInWater() && alien.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return alien.isInWater();
    }

    @Override
    public void start() {
        escapeTarget = findNearbyLand();
    }

    @Override
    public void stop() {
        escapeTarget = null;
        alien.getNavigation().stop();
    }

    @Override
    public void tick() {

        if (alien.isUnderWater()) {
            alien.setDeltaMovement(alien.getDeltaMovement().add(0, 0.08, 0));
        }
        if (escapeTarget != null) {
            if (alien.getNavigation().isDone()) {

                escapeTarget = findNearbyLand();
                if (escapeTarget != null) {
                    alien.getNavigation().moveTo(
                            escapeTarget.getX() + 0.5,
                            escapeTarget.getY(),
                            escapeTarget.getZ() + 0.5,
                            1.4);
                }
            }
        }
    }

    private BlockPos findNearbyLand() {
        if (!(alien.level() instanceof ServerLevel sl)) return null;
        BlockPos origin = alien.blockPosition();
        int bestDistSq = Integer.MAX_VALUE;
        BlockPos best = null;
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (!sl.hasChunkAt(new BlockPos(x, 64, z))) continue;
                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos below = new BlockPos(x, y - 1, z);

                if (!sl.getBlockState(below).getFluidState().isEmpty()) continue;
                int distSq = dx * dx + dz * dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = new BlockPos(x, y, z);
                }
            }
        }
        return best;
    }
}

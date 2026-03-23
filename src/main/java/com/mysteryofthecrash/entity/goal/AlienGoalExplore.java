package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class AlienGoalExplore extends Goal {

    private final AlienEntity alien;
    private boolean enabled = false;
    private boolean running = false;
    private Vec3 targetPos;
    private int stuck = 0;

    private static final float WALK_SPEED = 0.9f;

    public AlienGoalExplore(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public void setEnabled(boolean e) { this.enabled = e; }
    public boolean isActive() { return running && enabled; }

    @Override
    public boolean canUse() {
        if (!enabled) return false;
        return pickTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (!enabled) return false;
        if (alien.getNavigation().isDone()) return false;
        if (++stuck > 200) return false;
        return true;
    }

    @Override
    public void start() {
        running = true;
        stuck = 0;
        if (targetPos != null) {
            alien.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, WALK_SPEED);
        }
        alien.setExploring(true);
    }

    @Override
    public void stop() {
        running = false;
        alien.getNavigation().stop();
        alien.setExploring(false);
        targetPos = null;
    }

    @Override
    public void tick() {
        if (stuck % 40 == 0) {
            alien.getLearner().onExperiment(alien);
        }
    }

    private boolean pickTarget() {
        int range = switch (alien.getLifeStage()) {
            case CHILD -> 8;
            case YOUNG -> 20;
            case ADULT -> 48;
        };

        Vec3 pos = DefaultRandomPos.getPos(alien, range, 4);
        if (pos == null) return false;
        targetPos = pos;
        return true;
    }
}

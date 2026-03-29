package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class AlienGoalLevitate extends Goal {

    private static final double FALL_THRESHOLD = -0.15;
    private static final double LIFT_FORCE     = 0.20;
    private static final double MAX_LIFT_SPEED = 0.40;

    private final AlienEntity alien;

    public AlienGoalLevitate(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.noneOf(Flag.class)); 
    }

    @Override
    public boolean canUse() {
        if (!alien.getLifeStage().canMine) return false; 
        boolean falling   = !alien.onGround() && alien.getDeltaMovement().y < FALL_THRESHOLD;
        boolean wallStuck = alien.horizontalCollision && !alien.getNavigation().isDone();
        return falling || wallStuck;
    }

    @Override
    public boolean canContinueToUse() {
        if (!alien.getLifeStage().canMine) return false;
        boolean falling   = !alien.onGround() && alien.getDeltaMovement().y < 0.0;
        boolean wallStuck = alien.horizontalCollision && !alien.getNavigation().isDone();
        return falling || wallStuck;
    }

    @Override
    public void stop() {
        alien.setNoGravity(false);
    }

    @Override
    public void tick() {
        Vec3 motion = alien.getDeltaMovement();
        alien.setDeltaMovement(motion.x, Math.min(motion.y + LIFT_FORCE, MAX_LIFT_SPEED), motion.z);
        alien.setNoGravity(true);
    }
}

package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class AlienGoalFollowPlayer extends Goal {

    private final AlienEntity alien;
    private Player target;
    private boolean enabled = true;
    private boolean running = false;
    private int recalcTimer = 0;

    private static final float WALK_SPEED = 1.0f;

    public AlienGoalFollowPlayer(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isActive() { return running && enabled; }

    @Override
    public boolean canUse() {
        if (!enabled) return false;
        target = alien.level().getNearestPlayer(alien, 32.0);
        if (target == null) return false;

        if (alien.getTrustManager().isAvoidant(target.getUUID())) return false;

        float stopDist = alien.getTrustManager().preferredProximity(target.getUUID());
        return target.distanceTo(alien) > stopDist + 1.0f;
    }

    @Override
    public boolean canContinueToUse() {
        if (!enabled || target == null || !target.isAlive()) return false;
        float stopDist = alien.getTrustManager().preferredProximity(target.getUUID());
        return target.distanceTo(alien) > stopDist;
    }

    @Override
    public void start() {
        running = true;
        recalcTimer = 0;
    }

    @Override
    public void tick() {
        if (target == null) return;
        alien.getLookControl().setLookAt(target, 10f, alien.getMaxHeadXRot());

        if (--recalcTimer <= 0) {
            recalcTimer = 10;
            alien.getNavigation().moveTo(target, WALK_SPEED);
        }
    }

    @Override
    public void stop() {
        running = false;
        alien.getNavigation().stop();
    }
}

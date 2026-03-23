package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class AlienGoalAvoidThreat extends Goal {

    private final AlienEntity alien;
    private boolean enabled = true;
    private LivingEntity threat;
    private Vec3 fleeTarget;

    private static final TargetingConditions THREAT_CONDITIONS =
            TargetingConditions.forNonCombat().ignoreLineOfSight();

    public AlienGoalAvoidThreat(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setEnabled(boolean e) { this.enabled = e; }

    @Override
    public boolean canUse() {
        if (!enabled) return false;

        int fleeDist = switch (alien.getLifeStage()) {
            case CHILD -> 16;
            case YOUNG -> 10;
            case ADULT -> 6;
        };
        if (alien.getPersonality() == com.mysteryofthecrash.entity.Personality.PROTECTIVE) {
            fleeDist = 3;
        }

        List<Monster> threats = alien.level().getEntitiesOfClass(
                Monster.class,
                alien.getBoundingBox().inflate(fleeDist),
                m -> m.isAlive());

        if (threats.isEmpty()) return false;

        threat = threats.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(alien), b.distanceToSqr(alien)))
                .orElse(null);

        if (threat == null) return false;

        fleeTarget = DefaultRandomPos.getPosAway(alien, 12, 4, threat.position());
        return fleeTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return threat != null && threat.isAlive()
                && alien.distanceTo(threat) < 24
                && !alien.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (fleeTarget != null) {
            alien.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 1.3f);
        }
        alien.getNeeds().safety = Math.min(100f, alien.getNeeds().safety + 30f);

        if (alien.level().random.nextFloat() < 0.4f) {
            alien.getTelepathicChat().sendRandomMessage(alien,
                    alien.getLifeStage(), alien.getPersonality());
        }
    }

    @Override
    public void stop() {
        threat = null;
        fleeTarget = null;
    }
}

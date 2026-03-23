package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class AlienGoalRest extends Goal {

    private final AlienEntity alien;
    private boolean enabled = false;
    private boolean running = false;
    private int ticksResting = 0;
    private int maxRestTicks = 200;

    public AlienGoalRest(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    public void setEnabled(boolean e) { this.enabled = e; }
    public boolean isActive() { return running && enabled; }

    @Override
    public boolean canUse() {
        if (!enabled) return false;
        return alien.getNeeds().safety < 30f;
    }

    @Override
    public boolean canContinueToUse() {
        return enabled && ticksResting < maxRestTicks && alien.getNeeds().safety < 50f;
    }

    @Override
    public void start() {
        running = true;
        ticksResting = 0;
        alien.getNavigation().stop();
        float lazyMult = alien.getPersonality() == com.mysteryofthecrash.entity.Personality.LAZY
                ? 2.0f : 1.0f;
        maxRestTicks = (int)(200 * lazyMult);

        if (alien.level().random.nextFloat() < 0.3f) {
            alien.getTelepathicChat().sendRandomMessage(alien,
                    alien.getLifeStage(), alien.getPersonality());
        }
    }

    @Override
    public void tick() {
        ticksResting++;
        alien.getNavigation().stop();

        if (ticksResting % 40 == 0) {
            alien.getNeeds().feed(1f);
            var player = alien.level().getNearestPlayer(alien, 8.0);
            if (player != null) {
                alien.getTrustManager().onPlayerNearbyRest();
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        ticksResting = 0;
    }
}

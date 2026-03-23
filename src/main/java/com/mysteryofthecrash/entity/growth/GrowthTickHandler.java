package com.mysteryofthecrash.entity.growth;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.LifeStage;
import com.mysteryofthecrash.entity.Personality;
import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class GrowthTickHandler {

    private static final long TICKS_PER_DAY = 24000L;

    public void onSecondTick(AlienEntity alien, ServerLevel level) {
        long worldDay = level.getGameTime() / TICKS_PER_DAY;
        int  elapsedDays = (int) Math.max(0, worldDay - alien.getBirthDay());

        LifeStage currentStage = alien.getLifeStage();
        LifeStage newStage     = LifeStage.fromDays(elapsedDays);

        if (newStage != currentStage) {
            handleStageTransition(alien, currentStage, newStage);
        }

        Player nearest = level.getNearestPlayer(alien, 32.0);
        boolean nearPlayer   = nearest != null && nearest.distanceTo(alien) < 8.0;
        boolean isExploring  = alien.isCurrentGoalExploring();
        boolean isIdle       = alien.isIdle();
        boolean nearThreat   = !level.getNearbyEntities(
                net.minecraft.world.entity.LivingEntity.class,
                net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat(),
                alien, alien.getBoundingBox().inflate(12)).stream()
                .filter(e -> e instanceof net.minecraft.world.entity.monster.Monster)
                .toList().isEmpty();

        alien.getNeeds().tick(isIdle, isExploring, nearPlayer, nearThreat);

        if (currentStage == LifeStage.CHILD && !alien.getPersonalityResolver().isLocked()) {
            if (nearPlayer) {
                alien.getPersonalityResolver().recordProximityTick(0.01f);
            }
            if (isExploring) {
                alien.getPersonalityResolver().recordExplorationTick(0.01f);
            }
            if (nearThreat) {
                alien.getPersonalityResolver().recordDangerTick(0.01f);
            }
        }

        alien.setChanged();
    }

    private void handleStageTransition(AlienEntity alien, LifeStage from, LifeStage to) {
        MysteryOfTheCrash.LOGGER.info("[Alien] Life stage transition: {} → {}", from, to);

        if (from == LifeStage.CHILD && to == LifeStage.YOUNG) {
            Personality personality = alien.getPersonalityResolver().resolve();
            alien.setPersonality(personality);
            MysteryOfTheCrash.LOGGER.info("[Alien] Personality locked: {}", personality);

            alien.getTelepathicChat().sendTransitionMessage(alien,
                    "young_transition", personality);
        }

        if (from == LifeStage.YOUNG && to == LifeStage.ADULT) {
            alien.getTelepathicChat().sendTransitionMessage(alien,
                    "adult_transition", alien.getPersonality());
        }

        alien.setLifeStage(to);
        alien.applyStageAttributes(to);
    }
}

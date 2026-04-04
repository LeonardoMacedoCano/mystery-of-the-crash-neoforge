package com.mysteryofthecrash.entity.growth;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.LifeStage;
import com.mysteryofthecrash.entity.Personality;
import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class GrowthTickHandler {

    private static final long   TICKS_PER_DAY    = 24000L;
    private static final int    STUCK_TIMEOUT_SEC = 30;
    private static final double MOVE_THRESHOLD_SQ = 4.0;
    private static final double NEAR_SPAWN_SQ     = 100.0;

    private BlockPos lastCheckedPos = null;
    private int      stuckSeconds   = 0;

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

        alien.getNeeds().tick(isIdle, isExploring, nearPlayer, nearThreat, 1.0f);

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

        if (alien.getAlienBrain() != null
                && !alien.getAlienBrain().isUnderPlayerCommand()
                && !alien.getAlienBrain().isMining()
                && !alien.getAlienBrain().isReturningHome()) {

            BlockPos currentPos = alien.blockPosition();
            if (lastCheckedPos == null
                    || currentPos.distSqr(lastCheckedPos) > MOVE_THRESHOLD_SQ) {
                lastCheckedPos = currentPos;
                stuckSeconds   = 0;
            } else {
                stuckSeconds++;
                if (stuckSeconds >= STUCK_TIMEOUT_SEC) {
                    stuckSeconds = 0;
                    BlockPos spawn = alien.getCrashSitePos();
                    if (spawn != null && !spawn.equals(BlockPos.ZERO)) {
                        double distToSpawn = currentPos.distSqr(spawn);
                        if (distToSpawn <= NEAR_SPAWN_SQ) {
                        } else if (!level.hasChunkAt(spawn)) {
                            MysteryOfTheCrash.LOGGER.debug("[Alien] Stuck — spawn chunk not loaded, skipping TP");
                        } else {
                            int safeY = BlockUtil.getSafeY(level, spawn);
                            alien.teleportTo(spawn.getX() + 0.5, safeY, spawn.getZ() + 0.5);
                            alien.getAlienBrain().commandRelease();
                            MysteryOfTheCrash.LOGGER.info("[Alien] Stuck {}s — teleported to spawn", STUCK_TIMEOUT_SEC);
                        }
                    }
                }
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

            autoEquipLeatherArmor(alien);

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

    private void autoEquipLeatherArmor(AlienEntity alien) {
        var inv = alien.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ArmorItem armor)) continue;
            EquipmentSlot slot = armor.getEquipmentSlot();
            if (alien.getItemBySlot(slot).isEmpty()) {
                alien.setItemSlot(slot, stack.copy());
                alien.setDropChance(slot, 1.0f);
                inv.setItem(i, ItemStack.EMPTY);
                MysteryOfTheCrash.LOGGER.info("[Alien] Auto-equipped {} in {}", armor, slot);
            }
        }
    }
}

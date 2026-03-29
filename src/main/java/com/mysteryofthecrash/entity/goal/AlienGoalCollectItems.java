package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;
import java.util.List;

public class AlienGoalCollectItems extends Goal {

    private final AlienEntity alien;
    private boolean enabled = false;
    private ItemEntity targetItem;
    private int phase = 0;

    public AlienGoalCollectItems(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setEnabled(boolean e) { this.enabled = e; }

    @Override
    public boolean canUse() {
        if (!enabled) return false;
        if (alien.getTrustManager().getHighestTrust() < -20) return false;

        targetItem = findNearestItem();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!enabled) return false;
        if (phase == 0) return targetItem != null && targetItem.isAlive();
        return phase == 1 && alien.level().getNearestPlayer(alien, 16) != null;
    }

    @Override
    public void start() {
        phase = 0;
        if (targetItem != null) {
            alien.getNavigation().moveTo(targetItem, 1.1f);
        }
    }

    @Override
    public void tick() {
        if (phase == 0 && targetItem != null) {
            alien.getLookControl().setLookAt(targetItem, 10f, 10f);

            if (alien.distanceTo(targetItem) < 2.0f) {
                targetItem.discard();
                phase = 1;
                var player = alien.level().getNearestPlayer(alien, 16);
                if (player != null) {
                    alien.getNavigation().moveTo(player, 1.1f);
                }
            }
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        phase = 0;
        alien.getNavigation().stop();
    }

    private ItemEntity findNearestItem() {
        List<ItemEntity> items = alien.level().getEntitiesOfClass(
                ItemEntity.class,
                alien.getBoundingBox().inflate(6.0),
                item -> item.isAlive() && !item.hasPickUpDelay());

        if (items.isEmpty()) return null;

        ItemEntity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double d = alien.distanceToSqr(item);
            if (d < minDist) {
                minDist = d;
                nearest = item;
            }
        }
        return nearest;
    }
}

package com.mysteryofthecrash.entity.goal;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class AlienGoalEat extends Goal {

    private static final float HUNGER_THRESHOLD = 70f;
    private static final int   EAT_DURATION     = 32; 

    private final AlienEntity alien;
    private boolean enabled  = true;
    private boolean running  = false;
    private int     eatTimer = 0;
    private ItemStack foodItem = ItemStack.EMPTY;

    public AlienGoalEat(AlienEntity alien) {
        this.alien = alien;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void setEnabled(boolean e) { this.enabled = e; }

    @Override
    public boolean canUse() {
        if (!enabled) return false;
        if (alien.getNeeds().hunger < HUNGER_THRESHOLD) return false;
        return findFood() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return enabled && running && eatTimer < EAT_DURATION;
    }

    @Override
    public void start() {
        running  = true;
        eatTimer = 0;
        alien.getNavigation().stop();
        foodItem = findFood() != null ? findFood() : ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        eatTimer++;
        if (eatTimer >= EAT_DURATION) {
            consumeFood();
            running = false;
        }
    }

    @Override
    public void stop() {
        running  = false;
        eatTimer = 0;
        foodItem = ItemStack.EMPTY;
    }

    private void consumeFood() {
        SimpleContainer inv = alien.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food == null) continue;

            float nutrition = food.nutrition() * 5f;
            alien.getNeeds().feed(nutrition);
            alien.getNeeds().sleepiness = Math.max(0f, alien.getNeeds().sleepiness - 5f);
            stack.shrink(1);
            if (stack.isEmpty()) inv.setItem(i, ItemStack.EMPTY);

            MysteryOfTheCrash.LOGGER.info("[AlienEat] Ate {} — hunger -{}, now {:.0f}",
                    stack.getItem(), nutrition, alien.getNeeds().hunger);
            return;
        }
    }

    private ItemStack findFood() {
        SimpleContainer inv = alien.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) return stack;
        }
        return null;
    }
}

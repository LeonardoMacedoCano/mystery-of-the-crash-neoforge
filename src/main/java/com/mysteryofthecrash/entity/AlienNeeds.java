package com.mysteryofthecrash.entity;

import net.minecraft.nbt.CompoundTag;

public class AlienNeeds {

    public float curiosity   = 50f;

    public float boredom     = 30f;

    public float hunger      = 20f;

    public float socialNeed  = 40f;

    public float safety      = 10f;

    public float sleepiness  = 20f;

    private static final float CURIOSITY_DECAY_PER_SEC  = 1.0f;
    private static final float CURIOSITY_RISE_PER_SEC   = 0.4f;
    private static final float BOREDOM_RISE_PER_SEC     = 1.6f;
    private static final float BOREDOM_DECAY_PER_SEC    = 2.4f;
    private static final float HUNGER_BASE_PER_SEC      = 0.8f;
    private static final float HUNGER_URGENT_MULTIPLIER = 1.5f;
    private static final float HUNGER_URGENT_THRESHOLD  = 70f;
    private static final float SOCIAL_DECAY_PER_SEC     = 2.0f;
    private static final float SOCIAL_RISE_PER_SEC      = 1.2f;
    private static final float SAFETY_RISE_PER_SEC      = 3.0f;
    private static final float SAFETY_DECAY_PER_SEC     = 2.0f;
    private static final float SLEEPINESS_RISE_PER_SEC  = 0.4f;

    public void tick(boolean isIdle, boolean isExploring, boolean isNearPlayer,
                     boolean isNearThreat, float deltaSeconds) {
        curiosity  = clamp(curiosity  + (isExploring ? -CURIOSITY_DECAY_PER_SEC : CURIOSITY_RISE_PER_SEC) * deltaSeconds, 0, 100);
        boredom    = clamp(boredom    + (isIdle ? BOREDOM_RISE_PER_SEC : -BOREDOM_DECAY_PER_SEC) * deltaSeconds, 0, 100);

        float hungerRate = hunger > HUNGER_URGENT_THRESHOLD ? HUNGER_BASE_PER_SEC * HUNGER_URGENT_MULTIPLIER : HUNGER_BASE_PER_SEC;
        hunger     = clamp(hunger    + hungerRate * deltaSeconds, 0, 100);

        socialNeed = clamp(socialNeed + (isNearPlayer ? -SOCIAL_DECAY_PER_SEC : SOCIAL_RISE_PER_SEC) * deltaSeconds, 0, 100);
        safety     = clamp(safety    + (isNearThreat  ? SAFETY_RISE_PER_SEC   : -SAFETY_DECAY_PER_SEC) * deltaSeconds, 0, 100);
        sleepiness = clamp(sleepiness + SLEEPINESS_RISE_PER_SEC * deltaSeconds, 0, 100);
    }

    public void feed(float amount) {
        hunger = clamp(hunger - amount, 0, 100);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("curiosity",  curiosity);
        tag.putFloat("boredom",    boredom);
        tag.putFloat("hunger",     hunger);
        tag.putFloat("socialNeed", socialNeed);
        tag.putFloat("safety",     safety);
        tag.putFloat("sleepiness", sleepiness);
        return tag;
    }

    public void load(CompoundTag tag) {
        curiosity  = tag.getFloat("curiosity");
        boredom    = tag.getFloat("boredom");
        hunger     = tag.getFloat("hunger");
        socialNeed = tag.getFloat("socialNeed");
        safety     = tag.getFloat("safety");
        sleepiness = tag.contains("sleepiness") ? tag.getFloat("sleepiness") : 20f;
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}

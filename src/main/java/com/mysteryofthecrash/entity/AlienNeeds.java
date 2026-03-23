package com.mysteryofthecrash.entity;

import net.minecraft.nbt.CompoundTag;

public class AlienNeeds {

    public float curiosity   = 50f;

    public float boredom     = 30f;

    public float hunger      = 20f;

    public float socialNeed  = 40f;

    public float safety      = 10f;

    private static final float CURIOSITY_DECAY_RATE   = 0.05f;
    private static final float BOREDOM_RISE_RATE       = 0.08f;
    private static final float BOREDOM_DECAY_RATE      = 0.12f;
    private static final float HUNGER_RISE_RATE        = 0.04f;
    private static final float SOCIAL_RISE_RATE        = 0.06f;
    private static final float SOCIAL_DECAY_RATE       = 0.10f;

    public void tick(boolean isIdle, boolean isExploring, boolean isNearPlayer,
                     boolean isNearThreat) {
        if (isExploring) {
            curiosity  = clamp(curiosity  - CURIOSITY_DECAY_RATE * 20, 0, 100);
        } else {
            curiosity  = clamp(curiosity  + 0.02f * 20, 0, 100);
        }

        if (isIdle) {
            boredom    = clamp(boredom    + BOREDOM_RISE_RATE  * 20, 0, 100);
        } else {
            boredom    = clamp(boredom    - BOREDOM_DECAY_RATE * 20, 0, 100);
        }

        hunger         = clamp(hunger     + HUNGER_RISE_RATE   * 20, 0, 100);

        if (isNearPlayer) {
            socialNeed = clamp(socialNeed - SOCIAL_DECAY_RATE  * 20, 0, 100);
        } else {
            socialNeed = clamp(socialNeed + SOCIAL_RISE_RATE   * 20, 0, 100);
        }

        safety         = isNearThreat ? clamp(safety + 3f, 0, 100)
                                      : clamp(safety - 2f, 0, 100);
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
        return tag;
    }

    public void load(CompoundTag tag) {
        curiosity  = tag.getFloat("curiosity");
        boredom    = tag.getFloat("boredom");
        hunger     = tag.getFloat("hunger");
        socialNeed = tag.getFloat("socialNeed");
        safety     = tag.getFloat("safety");
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}

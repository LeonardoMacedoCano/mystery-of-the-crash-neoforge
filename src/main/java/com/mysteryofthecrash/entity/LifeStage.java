package com.mysteryofthecrash.entity;

public enum LifeStage {

    CHILD(0,  2,  8.0f, 0.18f, false, false),
    YOUNG(3,  14, 20.0f, 0.26f, true,  true),
    ADULT(15, Integer.MAX_VALUE, 40.0f, 0.30f, true, true);

    public final int   minDays;
    public final int   maxDays;
    public final float maxHealth;
    public final float moveSpeed;
    public final boolean canMine;
    public final boolean canEquipGear;

    LifeStage(int minDays, int maxDays, float maxHealth, float moveSpeed,
              boolean canMine, boolean canEquipGear) {
        this.minDays     = minDays;
        this.maxDays     = maxDays;
        this.maxHealth   = maxHealth;
        this.moveSpeed   = moveSpeed;
        this.canMine     = canMine;
        this.canEquipGear = canEquipGear;
    }

    public static LifeStage fromDays(int days) {
        for (LifeStage stage : values()) {
            if (days >= stage.minDays && days <= stage.maxDays) return stage;
        }
        return ADULT;
    }

    public boolean isAtLeast(LifeStage other) {
        return this.ordinal() >= other.ordinal();
    }
}

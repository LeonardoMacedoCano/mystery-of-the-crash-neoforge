package com.mysteryofthecrash.entity;

public enum LifeStage {

    CHILD(0, 6, 8.0f, 0.18f),
    YOUNG(7, 20, 18.0f, 0.24f),
    ADULT(21, Integer.MAX_VALUE, 30.0f, 0.30f);

    public final int minDays;
    public final int maxDays;

    public final float maxHealth;

    public final float moveSpeed;

    LifeStage(int minDays, int maxDays, float maxHealth, float moveSpeed) {
        this.minDays = minDays;
        this.maxDays = maxDays;
        this.maxHealth = maxHealth;
        this.moveSpeed = moveSpeed;
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

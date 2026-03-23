package com.mysteryofthecrash.entity.personality;

import com.mysteryofthecrash.entity.Personality;
import net.minecraft.nbt.CompoundTag;

public class PersonalityResolver {

    public float kindnessScore    = 0f;
    public float aggressionScore  = 0f;
    public float teachScore       = 0f;
    public float proximityScore   = 0f;
    public float explorationScore = 0f;
    public float dangerScore      = 0f;

    private boolean locked = false;
    private Personality lockedPersonality = null;

    public boolean isLocked() { return locked; }

    public Personality getLockedPersonality() { return lockedPersonality; }

    public void recordKindnessEvent(float weight) {
        if (!locked) kindnessScore += weight;
    }

    public void recordAggressionEvent(float weight) {
        if (!locked) aggressionScore += weight;
    }

    public void recordTeachEvent(float weight) {
        if (!locked) teachScore += weight;
    }

    public void recordProximityTick(float weight) {
        if (!locked) proximityScore += weight;
    }

    public void recordExplorationTick(float weight) {
        if (!locked) explorationScore += weight;
    }

    public void recordDangerTick(float weight) {
        if (!locked) dangerScore += weight;
    }

    public Personality resolve() {
        if (locked) return lockedPersonality;

        float[] scores = new float[Personality.values().length];

        scores[Personality.CURIOUS.ordinal()]     = teachScore * 2f + explorationScore * 1.5f;
        scores[Personality.LOYAL.ordinal()]        = kindnessScore * 2f + proximityScore * 1.5f;
        scores[Personality.INDEPENDENT.ordinal()]  = explorationScore * 1.5f + aggressionScore * 0.5f;
        scores[Personality.CHAOTIC.ordinal()]      = aggressionScore * 1.5f + dangerScore * 1f;
        scores[Personality.LAZY.ordinal()]         = proximityScore * 0.8f
                + Math.max(0, 200f - teachScore - explorationScore) * 0.3f;
        scores[Personality.PROTECTIVE.ordinal()]   = kindnessScore * 1f + dangerScore * 2f;

        int bestIdx = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[bestIdx]) bestIdx = i;
        }

        lockedPersonality = Personality.values()[bestIdx];
        locked = true;
        return lockedPersonality;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("kindness",    kindnessScore);
        tag.putFloat("aggression",  aggressionScore);
        tag.putFloat("teach",       teachScore);
        tag.putFloat("proximity",   proximityScore);
        tag.putFloat("exploration", explorationScore);
        tag.putFloat("danger",      dangerScore);
        tag.putBoolean("locked",    locked);
        if (locked && lockedPersonality != null) {
            tag.putString("personality", lockedPersonality.name());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        kindnessScore    = tag.getFloat("kindness");
        aggressionScore  = tag.getFloat("aggression");
        teachScore       = tag.getFloat("teach");
        proximityScore   = tag.getFloat("proximity");
        explorationScore = tag.getFloat("exploration");
        dangerScore      = tag.getFloat("danger");
        locked           = tag.getBoolean("locked");
        if (locked && tag.contains("personality")) {
            try {
                lockedPersonality = Personality.valueOf(tag.getString("personality"));
            } catch (IllegalArgumentException e) {
                lockedPersonality = Personality.CURIOUS;
            }
        }
    }
}

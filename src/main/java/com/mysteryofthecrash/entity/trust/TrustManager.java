package com.mysteryofthecrash.entity.trust;

import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.nbt.CompoundTag;

public class TrustManager {

    private float trust = 0f;

    private static final float MIN = -100f;
    private static final float MAX =  100f;

    public void onPlayerTaught()       { adjust(+12f, "player taught"); }

    public void onPlayerProtected()    { adjust(+8f,  "player protected"); }

    public void onPlayerFed()          { adjust(+6f,  "player fed"); }

    public void onPlayerNearbyRest()   { adjust(+0.5f,"player nearby rest"); }

    public void onPlayerAttacked()     { adjust(-20f, "player attacked"); }

    public void onPlayerAbandoned()    { adjust(-10f, "player abandoned"); }

    public void onForcedTask()         { adjust(-5f,  "forced task"); }

    public void onDangerExposure()     { adjust(-8f,  "danger exposure"); }

    public float getTrust() { return trust; }

    public float getObedienceChance() {
        return (trust + MAX) / (MAX * 2f);
    }

    public boolean willAssistAutonomously() {
        return trust >= 40f;
    }

    public boolean isAvoidant() {
        return trust <= -40f;
    }

    public float preferredProximity() {
        return 4f + (1f - getObedienceChance()) * 20f;
    }

    private void adjust(float delta, String reason) {
        float before = trust;
        trust = Math.max(MIN, Math.min(MAX, trust + delta));
        MysteryOfTheCrash.LOGGER.debug("[TrustManager] {} | {} → {}", reason, before, trust);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("trust", trust);
        return tag;
    }

    public void load(CompoundTag tag) {
        float raw = tag.getFloat("trust");
        trust = (Float.isNaN(raw) || Float.isInfinite(raw)) ? 0f
                : Math.max(MIN, Math.min(MAX, raw));
    }
}

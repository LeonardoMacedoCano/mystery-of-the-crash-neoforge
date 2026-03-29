package com.mysteryofthecrash.entity.trust;

import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrustManager {

    private final Map<UUID, Float> trustMap = new HashMap<>();

    private static final float MIN = -100f;
    private static final float MAX =  100f;

    public void onPlayerTaught(UUID id)     { adjust(id, +12f, "taught"); }
    public void onPlayerProtected(UUID id)  { adjust(id, +8f,  "protected"); }
    public void onPlayerFed(UUID id)        { adjust(id, +6f,  "fed"); }
    public void onPlayerNearbyRest(UUID id) { adjust(id, +0.5f,"nearby rest"); }
    public void onPlayerAttacked(UUID id)   { adjust(id, -20f, "attacked"); }
    public void onPlayerAbandoned(UUID id)  { adjust(id, -10f, "abandoned"); }
    public void onForcedTask(UUID id)       { adjust(id, -5f,  "forced task"); }

    public void onDangerExposure() {
        for (UUID id : trustMap.keySet()) adjust(id, -8f, "danger exposure");
    }

    public float getTrust(UUID id) {
        return trustMap.getOrDefault(id, 0f);
    }

    public float getHighestTrust() {
        return trustMap.values().stream().max(Float::compare).orElse(0f);
    }

    public float getObedienceChance(UUID id) {
        return (getTrust(id) + MAX) / (MAX * 2f);
    }

    public boolean willAssistAutonomously(UUID id) {
        return getTrust(id) >= 40f;
    }

    public boolean isAvoidant(UUID id) {
        return getTrust(id) <= -40f;
    }

    public boolean isAvoidantToAll() {
        if (trustMap.isEmpty()) return false;
        return trustMap.values().stream().allMatch(t -> t <= -40f);
    }

    public float preferredProximity(UUID id) {
        return 4f + (1f - getObedienceChance(id)) * 20f;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Float> entry : trustMap.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putString("uuid",  entry.getKey().toString());
            e.putFloat("trust",  entry.getValue());
            list.add(e);
        }
        tag.put("entries", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        trustMap.clear();
        if (tag.contains("entries", Tag.TAG_LIST)) {
            ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                try {
                    UUID  id  = UUID.fromString(e.getString("uuid"));
                    float raw = e.getFloat("trust");
                    if (!Float.isNaN(raw) && !Float.isInfinite(raw)) {
                        trustMap.put(id, Math.max(MIN, Math.min(MAX, raw)));
                    }
                } catch (IllegalArgumentException ignored) { }
            }
        } else if (tag.contains("trust")) {

        }
    }

    private void adjust(UUID id, float delta, String reason) {
        float before = trustMap.getOrDefault(id, 0f);
        float after  = Math.max(MIN, Math.min(MAX, before + delta));
        trustMap.put(id, after);
        MysteryOfTheCrash.LOGGER.debug("[Trust] {} | {} {} → {}", reason, id, before, after);
    }
}

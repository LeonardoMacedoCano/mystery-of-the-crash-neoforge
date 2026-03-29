package com.mysteryofthecrash.entity.learning;

import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MiningKnowledge {

    private final Map<MineableBlock, Float> proficiency = new EnumMap<>(MineableBlock.class);

    public float getProficiency(MineableBlock block) {
        return proficiency.getOrDefault(block, 0f);
    }

    public boolean isKnown(MineableBlock block) {
        return getProficiency(block) >= block.proficiencyToUnlock;
    }

    public List<MineableBlock> getKnownBlocks() {
        return Arrays.stream(MineableBlock.values())
                .filter(this::isKnown)
                .collect(Collectors.toList());
    }

    public boolean hasAnyKnowledge() {
        return Arrays.stream(MineableBlock.values()).anyMatch(this::isKnown);
    }

    public void addProficiency(MineableBlock block, float amount) {
        float before  = proficiency.getOrDefault(block, 0f);
        float updated = Math.min(100f, before + amount);
        proficiency.put(block, updated);

        if (before < block.proficiencyToUnlock && updated >= block.proficiencyToUnlock) {
            MysteryOfTheCrash.LOGGER.info("[MiningKnowledge] Unlocked: {} (proficiency: {})",
                    block.id, String.format("%.1f", updated));
        }
    }

    public void addProficiencyOnMine(MineableBlock block) {
        addProficiency(block, 1.5f);
    }

    public float getOverallProficiency() {
        if (proficiency.isEmpty()) return 0f;
        float sum = 0f;
        for (float v : proficiency.values()) sum += v;
        return sum / MineableBlock.values().length;
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        CompoundTag profTag = new CompoundTag();
        for (Map.Entry<MineableBlock, Float> e : proficiency.entrySet()) {
            profTag.putFloat(e.getKey().id, e.getValue());
        }
        root.put("proficiency", profTag);
        return root;
    }

    public void load(CompoundTag tag) {
        proficiency.clear();
        if (!tag.contains("proficiency")) return;
        CompoundTag profTag = tag.getCompound("proficiency");
        for (MineableBlock block : MineableBlock.values()) {
            if (profTag.contains(block.id)) {
                proficiency.put(block, profTag.getFloat(block.id));
            }
        }
    }
}

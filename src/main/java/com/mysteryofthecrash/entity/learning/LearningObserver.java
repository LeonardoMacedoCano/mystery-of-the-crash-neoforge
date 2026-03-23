package com.mysteryofthecrash.entity.learning;

import com.mysteryofthecrash.entity.AlienEntity;
import com.mysteryofthecrash.entity.KnowledgeFlags;
import com.mysteryofthecrash.entity.LifeStage;
import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

public class LearningObserver {

    private float miningProgress    = 0f;
    private float ironProgress      = 0f;
    private float storageProgress   = 0f;
    private float plantingProgress  = 0f;
    private float toolProgress      = 0f;

    public float miningSkill    = 0f;
    public float farmingSkill   = 0f;
    public float logisticsSkill = 0f;
    public float curiosityLevel = 50f;

    private static final float OBSERVATIONS_TO_LEARN = 10f;

    public void onPlayerBreakBlock(AlienEntity alien, Block block, Player player, BlockPos pos) {
        if (!isObserving(alien, player)) return;
        float mult = getMultiplier(alien.getLifeStage());

        addProgress(alien, "mining", mult * 0.1f, KnowledgeFlags.KNOWS_MINING,
                () -> miningProgress, v -> miningProgress = v);
        miningSkill = Math.min(100f, miningSkill + mult * 0.5f);

        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.RAW_IRON_BLOCK || block == Blocks.IRON_BLOCK) {
            addProgress(alien, "iron", mult * 0.2f, KnowledgeFlags.KNOWS_IRON,
                    () -> ironProgress, v -> ironProgress = v);
        }

        alien.getPersonalityResolver().recordTeachEvent(0.5f * mult);
    }

    public void onPlayerFarm(AlienEntity alien, Block block, Player player) {
        if (!isObserving(alien, player)) return;
        float mult = getMultiplier(alien.getLifeStage());

        if (block instanceof CropBlock || block == Blocks.FARMLAND
                || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            addProgress(alien, "planting", mult * 0.15f, KnowledgeFlags.KNOWS_PLANTING,
                    () -> plantingProgress, v -> plantingProgress = v);
            farmingSkill = Math.min(100f, farmingSkill + mult * 0.5f);
        }

        alien.getPersonalityResolver().recordTeachEvent(0.4f * mult);
    }

    public void onPlayerAccessStorage(AlienEntity alien, Player player) {
        if (!isObserving(alien, player)) return;
        float mult = getMultiplier(alien.getLifeStage());

        addProgress(alien, "storage", mult * 0.12f, KnowledgeFlags.KNOWS_STORAGE,
                () -> storageProgress, v -> storageProgress = v);
        logisticsSkill = Math.min(100f, logisticsSkill + mult * 0.3f);

        alien.getPersonalityResolver().recordTeachEvent(0.3f * mult);
    }

    public void onPlayerCraft(AlienEntity alien, Player player, RecipeHolder<?> recipe) {
        if (!isObserving(alien, player)) return;
        float mult = getMultiplier(alien.getLifeStage());

        addProgress(alien, "tool", mult * 0.08f, KnowledgeFlags.KNOWS_TOOL_USAGE,
                () -> toolProgress, v -> toolProgress = v);
        curiosityLevel = Math.min(100f, curiosityLevel + mult * 0.2f);

        alien.getPersonalityResolver().recordTeachEvent(0.6f * mult);
    }

    public void onExperiment(AlienEntity alien) {
        float mult = getMultiplier(alien.getLifeStage()) * 0.3f;

        if (!alien.hasKnowledge(KnowledgeFlags.KNOWS_MINING)) {
            addProgress(alien, "mining-experiment", mult * 0.05f, KnowledgeFlags.KNOWS_MINING,
                    () -> miningProgress, v -> miningProgress = v);
        }
        curiosityLevel = Math.min(100f, curiosityLevel + mult * 0.1f);
    }

    private boolean isObserving(AlienEntity alien, Player player) {
        return alien.distanceTo(player) <= 12.0
                && !alien.isCurrentGoalResting()
                && alien.getLifeStage() != null;
    }

    private float getMultiplier(LifeStage stage) {
        return switch (stage) {
            case CHILD -> 3.0f;
            case YOUNG -> 1.0f;
            case ADULT -> 0.3f;
        };
    }

    @FunctionalInterface interface FloatGetter { float get(); }
    @FunctionalInterface interface FloatSetter { void set(float v); }

    private void addProgress(AlienEntity alien, String name, float amount,
                             KnowledgeFlags flag, FloatGetter getter, FloatSetter setter) {
        if (alien.hasKnowledge(flag)) return;

        float progress = getter.get() + amount;
        setter.set(Math.min(1f, progress));

        if (progress >= 1f) {
            alien.learnKnowledge(flag);
            MysteryOfTheCrash.LOGGER.info("[Alien] Learned {} → {}", name, flag);
            alien.getTelepathicChat().sendRandomMessage(alien,
                    alien.getLifeStage(), alien.getPersonality());
        }
    }

    public net.minecraft.nbt.CompoundTag save() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putFloat("miningProgress",   miningProgress);
        tag.putFloat("ironProgress",     ironProgress);
        tag.putFloat("storageProgress",  storageProgress);
        tag.putFloat("plantingProgress", plantingProgress);
        tag.putFloat("toolProgress",     toolProgress);
        tag.putFloat("miningSkill",      miningSkill);
        tag.putFloat("farmingSkill",     farmingSkill);
        tag.putFloat("logisticsSkill",   logisticsSkill);
        tag.putFloat("curiosityLevel",   curiosityLevel);
        return tag;
    }

    public void load(net.minecraft.nbt.CompoundTag tag) {
        miningProgress   = tag.getFloat("miningProgress");
        ironProgress     = tag.getFloat("ironProgress");
        storageProgress  = tag.getFloat("storageProgress");
        plantingProgress = tag.getFloat("plantingProgress");
        toolProgress     = tag.getFloat("toolProgress");
        miningSkill      = tag.getFloat("miningSkill");
        farmingSkill     = tag.getFloat("farmingSkill");
        logisticsSkill   = tag.getFloat("logisticsSkill");
        curiosityLevel   = tag.getFloat("curiosityLevel");
    }
}

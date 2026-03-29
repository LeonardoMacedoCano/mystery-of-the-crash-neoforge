package com.mysteryofthecrash.entity.learning;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum MineableBlock {

    STONE        ("stone",          "Stone",          10f,  -64,  320,  Blocks.STONE, Blocks.COBBLESTONE),
    DIRT         ("dirt",           "Dirt",            5f,    0,  320,  Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.ROOTED_DIRT),
    GRAVEL       ("gravel",         "Gravel",          5f,  -64,  320,  Blocks.GRAVEL),
    SAND         ("sand",           "Sand",            5f,    0,  320,  Blocks.SAND, Blocks.SANDSTONE),
    COAL_ORE     ("coal_ore",       "Coal Ore",       15f,  -64,  192,  Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
    IRON_ORE     ("iron_ore",       "Iron Ore",       20f,  -64,   80,  Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
    COPPER_ORE   ("copper_ore",     "Copper Ore",     20f,   -8,   96,  Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE),
    GOLD_ORE     ("gold_ore",       "Gold Ore",       35f,  -64,   32,  Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE),
    LAPIS_ORE    ("lapis_ore",      "Lapis Ore",      40f,  -64,   64,  Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE),
    REDSTONE_ORE ("redstone_ore",   "Redstone Ore",   40f,  -64,   16,  Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
    DIAMOND_ORE  ("diamond_ore",    "Diamond Ore",    60f,  -64,   16,  Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
    EMERALD_ORE  ("emerald_ore",    "Emerald Ore",    70f,  -64,  320,  Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE),
    ANCIENT_DEBRIS("ancient_debris","Ancient Debris", 90f,    8,  128,  Blocks.ANCIENT_DEBRIS);

    public final String id;
    public final String displayName;
    public final float  proficiencyToUnlock;
    public final int    searchMinY;
    public final int    searchMaxY;
    public final List<Block> variants;

    MineableBlock(String id, String displayName, float unlock, int minY, int maxY, Block... blocks) {
        this.id                  = id;
        this.displayName         = displayName;
        this.proficiencyToUnlock = unlock;
        this.searchMinY          = minY;
        this.searchMaxY          = maxY;
        this.variants            = List.of(blocks);
    }

    public boolean matches(Block block) {
        return variants.contains(block);
    }

    public static Optional<MineableBlock> fromId(String id) {
        return Arrays.stream(values())
                .filter(b -> b.id.equalsIgnoreCase(id))
                .findFirst();
    }

    public static Optional<MineableBlock> fromBlock(Block block) {
        return Arrays.stream(values())
                .filter(b -> b.matches(block))
                .findFirst();
    }
}

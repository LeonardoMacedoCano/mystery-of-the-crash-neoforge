package com.mysteryofthecrash.registry;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.block.AlienDebrisBlock;
import com.mysteryofthecrash.block.SpaceshipCoreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MysteryOfTheCrash.MOD_ID);

    public static final DeferredBlock<AlienDebrisBlock> ALIEN_DEBRIS =
            BLOCKS.register("alien_debris", () -> new AlienDebrisBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(4.0f, 8.0f)
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 4)
                            .requiresCorrectToolForDrops()));

    public static final DeferredBlock<SpaceshipCoreBlock> SPACESHIP_CORE =
            BLOCKS.register("spaceship_core", () -> new SpaceshipCoreBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .strength(6.0f, 12.0f)
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 10)
                            .requiresCorrectToolForDrops()));
}

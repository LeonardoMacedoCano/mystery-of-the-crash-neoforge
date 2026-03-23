package com.mysteryofthecrash.registry;

import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MysteryOfTheCrash.MOD_ID);

    public static final DeferredItem<BlockItem> ALIEN_DEBRIS =
            ITEMS.registerSimpleBlockItem("alien_debris", ModBlocks.ALIEN_DEBRIS);

    public static final DeferredItem<BlockItem> SPACESHIP_CORE =
            ITEMS.registerSimpleBlockItem("spaceship_core", ModBlocks.SPACESHIP_CORE);
}

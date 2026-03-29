package com.mysteryofthecrash;

import com.mysteryofthecrash.event.ModEvents;
import com.mysteryofthecrash.registry.ModBlocks;
import com.mysteryofthecrash.registry.ModEntities;
import com.mysteryofthecrash.registry.ModItems;
import com.mysteryofthecrash.registry.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MysteryOfTheCrash.MOD_ID)
public class MysteryOfTheCrash {

    public static final String MOD_ID = "mysteryofthecrash";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public MysteryOfTheCrash(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModEvents::registerAttributes);

        NeoForge.EVENT_BUS.register(ModEvents.class);

        LOGGER.info("[MysteryOfTheCrash] Mod initializing — the alien is out there, somewhere.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[MysteryOfTheCrash] Common setup complete.");
    }
}

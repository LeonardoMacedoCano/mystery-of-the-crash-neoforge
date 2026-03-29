package com.mysteryofthecrash.client;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.registry.ModEntities;
import com.mysteryofthecrash.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MysteryOfTheCrash.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ALIEN_INVENTORY.get(), AlienInventoryScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AlienModelLayer.ALIEN_LAYER,       AlienModel::createBodyLayer);

        event.registerLayerDefinition(AlienModelLayer.ALIEN_INNER_ARMOR, AlienModel::createInnerArmorLayer);

        event.registerLayerDefinition(AlienModelLayer.ALIEN_OUTER_ARMOR, AlienModel::createOuterArmorLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ALIEN.get(), AlienRenderer::new);
    }
}

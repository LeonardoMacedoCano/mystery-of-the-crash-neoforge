package com.mysteryofthecrash.client;

import com.mysteryofthecrash.MysteryOfTheCrash;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class AlienModelLayer {

    public static final ModelLayerLocation ALIEN_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MysteryOfTheCrash.MOD_ID, "alien"), "main");
}

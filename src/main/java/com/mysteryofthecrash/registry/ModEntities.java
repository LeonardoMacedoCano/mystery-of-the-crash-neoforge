package com.mysteryofthecrash.registry;

import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MysteryOfTheCrash.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<AlienEntity>> ALIEN =
            ENTITY_TYPES.register("alien", () ->
                    EntityType.Builder.<AlienEntity>of(AlienEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.4f)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build(ResourceLocation.fromNamespaceAndPath(
                                    MysteryOfTheCrash.MOD_ID, "alien").toString()));
}

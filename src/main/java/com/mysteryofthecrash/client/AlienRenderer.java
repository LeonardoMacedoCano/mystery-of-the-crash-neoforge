package com.mysteryofthecrash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mysteryofthecrash.MysteryOfTheCrash;
import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class AlienRenderer extends MobRenderer<AlienEntity, AlienModel<AlienEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MysteryOfTheCrash.MOD_ID,
                    "textures/entity/alien.png");

    public AlienRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienModel<>(context.bakeLayer(AlienModelLayer.ALIEN_LAYER)), 0.5f);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new AlienModel<>(context.bakeLayer(AlienModelLayer.ALIEN_INNER_ARMOR)),
                new AlienModel<>(context.bakeLayer(AlienModelLayer.ALIEN_OUTER_ARMOR)),
                context.getModelManager()));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AlienEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(AlienEntity entity, PoseStack poseStack, float partialTick) {
        float scale = switch (entity.getLifeStage()) {
            case CHILD -> 0.55f;
            case YOUNG -> 1.00f;
            case ADULT -> 1.10f;
        };
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public void render(AlienEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}

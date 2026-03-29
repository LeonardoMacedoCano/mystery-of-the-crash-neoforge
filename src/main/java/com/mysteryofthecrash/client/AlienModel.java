package com.mysteryofthecrash.client;

import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class AlienModel<T extends AlienEntity> extends HumanoidModel<T> {

    public AlienModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4f, -8f, -4f, 8, 8, 8),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("hat",
                CubeListBuilder.create(),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4f, 0f, -2f, 8, 12, 4),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3f, -2f, -2f, 4, 12, 4),
                PartPose.offset(-5f, 2f, 0f));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1f, -2f, -2f, 4, 12, 4),
                PartPose.offset(5f, 2f, 0f));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2f, 0f, -2f, 4, 12, 4),
                PartPose.offset(-1.9f, 12f, 0f));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-2f, 0f, -2f, 4, 12, 4),
                PartPose.offset(1.9f, 12f, 0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createInnerArmorLayer() {
        return buildArmorLayer(new CubeDeformation(0.5f));
    }

    public static LayerDefinition createOuterArmorLayer() {
        return buildArmorLayer(new CubeDeformation(1.0f));
    }

    private static LayerDefinition buildArmorLayer(CubeDeformation def) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4f, -8f, -4f, 8, 8, 8, def),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("hat",
                CubeListBuilder.create(),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4f, 0f, -2f, 8, 12, 4, def),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3f, -2f, -2f, 4, 12, 4, def),
                PartPose.offset(-5f, 2f, 0f));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1f, -2f, -2f, 4, 12, 4, def),
                PartPose.offset(5f, 2f, 0f));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2f, 0f, -2f, 4, 12, 4, def),
                PartPose.offset(-1.9f, 12f, 0f));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-2f, 0f, -2f, 4, 12, 4, def),
                PartPose.offset(1.9f, 12f, 0f));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        head.y = (float)(Math.sin(ageInTicks * 0.05f) * 0.5);
    }
}

package com.mysteryofthecrash.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mysteryofthecrash.entity.AlienEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class AlienModel<T extends AlienEntity> extends EntityModel<T> {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public AlienModel(ModelPart root) {
        this.head     = root.getChild("head");
        this.body     = root.getChild("body");
        this.leftArm  = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg  = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4f, -10f, -4f, 8, 10, 8),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-3f, 0f, -2f, 6, 10, 4),
                PartPose.offset(0f, 0f, 0f));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-1f, -1f, -1f, 2, 12, 2),
                PartPose.offset(4f, 1f, 0f));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .mirror()
                        .addBox(-1f, -1f, -1f, 2, 12, 2),
                PartPose.offset(-4f, 1f, 0f));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-1.5f, 0f, -1.5f, 3, 8, 3),
                PartPose.offset(2f, 10f, 0f));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .mirror()
                        .addBox(-1.5f, 0f, -1.5f, 3, 8, 3),
                PartPose.offset(-2f, 10f, 0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot  = netHeadYaw  * ((float) Math.PI / 180f);
        head.xRot  = headPitch   * ((float) Math.PI / 180f);

        float swingCos = (float) Math.cos(limbSwing * 0.6662f);
        rightArm.xRot =  swingCos        * limbSwingAmount;
        leftArm.xRot  = -swingCos        * limbSwingAmount;
        rightLeg.xRot = -swingCos * 0.8f * limbSwingAmount;
        leftLeg.xRot  =  swingCos * 0.8f * limbSwingAmount;

        head.y = (float)(Math.sin(ageInTicks * 0.05f) * 0.5);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay, int color) {
        head.render(poseStack,     buffer, packedLight, packedOverlay, color);
        body.render(poseStack,     buffer, packedLight, packedOverlay, color);
        leftArm.render(poseStack,  buffer, packedLight, packedOverlay, color);
        rightArm.render(poseStack, buffer, packedLight, packedOverlay, color);
        leftLeg.render(poseStack,  buffer, packedLight, packedOverlay, color);
        rightLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}

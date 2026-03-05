package com.akah.blocklings.client.renderer.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.akah.blocklings.Blocklings;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.BlocklingHand;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * The model for the blockling.
 */
@OnlyIn(Dist.CLIENT)
public class BlocklingModel extends EntityModel<BlocklingEntity> implements ArmedModel
{
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling"), "main");

    public static final float BODY_BASE_ROT_X = 0.0872665f;
    public static final float RIGHT_LEG_BASE_ROT_X = -BODY_BASE_ROT_X;
    public static final float LEFT_LEG_BASE_ROT_X = -BODY_BASE_ROT_X;
    public static final float RIGHT_ARM_BASE_ROT_X = 0.785398f - BODY_BASE_ROT_X;
    public static final float LEFT_ARM_BASE_ROT_X = 0.785398f - BODY_BASE_ROT_X;

    private final ModelPart body;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    public BlocklingModel(@Nonnull ModelPart root)
    {
        body = root.getChild("body");
        rightLeg = body.getChild("right_leg");
        leftLeg = body.getChild("left_leg");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 0).addBox(-6.0f, -3.0f, -6.0f, 12.0f, 12.0f, 12.0f), PartPose.offsetAndRotation(0.0f, 13.0f, 0.0f, BODY_BASE_ROT_X, 0.0f, 0.0f));

        body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 24).addBox(-1.5f, 1.0f, -3.5f, 5.0f, 6.0f, 6.0f), PartPose.offsetAndRotation(-4.0f, 4.0f, 0.5f, -RIGHT_LEG_BASE_ROT_X, 0.0f, 0.0f));
        body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(42, 24).addBox(-3.5f, 1.0f, -3.5f, 5.0f, 6.0f, 6.0f), PartPose.offsetAndRotation(4.0f, 4.0f, 0.5f, -LEFT_LEG_BASE_ROT_X, 0.0f, 0.0f));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 12).addBox(0.0f, 0.0f, -7.0f, 2.0f, 6.0f, 6.0f), PartPose.offsetAndRotation(-8.0f, 0.0f, 0.0f, RIGHT_ARM_BASE_ROT_X, 0.0f, 0.0f));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(64, 12).addBox(-2.0f, 0.0f, -7.0f, 2.0f, 6.0f, 6.0f), PartPose.offsetAndRotation(8.0f, 0.0f, 0.0f, LEFT_ARM_BASE_ROT_X, 0.0f, 0.0f));
        body.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(22, 8).addBox(-1.0f, -0.2f, 1.5f, 2.0f, 3.0f, 1.0f), PartPose.offset(-2.0f, 3.0f, -8.0f));
        body.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(52, 8).addBox(-1.0f, -0.2f, 1.5f, 2.0f, 3.0f, 1.0f), PartPose.offset(2.0f, 3.0f, -8.0f));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(@Nonnull BlocklingEntity blockling, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch)
    {
        EntityDimensions size = blockling.getDimensions(Pose.STANDING);
        scaleX = size.width;
        scaleY = size.height;

        float partialTicks = ageInTicks % 1.0f;

        limbSwing *= 0.5f;
        limbSwing += ageInTicks / 20.0f;

        float rightLegSwingAmount = limbSwingAmount;
        float leftLegSwingAmount = limbSwingAmount;

        if (limbSwingAmount > 1.0f)
        {
            limbSwingAmount = 1.0f;
        }
        limbSwingAmount += 0.1f;

        float rightArmSwingAmount = limbSwingAmount;
        float leftArmSwingAmount = limbSwingAmount;

        float bodySwing = 0.0f;
        float rightArmSwing = 0.0f;
        float leftArmSwing = 0.0f;
        float rightLegSwing = 0.0f;
        float leftLegSwing = 0.0f;

        float weaponBonusRotX = 0.7f;

        BlocklingHand hand = blockling.getStats().hand.getValue();
        BlocklingHand attackingHand = blockling.getEquipment().findAttackingHand();

        if (blockling.getTarget() != null)
        {
            if (attackingHand == BlocklingHand.MAIN || attackingHand == BlocklingHand.BOTH)
            {
                rightArmSwing -= blockling.getEquipment().getHandStack(InteractionHand.MAIN_HAND).isEmpty() ? 0.0f : weaponBonusRotX;
                rightArmSwingAmount /= 2.0f;
            }

            if (attackingHand == BlocklingHand.OFF || attackingHand == BlocklingHand.BOTH)
            {
                leftArmSwing += blockling.getEquipment().getHandStack(InteractionHand.OFF_HAND).isEmpty() ? 0.0f : weaponBonusRotX;
                leftArmSwingAmount /= 2.0f;
            }
        }

        if (blockling.getActions().attack.isRunning(BlocklingHand.MAIN))
        {
            float percent = blockling.getActions().attack.percentThroughHandAction(-1) + (blockling.getActions().attack.percentThroughHandAction() - blockling.getActions().attack.percentThroughHandAction(-1)) * partialTicks;
            float attackSwing = Mth.cos(percent * (float) Math.PI / 2.0f) * 2.0f;
            rightArmSwing += blockling.getEquipment().getHandStack(InteractionHand.MAIN_HAND).isEmpty() ? -attackSwing : attackSwing;
        }

        if (blockling.getActions().attack.isRunning(BlocklingHand.OFF))
        {
            float percent = blockling.getActions().attack.percentThroughHandAction(-1) + (blockling.getActions().attack.percentThroughHandAction() - blockling.getActions().attack.percentThroughHandAction(-1)) * partialTicks;
            float attackSwing = Mth.cos(percent * (float) Math.PI / 2.0f) * 2.0f;
            leftArmSwing -= blockling.getEquipment().getHandStack(InteractionHand.OFF_HAND).isEmpty() ? -attackSwing : attackSwing;
        }

        if (blockling.getActions().gather.isRunning())
        {
            if (hand == BlocklingHand.MAIN || hand == BlocklingHand.BOTH)
            {
                rightArmSwing = Mth.cos(ageInTicks + (float) Math.PI) * 1.0f;
            }

            if (hand == BlocklingHand.OFF || hand == BlocklingHand.BOTH)
            {
                leftArmSwing = Mth.cos(ageInTicks + (float) Math.PI) * 1.0f;
            }
        }

        bodySwing += Mth.cos(limbSwing + (float) Math.PI) * limbSwingAmount * 0.1f;
        rightArmSwing += Mth.cos(limbSwing + (float) Math.PI) * rightArmSwingAmount * 0.8f;
        leftArmSwing += Mth.cos(limbSwing + (float) Math.PI) * leftArmSwingAmount * 0.8f;
        rightLegSwing += Mth.cos(limbSwing + (float) Math.PI) * rightLegSwingAmount * 0.5f;
        leftLegSwing += Mth.cos(limbSwing + (float) Math.PI) * leftLegSwingAmount * 0.5f;

        rightArm.xRot = rightArmSwing + RIGHT_ARM_BASE_ROT_X;
        leftArm.xRot = LEFT_ARM_BASE_ROT_X - leftArmSwing;
        rightLeg.xRot = RIGHT_LEG_BASE_ROT_X - rightLegSwing;
        leftLeg.xRot = leftLegSwing + LEFT_LEG_BASE_ROT_X;

        body.zRot = bodySwing;
        rightLeg.zRot = -body.zRot;
        leftLeg.zRot = -body.zRot;
    }

    @Override
    public void renderToBuffer(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a)
    {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.501, 0.0);
        poseStack.scale(scaleX, scaleY, scaleX);
        poseStack.translate(0.0, -1.501, 0.0);

        body.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);

        poseStack.popPose();
    }

    @Override
    public void translateToHand(@Nonnull HumanoidArm hand, @Nonnull PoseStack poseStack)
    {
        body.translateAndRotate(poseStack);

        if (hand == HumanoidArm.LEFT)
        {
            leftArm.translateAndRotate(poseStack);
        }
        else
        {
            rightArm.translateAndRotate(poseStack);
        }
    }
}

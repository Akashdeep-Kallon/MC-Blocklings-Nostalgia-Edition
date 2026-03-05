package com.akah.blocklings.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.akah.blocklings.client.renderer.entity.BlocklingRenderer;
import com.akah.blocklings.client.renderer.entity.model.BlocklingModel;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.util.ToolUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * The renderer for the items held by a blockling.
 */
@OnlyIn(Dist.CLIENT)
public class BlocklingHeldItemLayer extends RenderLayer<BlocklingEntity, BlocklingModel>
{
    public BlocklingHeldItemLayer(@Nonnull BlocklingRenderer blocklingRenderer)
    {
        super(blocklingRenderer);
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int packedLight, @Nonnull BlocklingEntity blockling, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        ItemStack mainStack = blockling.getMainHandItem();
        ItemStack offStack = blockling.getOffhandItem();

        if (!mainStack.isEmpty())
        {
            renderItem(poseStack, mainStack, false, blockling, buffer, packedLight);
        }

        if (!offStack.isEmpty())
        {
            renderItem(poseStack, offStack, true, blockling, buffer, packedLight);
        }
    }

    private void renderItem(@Nonnull PoseStack poseStack, @Nonnull ItemStack stack, boolean isLeftHand, @Nonnull BlocklingEntity blockling, @Nonnull MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.501, 0.0);
        poseStack.scale(blockling.getScale(), blockling.getScale(), blockling.getScale());
        poseStack.translate(0.0, -1.501, 0.0);
        getParentModel().translateToHand(isLeftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT, poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(190.0f));
        poseStack.translate((isLeftHand ? 1.0f : -1.0f) / 16.0f, -0.1f, getItemHandDisplacement(stack));

        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(blockling, stack, isLeftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, isLeftHand, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    private float getItemHandDisplacement(@Nonnull ItemStack stack)
    {
        if (ToolUtil.isWeapon(stack))
        {
            return -0.3044f;
        }

        return -0.3552f;
    }
}

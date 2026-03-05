package com.akah.blocklings.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.renderer.entity.layer.BlocklingHeldItemLayer;
import com.akah.blocklings.client.renderer.entity.model.BlocklingModel;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * The renderer for the blockling.
 */
public class BlocklingRenderer extends MobRenderer<BlocklingEntity, BlocklingModel>
{
    public BlocklingRenderer(@Nonnull EntityRendererProvider.Context context)
    {
        super(context, new BlocklingModel(context.bakeLayer(BlocklingModel.LAYER_LOCATION)), 1.0f);

        addLayer(new BlocklingHeldItemLayer(this));
    }

    @Override
    public void render(@Nonnull BlocklingEntity blockling, float yaw, float partialTicks, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int packedLight)
    {
        shadowRadius = blockling.getScale() * 0.5f;

        super.render(blockling, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    @Nonnull
    public ResourceLocation getTextureLocation(@Nonnull BlocklingEntity blockling)
    {
        return blockling.getBlocklingType() == blockling.getNaturalBlocklingType() ? blockling.getBlocklingType().entityTexture : blockling.getNaturalBlocklingType().getCombinedTexture(blockling.getBlocklingType(), blockling.getBlocklingTypeVariant());
    }
}

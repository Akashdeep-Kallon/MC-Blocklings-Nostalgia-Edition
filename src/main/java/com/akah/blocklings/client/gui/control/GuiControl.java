package com.akah.blocklings.client.gui.control;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.texture.Texture;
import com.akah.blocklings.client.gui.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * A facade around common GUI rendering operations.
 */
@OnlyIn(Dist.CLIENT)
public abstract class GuiControl
{
    public void renderRectangle(@Nonnull PoseStack matrixStack, double x, double y, int width, int height, int colour)
    {
        GuiGraphics gg = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        gg.pose().mulPoseMatrix(matrixStack.last().pose());
        gg.fill((int) Math.round(x), (int) Math.round(y), (int) Math.round(x + width), (int) Math.round(y + height), colour);
        gg.flush();
        RenderSystem.enableBlend();
    }

    public void renderCenteredRectangle(@Nonnull PoseStack matrixStack, double x, double y, double width, double height, int colour)
    {
        renderRectangle(matrixStack, x - width / 2.0, y - height / 2.0, (int) Math.round(width), (int) Math.round(height), colour);
    }

    protected void renderTexture(@Nonnull PoseStack matrixStack, @Nonnull Texture texture)
    {
        GuiUtil.get().bindTexture(texture.resourceLocation);
        GuiGraphics gg = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        gg.pose().mulPoseMatrix(matrixStack.last().pose());
        gg.blit(texture.resourceLocation, 0, 0, texture.x, texture.y, texture.width, texture.height);
        gg.flush();
    }

    protected void renderTexture(@Nonnull PoseStack matrixStack, @Nonnull Texture texture, double x, double y, double scaleX, double scaleY)
    {
        matrixStack.pushPose();
        matrixStack.translate((int) Math.round(x), (int) Math.round(y), 0.0);
        matrixStack.scale((float) scaleX, (float) scaleY, 1.0f);
        renderTexture(matrixStack, texture);
        matrixStack.popPose();
    }

    protected void renderShadowedText(@Nonnull PoseStack matrixStack, @Nonnull FormattedCharSequence text, int colour)
    {
        GuiUtil.get().renderShadowedText(matrixStack, text, 0, 0, colour);
    }

    protected void renderShadowedText(@Nonnull PoseStack matrixStack, @Nonnull FormattedCharSequence text, int x, int y, int colour)
    {
        GuiUtil.get().renderShadowedText(matrixStack, text, x, y, colour);
    }

    protected void renderText(@Nonnull PoseStack matrixStack, @Nonnull FormattedCharSequence text, int colour)
    {
        GuiUtil.get().renderText(matrixStack, text, 0, 0, colour);
    }

    public void renderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, double pixelScaleX, double pixelScaleY, @Nonnull Component tooltip)
    {
        List<FormattedCharSequence> tooltip2 = new ArrayList<>();
        tooltip2.add(tooltip.getVisualOrderText());
        renderTooltip(matrixStack, mouseX, mouseY, pixelScaleX, pixelScaleY, tooltip2);
    }

    public void renderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, double pixelScaleX, double pixelScaleY, @Nonnull List<FormattedCharSequence> tooltip)
    {
        GuiGraphics gg = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
        gg.pose().mulPoseMatrix(matrixStack.last().pose());
        gg.renderTooltip(Minecraft.getInstance().font, tooltip, (int) (mouseX / pixelScaleX), (int) (mouseY / pixelScaleY));
        gg.flush();
    }
}

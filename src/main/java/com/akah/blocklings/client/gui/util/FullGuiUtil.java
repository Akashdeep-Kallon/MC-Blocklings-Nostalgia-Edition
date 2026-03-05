package com.akah.blocklings.client.gui.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.texture.Texture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class FullGuiUtil extends GuiUtil
{
    @Nonnull
    private static final Minecraft mc = Minecraft.getInstance();

    @Override
    public float getGuiScale()
    {
        return (float) mc.getWindow().getGuiScale();
    }

    @Override
    public float getMaxGuiScale()
    {
        return (float) mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
    }

    @Override
    public int getPixelMouseX()
    {
        return (int) mc.mouseHandler.xpos();
    }

    @Override
    public int getPixelMouseY()
    {
        return (int) mc.mouseHandler.ypos();
    }

    @Override
    public boolean isKeyDown(int key)
    {
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), key);
    }

    @Override
    public boolean isKeyDown(@Nonnull KeyMapping key)
    {
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getKey().getValue());
    }

    @Override
    public boolean isControlKeyDown()
    {
        return Screen.hasControlDown();
    }

    @Override
    public boolean isCrouchKeyDown()
    {
        return isKeyDown(mc.options.keyShift);
    }

    @Override
    public boolean isCloseKey(int key)
    {
        return key == GLFW.GLFW_KEY_ESCAPE || isKeyDown(mc.options.keyInventory);
    }

    @Override
    public boolean isUnfocusTextFieldKey(int key)
    {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE;
    }

    @Nonnull
    @Override
    public FormattedText trimWithEllipsis(@Nonnull FormattedText text, int width)
    {
        FormattedText trimmed = trim(text, width);

        if (trimmed.getString().equals(text.getString()))
        {
            return text;
        }

        return FormattedText.of(mc.font.substrByWidth(trimmed, Math.max(0, width - mc.font.width("..."))).getString() + "...");
    }

    @Nonnull
    @Override
    public FormattedText trim(@Nonnull FormattedText text, int width)
    {
        return mc.font.substrByWidth(text, width);
    }

    @Nonnull
    @Override
    public List<FormattedCharSequence> split(@Nonnull FormattedText text, int width)
    {
        return new ArrayList<>(mc.font.split(text, width));
    }

    @Nonnull
    @Override
    public List<String> split(@Nonnull String text, int width)
    {
        return mc.font.getSplitter().splitLines(text, width, net.minecraft.network.chat.Style.EMPTY).stream()
                .map(FormattedText::getString)
                .collect(Collectors.toList());
    }

    @Override
    public int getTextWidth(@Nonnull String text)
    {
        return mc.font.width(text);
    }

    @Override
    public int getTextWidth(@Nonnull FormattedCharSequence text)
    {
        return mc.font.width(text);
    }

    @Override
    public int getLineHeight()
    {
        return mc.font.lineHeight;
    }

    @Override
    public void renderShadowedText(@Nonnull PoseStack matrixStack, @Nonnull FormattedCharSequence text, int x, int y, int color)
    {
        matrixStack.pushPose();
        matrixStack.translate(0.0, 0.0, 200.0);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder());
        mc.font.drawInBatch(text, x, y, color, true, matrixStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        bufferSource.endBatch();

        matrixStack.popPose();
    }

    @Override
    public void renderText(@Nonnull PoseStack matrixStack, @Nonnull FormattedCharSequence text, int x, int y, int color)
    {
        matrixStack.pushPose();
        matrixStack.translate(0.0, 0.0, 200.0);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder());
        mc.font.drawInBatch(text, x, y, color, false, matrixStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        bufferSource.endBatch();

        matrixStack.popPose();
    }

    @Override
    public void bindTexture(@Nonnull ResourceLocation texture)
    {
        RenderSystem.setShaderTexture(0, texture);
    }

    @Override
    public void bindTexture(@Nonnull Texture texture)
    {
        bindTexture(texture.resourceLocation);
    }

    @Override
    public void renderEntityOnScreen(@Nonnull PoseStack matrixStack, @Nonnull LivingEntity entity, int screenX, int screenY, float screenMouseX, float screenMouseY, float scale, boolean scaleToBoundingBox)
    {
        float scaleFactor = scaleToBoundingBox ? 16.0f / Math.max(entity.getBbWidth(), entity.getBbHeight()) : 16.0f;
        int renderScale = Math.max(1, Math.round(scale * scaleFactor));

        float correctedMouseX = screenX - screenMouseX;
        float correctedMouseY = screenY - screenMouseY;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource()),
                screenX,
                screenY,
                renderScale,
                correctedMouseX,
                correctedMouseY,
                entity
        );
    }

    @Override
    public void renderItemStack(@Nonnull PoseStack matrixStack, @Nonnull ItemStack stack, int x, int y, double z, float scale)
    {
        if (stack.isEmpty())
        {
            return;
        }

        // En 1.20.1 la forma correcta de renderizar items en GUI es GuiGraphics.renderItem().
        // Maneja internamente la iluminación (Lighting.setupForFlatItems / setupFor3DItems),
        // el atlas de texturas, y los transforms del modelo según ItemDisplayContext.GUI.
        // Usar itemRenderer.render() directamente tiene múltiples problemas de estado OpenGL.

        // x, y son coordenadas absolutas de GUI (ya divididas por guiScale en ItemControl).
        // scale es el tamaño deseado en píxeles GUI. El item por defecto ocupa 16x16.
        float normalizedScale = scale / 16.0f;

        net.minecraft.client.gui.GuiGraphics guiGraphics =
                new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());

        guiGraphics.pose().pushPose();
        // GuiGraphics.renderItem renderiza desde la esquina superior-izquierda del item.
        // Centramos el item en (x, y) compensando los 8 píxeles de la mitad del item (16/2 = 8).
        guiGraphics.pose().translate(x - 8.0f * normalizedScale, y - 8.0f * normalizedScale, z);
        guiGraphics.pose().scale(normalizedScale, normalizedScale, normalizedScale);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.flush();
        guiGraphics.pose().popPose();
    }
}
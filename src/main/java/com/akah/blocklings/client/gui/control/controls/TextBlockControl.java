package com.akah.blocklings.client.gui.control.controls;

import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.control.BaseControl;
import com.akah.blocklings.client.gui.control.Control;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.client.gui.util.ScissorStack;
import com.akah.blocklings.util.DoubleUtil;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Displays a block of text.
 */
@OnlyIn(Dist.CLIENT)
public class TextBlockControl extends Control
{
    /**
     * The text component to render.
     */
    @Nonnull
    private Component text = Component.literal("");

    /**
     * Whether to trim the text to fit the width of the control.
     */
    private boolean shouldTrimText = true;

    /**
     * Whether to draw shadowed text or not.
     */
    private boolean shouldRenderShadow = true;

    /**
     * The text colour.
     */
    private int textColour = 0xffffffff;

    /**
     * The screen x position to render the text.
     */
    private float textScreenX = 0;

    /**
     * The screen y position to render the text.
     */
    private float textScreenY = 0;

    /**
     * The line height.
     */
    private int lineHeight = GuiUtil.get().getLineHeight();

    /**
     */
    public TextBlockControl()
    {
        super();

        setInteractive(false);
        setFitHeightToContent(true);
    }

    @Override
    protected void measureSelf(double availableWidth, double availableHeight)
    {
        double width = getWidth();
        double height = getHeight();

        if (getWidthPercentage() != null && DoubleUtil.isPositiveAndFinite(availableWidth))
        {
            width = availableWidth * getWidthPercentage();
        }
        else if (shouldFitWidthToContent())
        {
            double textWidth = GuiUtil.get().getTextWidth(getTextString());
            width = textWidth + getPaddingWidth();
        }

        if (getHeightPercentage() != null && DoubleUtil.isPositiveAndFinite(availableHeight))
        {
            height = availableHeight * getHeightPercentage();
        }
        else if (shouldFitHeightToContent())
        {
            double textHeight = getLineHeight();
            height = textHeight + getPaddingHeight();
        }

        if (availableWidth >= 0.0)
        {
            setDesiredWidth(width);
        }

        if (availableHeight >= 0.0)
        {
            setDesiredHeight(height);
        }
    }

    @Override
    public void onRender(@Nonnull PoseStack matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
    {
        super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);

        double screenWidth = getPixelWidthWithoutPadding() / getGuiScale();
        double screenHeight = getPixelHeightWithoutPadding() / getGuiScale();
        double horizontalAlignment = getHorizontalContentAlignment() != null ? getHorizontalContentAlignment() : 0.0;
        double verticalAlignment = getVerticalContentAlignment() != null ? getVerticalContentAlignment() : 0.0;

        textScreenX = (float) (getPixelX() / getGuiScale() + (screenWidth - GuiUtil.get().getTextWidth(getTextToRender())) * horizontalAlignment + getPadding().left);
        textScreenY = (float) (getPixelY() / getGuiScale() + (screenHeight - getLineHeight()) * verticalAlignment + getPadding().top);

        float x = (float) (Math.round(textScreenX * getGuiScale()) / getGuiScale());
        float y = (float) (Math.round(textScreenY * getGuiScale()) / getGuiScale());

        float z = isDraggingOrAncestor() ? (float) getDraggedControl().getDragZ() : (float) getRenderZ();


        PoseStack matrixStack2 = new PoseStack();
        matrixStack2.translate(x, y, z);
        matrixStack2.scale((float) getScaleX(), (float) getScaleY(), 1.0f);

        if (shouldRenderShadow())
        {
            renderShadowedText(matrixStack2, getTextToRender(), getTextColour());
        }
        else
        {
            renderText(matrixStack2, getTextToRender(), getTextColour());
        }
    }

    @Override
    public void addChild(@Nonnull BaseControl child)
    {
        throw new UnsupportedOperationException("TextBlockControl does not support adding children.");
    }

    @Override
    public void insertChildBefore(@Nonnull BaseControl controlToInsert, @Nonnull BaseControl controlToInsertBefore)
    {
        throw new UnsupportedOperationException("TextBlockControl does not support adding children.");
    }

    @Override
    public void insertChildAfter(@Nonnull BaseControl controlToInsert, @Nonnull BaseControl controlToInsertAfter)
    {
        throw new UnsupportedOperationException("TextBlockControl does not support adding children.");
    }

    /**
     * @return gets the text to render (e.g. might be trimmed to fit).
     */
    public FormattedCharSequence getTextToRender()
    {
        FormattedCharSequence textToRender = text.getVisualOrderText();

        if (shouldTrimText())
        {
            textToRender = Language.getInstance().getVisualOrder(GuiUtil.get().trimWithEllipsis(getText(), (int) Math.round(getWidthWithoutPadding())));
        }

        return textToRender;
    }

    /**
     * @return the text to render as a string.
     */
    @Nonnull
    public String getTextString()
    {
        return text.getString();
    }

    /**
     * @return the text to render.
     */
    @Nonnull
    public Component getText()
    {
        return text;
    }

    /**
     * Sets the text to render.
     *
     * @param text the text to render.
     */
    public void setText(@Nonnull String text)
    {
        this.text = Component.literal(text);
    }

    /**
     * Sets the text to render.
     *
     * @param text the text to render.
     */
    public void setText(@Nonnull Component text)
    {
        this.text = text;
    }

    /**
     * @return whether to trim the text to fit the width of the control.
     */
    public boolean shouldTrimText()
    {
        return shouldTrimText;
    }

    /**
     * Sets whether to trim the text to fit the width of the control.
     */
    public void setShouldTrimText(boolean shouldTrimText)
    {
        this.shouldTrimText = shouldTrimText;
    }

    /**
     * @return whether to render shadowed text.
     */
    public boolean shouldRenderShadow()
    {
        return shouldRenderShadow;
    }

    /**
     * Sets whether to render shadowed text.
     */
    public void setShouldRenderShadow(boolean shouldRenderShadow)
    {
        this.shouldRenderShadow = shouldRenderShadow;
    }

    /**
     * @return the text colour.
     */
    @Nonnull
    public int getTextColour()
    {
        return textColour;
    }

    /**
     * Sets the text colour.
     */
    public void setTextColour(int textColour)
    {
        this.textColour = textColour;
    }

    /**
     * @return the line height.
     */
    public int getLineHeight()
    {
        return lineHeight - (shouldRenderShadow() ? 0 : 1);
    }

    /**
     * Sets the line height.
     *
     * @param lineHeight the line height.
     */
    public void setLineHeight(int lineHeight)
    {
        this.lineHeight = lineHeight;
    }

    /**
     * Sets the line height to be the default line height.
     */
    public void useDefaultLineHeight()
    {
        setLineHeight(GuiUtil.get().getLineHeight());
    }

    /**
     * Sets the line height to be the default line height minus the height of descenders.
     */
    public void useDescenderlessLineHeight()
    {
        setLineHeight(GuiUtil.get().getLineHeight() - 1);
    }
}

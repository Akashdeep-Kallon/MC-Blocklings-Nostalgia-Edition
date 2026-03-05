package com.akah.blocklings.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.UnaryOperator;

public class BlocklingsComponent implements Component
{
    private final MutableComponent delegate;

    public BlocklingsComponent(String key)
    {
        this.delegate = Component.translatable("blocklings." + key);
    }

    public BlocklingsComponent(String key, Object... objects)
    {
        this.delegate = Component.translatable("blocklings." + key, objects);
    }

    public BlocklingsComponent withStyle(ChatFormatting... formats)
    {
        delegate.withStyle(formats);
        return this;
    }

    public BlocklingsComponent withStyle(ChatFormatting format)
    {
        delegate.withStyle(format);
        return this;
    }

    public BlocklingsComponent withStyle(UnaryOperator<Style> styleUpdater)
    {
        delegate.withStyle(styleUpdater);
        return this;
    }

    public BlocklingsComponent withStyle(Style style)
    {
        delegate.withStyle(style);
        return this;
    }

    @Override
    public Style getStyle()
    {
        return delegate.getStyle();
    }

    @Override
    public ComponentContents getContents()
    {
        return delegate.getContents();
    }

    @Override
    public List<Component> getSiblings()
    {
        return delegate.getSiblings();
    }

    @Override
    public FormattedCharSequence getVisualOrderText()
    {
        return delegate.getVisualOrderText();
    }
}

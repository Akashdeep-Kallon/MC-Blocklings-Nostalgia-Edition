package com.akah.blocklings.entity.blockling.task.config.range;

import com.mojang.blaze3d.vertex.PoseStack;
import com.akah.blocklings.client.gui.control.BaseControl;
import com.akah.blocklings.client.gui.control.controls.config.FloatRangeControl;
import com.akah.blocklings.client.gui.util.GuiUtil;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.util.Version;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures an float range property.
 */
public class FloatRangeProperty extends RangeProperty<Float>
{
    /**
     * @param id            the id of the property (used for serialising\deserialising).
     * @param goal          the associated task's goal.
     * @param name          the name of the property.
     * @param desc          the description of the property.
     * @param min           the minimum value of the range.
     * @param max           the maximum value of the range.
     * @param startingValue the range starting value.
     */
    public FloatRangeProperty(@Nonnull String id, @Nonnull BlocklingGoal goal, @Nonnull Component name, @Nonnull Component desc, float min, float max, float startingValue)
    {
        super(id, goal, name, desc, min, max, startingValue);
    }

    @Override
    public CompoundTag writeToNBT(@Nonnull CompoundTag propertyTag)
    {
        propertyTag.putFloat("value", value);

        return super.writeToNBT(propertyTag);
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag propertyTag, @Nonnull Version tagVersion)
    {
        value = propertyTag.getFloat("value");

        super.readFromNBT(propertyTag, tagVersion);
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        buf.writeFloat(min);
        buf.writeFloat(max);
        buf.writeFloat(value);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        min = buf.readFloat();
        max = buf.readFloat();
        value = buf.readFloat();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    @Nonnull
    public BaseControl createControl()
    {
        FloatRangeControl range = new FloatRangeControl(min, max, value)
        {
            @Override
            public void onRenderTooltip(@Nonnull PoseStack matrixStack, double mouseX, double mouseY, float partialTicks)
            {
                if (!grabberControl.isPressed())
                {
                    List<FormattedCharSequence> tooltip = new ArrayList<>(GuiUtil.get().split(desc.copy().withStyle(ChatFormatting.GRAY), 200));
                    tooltip.add(0, name.copy().withStyle(ChatFormatting.WHITE).getVisualOrderText());

                    renderTooltip(matrixStack, mouseX, mouseY, tooltip);
                }
            }

            @Override
            public void setValue(@Nonnull Float value, boolean updateGrabberPosition, boolean postEvent)
            {
                value = Math.round(value * 10.0f) / 10.0f;

                super.setValue(value, updateGrabberPosition, postEvent);

                FloatRangeProperty.this.setValue(getValue(), true);
            }
        };
        range.valueFieldControl.setWidth(26);

        return range;
    }
}

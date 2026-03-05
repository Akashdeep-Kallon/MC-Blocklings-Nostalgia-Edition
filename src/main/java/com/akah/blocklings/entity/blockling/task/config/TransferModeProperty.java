package com.akah.blocklings.entity.blockling.task.config;

import com.akah.blocklings.client.gui.control.BaseControl;
import com.akah.blocklings.client.gui.control.controls.SingleSelectorStrip;
import com.akah.blocklings.client.gui.control.event.events.SelectionChangedEvent;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.util.BlocklingsComponent;
import com.akah.blocklings.util.Version;
import com.akah.blocklings.util.event.EventHandler;
import com.akah.blocklings.util.event.IEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class TransferModeProperty extends Property
{
    @Nonnull
    public final EventHandler<Mode> onModeChanged = new EventHandler<>();

    @Nonnull
    private Mode mode = Mode.WHITELIST;

    public TransferModeProperty(@Nonnull String id, @Nonnull BlocklingGoal goal, @Nonnull Component name, @Nonnull Component desc)
    {
        super(id, goal, name, desc);
    }

    @Override
    public CompoundTag writeToNBT(@Nonnull CompoundTag propertyTag)
    {
        propertyTag.putInt("mode", mode.ordinal());

        return super.writeToNBT(propertyTag);
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag propertyTag, @Nonnull Version tagVersion)
    {
        setMode(Mode.values()[propertyTag.getInt("mode")], false);

        super.readFromNBT(propertyTag, tagVersion);
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        buf.writeInt(mode.ordinal());
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        setMode(Mode.values()[buf.readInt()], false);
    }

    @Nonnull
    @Override
    public BaseControl createControl()
    {
        return createControl(Arrays.asList(Mode.values()));
    }

    @Nonnull
    public BaseControl createControl(@Nonnull List<Mode> options)
    {
        SingleSelectorStrip<Mode> selector = new SingleSelectorStrip<>();
        selector.setOptions(options);
        selector.setWidthPercentage(1.0);
        selector.setSelectedOption(getMode());
        selector.eventBus.subscribe((BaseControl c, SelectionChangedEvent<Mode> e) -> setMode(e.newItem, true));

        onModeChanged.subscribe((mode) ->
        {
            if (selector.getOptions().contains(mode))
            {
                selector.setSelectedOption(mode);
            }
        });

        return selector;
    }

    @Nonnull
    public Mode getMode()
    {
        return mode;
    }

    public void setMode(@Nonnull Mode mode)
    {
        setMode(mode, true);
    }

    public void setMode(@Nonnull Mode mode, boolean sync)
    {
        if (this.mode.equals(mode))
        {
            return;
        }

        this.mode = mode;
        onModeChanged.handle(mode);

        if (sync)
        {
            new TaskPropertyMessage(this).sync();
        }
    }

    public enum Mode implements IEvent
    {
        WHITELIST,
        TRANSFER_ALL,
        TAKE_ALL;

        @Override
        public String toString()
        {
            return new BlocklingsComponent("task.property.transfer_mode." + name().toLowerCase()).getString();
        }
    }
}

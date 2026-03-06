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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Used to configure an item configuration type property.
 */
public class ItemConfigurationTypeProperty extends Property
{
    /**
     * Invoked when the type of item configuration is changed.
     */
    @Nonnull
    public final EventHandler<Type> onTypeChanged = new EventHandler<>();

    /**
     * The current type of item configuration.
     */
    private Type type = Type.SIMPLE;

    /**
     * @param id the id of the property (used for syncing between serialising\deserialising).
     * @param goal the associated task's goal.
     * @param name the name of the property.
     * @param desc the description of the property.
     */
    public ItemConfigurationTypeProperty(@Nonnull String id, @Nonnull BlocklingGoal goal, @Nonnull Component name, @Nonnull Component desc)
    {
        super(id, goal, name, desc);
    }

    @Override
    public CompoundTag writeToNBT(@Nonnull CompoundTag propertyTag)
    {
        propertyTag.putInt("type", type.ordinal());

        return super.writeToNBT(propertyTag);
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag propertyTag, @Nonnull Version tagVersion)
    {
        setType(Type.values()[propertyTag.getInt("type")], false);

        super.readFromNBT(propertyTag, tagVersion);
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        buf.writeInt(type.ordinal());
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        setType(Type.values()[buf.readInt()], false);
    }

    @Nonnull
    @Override
    @OnlyIn(Dist.CLIENT)
    public Object createControl()
    {
        SingleSelectorStrip<Type> selector = new SingleSelectorStrip<>();
        selector.setOptions(Arrays.asList(Type.values()));
        selector.setWidthPercentage(1.0);
        selector.setSelectedOption(getType());
        selector.eventBus.subscribe((BaseControl c, SelectionChangedEvent<Type> e) ->
        {
            setType(e.newItem, true);
        });

        return selector;
    }

    /**
     * @return the current type of item configuration.
     */
    @Nonnull
    public Type getType()
    {
        return type;
    }

    /**
     * Sets the current type of item configuration and syncs to the client/server.
     *
     * @param type the new type.
     */
    public void setType(@Nonnull Type type)
    {
        setType(type, true);
    }

    /**
     * Sets the current type of item configuration.
     *
     * @param type the new type.
     * @param sync whether to sync to the client/server.
     */
    public void setType(@Nonnull Type type, boolean sync)
    {
        if (this.type.equals(type))
        {
            return;
        }

        this.type = type;

        onTypeChanged.handle(type);

        if (sync)
        {
            new TaskPropertyMessage(this).sync();
        }
    }

    /**
     * The different types of item configuration.
     */
    public enum Type implements IEvent
    {
        SIMPLE,
        ADVANCED;


        @Override
        public String toString()
        {
            return new BlocklingsComponent("task.property.item_configuration_type." + name().toLowerCase()).getString();
        }
    }
}
package com.akah.blocklings.entity.blockling.goal.config.iteminfo;

import com.akah.blocklings.util.Version;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A class used to store information about an item.
 */
public class ItemInfo
{
    /**
     * The item.
     */
    @Nonnull
    private Item item = Items.AIR;

    /**
     * The amount of the item needed in the inventory before the goal will start.
     */
    @Nullable
    private Integer startInventoryAmount = null;

    /**
     * The amount of the item needed in the inventory before the goal will stop.
     */
    @Nullable
    private Integer stopInventoryAmount = null;

    /**
     * The amount of the item needed in the container before the goal will start.
     */
    @Nullable
    private Integer startContainerAmount = null;

    /**
     * The amount of the item needed in the container before the goal will stop.
     */
    @Nullable
    private Integer stopContainerAmount = null;

    /**
     */
    public ItemInfo()
    {
    }

    /**
     * @param item the item.
     */
    public ItemInfo(@Nonnull Item item)
    {
        this(item, null, null, null, null);
    }

    /**
     * @param item the item.
     * @param startInventoryAmount the amount of the item needed in the inventory before the goal will start.
     * @param stopInventoryAmount the amount of the item needed in the inventory before the goal will stop.
     * @param startContainerAmount the amount of the item needed in the container before the goal will start.
     * @param stopContainerAmount the amount of the item needed in the container before the goal will stop.
     */
    public ItemInfo(@Nonnull Item item, @Nullable Integer startInventoryAmount, @Nullable Integer stopInventoryAmount, @Nullable Integer startContainerAmount, @Nullable Integer stopContainerAmount)
    {
        this.item = item;
        this.startInventoryAmount = startInventoryAmount;
        this.stopInventoryAmount = stopInventoryAmount;
        this.startContainerAmount = startContainerAmount;
        this.stopContainerAmount = stopContainerAmount;
    }

    /**
     * Writes the item info to a tag.
     *
     * @return the tag for the item info.
     */
    public CompoundTag writeToNBT()
    {
        CompoundTag tag = new CompoundTag();

        tag.putString("item", item.builtInRegistryHolder().key().location().toString());
        if (startInventoryAmount != null) tag.putInt("min_inventory_amount", startInventoryAmount);
        if (stopInventoryAmount != null) tag.putInt("max_inventory_amount", stopInventoryAmount);
        if (startContainerAmount != null) tag.putInt("min_container_amount", startContainerAmount);
        if (stopContainerAmount != null) tag.putInt("max_container_amount", stopContainerAmount);

        return tag;
    }

    /**
     * Reads the item info from the given tag.
     *
     * @param tag the tag to read from.
     * @param version the version of the tag.
     */
    public void readFromNBT(@Nonnull CompoundTag tag, @Nonnull Version version)
    {
        item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(tag.getString("item")));
        startInventoryAmount = tag.contains("min_inventory_amount") ? tag.getInt("min_inventory_amount") : null;
        stopInventoryAmount = tag.contains("max_inventory_amount") ? tag.getInt("max_inventory_amount") : null;
        startContainerAmount = tag.contains("min_container_amount") ? tag.getInt("min_container_amount") : null;
        stopContainerAmount = tag.contains("max_container_amount") ? tag.getInt("max_container_amount") : null;
    }

    /**
     * Writes the item info to the given packet buffer.
     *
     * @param buffer the packet buffer to write to.
     */
    public void encode(@Nonnull FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(item.builtInRegistryHolder().key().location());
        buffer.writeBoolean(startInventoryAmount != null);
        if (startInventoryAmount != null) buffer.writeInt(startInventoryAmount);
        buffer.writeBoolean(stopInventoryAmount != null);
        if (stopInventoryAmount != null) buffer.writeInt(stopInventoryAmount);
        buffer.writeBoolean(startContainerAmount != null);
        if (startContainerAmount != null) buffer.writeInt(startContainerAmount);
        buffer.writeBoolean(stopContainerAmount != null);
        if (stopContainerAmount != null) buffer.writeInt(stopContainerAmount);
    }

    /**
     * Reads the item info from the given packet buffer.
     *
     * @param buffer the packet buffer to read from.
     */
    public void decode(@Nonnull FriendlyByteBuf buffer)
    {
        item = BuiltInRegistries.ITEM.get(buffer.readResourceLocation());
        startInventoryAmount = buffer.readBoolean() ? buffer.readInt() : null;
        stopInventoryAmount = buffer.readBoolean() ? buffer.readInt() : null;
        startContainerAmount = buffer.readBoolean() ? buffer.readInt() : null;
        stopContainerAmount = buffer.readBoolean() ? buffer.readInt() : null;
    }

    /**
     * @return the item.
     */
    @Nonnull
    public Item getItem()
    {
        return item;
    }

    /**
     * Sets the item.
     *
     * @param item the item.
     */
    public void setItem(@Nonnull Item item)
    {
        this.item = item;
    }

    /**
     * @return the amount of the item needed in the container before the goal will start.
     */
    @Nullable
    public Integer getStartInventoryAmount()
    {
        return startInventoryAmount;
    }

    /**
     * Sets the amount of the item needed in the container before the goal will start.
     *
     * @param startInventoryAmount the amount.
     */
    public void setStartInventoryAmount(@Nullable Integer startInventoryAmount)
    {
        this.startInventoryAmount = startInventoryAmount;
    }

    /**
     * @return the amount of the item needed in the container before the goal will stop.
     */
    @Nullable
    public Integer getStopInventoryAmount()
    {
        return stopInventoryAmount;
    }

    /**
     * Sets the amount of the item needed in the container before the goal will stop.
     *
     * @param stopInventoryAmount the amount.
     */
    public void setStopInventoryAmount(@Nullable Integer stopInventoryAmount)
    {
        this.stopInventoryAmount = stopInventoryAmount;
    }

    /**
     * @return the amount of the item needed in the container before the goal will start.
     */
    @Nullable
    public Integer getStartContainerAmount()
    {
        return startContainerAmount;
    }

    /**
     * Sets the amount of the item needed in the container before the goal will start.
     *
     * @param startContainerAmount the amount.
     */
    public void setStartContainerAmount(@Nullable Integer startContainerAmount)
    {
        this.startContainerAmount = startContainerAmount;
    }

    /**
     * @return the amount of the item needed in the container before the goal will stop.
     */
    @Nullable
    public Integer getStopContainerAmount()
    {
        return stopContainerAmount;
    }

    /**
     * Sets the amount of the item needed in the container before the goal will stop.
     *
     * @param stopContainerAmount the amount.
     */
    public void setStopContainerAmount(@Nullable Integer stopContainerAmount)
    {
        this.stopContainerAmount = stopContainerAmount;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ItemInfo))
        {
            return false;
        }

        ItemInfo other = (ItemInfo) obj;
        return item == other.item;
    }

    @Override
    public int hashCode()
    {
        return item.hashCode();
    }
}

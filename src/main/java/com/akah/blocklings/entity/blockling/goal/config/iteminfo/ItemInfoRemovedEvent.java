package com.akah.blocklings.entity.blockling.goal.config.iteminfo;

import com.akah.blocklings.util.event.IEvent;

import javax.annotation.Nonnull;

/**
 * An event used when an item info is removed.
 */
public class ItemInfoRemovedEvent implements IEvent
{
    /**
     * The item info that was removed.
     */
    @Nonnull
    public final ItemInfo itemInfo;

    /**
     * @param itemInfo the item info that was removed.
     */
    public ItemInfoRemovedEvent(@Nonnull ItemInfo itemInfo)
    {
        this.itemInfo = itemInfo;
    }
}

package com.akah.blocklings.entity.blockling.goal.goals.container;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.config.ContainerInfo;
import com.akah.blocklings.entity.blockling.goal.config.iteminfo.ItemInfo;
import com.akah.blocklings.entity.blockling.skill.skills.GeneralSkills;
import com.akah.blocklings.entity.blockling.task.BlocklingTasks;
import com.akah.blocklings.entity.blockling.task.config.ItemConfigurationTypeProperty;
import com.akah.blocklings.entity.blockling.task.config.TransferModeProperty;
import com.akah.blocklings.inventory.AbstractInventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Finds nearby containers and takes items from them.
 */
public class BlocklingTakeContainerGoal extends BlocklingContainerGoal
{
    /**
     * @param taskId    the taskId associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks     the blockling tasks.
     */
    public BlocklingTakeContainerGoal(@Nonnull UUID taskId, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(taskId, blockling, tasks);

        if (!blockling.getSkills().getSkill(GeneralSkills.ADVANCED_COURIER).isBought())
        {
            transferModeProperty.setMode(TransferModeProperty.Mode.TAKE_ALL, false);
        }
    }

    @Override
    protected boolean tryTransferItems(@Nonnull ContainerInfo containerInfo, boolean simulate)
    {
        BlockEntity tileEntity = containerAsBlockEntity(containerInfo);

        if (tileEntity == null)
        {
            return false;
        }

        TransferModeProperty.Mode mode = transferModeProperty.getMode();
        if (mode == TransferModeProperty.Mode.TRANSFER_ALL || mode == TransferModeProperty.Mode.TAKE_ALL)
        {
            return tryTakeAllFromContainer(containerInfo, tileEntity, blockling.getEquipment(), simulate);
        }

        return tryTransferItemsWithWhitelist(containerInfo, tileEntity, blockling.getEquipment(), simulate);
    }

    private boolean tryTransferItemsWithWhitelist(@Nonnull ContainerInfo containerInfo, @Nonnull BlockEntity tileEntity, @Nonnull AbstractInventory inv, boolean simulate)
    {
        int remainingTakeAmount = getTransferAmount();
        boolean advanced = itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED;

        if (advanced)
        {
            syncAdvancedItemInfoSetWithWhitelist();

            for (ItemInfo itemInfo : itemInfoSet)
            {
                if (remainingTakeAmount <= 0)
                {
                    break;
                }

                int startInventoryAmount = itemInfo.getStartInventoryAmount() != null ? itemInfo.getStartInventoryAmount() : Integer.MAX_VALUE;
                int startContainerAmount = itemInfo.getStartContainerAmount() != null ? itemInfo.getStartContainerAmount() : 0;
                int stopInventoryAmount = itemInfo.getStopInventoryAmount() != null
                        ? itemInfo.getStopInventoryAmount()
                        : (itemInfo.getStartInventoryAmount() != null ? itemInfo.getStartInventoryAmount() : Integer.MAX_VALUE);
                int stopContainerAmount = itemInfo.getStopContainerAmount() != null
                        ? itemInfo.getStopContainerAmount()
                        : (itemInfo.getStartContainerAmount() != null ? itemInfo.getStartContainerAmount() : 0);
                Item item = itemInfo.getItem();

                remainingTakeAmount = tryTakeWhitelistedItem(containerInfo, tileEntity, inv, simulate, remainingTakeAmount, item,
                        startInventoryAmount, startContainerAmount, stopInventoryAmount, stopContainerAmount, true);

                if (simulate && remainingTakeAmount < getTransferAmount())
                {
                    return true;
                }
            }
        }
        else
        {
            for (Item item : getActiveWhitelistItems())
            {
                if (remainingTakeAmount <= 0)
                {
                    break;
                }

                remainingTakeAmount = tryTakeWhitelistedItem(containerInfo, tileEntity, inv, simulate, remainingTakeAmount, item,
                        Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 0, false);

                if (simulate && remainingTakeAmount < getTransferAmount())
                {
                    return true;
                }
            }
        }

        return remainingTakeAmount < getTransferAmount();
    }

    private int tryTakeWhitelistedItem(@Nonnull ContainerInfo containerInfo, @Nonnull BlockEntity tileEntity, @Nonnull AbstractInventory inv,
                                        boolean simulate, int remainingTakeAmount, @Nonnull Item item, int startInventoryAmount,
                                        int startContainerAmount, int stopInventoryAmount, int stopContainerAmount, boolean useThresholds)
    {
        ItemStack remainingStack = new ItemStack(item, remainingTakeAmount);
        int amountOfSpaceInInventoryForItem = remainingStack.getCount() - inv.addItem(remainingStack, true).getCount();

        if (amountOfSpaceInInventoryForItem == 0)
        {
            return remainingTakeAmount;
        }

        // [fix] Deduplicate handlers: a chest that exposes the same inventory from every
        // direction should only be accessed once — via the highest-priority direction.
        java.util.Set<IItemHandler> seenHandlers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        for (Direction direction : containerInfo.getSides())
        {
            if (amountOfSpaceInInventoryForItem == 0)
            {
                continue;
            }

            IItemHandler itemHandler = getItemHandler(tileEntity, direction);

            if (itemHandler == null)
            {
                return remainingTakeAmount;
            }

            // [fix] Skip duplicate handler references.
            if (!seenHandlers.add(itemHandler))
            {
                continue;
            }

            if (!hasItemInContainer(itemHandler, item))
            {
                continue;
            }

            if (useThresholds)
            {
                int inventoryAmount = countItemsInInventory(item);
                int containerAmount = countItemsInContainer(itemHandler, item);

                if (inventoryAmount >= stopInventoryAmount || containerAmount <= stopContainerAmount)
                {
                    continue;
                }

                if (getState() != State.ACTIVE && (inventoryAmount >= startInventoryAmount || containerAmount <= startContainerAmount))
                {
                    continue;
                }

                int takeLimit = Integer.MAX_VALUE;

                if (stopInventoryAmount != Integer.MAX_VALUE)
                {
                    takeLimit = Math.min(takeLimit, Math.max(0, stopInventoryAmount - inventoryAmount));
                }

                if (stopContainerAmount != 0)
                {
                    takeLimit = Math.min(takeLimit, Math.max(0, containerAmount - stopContainerAmount));
                }

                if (takeLimit <= 0)
                {
                    continue;
                }

                remainingTakeAmount = Math.min(remainingTakeAmount, takeLimit);
            }

            if (remainingTakeAmount <= 0)
            {
                break;
            }

            int amountToTake = Math.min(remainingTakeAmount, amountOfSpaceInInventoryForItem);
            ItemStack stackLeftToTake = new ItemStack(item, amountToTake);

            for (int slot = itemHandler.getSlots() - 1; slot >= 0 && !stackLeftToTake.isEmpty(); slot--)
            {
                stackLeftToTake.shrink(itemHandler.extractItem(slot, stackLeftToTake.getCount(), simulate).getCount());
            }

            int amountTaken = amountToTake - stackLeftToTake.getCount();

            if (amountTaken == 0)
            {
                continue;
            }

            if (!simulate)
            {
                inv.addItem(new ItemStack(item, amountTaken), false);
            }
            else
            {
                return remainingTakeAmount - amountTaken;
            }

            remainingTakeAmount -= amountTaken;
            amountOfSpaceInInventoryForItem -= amountTaken;
        }

        return remainingTakeAmount;
    }

    private boolean tryTakeAllFromContainer(@Nonnull ContainerInfo containerInfo, @Nonnull BlockEntity tileEntity, @Nonnull AbstractInventory inv, boolean simulate)
    {
        int remainingTakeAmount = getTransferAmount();
        boolean tookAny = false;

        // [fix] Deduplicate handlers for the same reason as tryTakeWhitelistedItem.
        java.util.Set<IItemHandler> seenHandlers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        for (Direction direction : containerInfo.getSides())
        {
            IItemHandler itemHandler = getItemHandler(tileEntity, direction);

            if (itemHandler == null)
            {
                continue;
            }

            // [fix] Skip duplicate handler references.
            if (!seenHandlers.add(itemHandler))
            {
                continue;
            }

            for (int slot = itemHandler.getSlots() - 1; slot >= 0 && remainingTakeAmount > 0; slot--)
            {
                ItemStack stackInSlot = itemHandler.getStackInSlot(slot);

                if (stackInSlot.isEmpty())
                {
                    continue;
                }

                ItemStack testStack = stackInSlot.copy();
                testStack.setCount(Math.min(stackInSlot.getCount(), remainingTakeAmount));
                int spaceInInventory = testStack.getCount() - inv.addItem(testStack.copy(), true).getCount();

                if (spaceInInventory == 0)
                {
                    continue;
                }

                int toTake = Math.min(spaceInInventory, remainingTakeAmount);
                ItemStack extracted = itemHandler.extractItem(slot, toTake, simulate);

                if (!extracted.isEmpty())
                {
                    if (!simulate)
                    {
                        inv.addItem(extracted, false);
                    }

                    remainingTakeAmount -= extracted.getCount();
                    tookAny = true;

                    if (simulate)
                    {
                        return true;
                    }
                }
            }
        }

        return tookAny;
    }

    @Override
    public boolean hasItemsToTransfer()
    {
        TransferModeProperty.Mode mode = transferModeProperty.getMode();
        if (mode == TransferModeProperty.Mode.TRANSFER_ALL || mode == TransferModeProperty.Mode.TAKE_ALL)
        {
            for (ContainerInfo containerInfo : containerInfos)
            {
                BlockEntity tileEntity = containerAsBlockEntity(containerInfo);
                if (tileEntity == null)
                {
                    continue;
                }

                if (tryTakeAllFromContainer(containerInfo, tileEntity, blockling.getEquipment(), true))
                {
                    return true;
                }
            }

            return false;
        }

        if (itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED)
        {
            syncAdvancedItemInfoSetWithWhitelist();

            for (ItemInfo itemInfo : itemInfoSet)
            {
                int startInventoryAmount = itemInfo.getStartInventoryAmount() != null
                        ? itemInfo.getStartInventoryAmount() : Integer.MAX_VALUE;
                int startContainerAmount = itemInfo.getStartContainerAmount() != null
                        ? itemInfo.getStartContainerAmount() : 0;
                int inventoryAmount = countItemsInInventory(itemInfo.getItem());

                // [fix] Respect start threshold to avoid searching for items while not yet under trigger amount.
                if (getState() != State.ACTIVE && inventoryAmount >= startInventoryAmount)
                {
                    continue;
                }

                if (!hasContainerAboveAmount(itemInfo.getItem(), startContainerAmount))
                {
                    continue;
                }

                for (ContainerInfo containerInfo : containerInfos)
                {
                    BlockEntity tileEntity = containerAsBlockEntity(containerInfo);

                    if (tileEntity == null)
                    {
                        continue;
                    }

                    for (Direction direction : containerInfo.getSides())
                    {
                        IItemHandler itemHandler = getItemHandler(tileEntity, direction);

                        if (itemHandler == null)
                        {
                            continue;
                        }

                        if (hasItemInContainer(itemHandler, itemInfo.getItem()))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        else
        {
            for (Item item : getActiveWhitelistItems())
            {
                for (ContainerInfo containerInfo : containerInfos)
                {
                    BlockEntity tileEntity = containerAsBlockEntity(containerInfo);

                    if (tileEntity == null)
                    {
                        continue;
                    }

                    for (Direction direction : containerInfo.getSides())
                    {
                        IItemHandler itemHandler = getItemHandler(tileEntity, direction);
                        if (itemHandler == null)
                        {
                            continue;
                        }

                        if (hasItemInContainer(itemHandler, item))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    private boolean hasContainerAboveAmount(@Nonnull Item item, int amount)
    {
        for (ContainerInfo containerInfo : containerInfos)
        {
            BlockEntity tileEntity = containerAsBlockEntity(containerInfo);
            if (tileEntity == null)
            {
                continue;
            }

            java.util.Set<IItemHandler> seenHandlers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

            for (Direction direction : containerInfo.getSides())
            {
                IItemHandler itemHandler = getItemHandler(tileEntity, direction);
                if (itemHandler == null)
                {
                    continue;
                }

                if (!seenHandlers.add(itemHandler))
                {
                    continue;
                }

                if (countItemsInContainer(itemHandler, item) > amount)
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isTakeItems()
    {
        return true;
    }
}
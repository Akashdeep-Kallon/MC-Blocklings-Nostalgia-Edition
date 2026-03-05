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
 * Finds nearby containers and deposits items into them.
 */
public class BlocklingDepositContainerGoal extends BlocklingContainerGoal
{
    /**
     * @param taskId    the taskId associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks     the blockling tasks.
     */
    public BlocklingDepositContainerGoal(@Nonnull UUID taskId, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(taskId, blockling, tasks);

        if (!blockling.getSkills().getSkill(GeneralSkills.ADVANCED_COURIER).isBought())
        {
            transferModeProperty.setMode(TransferModeProperty.Mode.TRANSFER_ALL, false);
        }
    }

    @Override
    protected boolean tryTransferItems(@Nonnull ContainerInfo containerInfo, boolean simulate)
    {
        TransferModeProperty.Mode mode = transferModeProperty.getMode();
        switch (mode)
        {
            case TRANSFER_ALL:
                return tryDepositAll(containerInfo, simulate);
            case WHITELIST:
            default:
                return tryDepositWithWhitelist(containerInfo, simulate);
        }
    }

    private boolean tryDepositWithWhitelist(@Nonnull ContainerInfo containerInfo, boolean simulate)
    {
        BlockEntity tileEntity = containerAsBlockEntity(containerInfo);

        if (tileEntity == null)
        {
            return false;
        }

        AbstractInventory inv = blockling.getEquipment();

        int remainingDepositAmount = getTransferAmount();

        if (itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED)
        {
            syncAdvancedItemInfoSetWithWhitelist();

            for (ItemInfo itemInfo : itemInfoSet)
            {
                if (remainingDepositAmount <= 0)
                {
                    break;
                }

                Item item = itemInfo.getItem();
                // [fix] When only start threshold is set, use it as stop threshold too.
                // This makes "Start at > X" also mean "keep at least X in inventory".
                int startInventoryAmount = itemInfo.getStartInventoryAmount() != null ? itemInfo.getStartInventoryAmount() : 0;
                int startContainerAmount = itemInfo.getStartContainerAmount() != null ? itemInfo.getStartContainerAmount() : Integer.MAX_VALUE;
                int stopInventoryAmount = itemInfo.getStopInventoryAmount() != null
                        ? itemInfo.getStopInventoryAmount()
                        : (itemInfo.getStartInventoryAmount() != null ? itemInfo.getStartInventoryAmount() : 0);
                int stopContainerAmount = itemInfo.getStopContainerAmount() != null
                        ? itemInfo.getStopContainerAmount()
                        : (itemInfo.getStartContainerAmount() != null ? itemInfo.getStartContainerAmount() : Integer.MAX_VALUE);

                remainingDepositAmount = tryDepositWhitelistedItem(containerInfo, tileEntity, inv, simulate, remainingDepositAmount,
                        item, startInventoryAmount, startContainerAmount, stopInventoryAmount, stopContainerAmount, true);

                if (simulate && remainingDepositAmount < getTransferAmount())
                {
                    return true;
                }
            }
        }
        else
        {
            for (Item item : getActiveWhitelistItems())
            {
                if (remainingDepositAmount <= 0)
                {
                    break;
                }

                remainingDepositAmount = tryDepositWhitelistedItem(containerInfo, tileEntity, inv, simulate, remainingDepositAmount,
                        item, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, false);

                if (simulate && remainingDepositAmount < getTransferAmount())
                {
                    return true;
                }
            }
        }

        return remainingDepositAmount < getTransferAmount();
    }

    private int tryDepositWhitelistedItem(@Nonnull ContainerInfo containerInfo, @Nonnull BlockEntity tileEntity, @Nonnull AbstractInventory inv,
                                          boolean simulate, int remainingDepositAmount, @Nonnull Item item, int startInventoryAmount,
                                          int startContainerAmount, int stopInventoryAmount, int stopContainerAmount, boolean useThresholds)
    {
        if (!hasItemInInventory(item))
        {
            return remainingDepositAmount;
        }

        // [fix] Track which IItemHandler instances have already been used so that a chest
        // that exposes the same inventory from every direction (vanilla chest: all 6 sides
        // return the identical capability object) is only accessed once — from the first
        // (= highest-priority) direction in the user's ordered list.
        // Without this guard the loop would re-enter the same handler repeatedly, causing
        // the priority setting to have no observable effect.
        java.util.Set<IItemHandler> seenHandlers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        for (Direction direction : containerInfo.getSides())
        {
            IItemHandler itemHandler = getItemHandler(tileEntity, direction);
            if (itemHandler == null)
            {
                continue;
            }

            // [fix] Skip handler if it has already been accessed via a higher-priority direction.
            if (!seenHandlers.add(itemHandler))
            {
                continue;
            }

            ItemStack remainingStack = new ItemStack(item, remainingDepositAmount);
            int amountOfSpaceInContainerForItem = 0;

            for (int i = 0; i < itemHandler.getSlots() && !remainingStack.isEmpty(); i++)
            {
                ItemStack remainderStack = itemHandler.insertItem(i, remainingStack, true);
                amountOfSpaceInContainerForItem += remainingStack.getCount() - remainderStack.getCount();
                remainingStack = remainderStack;
            }

            if (amountOfSpaceInContainerForItem == 0)
            {
                continue;
            }

            if (useThresholds)
            {
                int inventoryAmount = countItemsInInventory(item);
                int containerAmount = countItemsInContainer(itemHandler, item);

                if (inventoryAmount <= stopInventoryAmount || containerAmount >= stopContainerAmount)
                {
                    continue;
                }

                if (getState() != State.ACTIVE && (inventoryAmount <= startInventoryAmount || containerAmount >= startContainerAmount))
                {
                    continue;
                }

                int depositLimit = Integer.MAX_VALUE;

                if (stopInventoryAmount != 0)
                {
                    depositLimit = Math.min(depositLimit, Math.max(0, inventoryAmount - stopInventoryAmount));
                }

                if (stopContainerAmount != Integer.MAX_VALUE)
                {
                    depositLimit = Math.min(depositLimit, Math.max(0, stopContainerAmount - containerAmount));
                }

                if (depositLimit <= 0)
                {
                    continue;
                }

                remainingDepositAmount = Math.min(remainingDepositAmount, depositLimit);
            }

            if (remainingDepositAmount <= 0)
            {
                break;
            }

            int amountToDeposit = Math.min(remainingDepositAmount, amountOfSpaceInContainerForItem);
            ItemStack stackLeftToDeposit = new ItemStack(item, amountToDeposit);
            ItemStack stackDeposited = inv.takeItem(stackLeftToDeposit, simulate);

            int amountTaken = stackDeposited.getCount();

            if (amountTaken == 0)
            {
                continue;
            }

            if (!simulate)
            {
                for (int i = 0; i < itemHandler.getSlots() && !stackDeposited.isEmpty(); i++)
                {
                    stackDeposited = itemHandler.insertItem(i, stackDeposited, false);
                }
            }
            else
            {
                return remainingDepositAmount - amountTaken;
            }

            remainingDepositAmount -= amountTaken;
        }

        return remainingDepositAmount;
    }

    private boolean tryDepositAll(@Nonnull ContainerInfo containerInfo, boolean simulate)
    {
        BlockEntity tileEntity = containerAsBlockEntity(containerInfo);

        if (tileEntity == null)
        {
            return false;
        }

        AbstractInventory inv = blockling.getEquipment();
        int remainingAmount = getTransferAmount();
        boolean depositedAny = false;

        // [fix] Same handler deduplication as in tryDepositWhitelistedItem.
        java.util.Set<IItemHandler> seenHandlers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        for (Direction direction : containerInfo.getSides())
        {
            IItemHandler itemHandler = getItemHandler(tileEntity, direction);
            if (itemHandler == null)
            {
                continue;
            }

            // [fix] Skip if this exact handler object was already visited via a higher-priority direction.
            if (!seenHandlers.add(itemHandler))
            {
                continue;
            }

            for (int invSlot = 0; invSlot < inv.getContainerSize() && remainingAmount > 0; invSlot++)
            {
                ItemStack stackInInv = inv.getItem(invSlot);
                if (stackInInv.isEmpty())
                {
                    continue;
                }

                int toDeposit = Math.min(stackInInv.getCount(), remainingAmount);
                ItemStack stackToDeposit = stackInInv.copy();
                stackToDeposit.setCount(toDeposit);

                ItemStack remaining = stackToDeposit.copy();
                for (int slot = 0; slot < itemHandler.getSlots() && !remaining.isEmpty(); slot++)
                {
                    remaining = itemHandler.insertItem(slot, remaining, simulate);
                }

                int deposited = toDeposit - remaining.getCount();
                if (deposited > 0)
                {
                    if (!simulate)
                    {
                        stackInInv.shrink(deposited);
                        inv.setItem(invSlot, stackInInv);
                    }

                    remainingAmount -= deposited;
                    depositedAny = true;

                    if (simulate)
                    {
                        return true;
                    }
                }
            }
        }

        return depositedAny;
    }

    @Override
    public boolean hasItemsToTransfer()
    {
        TransferModeProperty.Mode mode = transferModeProperty.getMode();

        if (mode == TransferModeProperty.Mode.TRANSFER_ALL)
        {
            AbstractInventory inv = blockling.getEquipment();
            for (int slot = 0; slot < inv.getContainerSize(); slot++)
            {
                if (!inv.getItem(slot).isEmpty())
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
                if (!hasItemInInventory(itemInfo.getItem()))
                {
                    continue;
                }

                // [fix] Respect start threshold to avoid target searching while below trigger amount.
                int startInventoryAmount = itemInfo.getStartInventoryAmount() != null
                        ? itemInfo.getStartInventoryAmount() : 0;
                int startContainerAmount = itemInfo.getStartContainerAmount() != null
                        ? itemInfo.getStartContainerAmount() : Integer.MAX_VALUE;
                int inventoryAmount = countItemsInInventory(itemInfo.getItem());

                if (getState() != State.ACTIVE && inventoryAmount <= startInventoryAmount)
                {
                    continue;
                }

                if (startContainerAmount != Integer.MAX_VALUE && !hasContainerBelowAmount(itemInfo.getItem(), startContainerAmount))
                {
                    continue;
                }

                return true;
            }
        }
        else
        {
            for (Item item : getActiveWhitelistItems())
            {
                if (hasItemInInventory(item))
                {
                    return true;
                }
            }
        }

        return false;
    }


    private boolean hasContainerBelowAmount(@Nonnull Item item, int amount)
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

                if (countItemsInContainer(itemHandler, item) < amount)
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
        return false;
    }
}
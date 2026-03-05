package com.akah.blocklings.item;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.sound.BlocklingsSounds;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * An item used to return a blockling back to the owner.
 */
public class BlocklingWhistleItem extends Item
{
    /**
     * The key used by a blockling whistle stack to reference a blockling uuid.
     */
    @Nonnull
    public static final String BLOCKLING_UUID_KEY = "blockling_uuid";

    /**
     * The key used by a blockling whistle stack to store the blockling's name.
     */
    @Nonnull
    public static final String BLOCKLING_NAME_KEY = "blockling_name";

    /**
     * Maps the instances of blocklings to their respective blockling whistles.
     */
    @Nonnull
    public static final Map<BlocklingEntity, Set<ItemStack>> BLOCKLINGS_TO_WHISTLES = new HashMap<>();

    /**
     * Default constructor.
     */
    public BlocklingWhistleItem()
    {
        super(new Properties()
                                .stacksTo(1)
                .durability(64)
                .setNoRepair());
    }

    /**
     * Sets the blockling the whistle points to.
     *
     * @param stack the stack to set the blockling on.
     * @param blockling the blockling to apply to the whistle.
     */
    public static void setBlockling(@Nonnull ItemStack stack, @Nonnull BlocklingEntity blockling)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(BLOCKLING_UUID_KEY, blockling.getUUID());

        addStackToMap(blockling, stack);
    }

    /**
     * Adds the given whistle to the set of blockling whistles associated with the given blockling.
     *
     * @param blockling the blockling to associate the whistle with.
     * @param stack the stack to be associated with the given blockling.
     */
    public static void addStackToMap(@Nonnull BlocklingEntity blockling, @Nonnull ItemStack stack)
    {
        Set<ItemStack> stacks = BLOCKLINGS_TO_WHISTLES.getOrDefault(blockling, new HashSet<>());
        stacks.add(stack);
        BLOCKLINGS_TO_WHISTLES.put(blockling, stacks);
    }

    /**
     * Called when the blockling is destroyed from the world.
     * This should not be called every time the blockling is removed.
     * Only when the blockling is permanently removed from the world (e.g. death, packling).
     */
    public static void onBlocklingDestroyed(@Nonnull BlocklingEntity blockling)
    {
        Set<ItemStack> stacks = BLOCKLINGS_TO_WHISTLES.get(blockling);

        if (stacks != null)
        {
            for (ItemStack stack : stacks)
            {
                CompoundTag stackTag = stack.getTag();

                if (stackTag != null)
                {
                    stackTag.remove(BLOCKLING_UUID_KEY);
                    stackTag.remove(BLOCKLING_NAME_KEY);
                }
            }
        }

        BLOCKLINGS_TO_WHISTLES.remove(blockling);
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, Player player, @Nonnull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (world instanceof ServerLevel)
        {
            ServerLevel serverLevel = (ServerLevel) world;

            if (stack.hasTag())
            {
                CompoundTag stackTag = stack.getTag();

                if (stackTag.hasUUID(BLOCKLING_UUID_KEY))
                {
                    UUID blocklingId = stackTag.getUUID(BLOCKLING_UUID_KEY);
                    Entity entity = serverLevel.getEntity(blocklingId);

                    if (entity instanceof BlocklingEntity)
                    {
                        BlocklingEntity blockling = (BlocklingEntity) entity;

                        if (player == blockling.getOwner())
                        {
                            blockling.teleportTo(player.getX(), player.getY(), player.getZ());

                            stack.hurtAndBreak(1, player, playerEntity -> playerEntity.broadcastBreakEvent(hand));

                            serverLevel.playSound(null, player.blockPosition(), BlocklingsSounds.BLOCKLING_WHISTLE.get(), SoundSource.PLAYERS, 1.0f, 1.5f);
                        }
                    }
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public void inventoryTick(@Nonnull ItemStack stack, @Nonnull Level world, @Nonnull Entity entity, int something, boolean somethingElse)
    {
        super.inventoryTick(stack, world, entity, something, somethingElse);

        if (stack.hasTag() && stack.getTag().contains(BLOCKLING_UUID_KEY))
        {
            if (world.isClientSide)
            {
                BlocklingEntity blockling = findBlockling(stack, (ClientLevel) world);

                if (blockling != null)
                {
                    addStackToMap(blockling, stack);
                }

                String name = findBlocklingName(stack, (ClientLevel) world);

                if (name != null)
                {
                    stack.getTag().putString(BLOCKLING_NAME_KEY, name);
                }
            }
            else
            {
                BlocklingEntity blockling = (BlocklingEntity) ((ServerLevel) world).getEntity(stack.getTag().getUUID(BLOCKLING_UUID_KEY));

                if (blockling != null)
                {
                    addStackToMap(blockling, stack);

                    stack.getTag().putString(BLOCKLING_NAME_KEY, blockling.getCustomName().getString());
                }
            }
        }
    }

    /**
     * Finds the blockling in the world.
     *
     * @param stack the stack to find the blockling for.
     * @param world the client world.
     * @return the blockling.
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    private BlocklingEntity findBlockling(@Nonnull ItemStack stack, @Nonnull ClientLevel world)
    {
        if (stack.hasTag() && stack.getTag().contains(BLOCKLING_UUID_KEY))
        {
            Entity blockling = null;

            for (Entity entity : world.entitiesForRendering())
            {
                if (entity.getUUID().equals(stack.getTag().getUUID(BLOCKLING_UUID_KEY)))
                {
                    blockling = entity;
                    break;
                }
            }

            return blockling instanceof BlocklingEntity ? (BlocklingEntity) blockling : null;
        }

        return null;
    }

    /**
     * Finds the blockling's name on the client.
     *
     * @param stack the stack to find the name for.
     * @param world the client world.
     * @return the blockling's name.
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    private String findBlocklingName(@Nonnull ItemStack stack, @Nonnull ClientLevel world)
    {
        BlocklingEntity blockling = findBlockling(stack, world);

        if (blockling != null)
        {
            Component customName = blockling.getCustomName();

            if (customName != null)
            {
                return customName.getString();
            }
        }

        return null;
    }

    /**
     * Finds the blockling's location on the client.
     *
     * @param stack the stack to find the location for.
     * @param world the client world.
     * @return the blockling's location string.
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    private String findBlocklingLocation(@Nonnull ItemStack stack, @Nonnull ClientLevel world)
    {
        BlocklingEntity blockling = findBlockling(stack, world);

        if (blockling != null)
        {
            return String.format("%d %d %d", (int) blockling.getX(), (int) blockling.getY(), (int) blockling.getZ());
        }

        return null;
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack)
    {
        if (stack.hasTag() && stack.getTag().contains(BLOCKLING_NAME_KEY))
        {
            return Component.literal(super.getName(stack).getString() + " (" + stack.getTag().getString(BLOCKLING_NAME_KEY) + ")").withStyle(ChatFormatting.LIGHT_PURPLE);
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag)
    {
        if (world != null)
        {
            String location = findBlocklingLocation(stack, (ClientLevel) world);

            if (location != null)
            {
                tooltip.add(Component.literal(Component.translatable(getDescriptionId() + ".location").getString() + location).withStyle(ChatFormatting.GRAY));
            }
        }

        super.appendHoverText(stack, world, tooltip, flag);
    }
}

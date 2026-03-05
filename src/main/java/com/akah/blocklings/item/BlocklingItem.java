package com.akah.blocklings.item;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.entity.BlocklingsEntityTypes;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.BlocklingType;
import com.akah.blocklings.entity.blockling.attribute.BlocklingAttributes;
import com.akah.blocklings.entity.blockling.task.BlocklingTasks;
import com.akah.blocklings.entity.blockling.task.Task;
import com.akah.blocklings.util.BlocklingsResourceLocation;
import com.akah.blocklings.util.BlocklingsComponent;
import com.akah.blocklings.util.ObjectUtil;
import com.akah.blocklings.util.Version;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * An item used to spawn blocklings with data preserved.
 */
public class BlocklingItem extends Item
{
    /**
     * The default constructor.
     */
    public BlocklingItem()
    {
        super(new Item.Properties()
                .stacksTo(1));
    }

    /**
     * Creates a blockling item from a blockling.
     *
     * @param blockling the blockling to create the item from.
     * @return the blockling item.
     */
    @Nonnull
    public static ItemStack create(@Nonnull BlocklingEntity blockling)
    {
        ItemStack stack = new ItemStack(BlocklingsItems.BLOCKLING.get(), 1);
        stack.setHoverName(Component.literal(blockling.getCustomName().getString()).withStyle(ChatFormatting.GOLD));

        CompoundTag stackTag = stack.getOrCreateTag();

        CompoundTag entityTag = new CompoundTag();
        blockling.addAdditionalSaveData(entityTag);
        stackTag.put("entity", entityTag);

        stackTag.putString("custom_name", blockling.getCustomName().getString());
        stackTag.putInt("health", blockling.getStats().getHealth());
        stackTag.putInt("max_health", blockling.getStats().getMaxHealth());
        stackTag.putInt("combat_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.COMBAT).getValue());
        stackTag.putInt("mining_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.MINING).getValue());
        stackTag.putInt("woodcutting_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.WOODCUTTING).getValue());
        stackTag.putInt("farming_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.FARMING).getValue());
        stackTag.putInt("total_level", blockling.getStats().getLevelAttribute(BlocklingAttributes.Level.TOTAL).getValue());

        return stack;
    }

    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level world = context.getLevel();

        if (!world.isClientSide)
        {
            ItemStack stack = context.getItemInHand();
            BlockPos blockpos = context.getClickedPos();
            Direction direction = context.getClickedFace();
            BlockState blockstate = world.getBlockState(blockpos);

            if (!blockstate.getCollisionShape(world, blockpos).isEmpty())
            {
                blockpos = blockpos.relative(direction);
            }

            BlocklingEntity blockling = new BlocklingEntity(BlocklingsEntityTypes.BLOCKLING.get(), world);

            CompoundTag stackTag = stack.getTag();
            CompoundTag entityTag = null;

            if (stackTag != null && stackTag.contains("entity"))
            {
                entityTag = stackTag.getCompound("entity");
            }

            blockling.finalizeSpawn((ServerLevelAccessor) world, world.getCurrentDifficultyAt(blockpos), MobSpawnType.SPAWN_EGG, null, entityTag);

            if (entityTag == null || !entityTag.contains("blockling"))
            {
                for (Task task : blockling.getTasks().getPrioritisedTasks())
                {
                    if (task.isConfigured() && task.getType() == BlocklingTasks.WANDER)
                    {
                        task.setType(BlocklingTasks.FOLLOW, false);
                    }
                }
            }

            blockling.setPos(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5);
            blockling.tame(context.getPlayer());

            if (stack.getTag() != null)
            {
                if (stack.getTag().contains("custom_name"))
                {
                    blockling.setCustomName(Component.literal(stack.getTag().getString("custom_name")));
                }
            }

            world.addFreshEntity(blockling);

            if (!context.getPlayer().getAbilities().instabuild)
            {
                stack.shrink(1);
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag)
    {
        CompoundTag stackTag = stack.getTag();

        if (stackTag != null && stackTag.contains("entity"))
        {
            tooltip.add(Component.literal(new BlocklingsComponent("attribute.health.name").getString() + ": " + stackTag.getInt("health") + "/" + stackTag.getInt("max_health")).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal(new BlocklingsComponent("attribute.combat_level.name").getString() + ": " + stackTag.getInt("combat_level")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(new BlocklingsComponent("attribute.mining_level.name").getString() + ": " + stackTag.getInt("mining_level")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(new BlocklingsComponent("attribute.woodcutting_level.name").getString() + ": " + stackTag.getInt("woodcutting_level")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(new BlocklingsComponent("attribute.farming_level.name").getString() + ": " + stackTag.getInt("farming_level")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(new BlocklingsComponent("attribute.total_level.name").getString() + ": " + stackTag.getInt("total_level")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.empty());
        }

        super.appendHoverText(stack, world, tooltip, flag);
    }

    public static void registerItemModelsProperties()
    {
        ItemProperties.register(BlocklingsItems.BLOCKLING.get(), new BlocklingsResourceLocation("type"), (stack, world, entity, seed) ->
            {
                CompoundTag stackTag = stack.getTag();

                if (stackTag != null)
                {
                    CompoundTag entityTag = stackTag.getCompound("entity");

                    if (entityTag != null)
                    {
                        CompoundTag blocklingTag = entityTag.getCompound("blockling");

                        if (blocklingTag != null)
                        {
                            return BlocklingType.TYPES.indexOf(BlocklingType.find(blocklingTag.getString("type"), ObjectUtil.coalesce(new Version(blocklingTag.getString("blocklings_version")), Blocklings.VERSION)));
                        }
                    }
                }

                return 0;
            });
    }
}

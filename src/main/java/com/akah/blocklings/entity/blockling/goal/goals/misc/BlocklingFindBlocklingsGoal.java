package com.akah.blocklings.entity.blockling.goal.goals.misc;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingTargetGoal;
import com.akah.blocklings.entity.blockling.task.BlocklingTasks;
import com.akah.blocklings.entity.blockling.task.config.range.IntRangeProperty;
import com.akah.blocklings.util.BlocklingsComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Finds nearby blocklings of a certain type and leads the owner to them.
 */
public class BlocklingFindBlocklingsGoal extends BlocklingTargetGoal<BlocklingEntity>
{
    /**
     * The radius in chunks to search for blocklings.
     */
    @Nonnull
    public final IntRangeProperty chunkRangeProperty;

    /**
     * The time in ticks when the blockling last jumped.
     */
    private int lastJumpTicks = 0;

    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingFindBlocklingsGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        properties.add(chunkRangeProperty = new IntRangeProperty(
                "4142fcf1-8af9-4993-bf8d-5369ad58fe8d", this,
                new BlocklingsComponent("task.property.chunk_range.name"),
                new BlocklingsComponent("task.property.chunk_range.desc"),
                1, 6, 3));
    }

    @Override
    public boolean canUse()
    {
        LivingEntity owner = blockling.getOwner();

        if (owner == null)
        {
            return false;
        }
        else if (owner.distanceToSqr(blockling) > 16 * 16)
        {
            return false;
        }

        if (!super.canUse())
        {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!super.canContinueToUse())
        {
            return false;
        }

        return true;
    }

    @Override
    public void start()
    {
        super.start();
    }

    @Override
    public void stop()
    {
        super.stop();
    }

    @Override
    public void recalcTarget()
    {
        final int chunkRange = chunkRangeProperty.getValue();
        final double radius = chunkRange * 16.0;

        AABB searchBox = AABB.ofSize(blockling.position(), radius * 2.0, world.getHeight(), radius * 2.0);
        List<BlocklingEntity> blocklings = world.getEntitiesOfClass(BlocklingEntity.class, searchBox, this::isValidTarget);

        BlocklingEntity closestBlockling = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (BlocklingEntity candidate : blocklings)
        {
            double distanceSq = blockling.distanceToSqr(candidate);
            if (distanceSq < closestDistanceSq)
            {
                closestBlockling = candidate;
                closestDistanceSq = distanceSq;
            }
        }

        setTarget(closestBlockling);
    }

    @Override
    protected void checkForAndHandleInvalidTargets()
    {
        if (!isTargetValid())
        {
            markTargetBad();
        }
    }

    @Override
    public void markEntireTargetBad()
    {
        markTargetBad();
    }

    @Override
    public boolean isValidTarget(@Nullable BlocklingEntity target)
    {
        if (target != null)
        {
            if (target.getNaturalBlocklingType() != blockling.getBlocklingType())
            {
                return false;
            }
            else if (target.getOwner() != null && target.getOwner() == blockling.getOwner())
            {
                return false;
            }
            else if (target.isDeadOrDying())
            {
                return false;
            }
            else if (blockling.distanceToSqr(target) < getPathTargetRangeSq())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void tickGoal()
    {
        if (blockling.tickCount - lastJumpTicks > 100)
        {
            if (blockling.getY() + 2 < getTarget().getY())
            {
                blockling.setDeltaMovement(blockling.getDeltaMovement().add(0, 0.5, 0));
            }

            lastJumpTicks = blockling.tickCount;
        }
    }

    @Override
    protected void recalcPathTargetPosAndPath(boolean force)
    {
        trySetPathTarget(getTarget().blockPosition(), null);
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        return true;
    }

    @Override
    public int getRecalcInterval()
    {
        return 60;
    }

    @Override
    public int getPathRecalcInterval()
    {
        return 40;
    }

    @Override
    public float getPathTargetRangeSq()
    {
        return 8;
    }
}

package com.akah.blocklings.entity.blockling.goal.goals.misc;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.entity.blockling.task.BlocklingTasks;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Sets the blockling to sit.
 */
public class BlocklingSitGoal extends BlocklingGoal
{
    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingSitGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse()
    {
        if (!super.canUse())
        {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse()
    {
        return super.canUse();
    }

    @Override
    public void start()
    {
        super.start();
        blockling.getNavigation().stop();
        blockling.setInSittingPose(true);
        blockling.setOrderedToSit(true);
    }

    @Override
    public void tick()
    {
        super.tick();
        blockling.getNavigation().stop();
    }

    @Override
    public void stop()
    {
        super.stop();
        blockling.setInSittingPose(false);
        blockling.setOrderedToSit(false);
    }
}

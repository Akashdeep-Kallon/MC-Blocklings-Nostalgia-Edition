package com.akah.blocklings.entity.blockling.goal.goals.misc;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.entity.blockling.task.BlocklingTasks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Allows the blockling to wander aimlessly.
 */
public class BlocklingWanderGoal extends BlocklingGoal
{
    /**
     * The instance of the vanilla wander goal.
     */
    @Nonnull
    private final WaterAvoidingRandomStrollGoal vanillaWanderGoal;

    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingWanderGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        vanillaWanderGoal = new WaterAvoidingRandomStrollGoal(blockling, 1.0);

        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        if (blockling.isOrderedToSit())
        {
            return false;
        }

        if (!super.canUse())
        {
            return false;
        }

        return !blockling.isOrderedToSit() && vanillaWanderGoal.canUse();
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!super.canContinueToUse())
        {
            return false;
        }

        return !blockling.isOrderedToSit() && vanillaWanderGoal.canContinueToUse();
    }

    @Override
    public void start()
    {
        super.start();

        vanillaWanderGoal.start();
    }

    @Override
    public void stop()
    {
        super.stop();

        vanillaWanderGoal.stop();
    }

    @Override
    public void tick()
    {
        super.tick();

        vanillaWanderGoal.tick();
    }
}

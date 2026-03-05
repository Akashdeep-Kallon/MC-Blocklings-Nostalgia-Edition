package com.akah.blocklings.entity.blockling.goal.goals.misc;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.entity.ai.goal.PanicGoal;

import javax.annotation.Nonnull;

/**
 * Makes wild (untamed) blocklings flee after taking damage, similar to passive animals.
 */
public class BlocklingWildPanicGoal extends PanicGoal
{
    @Nonnull
    private final BlocklingEntity blockling;

    public BlocklingWildPanicGoal(@Nonnull BlocklingEntity blockling, double speedModifier)
    {
        super(blockling, speedModifier);

        this.blockling = blockling;
    }

    @Override
    public boolean canUse()
    {
        return !blockling.isTame() && super.canUse();
    }

    @Override
    public boolean canContinueToUse()
    {
        return !blockling.isTame() && super.canContinueToUse();
    }
}

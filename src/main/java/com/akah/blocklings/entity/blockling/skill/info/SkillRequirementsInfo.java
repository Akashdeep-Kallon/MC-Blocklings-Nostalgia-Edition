package com.akah.blocklings.entity.blockling.skill.info;

import com.akah.blocklings.entity.blockling.attribute.BlocklingAttributes;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Info regarding a skill's requirements.
 */
public class SkillRequirementsInfo
{
    /**
     * The levels required to buy a skill.
     */
    @Nonnull
    public final Map<BlocklingAttributes.Level, Integer> levels;

    /**
     * @param levels the levels required to buy a skill.
     */
    public SkillRequirementsInfo(@Nonnull Map<BlocklingAttributes.Level, Integer> levels)
    {
        this.levels = levels;
    }
}

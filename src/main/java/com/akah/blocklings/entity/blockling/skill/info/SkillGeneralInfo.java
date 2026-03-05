package com.akah.blocklings.entity.blockling.skill.info;

import com.akah.blocklings.entity.blockling.skill.Skill;
import com.akah.blocklings.util.BlocklingsComponent;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

/**
 * Info regarding the general properties of a skill.
 */
public class SkillGeneralInfo
{
    /**
     * The skill's type.
     */
    @Nonnull
    public final Skill.Type type;

    /**
     * The skill's name's translation text component.
     */
    @Nonnull
    public final Component name;

    /**
     * The skill's description's translation text component.
     */
    @Nonnull
    public final Component desc;

    /**
     * @param type the skill's type.
     * @param key the skill's key.
     */
    public SkillGeneralInfo(@Nonnull Skill.Type type, @Nonnull String key)
    {
        this.type = type;
        this.name = new SkillComponent(key + ".name");
        this.desc = new SkillComponent(key + ".desc");
    }

    /**
     * A blocklings translation text component for skills.
     */
    public static class SkillComponent extends BlocklingsComponent
    {
        /**
         * @param key the skill's key.
         */
        public SkillComponent(String key)
        {
            super("skill." + key);
        }
    }
}

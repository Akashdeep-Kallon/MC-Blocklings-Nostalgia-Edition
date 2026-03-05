package com.akah.blocklings.entity.blockling.skill.info;

import com.akah.blocklings.client.gui.texture.Texture;
import com.akah.blocklings.client.gui.texture.Textures;
import com.akah.blocklings.util.BlocklingsComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Info regarding a skill group.
 */
public class SkillGroupInfo
{
    /**
     * The skill group's id.
     */
    @Nonnull
    public final UUID id;

    /**
     * The skill group's key.
     */
    @Nonnull
    public final String key;

    /**
     * The skill group's background texture.
     */
    @Nonnull
    public final Textures.Skills.Tiles backgroundTexture;

    /**
     * The skill group's gui title.
     */
    @Nonnull
    public final Component guiTitle;

    /**
     * @param id the skill group's id in string form.
     * @param key the skill group's key.
     * @param backgroundTexture the skill group's background texture.
     */
    public SkillGroupInfo(@Nonnull String id, @Nonnull String key, @Nonnull Textures.Skills.Tiles backgroundTexture)
    {
        this.id = UUID.fromString(id);
        this.key = key;
        this.backgroundTexture = backgroundTexture;
        this.guiTitle = new BlocklingsComponent("skill_group." + key + ".gui_title");
    }
}

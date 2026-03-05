package com.akah.blocklings.entity.blockling.task;

import com.akah.blocklings.client.gui.texture.Texture;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.util.BlocklingsComponent;
import com.akah.blocklings.util.TriFunction;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class TaskType
{
    public final UUID id;
    public final Component name;
    public final Component desc;
    public final boolean isUnlockedByDefault;
    public final boolean isActiveByDefault;
    public final Texture texture;
    public final TriFunction<UUID, BlocklingEntity, BlocklingTasks, BlocklingGoal> createGoal;

    public TaskType(String id, String key, boolean unlockedByDefault, boolean activeByDefault, Texture texture, TriFunction<UUID, BlocklingEntity, BlocklingTasks, BlocklingGoal> createGoal)
    {
        this.id = UUID.fromString(id);
        this.name = new GoalComponent(key + ".name");
        this.desc = new GoalComponent(key + ".desc");
        this.isUnlockedByDefault = unlockedByDefault;
        this.isActiveByDefault = activeByDefault;
        this.texture = texture;
        this.createGoal = createGoal;
    }

    public class GoalComponent extends BlocklingsComponent
    {
        public GoalComponent(String key)
        {
            super("task." + key);
        }
    }

    @Override
    public String toString()
    {
        return name.getString();
    }
}

package com.akah.blocklings.network.messages;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.network.BlocklingMessage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A message used to sync something between a goal on the client/server.
 */
public abstract class GoalMessage<T extends BlocklingMessage<T>, G> extends BlocklingMessage<T>
{
    /**
     * The task id associated with the goal.
     */
    private UUID taskId;

    /**
     * Empty constructor used ONLY for decoding.
     */
    public GoalMessage()
    {
        super(null);
    }

    /**
     * @param blockling the blockling associated with the goal.
     * @param taskId the task id associated with the goal.
     */
    public GoalMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId)
    {
        super(blockling);
        this.taskId = taskId;
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        buf.writeUUID(taskId);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        taskId = buf.readUUID();
    }

    @Override
    protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling)
    {
        if (blockling.getTasks().getTask(taskId) == null)
        {
            Blocklings.LOGGER.warn("[fix] motivo: se ignoró GoalMessage porque la tarea {} ya no existe en {}.", taskId, blockling.getUUID());
            return;
        }

        BlocklingGoal goal = blockling.getTasks().getTask(taskId).getGoal();

        if (goal == null)
        {
            Blocklings.LOGGER.warn("[fix] motivo: se ignoró GoalMessage porque la tarea {} no tiene goal activo en {}.", taskId, blockling.getUUID());
            return;
        }

        try
        {
            handle(player, blockling, (G) goal);
        }
        catch (ClassCastException e)
        {
            Blocklings.LOGGER.error("Error handling goal message: " + e.getMessage());
        }
    }

    /**
     * Handles the message.
     *
     * @param player the player.
     * @param blockling the blockling.
     * @param goal the goal.
     */
    protected abstract void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling, @Nonnull G goal);
}

package com.akah.blocklings.network.messages;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.network.BlocklingMessage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.UUID;

public class TaskRemoveMessage extends BlocklingMessage<TaskRemoveMessage>
{
    /**
     * The task id.
     */
    private UUID taskId;

    /**
     * Empty constructor used ONLY for decoding.
     */
    public TaskRemoveMessage()
    {
        super(null);
    }

    /**
     * @param blockling the blockling.
     * @param taskId the task id.
     */
    public TaskRemoveMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId)
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
        blockling.getTasks().removeTask(taskId, false);
    }
}

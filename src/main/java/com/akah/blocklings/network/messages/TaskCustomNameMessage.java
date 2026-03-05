package com.akah.blocklings.network.messages;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.network.BlocklingMessage;
import com.akah.blocklings.util.FriendlyByteBufUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.UUID;

public class TaskCustomNameMessage extends BlocklingMessage<TaskCustomNameMessage>
{
    /**
     * The task id.
     */
    private UUID taskId;

    /**
     * The custom task name.
     */
    private String customName;

    /**
     * Empty constructor used ONLY for decoding.
     */
    public TaskCustomNameMessage()
    {
        super(null);
    }

    /**
     * @param blockling the blockling.
     * @param taskId the task id.
     * @param customName the custom task name.
     */
    public TaskCustomNameMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId, @Nonnull String customName)
    {
        super(blockling);
        this.taskId = taskId;
        this.customName = customName;
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        buf.writeUUID(taskId);
        FriendlyByteBufUtils.writeString(buf, customName);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        taskId = buf.readUUID();
        customName = FriendlyByteBufUtils.readString(buf);
    }

    @Override
    protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling)
    {
        blockling.getTasks().getTask(taskId).setCustomName(customName, false);
    }
}

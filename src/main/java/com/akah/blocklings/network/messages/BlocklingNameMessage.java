package com.akah.blocklings.network.messages;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.network.BlocklingMessage;
import com.akah.blocklings.util.FriendlyByteBufUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A message used to sync the blockling's name from the client to the server.
 */
public class BlocklingNameMessage extends BlocklingMessage<BlocklingNameMessage>
{
    /**
     * The blockling's name.
     */
    @Nonnull
    private String name = "";

    /**
     * Empty constructor used ONLY for decoding.
     */
    public BlocklingNameMessage()
    {
        super(null);
    }

    /**
     * @param blockling the blockling.
     * @param name the blockling's name.
     */
    public BlocklingNameMessage(@Nonnull BlocklingEntity blockling, @Nullable Component name)
    {
        super(blockling, false);
        this.name = name == null ? "" : name.getString();
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        FriendlyByteBufUtils.writeString(buf, name);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        name = FriendlyByteBufUtils.readString(buf);
    }

    @Override
    protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling)
    {
        blockling.setCustomName(name.equals("") ? null : Component.literal(name), false);
    }
}

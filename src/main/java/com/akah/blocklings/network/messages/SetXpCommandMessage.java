package com.akah.blocklings.network.messages;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.attribute.BlocklingAttributes.Level;
import com.akah.blocklings.network.Message;
import com.akah.blocklings.util.FriendlyByteBufUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public class SetXpCommandMessage extends Message
{
    /**
     * The level.
     */
    private Level level;

    /**
     * The value to set the level's xp to.
     */
    private int value;

    /**
     * @param level the level.
     * @param value the value to set the level's xp to.
     */
    public SetXpCommandMessage(@Nonnull Level targetLevel, int value)
    {
        this.level = targetLevel;
        this.value = value;
    }

    /**
     * Encodes the message.
     *
     * @param buf the buffer to encode to.
     */
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        FriendlyByteBufUtils.writeString(buf, level.name());
        buf.writeInt(value);
    }

    /**
     * Decodes and returns the message.
     *
     * @param buf the buffer to decode from.
     */
    @Nonnull
    public static SetXpCommandMessage decode(@Nonnull FriendlyByteBuf buf)
    {
        String levelName = FriendlyByteBufUtils.readString(buf);
        int requestedValue = buf.readInt();

        try
        {
            Level requestedLevel = Level.valueOf(levelName);
            int clampedValue = Mth.clamp(requestedValue, 0, Integer.MAX_VALUE);

            // [final-release-1.20.1] motivo: endurece decode frente a payloads manipulados y evita xp negativa/inválida.
            return new SetXpCommandMessage(requestedLevel, clampedValue);
        }
        catch (IllegalArgumentException ignored)
        {
            // [final-release-1.20.1] motivo: fallback seguro ante enums inválidos para no romper cliente/servidor por paquetes corruptos.
            return new SetXpCommandMessage(Level.COMBAT, 0);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx)
    {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() ->
        {
            boolean isClient = context.getDirection() == NetworkDirection.PLAY_TO_CLIENT;

            Player player = isClient ? getClientPlayer() : context.getSender();
            Objects.requireNonNull(player, "No player entity found when handling message.");

            if (isClient)
            {
                Entity entity = Minecraft.getInstance().crosshairPickEntity;

                if (entity instanceof BlocklingEntity)
                {
                    BlocklingEntity blockling = (BlocklingEntity) entity;

                    if (level == Level.TOTAL)
                    {
                        // [audit-1.20.1] motivo: evita alocaciones de Stream/List en un handler de red invocado frecuentemente.
                        for (Level level : Level.NON_TOTAL_VALUES)
                        {
                            blockling.getStats().getLevelXpIntAttribute(level).setValue(value);
                        }
                    }
                    else
                    {
                        blockling.getStats().getLevelXpIntAttribute(level).setValue(value);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}

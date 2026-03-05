package com.akah.blocklings.network;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class BlocklingMessage<T extends BlocklingMessage<T>> extends Message
{
    @Nullable
    protected BlocklingEntity blockling;

    protected int blocklingId;

    @Nonnull
    private UUID clientPlayerId = new UUID(0L, 0L);

    private boolean syncBackToClients = true;

    protected BlocklingMessage(@Nullable BlocklingEntity blockling)
    {
        this.blockling = blockling;

        if (blockling != null)
        {
            blocklingId = blockling.getId();

            if (blockling.level().isClientSide())
            {
                clientPlayerId = getClientPlayerId();
            }
        }
    }

    protected BlocklingMessage(@Nullable BlocklingEntity blockling, boolean syncBackToClients)
    {
        this(blockling);
        this.syncBackToClients = syncBackToClients;
    }

    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        buf.writeInt(blocklingId);
        buf.writeUUID(clientPlayerId);
        buf.writeBoolean(syncBackToClients);
    }

    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        blocklingId = buf.readInt();
        clientPlayerId = buf.readUUID();
        syncBackToClients = buf.readBoolean();
    }

    public void handle(@Nonnull Supplier<NetworkEvent.Context> ctx)
    {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() ->
        {
            boolean isClient = context.getDirection() == NetworkDirection.PLAY_TO_CLIENT;
            Player player = isClient ? getClientPlayer() : context.getSender();
            Objects.requireNonNull(player, "No player entity found when handling message.");

            Entity entity = player.level().getEntity(blocklingId);

            if (!(entity instanceof BlocklingEntity foundBlockling))
            {
                // [audit-1.20.1] motivo: protege el handler de paquetes malformados o ids stale en multiplayer.
                Blocklings.LOGGER.debug("Ignoring {} for non-blockling entity id {}", getClass().getSimpleName(), blocklingId);
                return;
            }

            blockling = foundBlockling;
            handle(player, blockling);

            if (!isClient && syncBackToClients)
            {
                // [audit-1.20.1] motivo: evita crear listas/streams por paquete al excluir emisor en rebroadcast.
                sendToAllClients(clientPlayerId);
            }
        });

        context.setPacketHandled(true);
    }

    protected abstract void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling);

    public void sync()
    {
        NetworkHandler.sync(blockling.level(), this);
    }

    public void sendToServer()
    {
        NetworkHandler.sendToServer(this);
    }

    public void sendToClient(Player player)
    {
        NetworkHandler.sendToClient(player, this);
    }

    public void sendToAllClients(List<Player> playersToIgnore)
    {
        NetworkHandler.sendToAllClients(blockling.level(), this, playersToIgnore);
    }

    public void sendToAllClients(@Nullable UUID playerIdToIgnore)
    {
        NetworkHandler.sendToAllClients(blockling.level(), this, playerIdToIgnore);
    }
}

package com.akah.blocklings.network;

import com.akah.blocklings.client.network.NetworkClientAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class Message
{
    public abstract void handle(Supplier<NetworkEvent.Context> ctx);

    /**
     * @return the uuid of the client player.
     */
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    protected UUID getClientPlayerId()
    {
        // [audit-1.20.1] motivo: evita referencia directa a Minecraft client en código común de networking.
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> NetworkClientAccess::getClientPlayerId);
    }

    /**
     * @return the client player.
     */
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    protected Player getClientPlayer()
    {
        // [audit-1.20.1] motivo: evita referencia directa a Minecraft client en código común de networking.
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> NetworkClientAccess::getClientPlayer);
    }
}

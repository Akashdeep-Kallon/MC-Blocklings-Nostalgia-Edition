package com.akah.blocklings.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class NetworkClientAccess
{
    private NetworkClientAccess()
    {
    }

    @Nonnull
    public static UUID getClientPlayerId()
    {
        // [audit-1.20.1] motivo: encapsula acceso client-only para evitar dependencias directas de clases comunes con Minecraft client.
        return Minecraft.getInstance().player.getUUID();
    }

    @Nonnull
    public static Player getClientPlayer()
    {
        // [audit-1.20.1] motivo: encapsula acceso client-only para evitar dependencias directas de clases comunes con Minecraft client.
        return Minecraft.getInstance().player;
    }
}

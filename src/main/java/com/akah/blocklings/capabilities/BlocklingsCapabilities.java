package com.akah.blocklings.capabilities;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nonnull;

/**
 * Handles the registration of capabilities.
 */
public class BlocklingsCapabilities
{
    /**
     * Registers all capabilities.
     */
    public static void register(@Nonnull RegisterCapabilitiesEvent event)
    {
        event.register(BlockSelectCapability.class);
    }
}

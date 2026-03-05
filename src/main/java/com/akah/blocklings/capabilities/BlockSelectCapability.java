package com.akah.blocklings.capabilities;

import com.akah.blocklings.Blocklings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A capability used to select a block from the world.
 */
@Mod.EventBusSubscriber(modid = Blocklings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlockSelectCapability
{
    public static final Capability<BlockSelectCapability> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    /**
     * Whether the player is currently selecting a block.
     */
    public boolean isSelecting = false;

    @SubscribeEvent
    public static void attachCapabilities(@Nonnull AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            BlockSelectCapability.Provider provider = new BlockSelectCapability.Provider();
            event.addCapability(ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "block_select_capability"), provider);
            event.addListener(provider::invalidate);
        }
    }

    /**
     * A provider for this capability.
     */
    public static class Provider implements ICapabilityProvider
    {
        /**
         * The default instance of this capability.
         */
        @Nonnull
        private final BlockSelectCapability defaultInstance = new BlockSelectCapability();

        /**
         * The lazy optional of this capability.
         */
        @Nonnull
        private final LazyOptional<BlockSelectCapability> lazyOptional = LazyOptional.of(() -> defaultInstance);

        /**
         * Invalidates the lazy optional.
         */
        public void invalidate()
        {
            lazyOptional.invalidate();
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
        {
            return cap == CAPABILITY ? lazyOptional.cast() : LazyOptional.empty();
        }
    }

}

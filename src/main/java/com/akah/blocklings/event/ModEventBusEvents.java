package com.akah.blocklings.event;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.entity.BlocklingsEntityTypes;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;

/**
 * Handles common mod bus events.
 */
@Mod.EventBusSubscriber(modid = Blocklings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents
{
    /**
     * Adds the additional attributes a blockling needs to function.
     */
    @SubscribeEvent
    public static void addEntityAttributes(@Nonnull EntityAttributeCreationEvent event)
    {
        event.put(BlocklingsEntityTypes.BLOCKLING.get(), BlocklingEntity.createAttributes().build());
    }
}

package com.akah.blocklings.entity;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;

/**
 * Compatibility alias for entity registrations in Forge 1.20.1.
 */
public class ModEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = BlocklingsEntityTypes.ENTITY_TYPES;

    public static final RegistryObject<EntityType<BlocklingEntity>> BLOCKLING = BlocklingsEntityTypes.BLOCKLING;

    public static void register(@Nonnull IEventBus modEventBus)
    {
        BlocklingsEntityTypes.register(modEventBus);
    }
}

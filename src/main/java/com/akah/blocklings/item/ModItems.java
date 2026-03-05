package com.akah.blocklings.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;

/**
 * Compatibility alias for item registrations in Forge 1.20.1.
 */
public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = BlocklingsItems.ITEMS;

    public static final RegistryObject<Item> BLOCKLING = BlocklingsItems.BLOCKLING;
    public static final RegistryObject<Item> BLOCKLING_WHISTLE = BlocklingsItems.BLOCKLING_WHISTLE;
    public static final RegistryObject<Item> BLOCKLING_SPAWN_EGG = BlocklingsItems.BLOCKLING_SPAWN_EGG;
    public static final RegistryObject<Item> VITAL_CORE = BlocklingsItems.VITAL_CORE;

    public static void register(@Nonnull IEventBus modEventBus)
    {
        BlocklingsItems.register(modEventBus);
    }
}

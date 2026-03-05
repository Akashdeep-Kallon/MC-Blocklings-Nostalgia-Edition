package com.akah.blocklings.entity;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

/**
 * Handles all the added entity types.
 */
public class BlocklingsEntityTypes
{
    /**
     * The deferred register to register the entity type.
     */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Blocklings.MODID);

    /**
     * The blockling entity type.
     */
    public static final RegistryObject<EntityType<BlocklingEntity>> BLOCKLING = ENTITY_TYPES.register("blockling",
        () -> EntityType.Builder.of(BlocklingEntity::new, MobCategory.CREATURE)
                                .sized(1.0f, 1.0f)
                                .build(Blocklings.MODID + ":blockling"));

    /**
     * Registers the entity types.
     *
     * @param modEventBus the mod event bus.
     */
    public static void register(@Nonnull IEventBus modEventBus)
    {
        ENTITY_TYPES.register(modEventBus);
    }
}

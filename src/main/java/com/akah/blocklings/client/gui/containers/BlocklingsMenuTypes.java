package com.akah.blocklings.client.gui.containers;

import com.akah.blocklings.Blocklings;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;

/**
 * Handles menu type registration.
 */
public final class BlocklingsMenuTypes
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Blocklings.MODID);

    public static final RegistryObject<MenuType<EquipmentContainer>> EQUIPMENT = MENU_TYPES.register("equipment", () -> IForgeMenuType.create((windowId, playerInventory, data) ->
    {
        int blocklingId = data.readInt();

        return new EquipmentContainer(windowId, playerInventory.player, playerInventory.player.level().getEntity(blocklingId) instanceof com.akah.blocklings.entity.blockling.BlocklingEntity blockling ? blockling : null);
    }));

    private BlocklingsMenuTypes()
    {

    }

    public static void register(@Nonnull IEventBus modEventBus)
    {
        MENU_TYPES.register(modEventBus);
    }
}

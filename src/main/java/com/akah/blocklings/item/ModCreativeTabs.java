package com.akah.blocklings.item;

import com.akah.blocklings.Blocklings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;

public class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Blocklings.MODID);

    public static final RegistryObject<CreativeModeTab> BLOCKLINGS_TAB = CREATIVE_TABS.register("blocklings", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(BlocklingsItems.BLOCKLING.get()))
            .title(Component.translatable("itemGroup.blocklings.blocklings"))
            .displayItems((params, output) -> {
                output.accept(BlocklingsItems.BLOCKLING.get());
                output.accept(BlocklingsItems.BLOCKLING_WHISTLE.get());
                output.accept(BlocklingsItems.BLOCKLING_SPAWN_EGG.get());
                output.accept(BlocklingsItems.VITAL_CORE.get());
            })
            .build());

    public static void register(@Nonnull IEventBus modEventBus)
    {
        CREATIVE_TABS.register(modEventBus);
    }
}

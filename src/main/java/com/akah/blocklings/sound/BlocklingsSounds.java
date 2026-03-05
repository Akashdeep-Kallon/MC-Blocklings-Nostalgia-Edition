package com.akah.blocklings.sound;

import com.akah.blocklings.Blocklings;
import com.akah.blocklings.util.BlocklingsResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class BlocklingsSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Blocklings.MODID);

    public static final RegistryObject<SoundEvent> BLOCKLING_WHISTLE = SOUNDS.register("blockling_whistle",
            () -> SoundEvent.createVariableRangeEvent(new BlocklingsResourceLocation("blockling_whistle")));

    public static void register(IEventBus modEventBus)
    {
        SOUNDS.register(modEventBus);
    }
}

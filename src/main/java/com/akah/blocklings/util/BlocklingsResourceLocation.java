package com.akah.blocklings.util;

import com.akah.blocklings.Blocklings;
import net.minecraft.resources.ResourceLocation;

public class BlocklingsResourceLocation extends ResourceLocation
{
    @SuppressWarnings("removal")
    public BlocklingsResourceLocation(String path)
    {
        super(Blocklings.MODID, path);
    }
}

package com.akah.blocklings.util;

import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;

public class FriendlyByteBufUtils
{
    public static String readString(FriendlyByteBuf buf)
    {
        return buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8).toString();
    }

    public static void writeString(FriendlyByteBuf buf, String string)
    {
        buf.writeInt(string.getBytes(StandardCharsets.UTF_8).length);
        buf.writeCharSequence(string, StandardCharsets.UTF_8);
    }
}

package com.mundosdeaxelis.cinematic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Un subtítulo que aparece entre los ticks "start" y "end" de la cinemática. */
public record Subtitle(int start, int end, Component text) {

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(start);
        buf.writeVarInt(end);
        buf.writeComponent(text);
    }

    public static Subtitle read(FriendlyByteBuf buf) {
        int start = buf.readVarInt();
        int end = buf.readVarInt();
        Component text = buf.readComponent();
        return new Subtitle(start, end, text);
    }
}

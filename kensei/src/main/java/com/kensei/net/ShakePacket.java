package com.kensei.net;

import com.kensei.client.ClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: sacudir la cámara (al conectar un golpe o al recibirlo). */
public class ShakePacket {
    public final float strength;
    public final int ticks;

    public ShakePacket(float strength, int ticks) {
        this.strength = strength;
        this.ticks = ticks;
    }

    public static void encode(ShakePacket m, FriendlyByteBuf buf) {
        buf.writeFloat(m.strength);
        buf.writeVarInt(m.ticks);
    }

    public static ShakePacket decode(FriendlyByteBuf buf) {
        return new ShakePacket(buf.readFloat(), buf.readVarInt());
    }

    public static void handle(ShakePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEffects.shake(m.strength, m.ticks)));
        c.setPacketHandled(true);
    }
}

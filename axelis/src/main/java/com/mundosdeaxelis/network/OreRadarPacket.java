package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador: lista de minerales detectados por el Pico Prospector (se dibujan con brillo a través de las paredes). */
public class OreRadarPacket {
    public final int ticks;
    public final long[] pos;
    public final int[] colors;

    public OreRadarPacket(int ticks, long[] pos, int[] colors) {
        this.ticks = ticks;
        this.pos = pos;
        this.colors = colors;
    }

    public static void encode(OreRadarPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.ticks);
        buf.writeVarInt(m.pos.length);
        for (int i = 0; i < m.pos.length; i++) {
            buf.writeLong(m.pos[i]);
            buf.writeInt(m.colors[i]);
        }
    }

    public static OreRadarPacket decode(FriendlyByteBuf buf) {
        int t = buf.readVarInt();
        int n = buf.readVarInt();
        long[] p = new long[n];
        int[] c = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = buf.readLong();
            c[i] = buf.readInt();
        }
        return new OreRadarPacket(t, p, c);
    }

    public static void handle(OreRadarPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.oreRadar(m)));
        c.setPacketHandled(true);
    }
}

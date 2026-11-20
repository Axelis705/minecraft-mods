package com.mundosdeaxelis.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente (etapa 255): estás dentro (1) o fuera (0) de la dungeon de un volcán del mundo; cambia la música. */
public class VolcanZonaPacket {
    public final int zone;

    public VolcanZonaPacket(int zone) {
        this.zone = zone;
    }

    public static void encode(VolcanZonaPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.zone);
    }

    public static VolcanZonaPacket decode(FriendlyByteBuf buf) {
        return new VolcanZonaPacket(buf.readByte());
    }

    public static void handle(VolcanZonaPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.mundosdeaxelis.client.AmbientMusic.setVolcanZone(m.zone)));
        c.setPacketHandled(true);
    }
}

package com.mundosdeaxelis.network;

import com.mundosdeaxelis.relic.PetData;
import com.mundosdeaxelis.relic.PetMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Jugador -> servidor: el nombre nuevo de su mascota, escrito en el menú de la mascota (etapa 254). */
public class PetNamePacket {
    public final int containerId;
    public final String name;

    public PetNamePacket(int containerId, String name) {
        this.containerId = containerId;
        this.name = name;
    }

    public static void encode(PetNamePacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.containerId);
        buf.writeUtf(m.name, PetData.MAX_NAME * 4);
    }

    public static PetNamePacket decode(FriendlyByteBuf buf) {
        return new PetNamePacket(buf.readVarInt(), buf.readUtf(PetData.MAX_NAME * 4));
    }

    public static void handle(PetNamePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp != null && sp.containerMenu instanceof PetMenu pm && pm.containerId == m.containerId) {
                pm.rename(sp, m.name);
            }
        });
        c.setPacketHandled(true);
    }
}

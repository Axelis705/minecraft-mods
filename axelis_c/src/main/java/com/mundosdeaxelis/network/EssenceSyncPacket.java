package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> jugador: su Esencia, su nivel de mesa, lo que ya descubrió en el Archivo y (a veces) un aviso para la pantalla de la mesa. */
public class EssenceSyncPacket {
    public final int essence;
    public final int level;
    public final List<String> archive;
    /** Contadores de garantía por caja (Épica, Legendaria, Mítica). */
    public final java.util.Map<String, int[]> pity;
    @Nullable
    public final Component toast;
    public final boolean good;

    public EssenceSyncPacket(int essence, int level, Collection<String> archive, java.util.Map<String, int[]> pity,
                             @Nullable Component toast, boolean good) {
        this.pity = pity;
        this.essence = essence;
        this.level = level;
        this.archive = new ArrayList<>(archive);
        this.toast = toast;
        this.good = good;
    }

    public static void encode(EssenceSyncPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.essence);
        buf.writeVarInt(m.level);
        buf.writeVarInt(m.archive.size());
        for (String s : m.archive) {
            buf.writeUtf(s, 64);
        }
        buf.writeVarInt(m.pity.size());
        for (java.util.Map.Entry<String, int[]> e : m.pity.entrySet()) {
            buf.writeUtf(e.getKey(), 32);
            for (int i = 0; i < 3; i++) {
                buf.writeVarInt(e.getValue()[i]);
            }
        }
        buf.writeBoolean(m.toast != null);
        if (m.toast != null) {
            buf.writeComponent(m.toast);
            buf.writeBoolean(m.good);
        }
    }

    public static EssenceSyncPacket decode(FriendlyByteBuf buf) {
        int es = buf.readVarInt(), lv = buf.readVarInt();
        int n = buf.readVarInt();
        List<String> a = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            a.add(buf.readUtf(64));
        }
        int np = buf.readVarInt();
        java.util.Map<String, int[]> pity = new java.util.LinkedHashMap<>();
        for (int i = 0; i < np; i++) {
            String id = buf.readUtf(32);
            pity.put(id, new int[]{buf.readVarInt(), buf.readVarInt(), buf.readVarInt()});
        }
        Component t = null;
        boolean good = false;
        if (buf.readBoolean()) {
            t = buf.readComponent();
            good = buf.readBoolean();
        }
        return new EssenceSyncPacket(es, lv, a, pity, t, good);
    }

    public static void handle(EssenceSyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.essence(m)));
        c.setPacketHandled(true);
    }
}

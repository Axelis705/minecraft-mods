package com.casino.net;

import com.casino.data.CasinoData;
import com.casino.data.CasinoStats;
import com.casino.plat.Net;
import com.casino.plat.Plat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Servidor -> jugador: lo que necesita la mini tarjeta de socio (lo apostado, los logros, si el bono está listo) y dónde están los
 * casinos (para mostrarla solo adentro). Se manda al entrar al mundo, después de cada jugada y cuando se construye un casino.
 */
public final class MemberSyncPacket {
    public final boolean member;
    public final long wagered;
    public final long achievements;
    public final boolean bonusReady;
    public final List<String> dims;
    public final List<int[]> boxes;

    public MemberSyncPacket(boolean member, long wagered, long achievements, boolean bonusReady, List<String> dims, List<int[]> boxes) {
        this.member = member;
        this.wagered = wagered;
        this.achievements = achievements;
        this.bonusReady = bonusReady;
        this.dims = dims;
        this.boxes = boxes;
    }

    public static void send(ServerPlayer p) {
        var server = Plat.server(p);
        if (server == null) {
            return;
        }
        CasinoStats.Entry e = CasinoStats.get(server).entry(p.getUUID());
        CasinoData d = CasinoData.get(server);
        CasinoNet.sendTo(p, new MemberSyncPacket(e != null && e.cardGiven, e == null ? 0 : e.wagered, e == null ? 0 : e.achievements,
                e == null || e.lastBonus < CasinoData.today(), new ArrayList<>(d.zoneDims()), new ArrayList<>(d.zoneBoxes())));
    }

    public static void sendAll(net.minecraft.server.MinecraftServer server) {
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                send(p);
            }
        }
    }

    public static void encode(MemberSyncPacket m, FriendlyByteBuf b) {
        b.writeBoolean(m.member);
        b.writeVarLong(m.wagered);
        b.writeLong(m.achievements);
        b.writeBoolean(m.bonusReady);
        int n = Math.min(256, m.dims.size());
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            b.writeUtf(m.dims.get(i), 256);
            for (int v : m.boxes.get(i)) {
                b.writeVarInt(v);
            }
        }
    }

    public static MemberSyncPacket decode(FriendlyByteBuf b) {
        boolean member = b.readBoolean();
        long w = b.readVarLong();
        long a = b.readLong();
        boolean bonus = b.readBoolean();
        int n = Math.min(256, b.readVarInt());
        List<String> dims = new ArrayList<>();
        List<int[]> boxes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dims.add(b.readUtf(256));
            int[] box = new int[6];
            for (int k = 0; k < 6; k++) {
                box[k] = b.readVarInt();
            }
            boxes.add(box);
        }
        return new MemberSyncPacket(member, w, a, bonus, dims, boxes);
    }

    public static void handle(MemberSyncPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.MemberHud.set(m));
    }
}

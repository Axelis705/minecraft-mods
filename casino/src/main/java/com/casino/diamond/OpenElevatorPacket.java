package com.casino.diamond;

import com.casino.plat.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Servidor -> jugador: abrir la botonera del elevador con las paradas (altura, nombre, tarjeta que piden). */
public final class OpenElevatorPacket {
    public final BlockPos pos;
    public final int[] ys;
    public final String[] floors;
    public final int[] access;
    public final int tier;
    public final boolean creative;

    public OpenElevatorPacket(BlockPos pos, int[] ys, String[] floors, int[] access, int tier, boolean creative) {
        this.pos = pos;
        this.ys = ys;
        this.floors = floors;
        this.access = access;
        this.tier = tier;
        this.creative = creative;
    }

    public static OpenElevatorPacket of(BlockPos pos, List<ElevatorBE> stops, int tier, boolean creative) {
        int n = stops.size();
        int[] ys = new int[n];
        String[] fl = new String[n];
        int[] ac = new int[n];
        for (int i = 0; i < n; i++) {
            ys[i] = stops.get(i).getBlockPos().getY();
            fl[i] = stops.get(i).floor;
            ac[i] = stops.get(i).access;
        }
        return new OpenElevatorPacket(pos, ys, fl, ac, tier, creative);
    }

    public static void encode(OpenElevatorPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.ys.length);
        for (int i = 0; i < m.ys.length; i++) {
            b.writeVarInt(m.ys[i]);
            b.writeUtf(m.floors[i], 64);
            b.writeVarInt(m.access[i]);
        }
        b.writeVarInt(m.tier);
        b.writeBoolean(m.creative);
    }

    public static OpenElevatorPacket decode(FriendlyByteBuf b) {
        BlockPos pos = b.readBlockPos();
        int n = Math.min(64, b.readVarInt());
        int[] ys = new int[n];
        String[] fl = new String[n];
        int[] ac = new int[n];
        for (int i = 0; i < n; i++) {
            ys[i] = b.readVarInt();
            fl[i] = b.readUtf(64);
            ac[i] = b.readVarInt();
        }
        return new OpenElevatorPacket(pos, ys, fl, ac, b.readVarInt(), b.readBoolean());
    }

    public static void handle(OpenElevatorPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.ClientHooks.openElevator(m));
    }
}

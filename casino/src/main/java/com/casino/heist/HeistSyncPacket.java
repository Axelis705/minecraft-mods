package com.casino.heist;

import com.casino.plat.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Servidor -> jugador: lo que muestra el panel del robo. role 0 = nada (se esconde), 1 = ladrón, 2 = de seguridad. timer: ticks de la
 * bóveda (positivo) o de cámaras apagadas (negativo).
 */
public final class HeistSyncPacket {
    public final int role;
    public final int phase;
    public final int objective;
    public final float suspicion;
    public final boolean alarm;
    public final int timer;
    public final int[] combo;
    public final long bag;
    public final int slots;
    public final int maxSlots;
    public final int robbers;

    public HeistSyncPacket(int role, int phase, int objective, float suspicion, boolean alarm, int timer, int[] combo, long bag, int slots, int maxSlots,
            int robbers) {
        this.role = role;
        this.phase = phase;
        this.objective = objective;
        this.suspicion = suspicion;
        this.alarm = alarm;
        this.timer = timer;
        this.combo = combo;
        this.bag = bag;
        this.slots = slots;
        this.maxSlots = maxSlots;
        this.robbers = robbers;
    }

    public static HeistSyncPacket none() {
        return new HeistSyncPacket(0, 0, 0, 0, false, 0, new int[]{-1, -1, -1}, 0, 0, 0, 0);
    }

    public static void encode(HeistSyncPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.role);
        b.writeVarInt(m.phase);
        b.writeVarInt(m.objective);
        b.writeFloat(m.suspicion);
        b.writeBoolean(m.alarm);
        b.writeInt(m.timer);
        for (int i = 0; i < 3; i++) {
            b.writeInt(m.combo[i]);
        }
        b.writeVarLong(m.bag);
        b.writeVarInt(m.slots);
        b.writeVarInt(m.maxSlots);
        b.writeVarInt(m.robbers);
    }

    public static HeistSyncPacket decode(FriendlyByteBuf b) {
        int role = b.readVarInt(), phase = b.readVarInt(), obj = b.readVarInt();
        float s = b.readFloat();
        boolean al = b.readBoolean();
        int timer = b.readInt();
        int[] c = {b.readInt(), b.readInt(), b.readInt()};
        return new HeistSyncPacket(role, phase, obj, s, al, timer, c, b.readVarLong(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(HeistSyncPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.HeistHud.set(m));
    }
}

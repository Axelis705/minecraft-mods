package com.kensei.net;

import com.kensei.client.ClientDuel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Servidor -> peleadores y espectadores de un duelo: todo lo que hace falta para dibujar los marcadores (o {@code active = false} al terminar). */
public class DuelStatePacket {
    public static final int PHASE_INTRO = 0;
    public static final int PHASE_FIGHT = 1;
    public static final int PHASE_ROUND_END = 2;
    public static final int PHASE_MATCH_END = 3;

    public static final int REASON_KO = 0;
    public static final int REASON_TIME = 1;
    public static final int REASON_DRAW = 2;

    public static final class F {
        public UUID id;
        public int entityId;
        public String name;
        public int team;
        public float hp;
        public float maxHp;
        public int energy;
        public int heavyCd;
        /** Estamina de 0 a 1 y si está agotado. */
        public float stamina = 1.0F;
        public boolean exhausted;
        public boolean guard;
        /** 0 = en pie, 1 = K.O., 2 = se fue. */
        public int state;
        public int parries;
        public int dodges;
        public int hits;
    }

    public boolean active;
    public int phase;
    /** Ticks que faltan para que termine la fase actual (cuenta atrás, fin de ronda, fin del duelo). */
    public int phaseTicks;
    public int phaseTotal;
    public int round;
    public int roundsToWin;
    public int roundTicks;
    public int timeLeft;
    public int winsA;
    public int winsB;
    /** Equipo que ganó la ronda o el duelo (-1 = nadie) y por qué. */
    public int winner = -1;
    public int reason;
    public final List<F> fighters = new ArrayList<>();

    public static void encode(DuelStatePacket m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.active);
        if (!m.active) {
            return;
        }
        buf.writeByte(m.phase);
        buf.writeVarInt(m.phaseTicks);
        buf.writeVarInt(m.phaseTotal);
        buf.writeByte(m.round);
        buf.writeByte(m.roundsToWin);
        buf.writeVarInt(m.roundTicks);
        buf.writeVarInt(m.timeLeft);
        buf.writeByte(m.winsA);
        buf.writeByte(m.winsB);
        buf.writeByte(m.winner);
        buf.writeByte(m.reason);
        buf.writeByte(m.fighters.size());
        for (F f : m.fighters) {
            buf.writeUUID(f.id);
            buf.writeVarInt(f.entityId);
            buf.writeUtf(f.name, 32);
            buf.writeByte(f.team);
            buf.writeFloat(f.hp);
            buf.writeFloat(f.maxHp);
            buf.writeVarInt(f.energy);
            buf.writeVarInt(f.heavyCd);
            buf.writeFloat(f.stamina);
            buf.writeBoolean(f.exhausted);
            buf.writeBoolean(f.guard);
            buf.writeByte(f.state);
            buf.writeVarInt(f.parries);
            buf.writeVarInt(f.dodges);
            buf.writeVarInt(f.hits);
        }
    }

    public static DuelStatePacket decode(FriendlyByteBuf buf) {
        DuelStatePacket m = new DuelStatePacket();
        m.active = buf.readBoolean();
        if (!m.active) {
            return m;
        }
        m.phase = buf.readByte();
        m.phaseTicks = buf.readVarInt();
        m.phaseTotal = buf.readVarInt();
        m.round = buf.readByte();
        m.roundsToWin = buf.readByte();
        m.roundTicks = buf.readVarInt();
        m.timeLeft = buf.readVarInt();
        m.winsA = buf.readByte();
        m.winsB = buf.readByte();
        m.winner = buf.readByte();
        m.reason = buf.readByte();
        int n = buf.readByte();
        for (int i = 0; i < n; i++) {
            F f = new F();
            f.id = buf.readUUID();
            f.entityId = buf.readVarInt();
            f.name = buf.readUtf(32);
            f.team = buf.readByte();
            f.hp = buf.readFloat();
            f.maxHp = buf.readFloat();
            f.energy = buf.readVarInt();
            f.heavyCd = buf.readVarInt();
            f.stamina = buf.readFloat();
            f.exhausted = buf.readBoolean();
            f.guard = buf.readBoolean();
            f.state = buf.readByte();
            f.parries = buf.readVarInt();
            f.dodges = buf.readVarInt();
            f.hits = buf.readVarInt();
            m.fighters.add(f);
        }
        return m;
    }

    public static void handle(DuelStatePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDuel.receive(m)));
        c.setPacketHandled(true);
    }
}

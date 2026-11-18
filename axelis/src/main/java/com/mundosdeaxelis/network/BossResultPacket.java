package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> jugador: resultado de vencer a un jefe, en una sola pantalla (en vez de varios mensajes de chat): tu parte del
 * daño, lo que recibiste, si el mundo subió de nivel y si se apuntó un avance nuevo en tu Archivo.
 */
public class BossResultPacket {
    /** item: id del objeto (vacío = Esencia). count: cantidad. sure: premio seguro (no depende de la suerte). */
    public record Got(String item, int count, boolean sure) {
    }

    /** Un premio de suerte que NO salió: kind 0 = objeto, 1 = Esencia, 2 = materiales (item = uno de ellos, solo para el ícono); pct = probabilidad que tenías. */
    public record Miss(String item, int kind, int pct) {
    }

    public final int stage;
    public final String boss;
    /** false = hizo muy poco daño y no recibe recompensa. */
    public final boolean eligible;
    public final int pct;
    public final int chance;
    public final List<Got> rewards;
    /** Nivel al que sube el mundo con este jefe (0 = no sube) y el % de vida/daño extra de los monstruos. */
    public final int tierLevel;
    public final int tierPct;
    public final boolean archiveNew;
    public final List<Miss> missed;

    public BossResultPacket(int stage, String boss, boolean eligible, int pct, int chance, List<Got> rewards, int tierLevel, int tierPct,
                            boolean archiveNew) {
        this(stage, boss, eligible, pct, chance, rewards, tierLevel, tierPct, archiveNew, new ArrayList<>());
    }

    public BossResultPacket(int stage, String boss, boolean eligible, int pct, int chance, List<Got> rewards, int tierLevel, int tierPct,
                            boolean archiveNew, List<Miss> missed) {
        this.missed = missed;
        this.stage = stage;
        this.boss = boss;
        this.eligible = eligible;
        this.pct = pct;
        this.chance = chance;
        this.rewards = rewards;
        this.tierLevel = tierLevel;
        this.tierPct = tierPct;
        this.archiveNew = archiveNew;
    }

    public static void encode(BossResultPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.stage);
        buf.writeUtf(m.boss, 64);
        buf.writeBoolean(m.eligible);
        buf.writeVarInt(m.pct);
        buf.writeVarInt(m.chance);
        buf.writeVarInt(m.rewards.size());
        for (Got g : m.rewards) {
            buf.writeUtf(g.item(), 128);
            buf.writeVarInt(g.count());
            buf.writeBoolean(g.sure());
        }
        buf.writeVarInt(m.tierLevel);
        buf.writeVarInt(m.tierPct);
        buf.writeBoolean(m.archiveNew);
        buf.writeVarInt(m.missed.size());
        for (Miss s : m.missed) {
            buf.writeUtf(s.item(), 128);
            buf.writeVarInt(s.kind());
            buf.writeVarInt(s.pct());
        }
    }

    public static BossResultPacket decode(FriendlyByteBuf buf) {
        int stage = buf.readVarInt();
        String boss = buf.readUtf(64);
        boolean eligible = buf.readBoolean();
        int pct = buf.readVarInt();
        int chance = buf.readVarInt();
        int n = buf.readVarInt();
        List<Got> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(new Got(buf.readUtf(128), buf.readVarInt(), buf.readBoolean()));
        }
        int tl = buf.readVarInt();
        int tp = buf.readVarInt();
        boolean an = buf.readBoolean();
        int nm = buf.readVarInt();
        List<Miss> ms = new ArrayList<>();
        for (int i = 0; i < nm; i++) {
            ms.add(new Miss(buf.readUtf(128), buf.readVarInt(), buf.readVarInt()));
        }
        return new BossResultPacket(stage, boss, eligible, pct, chance, l, tl, tp, an, ms);
    }

    public static void handle(BossResultPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.openBossResult(m)));
        c.setPacketHandled(true);
    }
}

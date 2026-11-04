package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> cliente: abre la ficha del jefe. Tiene dos modos: {@link #ENTRY} (al usar la Piedra de Portal: equipo
 * recomendado, qué hay en la dungeon, premios y quién viaja; el jefe y los premios salen como «?» hasta que lo venzas) y
 * {@link #DOSSIER} (en la antesala, antes de la pelea: el jefe, sus números, ataques y consejos). Todos los números los
 * calcula el servidor con {@code BossBalance}.
 */
public class BossBriefPacket {
    public static final int ENTRY = 0;
    public static final int DOSSIER = 1;

    /** Un premio de la lista: objeto, cantidad (texto) y etiqueta (0 segura, 1 probable, 2 posible, 3 rara). */
    public record Reward(String item, String amount, int tag) {
    }

    public final int mode;
    public final int stage;
    public final BlockPos pos;
    /** ¿Este jugador ya venció a este jefe? (si no, el jefe y los premios se muestran ocultos). */
    public final boolean known;
    /** Vida del jefe con los jugadores que viajarían, y la vida base (1 jugador). */
    public final int hpNow;
    public final int hpBase;
    /** Daño extra por los jugadores que viajarían (porcentaje) y lo que suma cada jugador extra (vida %, daño %). */
    public final int dmgBonusPct;
    public final int hpPerExtraPct;
    public final int dmgPerExtraPct;
    /** Corazones que quita el golpe fuerte con el set B y con el A de la caja de la etapa. */
    public final float strongB;
    public final float strongA;
    public final Component setB;
    public final Component setA;
    public final int armorB;
    public final int swordB;
    public final List<String> party;
    public final List<Reward> rewards;

    public BossBriefPacket(int mode, int stage, BlockPos pos, boolean known, int hpNow, int hpBase, int dmgBonusPct, int hpPerExtraPct,
                           int dmgPerExtraPct, float strongB, float strongA, Component setB, Component setA, int armorB, int swordB,
                           List<String> party, List<Reward> rewards) {
        this.mode = mode;
        this.stage = stage;
        this.pos = pos;
        this.known = known;
        this.hpNow = hpNow;
        this.hpBase = hpBase;
        this.dmgBonusPct = dmgBonusPct;
        this.hpPerExtraPct = hpPerExtraPct;
        this.dmgPerExtraPct = dmgPerExtraPct;
        this.strongB = strongB;
        this.strongA = strongA;
        this.setB = setB;
        this.setA = setA;
        this.armorB = armorB;
        this.swordB = swordB;
        this.party = party;
        this.rewards = rewards;
    }

    /** Copia de la ficha con otro valor de «jefe conocido» (para las pruebas). */
    public BossBriefPacket withKnown(boolean k) {
        return new BossBriefPacket(mode, stage, pos, k, hpNow, hpBase, dmgBonusPct, hpPerExtraPct, dmgPerExtraPct, strongB, strongA,
                setB, setA, armorB, swordB, party, rewards);
    }

    public static void encode(BossBriefPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.mode);
        buf.writeVarInt(m.stage);
        buf.writeBlockPos(m.pos);
        buf.writeBoolean(m.known);
        buf.writeVarInt(m.hpNow);
        buf.writeVarInt(m.hpBase);
        buf.writeVarInt(m.dmgBonusPct);
        buf.writeVarInt(m.hpPerExtraPct);
        buf.writeVarInt(m.dmgPerExtraPct);
        buf.writeFloat(m.strongB);
        buf.writeFloat(m.strongA);
        buf.writeComponent(m.setB);
        buf.writeComponent(m.setA);
        buf.writeVarInt(m.armorB);
        buf.writeVarInt(m.swordB);
        buf.writeVarInt(m.party.size());
        for (String s : m.party) {
            buf.writeUtf(s, 64);
        }
        buf.writeVarInt(m.rewards.size());
        for (Reward r : m.rewards) {
            buf.writeUtf(r.item(), 128);
            buf.writeUtf(r.amount(), 32);
            buf.writeVarInt(r.tag());
        }
    }

    public static BossBriefPacket decode(FriendlyByteBuf buf) {
        int mode = buf.readVarInt();
        int stage = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        boolean known = buf.readBoolean();
        int hpNow = buf.readVarInt();
        int hpBase = buf.readVarInt();
        int dmgBonus = buf.readVarInt();
        int hpPer = buf.readVarInt();
        int dmgPer = buf.readVarInt();
        float sb = buf.readFloat();
        float sa = buf.readFloat();
        Component setB = buf.readComponent();
        Component setA = buf.readComponent();
        int armorB = buf.readVarInt();
        int swordB = buf.readVarInt();
        int n = buf.readVarInt();
        List<String> party = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            party.add(buf.readUtf(64));
        }
        int rn = buf.readVarInt();
        List<Reward> rewards = new ArrayList<>();
        for (int i = 0; i < rn; i++) {
            rewards.add(new Reward(buf.readUtf(128), buf.readUtf(32), buf.readVarInt()));
        }
        return new BossBriefPacket(mode, stage, pos, known, hpNow, hpBase, dmgBonus, hpPer, dmgPer, sb, sa, setB, setA, armorB, swordB, party, rewards);
    }

    public static void handle(BossBriefPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.openBossBrief(m)));
        c.setPacketHandled(true);
    }
}

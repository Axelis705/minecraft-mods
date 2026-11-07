package com.pooltable.net;

import com.pooltable.block.VolleyCourtBE;
import com.pooltable.client.PoolClient;
import com.pooltable.game.VolleyMatch;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Servidor -> cliente: la partida de voleibol en este tick (marcador, pelota, jugadores y lo que acaba de pasar). */
public final class VolleySyncPacket {
    public static final String[] POPUPS = {"", "spike", "block", "net", "out", "point", "fault"};

    public final BlockPos pos;
    public final byte state;
    public final byte scoreA;
    public final byte scoreB;
    public final byte serving;
    public final byte touches;
    public final byte touchTeam;
    public final byte n;
    public final byte diff;
    public final byte serverIdx;
    public final byte popupCode;
    public byte setsA;
    public byte setsB;
    public byte setNo;
    public byte bestOf = 3;
    public byte target = 15;
    /** Calidad del último golpe (0 = ninguno, 1 mal, 2 bien, 3 perfecto) y quién lo dio. */
    public byte quality;
    public byte qualityBy = -1;
    public final float timer;
    public final float[] ball = new float[6];
    public final int count;
    public final byte[] team = new byte[6];
    public final byte[] flags = new byte[6];
    public final byte[] act = new byte[6];
    public final byte[] swing = new byte[6];
    public final int[] eid = new int[6];
    public final float[] px = new float[6];
    public final float[] pz = new float[6];
    public final float[] py = new float[6];

    private VolleySyncPacket(BlockPos pos, byte state, byte scoreA, byte scoreB, byte serving, byte touches, byte touchTeam, byte n, byte diff, byte serverIdx,
                             byte popupCode, float timer, int count) {
        this.pos = pos;
        this.state = state;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.serving = serving;
        this.touches = touches;
        this.touchTeam = touchTeam;
        this.n = n;
        this.diff = diff;
        this.serverIdx = serverIdx;
        this.popupCode = popupCode;
        this.timer = timer;
        this.count = count;
    }

    public String popup() {
        return popupCode >= 0 && popupCode < POPUPS.length ? POPUPS[popupCode] : "";
    }

    /** Arma el paquete con el estado actual de la partida. */
    public static VolleySyncPacket of(BlockPos pos, VolleyMatch m, Map<UUID, Integer> idxOf, List<VolleyCourtBE.Member> members, ServerLevel sl) {
        int popup = 0;
        if (m.evNet) {
            popup = 3;
        }
        for (int[] h : m.hits) {
            VolleyMatch.Act a = VolleyMatch.Act.values()[h[0]];
            if (a == VolleyMatch.Act.SPIKE) {
                popup = 1;
            } else if (a == VolleyMatch.Act.BLOCK) {
                popup = 2;
            }
        }
        if (m.evPoint != 0) {
            popup = !m.evFault.isEmpty() ? 6 : (m.evOut ? 4 : 5);
        }
        int cnt = Math.min(6, m.players.size());
        VolleySyncPacket p = new VolleySyncPacket(pos, (byte) m.state.ordinal(), (byte) m.scoreA, (byte) m.scoreB, (byte) m.serving, (byte) m.touches,
                (byte) m.touchTeam, (byte) m.n, (byte) m.diff, (byte) m.serverIdx, (byte) popup, (float) m.timer, cnt);
        p.setsA = (byte) m.setsA;
        p.setsB = (byte) m.setsB;
        p.setNo = (byte) m.setNo;
        p.bestOf = (byte) m.bestOf;
        p.target = (byte) m.target();
        if (m.evQuality >= 0 && m.evQualityBy >= 0) {
            p.quality = (byte) (m.evQuality >= 0.8 ? 3 : (m.evQuality >= 0.5 ? 2 : 1));
            p.qualityBy = (byte) m.evQualityBy;
        }
        for (int i = 0; i < 3; i++) {
            p.ball[i] = (float) m.ball[i];
            p.ball[3 + i] = (float) m.vel[i];
        }
        for (int i = 0; i < cnt; i++) {
            VolleyMatch.P q = m.players.get(i);
            p.team[i] = (byte) q.team;
            p.flags[i] = (byte) ((q.human ? 1 : 0) | (q.y > 0.05 ? 2 : 0));
            p.act[i] = (byte) q.lastAct.ordinal();
            p.swing[i] = (byte) Math.max(0, Math.min(100, (int) (q.swing * 100)));
            p.eid[i] = -1;
            p.px[i] = (float) q.x;
            p.pz[i] = (float) q.z;
            p.py[i] = (float) q.y;
        }
        for (VolleyCourtBE.Member mem : members) {
            Integer idx = idxOf.get(mem.id);
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(mem.id);
            if (idx != null && sp != null && idx < cnt) {
                p.eid[idx] = sp.getId();
            }
        }
        return p;
    }

    public static void encode(VolleySyncPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeByte(m.state);
        b.writeByte(m.scoreA);
        b.writeByte(m.scoreB);
        b.writeByte(m.serving);
        b.writeByte(m.touches);
        b.writeByte(m.touchTeam);
        b.writeByte(m.n);
        b.writeByte(m.diff);
        b.writeByte(m.serverIdx);
        b.writeByte(m.popupCode);
        b.writeFloat(m.timer);
        b.writeByte(m.setsA);
        b.writeByte(m.setsB);
        b.writeByte(m.setNo);
        b.writeByte(m.bestOf);
        b.writeByte(m.target);
        b.writeByte(m.quality);
        b.writeByte(m.qualityBy);
        for (int i = 0; i < 6; i++) {
            b.writeFloat(m.ball[i]);
        }
        b.writeByte(m.count);
        for (int i = 0; i < m.count; i++) {
            b.writeByte(m.team[i]);
            b.writeByte(m.flags[i]);
            b.writeByte(m.act[i]);
            b.writeByte(m.swing[i]);
            b.writeInt(m.eid[i]);
            b.writeFloat(m.px[i]);
            b.writeFloat(m.pz[i]);
            b.writeFloat(m.py[i]);
        }
    }

    public static VolleySyncPacket decode(FriendlyByteBuf b) {
        BlockPos pos = b.readBlockPos();
        byte state = b.readByte();
        byte sa = b.readByte();
        byte sb = b.readByte();
        byte serving = b.readByte();
        byte touches = b.readByte();
        byte touchTeam = b.readByte();
        byte n = b.readByte();
        byte diff = b.readByte();
        byte serverIdx = b.readByte();
        byte popup = b.readByte();
        float timer = b.readFloat();
        byte setsA = b.readByte();
        byte setsB = b.readByte();
        byte setNo = b.readByte();
        byte bestOf = b.readByte();
        byte target = b.readByte();
        byte quality = b.readByte();
        byte qualityBy = b.readByte();
        float[] ball = new float[6];
        for (int i = 0; i < 6; i++) {
            ball[i] = b.readFloat();
        }
        int count = Math.max(0, Math.min(6, b.readByte()));
        VolleySyncPacket p = new VolleySyncPacket(pos, state, sa, sb, serving, touches, touchTeam, n, diff, serverIdx, popup, timer, count);
        System.arraycopy(ball, 0, p.ball, 0, 6);
        p.setsA = setsA;
        p.setsB = setsB;
        p.setNo = setNo;
        p.bestOf = bestOf;
        p.target = target;
        p.quality = quality;
        p.qualityBy = qualityBy;
        for (int i = 0; i < count; i++) {
            p.team[i] = b.readByte();
            p.flags[i] = b.readByte();
            p.act[i] = b.readByte();
            p.swing[i] = b.readByte();
            p.eid[i] = b.readInt();
            p.px[i] = b.readFloat();
            p.pz[i] = b.readFloat();
            p.py[i] = b.readFloat();
        }
        return p;
    }

    public static void handle(VolleySyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PoolClient.onVolleySync(m)));
        ctx.get().setPacketHandled(true);
    }
}

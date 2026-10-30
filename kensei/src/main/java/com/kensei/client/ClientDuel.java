package com.kensei.client;

import com.kensei.combat.KatanaUtil;
import com.kensei.net.DuelStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Lo último que el servidor contó del duelo en el que está este jugador (como peleador o como espectador). Solo se usa en el cliente. */
public final class ClientDuel {
    private static DuelStatePacket state = null;
    private static long recvMs = 0;
    private static int lastRound = -1;
    private static int lastPhase = -1;

    private ClientDuel() {
    }

    public static void receive(DuelStatePacket m) {
        if (!m.active) {
            clear();
            return;
        }
        boolean newRound = state == null || m.round != lastRound;
        boolean fightStart = state != null && m.phase == DuelStatePacket.PHASE_FIGHT && lastPhase != DuelStatePacket.PHASE_FIGHT;
        state = m;
        recvMs = System.currentTimeMillis();
        lastRound = m.round;
        lastPhase = m.phase;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && self() != null && (newRound || fightStart)) {
            autoLock(mc.player);
        }
    }

    public static void clear() {
        state = null;
        lastRound = -1;
        lastPhase = -1;
        DuelHud.clear();
    }

    public static boolean active() {
        return state != null;
    }

    public static DuelStatePacket state() {
        return state;
    }

    /** Yo, si soy peleador (null si solo miro). */
    public static DuelStatePacket.F self() {
        Minecraft mc = Minecraft.getInstance();
        if (state == null || mc.player == null) {
            return null;
        }
        for (DuelStatePacket.F f : state.fighters) {
            if (f.id.equals(mc.player.getUUID())) {
                return f;
            }
        }
        return null;
    }

    /** 0 = no es de este duelo (o no estoy peleando), 1 = aliado (mismo equipo), 2 = enemigo. Sirve para marcar a cada uno y no fijar a los aliados. */
    public static int relation(Entity e) {
        DuelStatePacket.F me = self();
        if (me == null || e == null || e == Minecraft.getInstance().player) {
            return 0;
        }
        for (DuelStatePacket.F f : state.fighters) {
            if (f.id.equals(e.getUUID())) {
                return f.team == me.team ? 1 : 2;
            }
        }
        return 0;
    }

    /** Ticks que le faltan a la fase actual, contando lo que pasó desde que llegó el último aviso. */
    public static int phaseTicksLeft() {
        if (state == null) {
            return 0;
        }
        return Math.max(0, state.phaseTicks - (int) ((System.currentTimeMillis() - recvMs) / 50));
    }

    public static int timeLeftTicks() {
        if (state == null) {
            return 0;
        }
        if (state.phase != DuelStatePacket.PHASE_FIGHT) {
            return state.timeLeft;
        }
        return Math.max(0, state.timeLeft - (int) ((System.currentTimeMillis() - recvMs) / 50));
    }

    /** Al empezar cada ronda la mira se fija sola en el rival más cercano (se puede soltar con el clic central). */
    private static void autoLock(LocalPlayer p) {
        DuelStatePacket.F me = self();
        if (me == null || !KatanaUtil.holdsKatana(p)) {
            return;
        }
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (DuelStatePacket.F f : state.fighters) {
            if (f.team == me.team || f.state != 0) {
                continue;
            }
            Entity e = Minecraft.getInstance().level == null || f.entityId < 0 ? null : Minecraft.getInstance().level.getEntity(f.entityId);
            if (e instanceof LivingEntity le && le.isAlive()) {
                double d = le.distanceToSqr(p);
                if (d < bd) {
                    bd = d;
                    best = le;
                }
            }
        }
        if (best != null) {
            ClientLock.lockOn(p, best);
        }
    }
}

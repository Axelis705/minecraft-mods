package com.celular.call;

import com.celular.CelularReg;
import com.celular.net.CelularNet;
import com.celular.net.CelularNet.CallAct;
import com.celular.net.CelularNet.CallState;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import com.celular.voice.VoiceBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Las llamadas: quién llama a quién, si está sonando o ya contestaron. El celular de quien recibe suena de verdad (lo oyen los que
 * están cerca) y si nadie contesta en 30 segundos queda como llamada perdida. Con Simple Voice Chat la voz va por {@code VoicePlugin}.
 */
public final class CallManager {
    private static final long RING_MS = 30_000;
    private static final long RING_EVERY_MS = 2_600;

    private static final class Call {
        final UUID caller, callee;
        final String callerName, calleeName;
        final MinecraftServer server;
        volatile boolean active;
        long start;
        ScheduledFuture<?> timeout, ring;

        Call(ServerPlayer a, ServerPlayer b, MinecraftServer s) {
            caller = a.getUUID();
            callee = b.getUUID();
            callerName = a.getScoreboardName();
            calleeName = b.getScoreboardName();
            server = s;
        }

        UUID other(UUID me) {
            return me.equals(caller) ? callee : caller;
        }

        String otherName(UUID me) {
            return me.equals(caller) ? calleeName : callerName;
        }

        void cancelTimers() {
            if (timeout != null) {
                timeout.cancel(false);
            }
            if (ring != null) {
                ring.cancel(false);
            }
        }
    }

    /** Cada jugador en una llamada (el que llama y el que recibe apuntan a la misma). La lee también el hilo del chat de voz. */
    private static final Map<UUID, Call> CALLS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Celular llamadas");
        t.setDaemon(true);
        return t;
    });

    private CallManager() {
    }

    /** Con quién está hablando este jugador ahora mismo (solo si ya contestaron), o null. */
    public static UUID voicePartner(UUID me) {
        Call c = CALLS.get(me);
        return c != null && c.active ? c.other(me) : null;
    }

    public static CelularNet.Contacts contacts(ServerPlayer me) {
        CelularNet.Contacts c = new CelularNet.Contacts();
        MinecraftServer s = Plat.server(me);
        if (s == null) {
            return c;
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p != me) {
                c.ids.add(p.getUUID());
                c.names.add(p.getScoreboardName());
                c.phone.add(CelularReg.phoneColor(p) >= 0);
            }
        }
        return c;
    }

    private static void send(MinecraftServer s, UUID who, CallState st) {
        ServerPlayer p = s.getPlayerList().getPlayer(who);
        if (p != null) {
            Net.toPlayer(p, st);
        }
    }

    private static void idle(ServerPlayer p, int reason, UUID other, String name) {
        Net.toPlayer(p, new CallState(CallState.IDLE, reason, other, name, 0, VoiceBridge.available));
    }

    public static void action(ServerPlayer p, int action, UUID target) {
        MinecraftServer s = Plat.server(p);
        if (p == null || s == null) {
            return;
        }
        UUID me = p.getUUID();
        switch (action) {
            case CallAct.DIAL -> dial(p, s, target);
            case CallAct.ACCEPT -> {
                Call c = CALLS.get(me);
                if (c != null && !c.active && me.equals(c.callee)) {
                    c.cancelTimers();
                    c.active = true;
                    c.start = System.currentTimeMillis();
                    send(s, c.caller, new CallState(CallState.ACTIVE, 0, c.callee, c.calleeName, 0, VoiceBridge.available));
                    send(s, c.callee, new CallState(CallState.ACTIVE, 0, c.caller, c.callerName, 0, VoiceBridge.available));
                }
            }
            case CallAct.DECLINE, CallAct.HANGUP -> {
                Call c = CALLS.get(me);
                if (c != null) {
                    int forOther = c.active ? CallState.R_ENDED : (me.equals(c.callee) ? CallState.R_DECLINED : CallState.R_MISSED);
                    end(c, me, CallState.R_ENDED, forOther);
                }
            }
            default -> {
            }
        }
    }

    private static void dial(ServerPlayer p, MinecraftServer s, UUID target) {
        UUID me = p.getUUID();
        if (CALLS.containsKey(me) || target == null || CelularReg.phoneColor(p) < 0) {
            Call cur = CALLS.get(me);
            if (cur == null) {
                idle(p, CallState.R_ENDED, target, "");
            }
            return;
        }
        ServerPlayer t = s.getPlayerList().getPlayer(target);
        if (target.equals(me)) {
            idle(p, CallState.R_SELF, target, p.getScoreboardName());
            return;
        }
        if (t == null) {
            idle(p, CallState.R_OFFLINE, target, "");
            return;
        }
        if (CelularReg.phoneColor(t) < 0) {
            idle(p, CallState.R_NO_PHONE, target, t.getScoreboardName());
            return;
        }
        if (CALLS.containsKey(target)) {
            idle(p, CallState.R_BUSY, target, t.getScoreboardName());
            return;
        }
        Call c = new Call(p, t, s);
        CALLS.put(me, c);
        CALLS.put(target, c);
        Net.toPlayer(p, new CallState(CallState.DIALING, 0, target, c.calleeName, 0, VoiceBridge.available));
        Net.toPlayer(t, new CallState(CallState.INCOMING, 0, me, c.callerName, 0, VoiceBridge.available));
        // el celular suena donde está quien recibe (lo oyen los de cerca) hasta que contesten
        c.ring = TIMER.scheduleAtFixedRate(() -> s.execute(() -> {
            if (CALLS.get(c.callee) == c && !c.active) {
                ServerPlayer callee = s.getPlayerList().getPlayer(c.callee);
                if (callee != null) {
                    callee.level().playSound(null, callee.getX(), callee.getY() + 1, callee.getZ(), CelularReg.RINGTONE.get(),
                            SoundSource.PLAYERS, 0.9F, 1.0F);
                }
            }
        }), 0, RING_EVERY_MS, TimeUnit.MILLISECONDS);
        c.timeout = TIMER.schedule(() -> s.execute(() -> {
            if (CALLS.get(c.callee) == c && !c.active) {
                end(c, null, CallState.R_NO_ANSWER, CallState.R_MISSED);
                ServerPlayer callee = s.getPlayerList().getPlayer(c.callee);
                if (callee != null) {
                    Net.toPlayer(callee, new CelularNet.Notify(CelularNet.Notify.MISSED_CALL, c.callerName, ""));
                }
            }
        }), RING_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Termina la llamada. {@code by} es quien colgó (o null si se acabó el tiempo); a esa persona le llega {@code forMe} y a la otra
     * {@code forOther} (si by es null: al que llamó forMe y al que recibía forOther).
     */
    private static void end(Call c, UUID by, int forMe, int forOther) {
        c.cancelTimers();
        CALLS.remove(c.caller, c);
        CALLS.remove(c.callee, c);
        UUID a = by == null ? c.caller : by;
        UUID b = c.other(a);
        int secs = c.active ? (int) ((System.currentTimeMillis() - c.start) / 1000) : 0;
        send(c.server, a, new CallState(CallState.IDLE, forMe, b, c.otherName(a), secs, VoiceBridge.available));
        send(c.server, b, new CallState(CallState.IDLE, forOther, a, c.otherName(b), secs, VoiceBridge.available));
        ServerPlayer pb = c.server.getPlayerList().getPlayer(b);
        if (pb != null && c.active) {
            pb.level().playSound(null, pb.blockPosition(), CelularReg.HANGUP.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
        }
    }

    /** Si alguien se desconecta en plena llamada, la llamada se corta. */
    public static void logout(ServerPlayer p) {
        Call c = CALLS.get(p.getUUID());
        if (c != null) {
            end(c, p.getUUID(), CallState.R_ENDED, c.active ? CallState.R_ENDED : (p.getUUID().equals(c.caller) ? CallState.R_MISSED
                    : CallState.R_NO_ANSWER));
        }
    }

    /** Al cerrar el mundo: se olvidan todas las llamadas. */
    public static void clear() {
        for (Call c : CALLS.values()) {
            c.cancelTimers();
        }
        CALLS.clear();
    }
}

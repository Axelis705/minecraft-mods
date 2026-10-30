package com.kensei.duel;

import com.kensei.KenseiConfig;
import com.kensei.item.KatanaItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.kensei.entity.RivalEntity;
import com.kensei.net.KenseiNet;
import com.kensei.registry.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lleva la cuenta de los desafíos pendientes y de los duelos en marcha: quién desafió a quién, quién aceptó, y arranca el duelo cuando todos dicen que sí. */
public final class DuelManager {
    private static final List<Duel> DUELS = new ArrayList<>();
    private static final List<Pending> PENDING = new ArrayList<>();

    private static final class Pending {
        final UUID host;
        final List<UUID> teamA = new ArrayList<>();
        final List<UUID> teamB = new ArrayList<>();
        final Set<UUID> accepted = new HashSet<>();
        /** Rivales de práctica que entran en cada equipo (se crean al empezar). */
        int botsA = 0;
        int botsB = 0;
        final long expireMs;
        long lastNag = 0;

        Pending(UUID host, long expireMs) {
            this.host = host;
            this.expireMs = expireMs;
        }

        boolean involves(UUID id) {
            return teamA.contains(id) || teamB.contains(id);
        }

        int size() {
            return teamA.size() + teamB.size();
        }

        int totalA() {
            return teamA.size() + botsA;
        }

        int totalB() {
            return teamB.size() + botsB;
        }
    }

    private DuelManager() {
    }

    // ------------------------------------------------------------------ consultas

    /** El duelo en el que está este jugador como peleador (no si ya se fue), o null. */
    public static Duel fighterDuel(UUID id) {
        for (Duel d : DUELS) {
            Duel.Fighter f = d.find(id);
            if (f != null && !f.gone) {
                return d;
            }
        }
        return null;
    }

    /** El duelo en el que está como peleador o espectador, o null. */
    public static Duel of(UUID id) {
        Duel d = fighterDuel(id);
        if (d != null) {
            return d;
        }
        for (Duel o : DUELS) {
            if (o.isSpectator(id)) {
                return o;
            }
        }
        return null;
    }

    public static Duel fighterDuel(Entity e) {
        return e instanceof ServerPlayer || e instanceof RivalEntity ? fighterDuel(e.getUUID()) : null;
    }

    /** ¿Son del mismo equipo en un duelo? (no se pueden dañar entre sí) */
    public static boolean allies(Entity a, Entity b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        Duel d = fighterDuel(a);
        if (d == null || d != fighterDuel(b)) {
            return false;
        }
        return d.find(a.getUUID()).team == d.find(b.getUUID()).team;
    }

    /** ¿No puede atacar ni esquivar ahora? (cuenta atrás, fin de ronda o ya cayó) */
    public static boolean frozen(Entity e) {
        Duel d = fighterDuel(e);
        return d != null && d.frozen(e.getUUID());
    }

    public static void stat(Entity e, int kind) {
        Duel d = fighterDuel(e);
        if (d != null) {
            d.stat(e.getUUID(), kind);
        }
    }

    private static Pending pendingFor(UUID id) {
        for (Pending p : PENDING) {
            if (p.involves(id)) {
                return p;
            }
        }
        return null;
    }

    private static boolean hasKatana(ServerPlayer p) {
        return p.getInventory().hasAnyMatching(s -> s.getItem() instanceof KatanaItem);
    }

    /** Por qué este jugador no puede entrar a un duelo (null = puede). */
    private static Component problem(ServerPlayer p, ServerPlayer host) {
        if (of(p.getUUID()) != null || pendingFor(p.getUUID()) != null) {
            return Component.translatable("kensei.duel.busy", p.getGameProfile().getName());
        }
        if (!p.isAlive() || p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            return Component.translatable("kensei.duel.busy", p.getGameProfile().getName());
        }
        if (p.level() != host.level()) {
            return Component.translatable("kensei.duel.other_dim", p.getGameProfile().getName());
        }
        if (KenseiConfig.DUEL_REQUIRE_KATANA.get() && !hasKatana(p)) {
            return Component.translatable("kensei.duel.need_katana", p.getGameProfile().getName());
        }
        return null;
    }

    // ------------------------------------------------------------------ tecla y comandos

    /** Se apretó la tecla de duelo: acepta un desafío pendiente o desafía al jugador al que se apunta. */
    public static void onKey(ServerPlayer p, int targetId) {
        if (!KenseiConfig.DUEL_ENABLED.get()) {
            p.displayClientMessage(Component.translatable("kensei.duel.disabled"), true);
            return;
        }
        if (of(p.getUUID()) != null) {
            return;
        }
        Pending pend = pendingFor(p.getUUID());
        if (pend != null) {
            if (pend.host.equals(p.getUUID())) {
                p.displayClientMessage(Component.translatable("kensei.duel.waiting"), true);
            } else {
                accept(p);
            }
            return;
        }
        // sin desafío pendiente: se abre el menú para elegir 1v1 o 2v2 y contra quién
        KenseiNet.openDuelMenu(p, targetId);
    }

    /** El menú de duelo pidió empezar: modo 1 (1v1) o 2 (2v2) y los puestos (id de entidad; -1 = rival de práctica). */
    public static void request(ServerPlayer host, int mode, int[] slots) {
        if (!KenseiConfig.DUEL_ENABLED.get()) {
            host.sendSystemMessage(Component.translatable("kensei.duel.disabled").withStyle(ChatFormatting.RED));
            return;
        }
        if (of(host.getUUID()) != null || pendingFor(host.getUUID()) != null) {
            host.sendSystemMessage(Component.translatable("kensei.duel.busy", host.getGameProfile().getName()).withStyle(ChatFormatting.RED));
            return;
        }
        if ((mode != 1 && mode != 2) || slots.length != (mode == 1 ? 1 : 3)) {
            return;
        }
        List<ServerPlayer> a = new ArrayList<>();
        List<ServerPlayer> b = new ArrayList<>();
        a.add(host);
        int botsA = 0;
        int botsB = 0;
        Set<UUID> used = new HashSet<>();
        used.add(host.getUUID());
        for (int i = 0; i < slots.length; i++) {
            boolean teamA = mode == 2 && i == 0;
            int id = slots[i];
            if (id < 0) {
                if (teamA) {
                    botsA++;
                } else {
                    botsB++;
                }
                continue;
            }
            Entity e = host.level().getEntity(id);
            if (!(e instanceof ServerPlayer sp) || !used.add(sp.getUUID())) {
                host.sendSystemMessage(Component.translatable("kensei.duel.bad_slot").withStyle(ChatFormatting.RED));
                return;
            }
            (teamA ? a : b).add(sp);
        }
        create(host, a, b, botsA, botsB);
    }

    public static void challenge(ServerPlayer host, ServerPlayer target) {
        create(host, List.of(host), List.of(target), 0, 0);
    }

    public static void challenge2v2(ServerPlayer host, ServerPlayer ally, ServerPlayer foe1, ServerPlayer foe2) {
        Set<UUID> ids = new HashSet<>(List.of(host.getUUID(), ally.getUUID(), foe1.getUUID(), foe2.getUUID()));
        if (ids.size() < 4) {
            host.sendSystemMessage(Component.translatable("kensei.duel.need_four").withStyle(ChatFormatting.RED));
            return;
        }
        create(host, List.of(host, ally), List.of(foe1, foe2), 0, 0);
    }

    private static void create(ServerPlayer host, List<ServerPlayer> a, List<ServerPlayer> b, int botsA, int botsB) {
        if (!KenseiConfig.DUEL_ENABLED.get()) {
            host.sendSystemMessage(Component.translatable("kensei.duel.disabled").withStyle(ChatFormatting.RED));
            return;
        }
        if (b.isEmpty() && botsB == 0) {
            host.displayClientMessage(Component.translatable("kensei.duel.need_target"), true);
            return;
        }
        if (a.size() == 1 && b.size() == 1 && a.get(0) == b.get(0)) {
            host.displayClientMessage(Component.translatable("kensei.duel.need_target"), true);
            return;
        }
        List<ServerPlayer> all = new ArrayList<>(a);
        all.addAll(b);
        for (ServerPlayer p : all) {
            Component err = problem(p, host);
            if (err != null) {
                host.sendSystemMessage(err.copy().withStyle(ChatFormatting.RED));
                return;
            }
        }
        if (all.size() == 1) {
            // solo el que desafía y rivales de práctica: no hay a quién esperar
            launch(host.getServer(), host, a, b, botsA, botsB);
            return;
        }
        Pending pend = new Pending(host.getUUID(), System.currentTimeMillis() + KenseiConfig.DUEL_CHALLENGE_SECONDS.get() * 1000L);
        for (ServerPlayer p : a) {
            pend.teamA.add(p.getUUID());
        }
        for (ServerPlayer p : b) {
            pend.teamB.add(p.getUUID());
        }
        pend.botsA = botsA;
        pend.botsB = botsB;
        pend.accepted.add(host.getUUID());
        PENDING.add(pend);
        String mode = pend.totalA() == 1 ? "1v1" : "2v2";
        host.sendSystemMessage(Component.translatable("kensei.duel.sent", mode, KenseiConfig.DUEL_CHALLENGE_SECONDS.get()).withStyle(ChatFormatting.GOLD));
        for (ServerPlayer p : all) {
            if (p == host) {
                continue;
            }
            MutableComponent accept = Component.translatable("kensei.duel.accept_btn").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kensei duel accept")));
            MutableComponent decline = Component.translatable("kensei.duel.decline_btn").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kensei duel decline")));
            p.sendSystemMessage(Component.literal("⚔ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("kensei.duel.invite", host.getGameProfile().getName(), mode).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("  ")).append(accept).append(Component.literal(" ")).append(decline));
        }
    }

    public static void accept(ServerPlayer p) {
        Pending pend = pendingFor(p.getUUID());
        if (pend == null) {
            p.sendSystemMessage(Component.translatable("kensei.duel.nothing").withStyle(ChatFormatting.RED));
            return;
        }
        if (pend.accepted.contains(p.getUUID())) {
            return;
        }
        if (KenseiConfig.DUEL_REQUIRE_KATANA.get() && !hasKatana(p)) {
            p.sendSystemMessage(Component.translatable("kensei.duel.need_katana", p.getGameProfile().getName()).withStyle(ChatFormatting.RED));
            return;
        }
        pend.accepted.add(p.getUUID());
        MinecraftServer server = p.getServer();
        for (UUID id : allOf(pend)) {
            ServerPlayer o = server.getPlayerList().getPlayer(id);
            if (o != null) {
                o.sendSystemMessage(Component.translatable("kensei.duel.accepted", p.getGameProfile().getName()).withStyle(ChatFormatting.GREEN));
            }
        }
        if (pend.accepted.size() >= pend.size()) {
            PENDING.remove(pend);
            start(server, pend);
        }
    }

    public static void decline(ServerPlayer p) {
        Pending pend = pendingFor(p.getUUID());
        if (pend == null) {
            p.sendSystemMessage(Component.translatable("kensei.duel.nothing").withStyle(ChatFormatting.RED));
            return;
        }
        PENDING.remove(pend);
        MinecraftServer server = p.getServer();
        for (UUID id : allOf(pend)) {
            ServerPlayer o = server.getPlayerList().getPlayer(id);
            if (o != null) {
                o.sendSystemMessage(Component.translatable("kensei.duel.declined", p.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    private static List<UUID> allOf(Pending p) {
        List<UUID> l = new ArrayList<>(p.teamA);
        l.addAll(p.teamB);
        return l;
    }

    /** Salir: el espectador deja de mirar, el peleador abandona, o se cancela el desafío en el que estaba. */
    public static void leave(ServerPlayer p) {
        Duel d = of(p.getUUID());
        if (d != null) {
            d.leave(p);
            p.sendSystemMessage(Component.translatable("kensei.duel.left").withStyle(ChatFormatting.YELLOW));
            return;
        }
        Pending pend = pendingFor(p.getUUID());
        if (pend != null) {
            decline(p);
            return;
        }
        p.sendSystemMessage(Component.translatable("kensei.duel.nothing").withStyle(ChatFormatting.RED));
    }

    public static void watch(ServerPlayer viewer, ServerPlayer target) {
        Duel d = fighterDuel(target.getUUID());
        if (d == null) {
            viewer.sendSystemMessage(Component.translatable("kensei.duel.watch_none", target.getGameProfile().getName()).withStyle(ChatFormatting.RED));
            return;
        }
        if (of(viewer.getUUID()) != null || pendingFor(viewer.getUUID()) != null) {
            viewer.sendSystemMessage(Component.translatable("kensei.duel.busy", viewer.getGameProfile().getName()).withStyle(ChatFormatting.RED));
            return;
        }
        d.addSpectator(viewer);
    }

    // ------------------------------------------------------------------ empezar

    private static void start(MinecraftServer server, Pending pend) {
        List<ServerPlayer> a = new ArrayList<>();
        List<ServerPlayer> b = new ArrayList<>();
        for (UUID id : pend.teamA) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                a.add(p);
            }
        }
        for (UUID id : pend.teamB) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                b.add(p);
            }
        }
        ServerPlayer host = server.getPlayerList().getPlayer(pend.host);
        if (host == null || a.size() != pend.teamA.size() || b.size() != pend.teamB.size()) {
            return; // alguien se desconectó
        }
        for (ServerPlayer p : a) {
            if (p.level() != host.level() || !p.isAlive()) {
                host.sendSystemMessage(Component.translatable("kensei.duel.other_dim", p.getGameProfile().getName()).withStyle(ChatFormatting.RED));
                return;
            }
        }
        for (ServerPlayer p : b) {
            if (p.level() != host.level() || !p.isAlive()) {
                host.sendSystemMessage(Component.translatable("kensei.duel.other_dim", p.getGameProfile().getName()).withStyle(ChatFormatting.RED));
                return;
            }
        }
        launch(server, host, a, b, pend.botsA, pend.botsB);
    }

    /** Busca la arena, crea los rivales de práctica que hagan falta y arranca el duelo. */
    private static void launch(MinecraftServer server, ServerPlayer host, List<ServerPlayer> a, List<ServerPlayer> b, int botsA, int botsB) {
        ServerLevel level = host.serverLevel();
        int radius = Math.max(a.size() + botsA, b.size() + botsB) <= 1 ? 15 : 19;
        DuelArena arena = findArena(level, host, radius);
        if (arena == null) {
            for (ServerPlayer p : concat(a, b)) {
                p.sendSystemMessage(Component.translatable("kensei.duel.no_arena").withStyle(ChatFormatting.RED));
            }
            return;
        }
        List<LivingEntity> ta = new ArrayList<>(a);
        List<LivingEntity> tb = new ArrayList<>(b);
        List<RivalEntity> bots = new ArrayList<>();
        for (int i = 0; i < botsA; i++) {
            RivalEntity r = newBot(level, arena, botsA == 1 ? "Aliado (bot)" : "Aliado " + (i + 1) + " (bot)");
            if (r != null) {
                ta.add(r);
                bots.add(r);
            }
        }
        for (int i = 0; i < botsB; i++) {
            RivalEntity r = newBot(level, arena, botsB == 1 ? "Rival" : "Rival " + (i + 1));
            if (r != null) {
                tb.add(r);
                bots.add(r);
            }
        }
        if (bots.size() != botsA + botsB) {
            for (RivalEntity r : bots) {
                r.discard();
            }
            host.sendSystemMessage(Component.translatable("kensei.duel.no_arena").withStyle(ChatFormatting.RED));
            return;
        }
        Duel duel = new Duel(server, level, arena, ta, tb);
        DUELS.add(duel);
        duel.begin();
        if (!bots.isEmpty()) {
            host.sendSystemMessage(Component.literal("⚔ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("kensei.duel.bot_started", duel.versusText()).withStyle(ChatFormatting.YELLOW)));
        } else if (KenseiConfig.DUEL_ANNOUNCE.get()) {
            Component msg = Component.literal("⚔ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("kensei.duel.announce", duel.versusText()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("  ")).append(duel.watchLink());
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    /** Un sitio libre en el cielo para la arena, cerca del que desafía (null si no hay). */
    private static DuelArena findArena(ServerLevel level, ServerPlayer host, int radius) {
        for (int attempt = 0; attempt < 6; attempt++) {
            DuelArena cand = new DuelArena(level, host.blockPosition().getX(), host.blockPosition().getZ() + attempt * (radius * 2 + 12), radius);
            boolean overlaps = false;
            for (Duel d : DUELS) {
                if (d.level == level && d.arena.bounds().intersects(cand.bounds())) {
                    overlaps = true;
                }
            }
            if (!overlaps && !cand.crowded()) {
                return cand;
            }
        }
        return null;
    }

    /** Duelo de práctica contra rivales del mod (comando): 1v1, o 2v2 con un rival aliado. Empieza al instante. */
    public static void startBotDuel(ServerPlayer host, boolean twoVsTwo) {
        if (twoVsTwo) {
            request(host, 2, new int[] {-1, -1, -1});
        } else {
            request(host, 1, new int[] {-1});
        }
    }

    private static RivalEntity newBot(ServerLevel level, DuelArena arena, String name) {
        RivalEntity r = ModEntities.RIVAL.get().create(level);
        if (r == null) {
            return null;
        }
        Vec3 c = arena.center();
        r.moveTo(c.x, c.y, c.z, 0.0F, 0.0F);
        r.setDuelBot(true);
        r.setCustomName(Component.literal(name));
        r.setCustomNameVisible(true);
        level.addFreshEntity(r);
        return r;
    }

    private static List<ServerPlayer> concat(List<ServerPlayer> a, List<ServerPlayer> b) {
        List<ServerPlayer> l = new ArrayList<>(a);
        l.addAll(b);
        return l;
    }

    // ------------------------------------------------------------------ cada tick, desconexiones y apagado

    public static void tick(MinecraftServer server) {
        if (!PENDING.isEmpty()) {
            long ms = System.currentTimeMillis();
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending pend = it.next();
                ServerPlayer host = server.getPlayerList().getPlayer(pend.host);
                if (host == null || ms > pend.expireMs) {
                    it.remove();
                    for (UUID id : allOf(pend)) {
                        ServerPlayer o = server.getPlayerList().getPlayer(id);
                        if (o != null) {
                            o.sendSystemMessage(Component.translatable("kensei.duel.expired").withStyle(ChatFormatting.YELLOW));
                        }
                    }
                    continue;
                }
                if (ms - pend.lastNag > 2000) {
                    pend.lastNag = ms;
                    for (UUID id : allOf(pend)) {
                        if (!pend.accepted.contains(id)) {
                            ServerPlayer o = server.getPlayerList().getPlayer(id);
                            if (o != null) {
                                o.displayClientMessage(Component.translatable("kensei.duel.invite_bar", host.getGameProfile().getName()), true);
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < DUELS.size(); i++) {
            Duel d = DUELS.get(i);
            d.tick();
            if (d.finished) {
                DUELS.remove(i--);
            }
        }
    }

    public static void onLogout(ServerPlayer p) {
        Pending pend = pendingFor(p.getUUID());
        if (pend != null) {
            PENDING.remove(pend);
            for (UUID id : allOf(pend)) {
                ServerPlayer o = p.getServer().getPlayerList().getPlayer(id);
                if (o != null && o != p) {
                    o.sendSystemMessage(Component.translatable("kensei.duel.expired").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    /** Se apaga el servidor (o se cierra el mundo): todos vuelven a su sitio y las arenas se deshacen. */
    public static void abortAll() {
        for (Duel d : new ArrayList<>(DUELS)) {
            d.finish(true);
        }
        DUELS.clear();
        PENDING.clear();
    }

    /** Termina todos los duelos (comando de administrador). */
    public static int endAll() {
        int n = DUELS.size();
        abortAll();
        return n;
    }
}

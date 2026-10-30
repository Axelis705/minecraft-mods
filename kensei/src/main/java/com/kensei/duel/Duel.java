package com.kensei.duel;

import com.kensei.KenseiConfig;
import com.kensei.combat.CombatState;
import com.kensei.combat.KatanaUtil;
import com.kensei.net.DuelStatePacket;
import com.kensei.net.FxPacket;
import com.kensei.net.KenseiNet;
import com.kensei.registry.ModSounds;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.kensei.entity.RivalEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un duelo en marcha: 1 contra 1 o 2 contra 2 en una arena propia, a rondas. Cada ronda tiene cuenta atrás, pelea con tiempo, K.O. y
 * pausa; gana el que llegue primero a la mayoría de rondas. Los espectadores ven todo desde el borde en modo espectador. Solo se usa en
 * el hilo del servidor.
 */
public final class Duel {
    public enum Phase { INTRO, FIGHT, ROUND_END, MATCH_END }

    private static final int INTRO_TICKS = 80;
    private static final int ROUND_END_TICKS = 75;
    private static final int MATCH_END_TICKS = 110;
    private static final UUID HP_MOD = UUID.fromString("7c1c2f4e-5d5c-4f6a-9a53-3d1d2c9b0e11");

    public static final class Fighter {
        public final UUID id;
        public final String name;
        public final int team;
        public final int slot;
        /** Es un rival de práctica (no un jugador). */
        public final boolean bot;
        public boolean ko;
        /** Cayó pero su equipo sigue: mira desde el aire hasta que acabe la ronda. */
        public boolean down;
        /** Se desconectó o abandonó. */
        public boolean gone;
        public int parries;
        public int dodges;
        public int hits;

        Fighter(UUID id, String name, int team, int slot, boolean bot) {
            this.id = id;
            this.name = name;
            this.team = team;
            this.slot = slot;
            this.bot = bot;
        }

        boolean alive() {
            return !ko && !gone;
        }
    }

    public final MinecraftServer server;
    public final ServerLevel level;
    public final DuelArena arena;
    public final List<Fighter> fighters = new ArrayList<>();
    public final Set<UUID> spectators = new LinkedHashSet<>();
    private final int teamSize;
    private final int roundsToWin;
    private final int roundTicks;

    private Phase phase = Phase.INTRO;
    private long phaseStart;
    private long phaseEnd;
    private int round = 1;
    private int timeLeft;
    private final int[] wins = new int[2];
    private int lastWinner = -1;
    private int lastReason = 0;
    private int drawStreak = 0;
    private boolean matchOver = false;
    private int matchWinner = -1;
    public boolean finished = false;

    public Duel(MinecraftServer server, ServerLevel level, DuelArena arena, List<? extends LivingEntity> teamA, List<? extends LivingEntity> teamB) {
        this.server = server;
        this.level = level;
        this.arena = arena;
        this.teamSize = Math.max(teamA.size(), teamB.size());
        this.roundsToWin = KenseiConfig.DUEL_ROUNDS.get() / 2 + 1;
        this.roundTicks = KenseiConfig.DUEL_ROUND_SECONDS.get() * 20;
        for (int i = 0; i < teamA.size(); i++) {
            fighters.add(fighterOf(teamA.get(i), 0, i));
        }
        for (int i = 0; i < teamB.size(); i++) {
            fighters.add(fighterOf(teamB.get(i), 1, i));
        }
    }

    private static Fighter fighterOf(LivingEntity e, int team, int slot) {
        String name = e instanceof ServerPlayer sp ? sp.getGameProfile().getName() : e.getName().getString();
        return new Fighter(e.getUUID(), name, team, slot, !(e instanceof ServerPlayer));
    }

    // ------------------------------------------------------------------ consultas

    public Fighter find(UUID id) {
        for (Fighter f : fighters) {
            if (f.id.equals(id)) {
                return f;
            }
        }
        return null;
    }

    public boolean isSpectator(UUID id) {
        return spectators.contains(id);
    }

    public Phase phase() {
        return phase;
    }

    /** Durante la cuenta atrás, el fin de ronda y para el que ya cayó no se puede atacar ni recibir golpes. */
    public boolean frozen(UUID id) {
        Fighter f = find(id);
        return f == null || phase != Phase.FIGHT || f.ko || f.gone;
    }

    private ServerPlayer player(UUID id) {
        return server.getPlayerList().getPlayer(id);
    }

    /** El ser vivo de este peleador (jugador conectado o rival de práctica), o null si ya no está. */
    private LivingEntity entity(Fighter f) {
        if (f.bot) {
            Entity e = level.getEntity(f.id);
            return e instanceof RivalEntity r && !r.isRemoved() ? r : null;
        }
        return player(f.id);
    }

    /** ¿Hay rivales de práctica en este duelo? */
    public boolean hasBots() {
        for (Fighter f : fighters) {
            if (f.bot) {
                return true;
            }
        }
        return false;
    }

    /** El enemigo vivo más cercano a este peleador (para que el rival de práctica sepa a quién perseguir), o null. */
    public LivingEntity enemyFor(UUID id) {
        Fighter me = find(id);
        if (me == null) {
            return null;
        }
        LivingEntity self = entity(me);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Fighter f : fighters) {
            if (f.team == me.team || !f.alive()) {
                continue;
            }
            LivingEntity e = entity(f);
            if (e == null) {
                continue;
            }
            double d = self == null ? 0.0 : self.distanceToSqr(e);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private long now() {
        return level.getGameTime();
    }

    private List<ServerPlayer> everyone() {
        List<ServerPlayer> list = new ArrayList<>();
        for (Fighter f : fighters) {
            ServerPlayer p = f.gone ? null : player(f.id);
            if (p != null) {
                list.add(p);
            }
        }
        for (UUID id : spectators) {
            ServerPlayer p = player(id);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    private String teamNames(int team) {
        StringBuilder sb = new StringBuilder();
        for (Fighter f : fighters) {
            if (f.team == team) {
                if (sb.length() > 0) {
                    sb.append(" + ");
                }
                sb.append(f.name);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ empezar

    public void begin() {
        arena.build();
        for (Fighter f : fighters) {
            LivingEntity e = entity(f);
            if (e == null) {
                f.gone = true;
                f.ko = true;
                continue;
            }
            if (e instanceof ServerPlayer p) {
                DuelBackup.save(p);
            }
            applyHealth(e);
        }
        startRound();
    }

    private void applyHealth(LivingEntity p) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) {
            return;
        }
        inst.removeModifier(HP_MOD);
        double extra = KenseiConfig.DUEL_HEALTH.get() - inst.getBaseValue();
        if (extra > 0 || (extra < 0 && p instanceof RivalEntity)) { // el rival de práctica tiene la misma vida que el jugador en el duelo
            inst.addTransientModifier(new AttributeModifier(HP_MOD, "kensei_duel", extra, AttributeModifier.Operation.ADDITION));
        }
    }

    private void freeze(LivingEntity p, int ticks) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 10, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false, false));
    }

    private void unfreeze(ServerPlayer p) {
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        p.removeEffect(MobEffects.JUMP);
    }

    private void prepare(ServerPlayer p, Fighter f) {
        p.removeAllEffects();
        p.clearFire();
        p.stopUsingItem();
        p.setGameMode(GameType.ADVENTURE);
        Vec3 s = arena.spawn(f.team, f.slot, teamSize);
        p.setDeltaMovement(0, 0, 0);
        p.fallDistance = 0;
        p.teleportTo(level, s.x, s.y, s.z, DuelArena.spawnYaw(f.team), 8.0F);
        applyHealth(p);
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(20.0F);
        p.invulnerableTime = 0;
        p.setAbsorptionAmount(0.0F);
        CombatState.of(p).reset();
        freeze(p, INTRO_TICKS + 4);
    }

    private void prepareBot(RivalEntity b, Fighter f) {
        b.removeAllEffects();
        b.clearFire();
        Vec3 s = arena.spawn(f.team, f.slot, teamSize);
        float yaw = DuelArena.spawnYaw(f.team);
        b.setDeltaMovement(0, 0, 0);
        b.fallDistance = 0;
        b.moveTo(s.x, s.y, s.z, yaw, 0.0F);
        b.setYHeadRot(yaw);
        b.setYBodyRot(yaw);
        applyHealth(b);
        b.setHealth(b.getMaxHealth());
        b.invulnerableTime = 0;
        b.setAbsorptionAmount(0.0F);
        b.resetCombat();
        b.getNavigation().stop();
    }

    private void startRound() {
        phase = Phase.INTRO;
        phaseStart = now();
        phaseEnd = phaseStart + INTRO_TICKS;
        timeLeft = roundTicks;
        lastWinner = -1;
        for (Fighter f : fighters) {
            f.ko = f.gone;
            f.down = false;
            LivingEntity e = entity(f);
            if (e instanceof ServerPlayer p && !f.gone) {
                prepare(p, f);
            } else if (e instanceof RivalEntity b && !f.gone) {
                prepareBot(b, f);
            }
        }
        sendState();
    }

    // ------------------------------------------------------------------ cada tick

    public void tick() {
        if (finished) {
            return;
        }
        long now = now();
        // los que se desconectaron
        for (Fighter f : fighters) {
            if (!f.gone && entity(f) == null) {
                markGone(f);
            }
        }
        if (finished) {
            return;
        }
        switch (phase) {
            case INTRO -> {
                long el = now - phaseStart;
                if (el == 24 || el == 42 || el == 60) {
                    sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9F, el == 60 ? 1.5F : 1.0F);
                }
                if (now >= phaseEnd) {
                    phase = Phase.FIGHT;
                    phaseStart = now;
                    for (Fighter f : fighters) {
                        ServerPlayer p = player(f.id);
                        if (p != null && f.alive()) {
                            unfreeze(p);
                        }
                    }
                    sound(SoundEvents.ENDER_DRAGON_GROWL, 0.35F, 1.3F);
                    sound(ModSounds.HEAVY_CHARGE.get(), 0.7F, 1.2F);
                    sendState();
                }
            }
            case FIGHT -> {
                timeLeft--;
                if (timeLeft <= 0) {
                    timeUp();
                } else if (now % 2 == 0) {
                    sendState();
                }
            }
            case ROUND_END -> {
                if (now >= phaseEnd) {
                    if (matchOver) {
                        toMatchEnd();
                    } else {
                        round++;
                        startRound();
                    }
                } else if (now % 5 == 0) {
                    sendState();
                }
            }
            case MATCH_END -> {
                if (now >= phaseEnd) {
                    finish(false);
                } else if (now % 10 == 0) {
                    sendState();
                }
            }
            default -> {
            }
        }
        if (finished) {
            return;
        }
        // espectadores: que no se alejen
        Vec3 spot = arena.spectatorSpot();
        for (UUID id : new ArrayList<>(spectators)) {
            ServerPlayer p = player(id);
            if (p == null) {
                spectators.remove(id);
                continue;
            }
            double dx = p.getX() - arena.cx;
            double dz = p.getZ() - arena.cz;
            if (p.level() != level || dx * dx + dz * dz > Math.pow(arena.radius + 30, 2) || p.getY() < arena.floorY - 12 || p.getY() > arena.floorY + 45) {
                p.teleportTo(level, spot.x, spot.y, spot.z, 180.0F, 15.0F);
            }
        }
    }

    // ------------------------------------------------------------------ K.O. y fin de ronda

    /** Un peleador iba a morir: se queda con 1 de vida y cae K.O. Devuelve true si el golpe fatal fue de verdad contado. */
    public void onFatal(LivingEntity victim, Entity attacker) {
        Fighter f = find(victim.getUUID());
        if (f == null || phase != Phase.FIGHT || f.ko) {
            return;
        }
        f.ko = true;
        victim.setHealth(1.0F);
        victim.stopUsingItem();
        victim.removeAllEffects();
        victim.clearFire();
        freeze(victim, 200);
        KenseiNet.fx(level, FxPacket.KO, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 0.0F, 0,
                attacker == null ? -1 : attacker.getId(), victim.getId(), victim.getYRot());
        sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.5F);
        boolean teamAlive = false;
        for (Fighter o : fighters) {
            if (o.team == f.team && o.alive()) {
                teamAlive = true;
            }
        }
        if (teamAlive) {
            // su compañero sigue peleando: mientras tanto mira la pelea desde el aire
            f.down = true;
            if (victim instanceof ServerPlayer vp) {
                vp.setGameMode(GameType.SPECTATOR);
            }
        }
        checkRoundEnd();
        sendState();
    }

    private int aliveCount(int team) {
        int n = 0;
        for (Fighter f : fighters) {
            if (f.team == team && f.alive()) {
                n++;
            }
        }
        return n;
    }

    private void checkRoundEnd() {
        if (phase != Phase.FIGHT) {
            return;
        }
        int a = aliveCount(0);
        int b = aliveCount(1);
        if (a > 0 && b > 0) {
            return;
        }
        endRound(a > 0 ? 0 : b > 0 ? 1 : -1, a == 0 && b == 0 ? DuelStatePacket.REASON_DRAW : DuelStatePacket.REASON_KO);
    }

    private double teamHealth(int team) {
        double hp = 0;
        double max = 0;
        for (Fighter f : fighters) {
            if (f.team != team) {
                continue;
            }
            LivingEntity p = entity(f);
            max += p == null ? 20.0 : p.getMaxHealth();
            if (p != null && f.alive()) {
                hp += p.getHealth();
            }
        }
        return max <= 0 ? 0 : hp / max;
    }

    private void timeUp() {
        double a = teamHealth(0);
        double b = teamHealth(1);
        if (Math.abs(a - b) < 0.005) {
            endRound(-1, DuelStatePacket.REASON_DRAW);
        } else {
            endRound(a > b ? 0 : 1, DuelStatePacket.REASON_TIME);
        }
    }

    private void endRound(int winner, int reason) {
        phase = Phase.ROUND_END;
        phaseStart = now();
        phaseEnd = phaseStart + ROUND_END_TICKS;
        lastWinner = winner;
        lastReason = reason;
        for (ServerPlayer p : everyone()) {
            if (find(p.getUUID()) != null) {
                p.stopUsingItem();
            }
        }
        if (winner >= 0) {
            wins[winner]++;
            drawStreak = 0;
            if (wins[winner] >= roundsToWin) {
                matchOver = true;
                matchWinner = winner;
            }
        } else if (++drawStreak >= 2) {
            matchOver = true; // dos empates seguidos: el duelo queda empatado
            matchWinner = -1;
        }
        if (reason != DuelStatePacket.REASON_KO) {
            sound(SoundEvents.BELL_BLOCK, 1.0F, 0.8F);
        }
        sendState();
    }

    private void toMatchEnd() {
        phase = Phase.MATCH_END;
        phaseStart = now();
        phaseEnd = phaseStart + MATCH_END_TICKS;
        for (Fighter f : fighters) {
            ServerPlayer p = player(f.id);
            if (p != null) {
                unfreeze(p);
                if (f.down) {
                    f.down = false;
                    p.setGameMode(GameType.ADVENTURE);
                }
            }
        }
        sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
        Component msg;
        if (matchWinner < 0) {
            msg = Component.translatable("kensei.duel.draw");
        } else {
            msg = Component.translatable("kensei.duel.winner", teamNames(matchWinner), wins[matchWinner], wins[1 - matchWinner]);
        }
        if (KenseiConfig.DUEL_ANNOUNCE.get()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("⚔ ").withStyle(ChatFormatting.GOLD).append(msg), false);
        } else {
            for (ServerPlayer p : everyone()) {
                p.sendSystemMessage(msg);
            }
        }
        sendState();
    }

    private void markGone(Fighter f) {
        f.gone = true;
        f.ko = true;
        for (ServerPlayer p : everyone()) {
            p.sendSystemMessage(Component.translatable("kensei.duel.forfeit", f.name).withStyle(ChatFormatting.YELLOW));
        }
        // si todo su equipo se fue, pierde el duelo ya
        if (allGone(f.team) && phase != Phase.MATCH_END) {
            matchOver = true;
            matchWinner = 1 - f.team;
            wins[matchWinner] = Math.max(wins[matchWinner], roundsToWin);
            toMatchEnd();
            return;
        }
        if (phase == Phase.FIGHT) {
            checkRoundEnd();
        }
        sendState();
    }

    private boolean allGone(int team) {
        for (Fighter f : fighters) {
            if (f.team == team && !f.gone) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ espectadores y salidas

    public void addSpectator(ServerPlayer p) {
        if (spectators.contains(p.getUUID()) || find(p.getUUID()) != null) {
            return;
        }
        DuelBackup.save(p);
        spectators.add(p.getUUID());
        p.setGameMode(GameType.SPECTATOR);
        Vec3 s = arena.spectatorSpot();
        p.teleportTo(level, s.x, s.y, s.z, 180.0F, 15.0F);
        p.sendSystemMessage(Component.translatable("kensei.duel.watching").withStyle(ChatFormatting.AQUA));
        KenseiNet.sendDuel(p, buildState());
    }

    /** Alguien sale: un espectador deja de mirar; un peleador abandona (pierde). */
    public void leave(ServerPlayer p) {
        UUID id = p.getUUID();
        if (spectators.remove(id)) {
            DuelBackup.restore(p, false);
            KenseiNet.sendDuel(p, inactive());
            return;
        }
        Fighter f = find(id);
        if (f == null || f.gone) {
            return;
        }
        f.gone = true;
        f.ko = true;
        p.removeAllEffects();
        removeHealth(p);
        DuelBackup.restore(p, KenseiConfig.DUEL_RESTORE_HEALTH.get());
        KenseiNet.sendDuel(p, inactive());
        for (ServerPlayer o : everyone()) {
            o.sendSystemMessage(Component.translatable("kensei.duel.forfeit", f.name).withStyle(ChatFormatting.YELLOW));
        }
        if (allGone(f.team) && phase != Phase.MATCH_END) {
            matchOver = true;
            matchWinner = 1 - f.team;
            wins[matchWinner] = Math.max(wins[matchWinner], roundsToWin);
            toMatchEnd();
        } else if (phase == Phase.FIGHT) {
            checkRoundEnd();
        }
        sendState();
    }

    private void removeHealth(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst != null) {
            inst.removeModifier(HP_MOD);
        }
    }

    /** Termina el duelo: devuelve a todos a su sitio y la arena a como estaba. */
    public void finish(boolean abort) {
        if (finished) {
            return;
        }
        finished = true;
        boolean vitals = KenseiConfig.DUEL_RESTORE_HEALTH.get();
        for (Fighter f : fighters) {
            if (f.bot) {
                Entity b = level.getEntity(f.id);
                if (b != null) {
                    b.discard();
                }
                continue;
            }
            ServerPlayer p = player(f.id);
            if (p == null) {
                continue; // se restaura al volver a entrar (el respaldo queda en sus datos)
            }
            unfreeze(p);
            p.removeAllEffects();
            removeHealth(p);
            if (!f.gone) {
                DuelBackup.restore(p, vitals);
            }
            KenseiNet.sendDuel(p, inactive());
        }
        for (UUID id : spectators) {
            ServerPlayer p = player(id);
            if (p != null) {
                DuelBackup.restore(p, false);
                KenseiNet.sendDuel(p, inactive());
            }
        }
        spectators.clear();
        arena.restore();
        if (abort) {
            for (Fighter f : fighters) {
                ServerPlayer p = player(f.id);
                if (p != null) {
                    p.sendSystemMessage(Component.translatable("kensei.duel.aborted").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    // ------------------------------------------------------------------ estadísticas

    public void stat(UUID id, int kind) {
        Fighter f = find(id);
        if (f == null) {
            return;
        }
        switch (kind) {
            case 0 -> f.parries++;
            case 1 -> f.dodges++;
            default -> f.hits++;
        }
    }

    // ------------------------------------------------------------------ avisos a los clientes

    private void sound(SoundEvent s, float vol, float pitch) {
        for (ServerPlayer p : everyone()) {
            p.playNotifySound(s, SoundSource.MASTER, vol, pitch);
        }
    }

    private static DuelStatePacket inactive() {
        return new DuelStatePacket();
    }

    private DuelStatePacket buildState() {
        DuelStatePacket m = new DuelStatePacket();
        m.active = true;
        m.phase = switch (phase) {
            case INTRO -> DuelStatePacket.PHASE_INTRO;
            case FIGHT -> DuelStatePacket.PHASE_FIGHT;
            case ROUND_END -> DuelStatePacket.PHASE_ROUND_END;
            case MATCH_END -> DuelStatePacket.PHASE_MATCH_END;
        };
        long now = now();
        m.phaseTicks = phase == Phase.FIGHT ? 0 : (int) Math.max(0, phaseEnd - now);
        m.phaseTotal = phase == Phase.INTRO ? INTRO_TICKS : phase == Phase.ROUND_END ? ROUND_END_TICKS : phase == Phase.MATCH_END ? MATCH_END_TICKS : 0;
        m.round = round;
        m.roundsToWin = roundsToWin;
        m.roundTicks = roundTicks;
        m.timeLeft = Math.max(0, timeLeft);
        m.winsA = wins[0];
        m.winsB = wins[1];
        m.winner = phase == Phase.MATCH_END ? matchWinner : lastWinner;
        m.reason = lastReason;
        for (Fighter f : fighters) {
            DuelStatePacket.F o = new DuelStatePacket.F();
            o.id = f.id;
            o.name = f.name;
            o.team = f.team;
            o.parries = f.parries;
            o.dodges = f.dodges;
            o.hits = f.hits;
            o.state = f.gone ? 2 : f.ko ? 1 : 0;
            LivingEntity le = entity(f);
            if (le instanceof RivalEntity b) {
                o.entityId = b.getId();
                o.hp = b.getHealth();
                o.maxHp = b.getMaxHealth();
                o.energy = b.energyInt();
                o.heavyCd = b.heavyCooldownTicks();
                o.stamina = b.staminaFraction();
                o.exhausted = b.isExhausted();
                o.guard = b.isGuarding();
            } else if (le instanceof ServerPlayer p) {
                o.entityId = p.getId();
                o.hp = p.getHealth();
                o.maxHp = p.getMaxHealth();
                CombatState s = CombatState.of(p);
                o.energy = (int) s.energy;
                o.heavyCd = (int) Math.max(0, s.heavyReadyAt - now);
                o.stamina = Math.max(0.0F, Math.min(1.0F, s.stamina / KenseiConfig.staminaMax()));
                o.exhausted = s.exhausted(now);
                o.guard = KatanaUtil.guarding(p);
            } else {
                o.entityId = -1;
                o.maxHp = 20.0F;
            }
            m.fighters.add(o);
        }
        return m;
    }

    public void sendState() {
        DuelStatePacket m = buildState();
        for (ServerPlayer p : everyone()) {
            KenseiNet.sendDuel(p, m);
        }
    }

    /** Un texto con botón para que otros vengan a ver. */
    public MutableComponent watchLink() {
        String first = fighters.get(0).name;
        for (Fighter f : fighters) {
            if (!f.bot) {
                first = f.name;
                break;
            }
        }
        String cmd = "/kensei duel watch " + first;
        return Component.translatable("kensei.duel.watch_btn").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)));
    }

    public String versusText() {
        return teamNames(0) + "  vs  " + teamNames(1);
    }
}

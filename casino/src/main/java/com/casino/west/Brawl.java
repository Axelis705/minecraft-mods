package com.casino.west;

import com.casino.chip.Chips;
import com.casino.entity.DealerAnim;
import com.casino.entity.DealerEntity;
import com.casino.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pelea de cantina: de vez en cuando dos vaqueros del saloon se agarran a golpes. La pianola se calla, los demás se emocionan, los
 * jugadores pueden apostarle a uno (clic derecho al peleador, 25 fichas, paga x1.9) y al final el sheriff pone orden. La dirige la
 * pianola del saloon ({@link PianoBE}): solo hay pelea si alguien está cerca para verla.
 */
public final class Brawl {
    public static final int BET = 25;
    public static final int PAY_X10 = 19;
    private static final int APPROACH = 20, BET_WINDOW = 70, HP = 4, END_SHOW = 60;
    /** Peleadores que están en una pelea ahora mismo (para el clic de apostar). */
    private static final Map<UUID, Brawl> BY_FIGHTER = new HashMap<>();

    private final ServerLevel level;
    private final BlockPos piano;
    private final DealerEntity[] f = new DealerEntity[2];
    private final Vec3[] spot = new Vec3[2];
    private final Vec3[] from = new Vec3[2];
    private final int[] hp = {HP, HP};
    private final Map<UUID, int[]> bets = new HashMap<>();
    private int t;
    private int nextHit;
    private int winner = -1;
    private int endAt;
    private boolean done;

    private Brawl(ServerLevel level, BlockPos piano, DealerEntity a, DealerEntity b) {
        this.level = level;
        this.piano = piano;
        f[0] = a;
        f[1] = b;
    }

    /** Busca dos vaqueros cerca de la pianola y arma la pelea (o null si no hay con quién). */
    static Brawl start(ServerLevel level, BlockPos piano) {
        List<DealerEntity> pool = new ArrayList<>();
        for (DealerEntity d : level.getEntitiesOfClass(DealerEntity.class, new AABB(piano).inflate(14, 3, 14))) {
            if (d.isPatron() && d.getVariant() >= DealerEntity.WEST && !d.isPassenger() && !BY_FIGHTER.containsKey(d.getUUID())
                    && Math.abs(d.getY() - piano.getY()) < 1.5) {
                pool.add(d);
            }
        }
        if (pool.size() < 2) {
            return null;
        }
        DealerEntity a = pool.get(level.getRandom().nextInt(pool.size()));
        DealerEntity b = null;
        double best = Double.MAX_VALUE;
        for (DealerEntity d : pool) {
            double dist = d.distanceToSqr(a);
            if (d != a && dist < best && dist > 1) {
                best = dist;
                b = d;
            }
        }
        if (b == null || best > 12 * 12) {
            return null;
        }
        Brawl w = new Brawl(level, piano, a, b);
        Vec3 mid = a.position().add(b.position()).scale(0.5);
        Vec3 dir = b.position().subtract(a.position()).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : dir.normalize();
        w.spot[0] = mid.subtract(dir.scale(0.75));
        w.spot[1] = mid.add(dir.scale(0.75));
        for (int i = 0; i < 2; i++) {
            DealerEntity d = w.f[i];
            d.getNavigation().stop();
            d.setNoAi(true);
            d.setCombat(4);
            w.from[i] = d.position();
            BY_FIGHTER.put(d.getUUID(), w);
        }
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
        Component names = Component.translatable("casino.brawl.start", name(a), name(b), BET, "x1.9");
        for (Player p : level.players()) {
            if (p.distanceToSqr(mid) < 24 * 24) {
                Plat.msg(p, names.copy().withStyle(ChatFormatting.GOLD), false);
            }
        }
        // los demás vaqueros se emocionan
        for (DealerEntity d : pool) {
            if (d != a && d != b && d.distanceToSqr(mid) < 10 * 10) {
                d.anim(DealerAnim.CHEER);
            }
        }
        return w;
    }

    static Component name(DealerEntity d) {
        return Component.translatable("casino.npc.patron_" + d.getVariant());
    }

    boolean done() {
        return done;
    }

    /** Clic derecho a un peleador: apostarle (solo al principio de la pelea). */
    public static boolean interact(DealerEntity d, ServerPlayer p) {
        Brawl w = BY_FIGHTER.get(d.getUUID());
        if (w == null) {
            return false;
        }
        int side = w.f[0] == d ? 0 : 1;
        if (w.t > BET_WINDOW || w.winner >= 0) {
            Plat.msg(p, Component.translatable("casino.brawl.closed").withStyle(ChatFormatting.YELLOW), true);
            return true;
        }
        if (w.bets.containsKey(p.getUUID())) {
            Plat.msg(p, Component.translatable("casino.brawl.already").withStyle(ChatFormatting.YELLOW), true);
            return true;
        }
        if (!Chips.take(p, BET)) {
            Plat.msg(p, Component.translatable("casino.plinko.no_chips", BET).withStyle(ChatFormatting.RED), true);
            return true;
        }
        w.bets.put(p.getUUID(), new int[]{side, BET});
        Plat.msg(p, Component.translatable("casino.brawl.bet", BET, name(d)).withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** Un tick de la pelea (lo llama la pianola en el servidor). */
    void tick() {
        if (done) {
            return;
        }
        for (DealerEntity d : f) {
            if (d == null || d.isRemoved()) {
                abort();
                return;
            }
        }
        t++;
        // se acercan y se ponen frente a frente
        if (t <= APPROACH) {
            double k = t / (double) APPROACH;
            k = k * k * (3 - 2 * k);
            for (int i = 0; i < 2; i++) {
                Vec3 p = from[i].add(spot[i].subtract(from[i]).scale(k));
                f[i].moveTo(p.x, f[i].getY(), p.z, f[i].getYRot(), 0);
            }
        }
        face();
        if (winner >= 0) {
            if (t < endAt - END_SHOW + 10) {
                // el perdedor sale volando un poco para atrás
                int l = 1 - winner;
                Vec3 back = f[l].position().subtract(f[winner].position()).multiply(1, 0, 1).normalize().scale(0.18);
                f[l].moveTo(f[l].getX() + back.x, f[l].getY(), f[l].getZ() + back.z, f[l].getYRot(), 0);
            }
            if (t >= endAt) {
                finish();
            }
            return;
        }
        if (t == APPROACH + 4) {
            level.playSound(null, f[0].getX(), f[0].getY() + 1, f[0].getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.7F);
        }
        if (t >= APPROACH + 10 && t >= nextHit) {
            int a = level.getRandom().nextInt(2), b = 1 - a;
            DealerEntity att = f[a], def = f[b];
            att.anim(level.getRandom().nextBoolean() ? DealerAnim.PUNCH : DealerAnim.PUNCH_L);
            nextHit = t + 8 + level.getRandom().nextInt(7);
            boolean hit = level.getRandom().nextFloat() < 0.62F;
            Vec3 head = def.position().add(0, 1.6, 0);
            if (hit) {
                hp[b]--;
                level.playSound(null, head.x, head.y, head.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.3F);
                level.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 8, 0.15, 0.15, 0.15, 0.2);
                Vec3 push = def.position().subtract(att.position()).multiply(1, 0, 1).normalize().scale(0.12);
                def.moveTo(def.getX() + push.x, def.getY(), def.getZ() + push.z, def.getYRot(), 0);
                att.moveTo(att.getX() + push.x, att.getY(), att.getZ() + push.z, att.getYRot(), 0);
            } else {
                level.playSound(null, head.x, head.y, head.z, SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 0.8F, 1.2F);
            }
            if (hp[b] <= 0) {
                win(a);
            }
        }
        // si tarda mucho, el sheriff la para (gana el que lleve más vida)
        if (winner < 0 && t > 400) {
            win(hp[0] >= hp[1] ? 0 : 1);
        }
    }

    private void face() {
        for (int i = 0; i < 2; i++) {
            DealerEntity me = f[i], other = f[1 - i];
            double dx = other.getX() - me.getX(), dz = other.getZ() - me.getZ();
            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            me.setYRot(yaw);
            me.setYHeadRot(yaw);
            me.setYBodyRot(yaw);
        }
    }

    private void win(int w) {
        winner = w;
        endAt = t + END_SHOW;
        f[w].anim(DealerAnim.CHEER);
        f[1 - w].anim(DealerAnim.SHAKE);
        f[1 - w].setCombat(0);
        Vec3 mid = f[0].position().add(f[1].position()).scale(0.5);
        level.playSound(null, mid.x, mid.y + 1, mid.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 1.2F, 0.7F);
        // el sheriff pone orden
        DealerEntity sheriff = null;
        double best = 30 * 30;
        for (DealerEntity d : level.getEntitiesOfClass(DealerEntity.class, new AABB(BlockPos.containing(mid)).inflate(30, 6, 30))) {
            if (d.getGame() == DealerEntity.GAME_GUARD && d.getVariant() >= DealerEntity.WEST && d.distanceToSqr(mid) < best) {
                best = d.distanceToSqr(mid);
                sheriff = d;
            }
        }
        if (sheriff != null) {
            sheriff.say(DealerEntity.SAY + 3);
            sheriff.anim(DealerAnim.STOP);
        }
        Component wn = name(f[w]);
        for (Player p : level.players()) {
            if (p.distanceToSqr(mid) < 24 * 24) {
                Plat.msg(p, Component.translatable("casino.brawl.end", wn).withStyle(ChatFormatting.GOLD), false);
            }
        }
        // pagar apuestas
        for (Map.Entry<UUID, int[]> e : bets.entrySet()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                continue;
            }
            int[] b = e.getValue();
            if (b[0] == w) {
                int pay = b[1] * PAY_X10 / 10;
                Chips.give(p, pay);
                Plat.msg(p, Component.translatable("casino.brawl.won", pay).withStyle(ChatFormatting.GREEN), true);
            } else {
                Plat.msg(p, Component.translatable("casino.brawl.lost", b[1]).withStyle(ChatFormatting.RED), true);
            }
        }
        bets.clear();
    }

    private void finish() {
        for (DealerEntity d : f) {
            if (d != null && !d.isRemoved()) {
                d.setCombat(0);
                d.setNoAi(false);
                BY_FIGHTER.remove(d.getUUID());
            }
        }
        done = true;
    }

    /** Algo salió mal (un peleador desapareció): se devuelven las apuestas y todos a sus mesas. */
    void abort() {
        for (Map.Entry<UUID, int[]> e : bets.entrySet()) {
            ServerPlayer p = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                Chips.give(p, e.getValue()[1]);
            }
        }
        bets.clear();
        for (DealerEntity d : f) {
            if (d != null) {
                if (!d.isRemoved()) {
                    d.setCombat(0);
                    d.setNoAi(false);
                }
                BY_FIGHTER.remove(d.getUUID());
            }
        }
        done = true;
    }

    /** ¿Hay alguien cerca de la pianola para ver la pelea? */
    static boolean audience(ServerLevel level, BlockPos piano) {
        for (Player p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(piano.getX() + 0.5, piano.getY(), piano.getZ() + 0.5) < 18 * 18) {
                return true;
            }
        }
        return false;
    }

    static boolean entityGone(Entity e) {
        return e == null || e.isRemoved();
    }
}

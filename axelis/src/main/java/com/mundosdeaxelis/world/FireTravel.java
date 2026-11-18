package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.AnclaMagma;
import com.mundosdeaxelis.entity.CorazonMagma;
import com.mundosdeaxelis.entity.LarvaMagma;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.FadePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.relic.RelicFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Viaje al corazón del volcán (jefe de la etapa 4). Lo llama {@link BossTravel#enter}. Los jugadores aparecen en la antesala
 * (reciben la ficha del jefe y pueden salir sin pelear); al cruzar el puente y pisar la arena empieza la intro: tiembla la
 * caverna, el techo se agrieta y baja el Corazón de Magma colgado de sus cadenas. Entonces se sella el túnel y empieza la pelea.
 * Quien cae al mar de lava sale escupido de vuelta al centro (con daño), para que no pierda sus cosas.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class FireTravel {
    private static final int DARK = 0x1A0804;
    private static final AABB ALL = new AABB(-70, 0, -70, 70, 200, 100);

    private enum Phase {WAITING, INTRO, FIGHT}

    private Phase phase = Phase.WAITING;
    private int emptyT;

    private static final Map<ResourceKey<Level>, FireTravel> INSTANCES = new HashMap<>();

    public static void clearRuntime() {
        INSTANCES.clear();
    }

    private FireTravel() {
    }

    public static FireTravel of(Level level) {
        return INSTANCES.computeIfAbsent(level.dimension(), k -> new FireTravel());
    }

    public static void enter(MinecraftServer server, List<ServerPlayer> players) {
        ServerLevel level = server.getLevel(ModDimensions.VOLCANO_COPIES.get(0));
        if (level == null) {
            for (ServerPlayer p : players) {
                p.sendSystemMessage(Component.literal("No se encontró esta dimensión. Revisa que el mod esté bien instalado."));
            }
            return;
        }
        List<UUID> ids = new ArrayList<>();
        for (ServerPlayer p : players) {
            ids.add(p.getUUID());
            ModNetwork.sendTo(p, new FadePacket(DARK, 0, 6000, 0));
            Titles.actionBar(p, "Bajas al corazón del volcán... un momento.");
        }
        start(server, ids);
    }

    private static void start(MinecraftServer server, List<UUID> ids) {
        if (BossTravel.locked(server)) {
            Scheduler.runLater(20, () -> start(server, ids));
            return;
        }
        ServerLevel level = BossTravel.pickCopy(server, CorazonMagma.STAGE);
        if (level == null) {
            BossTravel.noCopy(server, ids, DARK);
            return;
        }
        BossTravel.lock(server);
        FireTravel me = of(level);
        boolean[] entered = {false};
        BossTravel.watchdog(server, ids, entered);
        FireArena.ensureBuilt(level, BossTravel.guard(server, ids, entered, () -> {
            me.resetFight(level);
            entered[0] = true;
            me.finishEnter(server, ids, level);
        }));
    }

    void resetFight(ServerLevel level) {
        DungeonReset.bump(level);
        for (CorazonMagma b : level.getEntitiesOfClass(CorazonMagma.class, ALL)) {
            b.clearFight(level);
            b.discard();
        }
        for (AnclaMagma a : level.getEntitiesOfClass(AnclaMagma.class, ALL)) {
            a.discard();
        }
        for (LarvaMagma l : level.getEntitiesOfClass(LarvaMagma.class, ALL)) {
            l.discard();
        }
        Props.clear(level);
        FireArena.sealGateway(level, false);
        FireArena.closeCeiling(level);
        FireArena.restoreFloor(level);
        phase = Phase.WAITING;
    }

    private void finishEnter(MinecraftServer server, List<UUID> ids, ServerLevel level) {
        BossTravel.unlock();
        List<ServerPlayer> group = new ArrayList<>();
        int i = 0;
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                continue;
            }
            if (!BossTravel.inArena(p.level())) {
                BossTravel.saveReturn(p);
            }
            Vec3 s = FireArena.entryPoint(i);
            p.teleportTo(level, s.x, s.y, s.z, 180.0F, 0.0F);
            p.resetFallDistance();
            p.clearFire();
            ModNetwork.sendTo(p, new FadePacket(DARK, 0, 5, 40));
            Titles.show(p, "Corazón del volcán", "Cruza el puente: algo late bajo la piedra");
            group.add(p);
            i++;
        }
        Scheduler.runLater(60, () -> {
            if (phase != Phase.WAITING) {
                return;
            }
            List<ServerPlayer> now = alive(level);
            BossSkip.offer(now);
            for (ServerPlayer q : group) {
                if (q.level() == level) {
                    BossSkip.dossier(q, now);
                }
            }
        });
    }

    public boolean canLeave() {
        return phase == Phase.WAITING;
    }

    public boolean running(ServerLevel level) {
        if (phase != Phase.FIGHT) {
            return !level.players().isEmpty();
        }
        for (CorazonMagma b : level.getEntitiesOfClass(CorazonMagma.class, ALL)) {
            if (b.isAlive()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        boolean slow = e.getServer().getTickCount() % 5 == 0;
        for (ResourceKey<Level> key : ModDimensions.VOLCANO_COPIES) {
            ServerLevel l = e.getServer().getLevel(key);
            if (l == null || l.players().isEmpty() && !slow) {
                continue;
            }
            rescueFromLava(l);
            if (slow) {
                of(l).tick(l);
            }
        }
    }

    /** Quien cae al mar de lava sale escupido al centro de la arena (con daño y ardiendo un poco). */
    private static void rescueFromLava(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || !FireArena.fellIn(p.getX(), p.getY(), p.getZ())) {
                continue;
            }
            boolean inArena = FireArena.inArena(p.getX(), p.getZ(), -1.0);
            Vec3 to;
            if (inArena) {
                Vec3 d = new Vec3(p.getX() - 0.5, 0, p.getZ() - 0.5);
                d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
                to = new Vec3(0.5 + d.x * 4.5, FireArena.Y + 1.2, 0.5 + d.z * 4.5);
            } else {
                // en el foso alrededor (o bajo el puente): de vuelta al puente
                to = new Vec3(0.5, FireArena.Y + 1.2, Math.max(FireArena.R + 2.5, Math.min(FireArena.AZ - FireArena.AR, p.getZ())));
            }
            level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 0.5, p.getZ(), 20, 0.4, 0.4, 0.4, 0.0);
            p.teleportTo(level, to.x, to.y, to.z, p.getYRot(), p.getXRot());
            p.setDeltaMovement(0, 0.5, 0);
            p.hurtMarked = true;
            p.resetFallDistance();
            p.hurt(level.damageSources().lava(), Math.max(4.0F, p.getMaxHealth() * 0.2F));
            p.setSecondsOnFire(2);
            level.playSound(null, to.x, to.y, to.z, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 2.0F, 0.6F);
            level.sendParticles(ParticleTypes.LAVA, to.x, to.y, to.z, 16, 0.4, 0.2, 0.4, 0.0);
            Titles.actionBar(p, "¡La lava te escupe de vuelta!");
        }
    }

    private void tick(ServerLevel level) {
        if (alive(level).isEmpty() && BossTravel.hasSpectator(level)) {
            emptyT = 0;
            return;
        }
        if (alive(level).isEmpty()) {
            if (phase != Phase.WAITING && ++emptyT > 8) {
                resetFight(level);
                emptyT = 0;
            }
            return;
        }
        emptyT = 0;
        // ceniza que cae y burbujas de lava mientras se espera
        if (level.random.nextInt(2) == 0) {
            level.sendParticles(ParticleTypes.WHITE_ASH, 0.5, FireArena.Y + 12, 0.5, 30, 14.0, 6.0, 14.0, 0.0);
            level.sendParticles(ParticleTypes.LAVA, 0.5 + (level.random.nextDouble() - 0.5) * 50, FireArena.Y - 3.5,
                    0.5 + (level.random.nextDouble() - 0.5) * 50, 2, 1.0, 0.0, 1.0, 0.0);
        }
        if (phase == Phase.WAITING) {
            for (ServerPlayer p : level.players()) {
                if (p.isAlive() && !p.isSpectator() && FireArena.inArena(p.getX(), p.getZ(), 3.0) && p.getY() > FireArena.Y - 1) {
                    phase = Phase.INTRO;
                    intro(level);
                    return;
                }
            }
        }
    }

    private static List<ServerPlayer> alive(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator()) {
                out.add(p);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ intro: el techo se agrieta y baja el corazón

    private void intro(ServerLevel level) {
        final int epoch = DungeonReset.epoch(level);
        Vec3 top = FireArena.heartHidden();
        Vec3 ceil = new Vec3(0.5, FireArena.Y + FireArena.DOME + FireArena.DOME_DY - 1, 0.5);
        for (ServerPlayer p : alive(level)) {
            Fx.screen(p, ScreenFxPacket.DARK, 0x1A0602, 50);
            Titles.actionBar(p, "La caverna tiembla...");
        }
        level.playSound(null, ceil.x, ceil.y, ceil.z, com.mundosdeaxelis.registry.ModSounds.CORAZON_RUGIDO.get(), SoundSource.HOSTILE, 5.0F, 0.8F);
        // temblores y polvo del techo
        for (int k = 0; k < 6; k++) {
            final int kk = k;
            Scheduler.runLater(k * 10, () -> {
                if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
                    return;
                }
                RelicFx.shakeNear(level, FireArena.center(), 80, 0.4F + kk * 0.15F, 10);
                level.sendParticles(ParticleTypes.ASH, ceil.x, ceil.y, ceil.z, 60, 4.0, 0.5, 4.0, 0.0);
                level.sendParticles(ParticleTypes.LAVA, ceil.x, ceil.y - 0.5, ceil.z, 6, 2.0, 0.2, 2.0, 0.0);
                level.playSound(null, ceil.x, ceil.y, ceil.z, SoundEvents.BASALT_BREAK, SoundSource.HOSTILE, 4.0F, 0.4F + kk * 0.05F);
                level.playSound(null, ceil.x, ceil.y, ceil.z, com.mundosdeaxelis.registry.ModSounds.CORAZON_LATIDO.get(), SoundSource.HOSTILE, 5.0F, 0.8F);
            });
        }
        // el corazón espera escondido encima del techo
        Scheduler.runLater(2, () -> {
            if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
                return;
            }
            CorazonMagma boss = ModEntities.CORAZON_MAGMA.get().create(level);
            if (boss == null) {
                phase = Phase.WAITING;
                return;
            }
            boss.moveTo(top.x, top.y, top.z, 0.0F, 0.0F);
            boss.setHome(FireArena.heartHome());
            boss.setInvulnerable(true);
            level.addFreshEntity(boss);
        });
        // la grieta se abre en 4 golpes
        for (int k = 0; k < 4; k++) {
            final int kk = k;
            Scheduler.runLater(60 + k * 8, () -> {
                if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
                    return;
                }
                FireArena.openCeiling(level, kk);
                level.playSound(null, ceil.x, ceil.y, ceil.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.5F);
                RelicFx.shakeNear(level, FireArena.center(), 80, 1.2F, 12);
            });
        }
        // y baja
        Scheduler.runLater(96, () -> {
            if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
                return;
            }
            CorazonMagma boss = find(level);
            if (boss == null) {
                phase = Phase.WAITING;
                return;
            }
            boss.startDescend();
            Vfx.column(level, new Vec3(0.5, FireArena.Y + 1, 0.5), 0xFF6A1A, 3.0, 26.0, 90);
            for (ServerPlayer p : alive(level)) {
                Titles.show(p, "Corazón de Magma", "El latido del volcán");
            }
        });
        Scheduler.runLater(200, () -> wake(level, 0, epoch));
    }

    public static CorazonMagma find(ServerLevel level) {
        for (CorazonMagma b : level.getEntitiesOfClass(CorazonMagma.class, ALL)) {
            if (b.isAlive()) {
                return b;
            }
        }
        return null;
    }

    private void wake(ServerLevel level, int tries, int epoch) {
        if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
            return;
        }
        List<ServerPlayer> ps = alive(level);
        boolean allIn = true;
        for (ServerPlayer p : ps) {
            if (!FireArena.inArena(p.getX(), p.getZ(), 1.0)) {
                allIn = false;
            }
        }
        if (!allIn && tries < 12) {
            if (tries == 0) {
                for (ServerPlayer p : ps) {
                    Titles.actionBar(p, "El túnel se cierra en unos segundos: entrad todos");
                }
            }
            Scheduler.runLater(20, () -> wake(level, tries + 1, epoch));
            return;
        }
        for (ServerPlayer p : ps) {
            if (!FireArena.inArena(p.getX(), p.getZ(), 1.0)) {
                p.teleportTo(level, 0.5 + (level.random.nextDouble() - 0.5) * 4.0, FireArena.Y + 1.0, FireArena.R - 4.0, 180.0F, 0.0F);
            }
        }
        FireArena.sealGateway(level, true);
        phase = Phase.FIGHT;
        CorazonMagma boss = find(level);
        if (boss != null) {
            boss.setInvulnerable(false);
        }
        BossSkip.remind(ps);
    }
}

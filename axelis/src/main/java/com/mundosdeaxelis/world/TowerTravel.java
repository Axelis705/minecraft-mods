package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Archimago;
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
 * Viaje a la cima de la Torre del Archimago (jefe de la etapa 5). Lo llama {@link BossTravel#enter}. Los jugadores aparecen en
 * la biblioteca (reciben la ficha del jefe y pueden salir sin pelear); al cruzar el puente y pisar la arena empieza la intro:
 * libros que giran, runas y cristales, y el Archimago baja del cielo. Entonces el vitral de la puerta se cierra y empieza la pelea.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class TowerTravel {
    private static final int DARK = 0x120A20;
    private static final AABB ALL = new AABB(-70, 0, -70, 70, 200, 100);

    private enum Phase {WAITING, INTRO, FIGHT}

    private Phase phase = Phase.WAITING;
    private int emptyT;
    /** Los libros embrujados del puente ya salieron en esta partida. */
    private boolean bridgeGuards;

    private static final Map<ResourceKey<Level>, TowerTravel> INSTANCES = new HashMap<>();

    /** Al cerrar el servidor (o abrir otro mundo en el mismo juego) se olvida lo que quedó de la sesión anterior. */
    public static void clearRuntime() {
        INSTANCES.clear();
    }

    private TowerTravel() {
    }

    /** El estado de la torre de esta copia de dimensión (cada copia tiene el suyo). */
    public static TowerTravel of(Level level) {
        return INSTANCES.computeIfAbsent(level.dimension(), k -> new TowerTravel());
    }

    public static void enter(MinecraftServer server, List<ServerPlayer> players) {
        ServerLevel level = server.getLevel(ModDimensions.TOWER_COPIES.get(0));
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
            Titles.actionBar(p, "La Torre del Archimago se abre... un momento.");
        }
        start(server, ids);
    }

    private static void start(MinecraftServer server, List<UUID> ids) {
        if (BossTravel.locked(server)) {
            // otra entrada está construyendo una dungeon: esperar a que termine
            Scheduler.runLater(20, () -> start(server, ids));
            return;
        }
        ServerLevel level = BossTravel.pickCopy(server, 4);
        if (level == null) {
            BossTravel.noCopy(server, ids, DARK);
            return;
        }
        BossTravel.lock(server);
        TowerTravel me = of(level);
        boolean[] entered = {false};
        BossTravel.watchdog(server, ids, entered);
        // cada grupo que pulsa «Comenzar» usa una copia libre y la pelea empieza de cero (se borra un Archimago que se hubiera quedado)
        TowerArena.ensureBuilt(level, BossTravel.guard(server, ids, entered, () -> {
            me.resetFight(level);
            entered[0] = true;
            me.finishEnter(server, ids, level);
        }));
    }

    private void resetFight(ServerLevel level) {
        bridgeGuards = false;
        clearBooks(level);
        DungeonReset.bump(level); // el Archimago anterior (cargado o guardado en una zona descargada) ya no vale
        for (Archimago b : level.getEntitiesOfClass(Archimago.class, ALL)) {
            b.clearFight(level);
            b.discard();
        }
        Props.clear(level);
        TowerArena.sealGateway(level, false);
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
            Vec3 s = TowerArena.entryPoint(i);
            p.teleportTo(level, s.x, s.y, s.z, 180.0F, 0.0F);
            p.resetFallDistance();
            ModNetwork.sendTo(p, new FadePacket(DARK, 0, 5, 40));
            Titles.show(p, "Biblioteca de la Torre", "Cruza el puente: el Archimago te espera arriba");
            group.add(p);
            i++;
        }
        // un momento después: el aviso de salir y la ficha del jefe
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

    /** ¿Pueden salir todavía sin pelear? Solo antes de que el Archimago despierte. */
    public boolean canLeave() {
        return phase == Phase.WAITING;
    }

    /** ¿Hay algo en marcha (jugadores esperando o Archimago vivo)? Si no, quien aparezca ahí vuelve a casa. */
    public boolean running(ServerLevel level) {
        if (phase != Phase.FIGHT) {
            return !level.players().isEmpty();
        }
        for (Archimago b : level.getEntitiesOfClass(Archimago.class, ALL)) {
            if (b.isAlive()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ intro

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 5 != 0) {
            return;
        }
        for (ResourceKey<Level> key : ModDimensions.TOWER_COPIES) {
            ServerLevel l = e.getServer().getLevel(key);
            if (l != null) {
                of(l).tick(l);
            }
        }
    }

    private void tick(ServerLevel level) {
        if (alive(level).isEmpty()) {
            // torre vacía (todos murieron o salieron): el Archimago desaparece y la pelea queda lista para empezar de cero
            if (phase != Phase.WAITING && ++emptyT > 8) {
                resetFight(level);
                emptyT = 0;
            }
            return;
        }
        emptyT = 0;
        // chispas de magia en la biblioteca mientras se espera
        if (phase == Phase.WAITING && level.random.nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, 0.5, TowerArena.Y + 2.5, TowerArena.AZ + 0.5, 12, 3.0, 1.5, 3.0, 0.6);
        }
        if (phase == Phase.WAITING && !bridgeGuards) {
            for (ServerPlayer p : alive(level)) {
                if (p.getZ() < TowerArena.AZ - 9 && p.getZ() > 24 && p.getY() > TowerArena.Y - 2) {
                    bridgeGuards = true;
                    spawnBooks(level, alive(level).size());
                    break;
                }
            }
        }
        if (phase == Phase.WAITING) {
            for (ServerPlayer p : level.players()) {
                if (p.isAlive() && !p.isSpectator() && TowerArena.inArena(p.getX(), p.getZ(), 3.0) && p.getY() > TowerArena.Y - 2) {
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

    /** Libros embrujados que salen de la torre y cortan el paso en el puente (2, y uno más por cada jugador extra; máximo 4). */
    private static void spawnBooks(ServerLevel level, int players) {
        int n = Math.min(4, 1 + Math.max(1, players));
        for (int k = 0; k < n; k++) {
            com.mundosdeaxelis.entity.LibroEmbrujado b = com.mundosdeaxelis.registry.ModEntities.LIBRO_EMBRUJADO.get().create(level);
            if (b == null) {
                continue;
            }
            double x = (k % 2 == 0 ? -2.5 : 3.5);
            double z = 24.0 + k * 2.5;
            b.moveTo(x, TowerArena.Y + 3.0, z, 180.0F, 0.0F);
            level.addFreshEntity(b);
            level.sendParticles(ParticleTypes.ENCHANT, x, TowerArena.Y + 3.4, z, 30, 0.4, 0.4, 0.4, 0.8);
        }
        level.playSound(null, 0.5, TowerArena.Y + 2, 28, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    /** Al empezar la pelea (o reiniciarla) se van los libros que queden: el Archimago pelea solo. */
    private static void clearBooks(ServerLevel level) {
        for (com.mundosdeaxelis.entity.LibroEmbrujado b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.LibroEmbrujado.class, ALL)) {
            level.sendParticles(ParticleTypes.ENCHANT, b.getX(), b.getY() + 0.4, b.getZ(), 20, 0.3, 0.3, 0.3, 0.6);
            b.discard();
        }
    }

    private void intro(ServerLevel level) {
        clearBooks(level);
        final int epoch = DungeonReset.epoch(level);
        List<ServerPlayer> watchers = alive(level);
        if (TowerIntro.canPlay(watchers)) {
            // intro épica (cinemática); al acabar o saltarla el Archimago ya está en su sitio y se espera a todos para sellar la puerta
            TowerIntro.play(level, watchers, () -> {
                Archimago boss = TowerIntro.find(level);
                if (boss == null || phase != Phase.INTRO) {
                    return;
                }
                wake(level, boss, 0, epoch);
            });
            return;
        }
        // intro corta (si alguien ya está viendo otra cinemática)
        Vec3 spot = TowerArena.bossSpot();
        for (ServerPlayer p : watchers) {
            Fx.screen(p, ScreenFxPacket.DARK, 0x0A0418, 50);
        }
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 4.0F, 0.6F);
        Vfx.rune(level, spot.add(0, 0.12, 0), 0xB46EFF, 6.0, 90);
        Scheduler.runLater(40, () -> {
            if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
                return;
            }
            Archimago boss = ModEntities.ARCHIMAGO.get().create(level);
            if (boss == null) {
                phase = Phase.WAITING;
                return;
            }
            boss.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
            boss.setHome(TowerArena.center());
            boss.setInvulnerable(true);
            boss.setNoAi(true);
            level.addFreshEntity(boss);
            level.sendParticles(ParticleTypes.PORTAL, spot.x, spot.y + 2.0, spot.z, 120, 0.8, 1.6, 0.8, 0.6);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 4.0F, 0.7F);
            Vfx.column(level, spot.add(0, 0.1, 0), 0xD8FBFF, 1.4, 24.0, 50);
            RelicFx.shakeNear(level, spot, 70, 0.6F, 25);
            for (ServerPlayer p : alive(level)) {
                Titles.show(p, "Archimago", "Señor de la Torre");
            }
            Scheduler.runLater(70, () -> wake(level, boss, 0, epoch));
        });
    }

    private void wake(ServerLevel level, Archimago boss, int tries, int epoch) {
        if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
            return; // la torre se reinició mientras tanto
        }
        List<ServerPlayer> ps = alive(level);
        boolean allIn = true;
        for (ServerPlayer p : ps) {
            if (!TowerArena.inArena(p.getX(), p.getZ(), 1.0)) {
                allIn = false;
            }
        }
        // se espera (hasta 12 s) a quien venga por el puente; después se le mete dentro de la arena
        if (!allIn && tries < 12) {
            if (tries == 0) {
                for (ServerPlayer p : ps) {
                    Titles.actionBar(p, "El vitral se cierra en unos segundos: entrad todos");
                }
            }
            Scheduler.runLater(20, () -> wake(level, boss, tries + 1, epoch));
            return;
        }
        for (ServerPlayer p : ps) {
            if (!TowerArena.inArena(p.getX(), p.getZ(), 1.0)) {
                p.teleportTo(level, 0.5 + (level.random.nextDouble() - 0.5) * 4.0, TowerArena.Y + 1.0, TowerArena.R - 4.0, 180.0F, 0.0F);
            }
        }
        TowerArena.sealGateway(level, true);
        phase = Phase.FIGHT;
        if (boss.isAlive()) {
            boss.setHome(TowerArena.center());
            boss.setNoAi(false);
            boss.setInvulnerable(false);
        }
        BossSkip.remind(ps);
    }
}

package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.LoboArtico;
import com.mundosdeaxelis.entity.LoboEscarcha;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.FadePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.RelicFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * Viaje a la sala del Lobo Ártico (jefe de la etapa 3). Lo llama {@link BossTravel#enter}. Los jugadores aparecen en la
 * antesala (reciben la ficha del jefe y pueden salir sin pelear); al pisar la arena empieza una intro corta y tranquila:
 * ventisca, el lobo aparece al fondo y aúlla; entonces la puerta se sella y la pelea empieza.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class IceTravel {
    private static final int DARK = 0x0B1620;
    private static final AABB ALL = new AABB(-70, 0, -70, 70, 200, 100);

    private enum Phase {WAITING, INTRO, FIGHT}

    private Phase phase = Phase.WAITING;
    /** La pelea llegó a empezar y la sala puede tener hielo derretido: hay que reconstruirla al volver a entrar. */
    private boolean dirtyArena;
    private int emptyT;

    private static final Map<ResourceKey<Level>, IceTravel> INSTANCES = new HashMap<>();

    private IceTravel() {
    }

    /** El estado de la sala de esta copia de dimensión (cada copia tiene el suyo). */
    public static IceTravel of(Level level) {
        return INSTANCES.computeIfAbsent(level.dimension(), k -> new IceTravel());
    }

    public static void enter(MinecraftServer server, List<ServerPlayer> players) {
        ServerLevel level = server.getLevel(ModDimensions.ICE_COPIES.get(0));
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
            Titles.actionBar(p, "El Palacio de hielo se abre... un momento.");
        }
        start(server, ids);
    }

    private static void start(MinecraftServer server, List<UUID> ids) {
        if (BossTravel.locked(server)) {
            // otra entrada está construyendo una dungeon: esperar a que termine
            Scheduler.runLater(20, () -> start(server, ids));
            return;
        }
        ServerLevel level = BossTravel.pickCopy(server, 3);
        if (level == null) {
            BossTravel.noCopy(server, ids, DARK);
            return;
        }
        BossTravel.lock(server);
        IceTravel me = of(level);
        // cada grupo que pulsa «Comenzar» usa una copia libre y la pelea empieza de cero (se borra un lobo que se hubiera quedado)
        IceArena.ensureBuilt(level, () -> {
            me.resetFight(level);
            if (me.dirtyArena) {
                // hubo pelea: la sala se vuelve a construir para que esté como nueva
                IceArena.rebuild(level, () -> {
                    me.dirtyArena = false;
                    me.finishEnter(server, ids, level);
                });
                return;
            }
            me.finishEnter(server, ids, level);
        });
    }

    private void resetFight(ServerLevel level) {
        DungeonReset.bump(level); // el lobo anterior (cargado o guardado en una zona descargada) ya no vale
        for (LoboArtico b : level.getEntitiesOfClass(LoboArtico.class, ALL)) {
            b.clearFight(level);
            b.discard();
        }
        for (LoboEscarcha s : level.getEntitiesOfClass(LoboEscarcha.class, ALL)) {
            s.discard();
        }
        HeatZones.clear(level);
        IceArena.sealGateway(level, false);
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
            Vec3 s = IceArena.entryPoint(i);
            p.teleportTo(level, s.x, s.y, s.z, 180.0F, 0.0F);
            p.resetFallDistance();
            ModNetwork.sendTo(p, new FadePacket(DARK, 0, 5, 40));
            Titles.show(p, "Sala del Lobo Ártico", "El Palacio de hielo te espera");
            group.add(p);
            i++;
        }
        // un momento después: el aviso de salir y la ficha del jefe
        Scheduler.runLater(60, () -> {
            if (phase != Phase.WAITING) {
                return;
            }
            List<ServerPlayer> now = new ArrayList<>();
            for (ServerPlayer q : level.players()) {
                if (q.isAlive() && !q.isSpectator()) {
                    now.add(q);
                }
            }
            BossSkip.offer(now);
            for (ServerPlayer q : group) {
                if (q.level() == level) {
                    BossSkip.dossier(q, now);
                }
            }
        });
    }

    /** ¿Pueden salir todavía sin pelear? Solo antes de que el lobo despierte. */
    public boolean canLeave() {
        return phase == Phase.WAITING;
    }

    /** ¿Hay algo en marcha (jugadores esperando o lobo vivo)? Si no, quien aparezca ahí vuelve a casa. */
    public boolean running(ServerLevel level) {
        if (phase != Phase.FIGHT) {
            return !level.players().isEmpty();
        }
        for (LoboArtico b : level.getEntitiesOfClass(LoboArtico.class, ALL)) {
            if (b.isAlive()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ intro

    private int introT;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 5 != 0) {
            return;
        }
        for (ResourceKey<Level> key : ModDimensions.ICE_COPIES) {
            ServerLevel l = e.getServer().getLevel(key);
            if (l != null) {
                of(l).tick(l);
            }
        }
    }

    private void tick(ServerLevel level) {
        if (alive(level).isEmpty()) {
            // sala vacía (todos murieron o salieron): el lobo desaparece casi al instante y la pelea queda lista para empezar de cero
            if (phase != Phase.WAITING && ++emptyT > 8) {
                resetFight(level);
                emptyT = 0;
            }
            return;
        }
        emptyT = 0;
        if (phase == Phase.WAITING) {
            for (ServerPlayer p : level.players()) {
                if (p.isAlive() && !p.isSpectator() && IceArena.inArena(p.getX(), p.getZ(), 4.0) && p.getY() > IceArena.Y - 2) {
                    phase = Phase.INTRO;
                    dirtyArena = true;
                    introT = 0;
                    intro(level);
                    return;
                }
            }
        } else if (phase == Phase.FIGHT) {
            // si el lobo ya no vive y nadie lo notó (p. ej. se borró), no hace falta nada: la victoria la gestiona él mismo
            return;
        }
    }

    private List<ServerPlayer> alive(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator()) {
                out.add(p);
            }
        }
        return out;
    }

    private void intro(ServerLevel level) {
        final int epoch = DungeonReset.epoch(level);
        List<ServerPlayer> watchers = alive(level);
        if (IceIntro.canPlay(watchers)) {
            // intro épica (cinemática); al acabar o saltarla el lobo ya está en su sitio y se pasa a esperar a todos y sellar la puerta
            IceIntro.play(level, watchers, () -> {
                LoboArtico boss = IceIntro.find(level);
                if (boss == null || phase != Phase.INTRO) {
                    return;
                }
                wake(level, boss, 0, epoch);
            });
            return;
        }
        Vec3 spot = IceArena.bossSpot();
        Vec3 c = IceArena.center();
        // 0 s: se levanta una ventisca
        for (ServerPlayer p : alive(level)) {
            Fx.screen(p, ScreenFxPacket.WHITEOUT, 0xDDEEFF, 60);
        }
        level.playSound(null, c.x, c.y, c.z, ModSounds.LOBO_WIND.get(), SoundSource.AMBIENT, 6.0F, 0.8F);
        Scheduler.runLater(20, () -> {
            Vfx.rune(level, spot.add(0, 0.12, 0), 0x9FE3FF, 7.0, 90);
            Vfx.ring(level, spot.add(0, 0.12, 0), 0xE8F8FF, 3.0, 40);
            level.sendParticles(ParticleTypes.SNOWFLAKE, spot.x, spot.y + 1.5, spot.z, 120, 8.0, 2.5, 8.0, 0.2);
        });
        // 2,5 s: el lobo aparece de pie en medio de la nieve y aúlla
        Scheduler.runLater(50, () -> {
            if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
                return; // la sala se reinició mientras tanto
            }
            LoboArtico boss = ModEntities.LOBO_ARTICO.get().create(level);
            if (boss == null) {
                phase = Phase.WAITING;
                return;
            }
            boss.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
            boss.setHome(c);
            boss.setInvulnerable(true);
            boss.setNoAi(true);
            level.addFreshEntity(boss);
            level.playSound(null, spot.x, spot.y, spot.z, ModSounds.LOBO_HOWL.get(), SoundSource.HOSTILE, 5.0F, 0.85F);
            Vfx.column(level, spot.add(0, 0.1, 0), 0xE8F8FF, 1.6, 24.0, 50);
            RelicFx.shakeNear(level, spot, 70, 0.7F, 30);
            for (ServerPlayer p : alive(level)) {
                Titles.show(p, "Lobo Ártico", "Guardián del Palacio de hielo");
            }
            // 4,5 s después despierta: se sella la puerta y la pelea empieza
            Scheduler.runLater(90, () -> wake(level, boss, 0, epoch));
        });
    }

    private void wake(ServerLevel level, LoboArtico boss, int tries, int epoch) {
        if (DungeonReset.epoch(level) != epoch || phase != Phase.INTRO) {
            return; // la sala se reinició mientras tanto
        }
        List<ServerPlayer> ps = alive(level);
        boolean allIn = true;
        for (ServerPlayer p : ps) {
            if (!IceArena.inArena(p.getX(), p.getZ(), 1.0)) {
                allIn = false;
            }
        }
        // se espera (hasta 12 s) a quien venga por el pasillo; después se le mete dentro de la arena
        if (!allIn && tries < 12) {
            if (tries == 0) {
                for (ServerPlayer p : ps) {
                    Titles.actionBar(p, "La puerta se cierra en unos segundos: entrad todos");
                }
            }
            Scheduler.runLater(20, () -> wake(level, boss, tries + 1, epoch));
            return;
        }
        for (ServerPlayer p : ps) {
            if (!IceArena.inArena(p.getX(), p.getZ(), 1.0)) {
                p.teleportTo(level, 0.5 + (level.random.nextDouble() - 0.5) * 4.0, IceArena.Y + 1.0, IceArena.R - 4.0, 180.0F, 0.0F);
            }
        }
        IceArena.sealGateway(level, true);
        phase = Phase.FIGHT;
        if (boss.isAlive()) {
            boss.setNoAi(false);
            boss.setInvulnerable(false);
        }
        BossSkip.remind(ps);
    }
}

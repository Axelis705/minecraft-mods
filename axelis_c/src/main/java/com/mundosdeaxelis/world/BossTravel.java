package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.ReyBrote;
import com.mundosdeaxelis.network.FadePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Viajes a las arenas de los jefes de etapa: se entra con la Piedra de Portal (todos los jugadores cercanos juntos) y se
 * vuelve cuando el jefe cae (con una cuenta atrás) o con el comando de pruebas. Guarda de dónde viene cada jugador.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class BossTravel {
    private static final int DARK = 0x0B1A0E;
    private static final String K_DIM = "mdc_bossret_dim";
    private static final String K_X = "mdc_bossret_x";
    private static final String K_Y = "mdc_bossret_y";
    private static final String K_Z = "mdc_bossret_z";

    private BossTravel() {
    }

    public static ResourceKey<Level> dimensionOf(int stage) {
        return stage == 3 ? ModDimensions.BOSS_ICE : stage == 2 ? ModDimensions.BOSS_FORGE : ModDimensions.BOSS_FOREST;
    }

    /** Etapa del jefe de la dungeon en la que está este nivel (0 si no es una dungeon). Cada dungeon nueva añade aquí su línea. */
    public static int stageOf(Level level) {
        if (ModDimensions.isIce(level.dimension())) {
            return 3;
        }
        if (ModDimensions.isForge(level.dimension())) {
            return 2;
        }
        return ModDimensions.isForest(level.dimension()) ? 1 : 0;
    }

    /** ¿Es una de las dimensiones de los jefes (dungeon + arena)? */
    public static boolean inArena(Level level) {
        return ModDimensions.isForest(level.dimension()) || ModDimensions.isForge(level.dimension())
                || ModDimensions.isIce(level.dimension());
    }

    public static void enter(MinecraftServer server, List<ServerPlayer> players, int stage) {
        if (stage == 3) {
            IceTravel.enter(server, players);
            return;
        }
        if (stage == 2) {
            ForgeTravel.enter(server, players);
            return;
        }
        ServerLevel level = server.getLevel(dimensionOf(stage));
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
            Titles.actionBar(p, "El bosque despierta... un momento.");
        }
        start(server, ids);
    }

    // ------------------------------------------------------------------ copias

    private static boolean lockOn;
    private static long lockSince;

    /** ¿Hay otra entrada construyendo una dungeon? Solo una a la vez (si se atasca, el candado caduca a los 60 s). */
    static boolean locked(MinecraftServer server) {
        if (lockOn && server.getTickCount() - lockSince > 1200L) {
            lockOn = false;
        }
        return lockOn;
    }

    static void lock(MinecraftServer server) {
        lockOn = true;
        lockSince = server.getTickCount();
    }

    static void unlock() {
        lockOn = false;
    }

    /** La primera copia libre (sin jugadores) de la dungeon de esta etapa, o null si todas están ocupadas. */
    static ServerLevel pickCopy(MinecraftServer server, int stage) {
        for (ResourceKey<Level> key : ModDimensions.copiesOf(stage)) {
            ServerLevel l = server.getLevel(key);
            if (l != null && l.players().isEmpty()) {
                return l;
            }
        }
        return null;
    }

    /** Todas las copias están ocupadas: se avisa a quien quería entrar y se quita la pantalla oscura. */
    static void noCopy(MinecraftServer server, List<UUID> ids, int dark) {
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                ModNetwork.sendTo(p, new FadePacket(dark, 0, 5, 40));
                p.sendSystemMessage(Component.literal("Todas las copias de esta dungeon están ocupadas. Espera a que otro grupo termine e inténtalo de nuevo.")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }

    private static void start(MinecraftServer server, List<UUID> ids) {
        if (locked(server)) {
            // otra entrada está construyendo una dungeon: esperar a que termine
            Scheduler.runLater(20, () -> start(server, ids));
            return;
        }
        ServerLevel level = pickCopy(server, 1);
        if (level == null) {
            noCopy(server, ids, DARK);
            return;
        }
        lock(server);
        // cada grupo que pulsa «Comenzar» usa una copia libre y genera en ella una dungeon NUEVA con el número de jugadores que entran
        ForestArena.ensureBuilt(level, () -> {
            ForestArena.openGateway(level);
            DungeonManager.of(level).reset(level);
            DungeonLayout lay = DungeonLayout.generate(new java.util.Random().nextLong(), DungeonLayout.clampPlayers(ids.size()));
            ForestDungeon.build(level, lay, () -> {
                DungeonManager.of(level).begin(level, lay);
                finishEnter(server, ids, level);
            });
        });
    }

    private static void finishEnter(MinecraftServer server, List<UUID> ids, ServerLevel level) {
        unlock();
        int i = 0;
        DungeonLayout lay = DungeonManager.of(level).layout();
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                continue;
            }
            if (!inArena(p.level())) {
                saveReturn(p);
            }
            Vec3 s = DungeonManager.of(level).entryPoint(level, i);
            p.teleportTo(level, s.x, s.y, s.z, 180.0F, 0.0F);
            p.resetFallDistance();
            ModNetwork.sendTo(p, new FadePacket(DARK, 0, 5, 40));
            DungeonManager.of(level).sendMap(p);
            Titles.show(p, "Santuario del Rey Brote", lay == null ? "Abre camino sala por sala" : lay.cells.size() + " salas · busca llaves y pruebas");
            i++;
        }
    }

    /** ¿Hay algún jugador vivo (y no espectador) en este nivel? */
    static boolean hasPlayer(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator()) {
                return true;
            }
        }
        return false;
    }

    /** Hace aparecer al jefe si no hay uno vivo ya. */
    public static void spawnBoss(ServerLevel level) {
        if (!hasPlayer(level)) {
            return; // nadie dentro: no hay pelea que empezar (p. ej. todos murieron durante la intro)
        }
        for (ReyBrote r : level.getEntitiesOfClass(ReyBrote.class, new net.minecraft.world.phys.AABB(-60, 0, -60, 60, 200, 60))) {
            if (r.isAlive()) {
                return;
            }
        }
        ReyBrote boss = ModEntities.REY_BROTE.get().create(level);
        if (boss == null) {
            return;
        }
        Vec3 c = ForestArena.bossSpot();
        boss.moveTo(c.x, c.y, c.z, 0.0F, 0.0F);
        level.addFreshEntity(boss);
        // pasado un rato, el túnel hacia la dungeon se cierra para que durante el combate no se vea lo de atrás
        Scheduler.runLater(800, () -> sealWhenClear(level, 0));
    }

    private static void sealWhenClear(ServerLevel level, int tries) {
        boolean alive = false;
        for (ReyBrote r : level.getEntitiesOfClass(ReyBrote.class, new net.minecraft.world.phys.AABB(-60, 0, -60, 60, 200, 60))) {
            alive |= r.isAlive();
        }
        if (!alive || level.players().isEmpty()) {
            return;
        }
        if (ForestArena.tunnelBusy(level) && tries < 30) {
            Scheduler.runLater(20, () -> sealWhenClear(level, tries + 1));
            return;
        }
        ForestArena.sealGateway(level, true);
    }

    // ------------------------------------------------------------------ regreso

    static void saveReturn(ServerPlayer p) {
        var d = p.getPersistentData();
        d.putString(K_DIM, p.level().dimension().location().toString());
        d.putDouble(K_X, p.getX());
        d.putDouble(K_Y, p.getY());
        d.putDouble(K_Z, p.getZ());
    }

    /** Sitio desde el que entró el jugador (nivel cargado y posición), o null si no se guardó o la dimensión ya no existe. */
    public static net.minecraft.core.GlobalPos returnSpot(ServerPlayer p) {
        var d = p.getPersistentData();
        if (!d.contains(K_DIM)) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new ResourceLocation(d.getString(K_DIM)));
        if (p.server.getLevel(key) == null) {
            return null;
        }
        return net.minecraft.core.GlobalPos.of(key, BlockPos.containing(d.getDouble(K_X), d.getDouble(K_Y), d.getDouble(K_Z)));
    }

    /** Olvida el sitio de entrada (el jugador murió o ya salió). */
    public static void clearReturn(ServerPlayer p) {
        var d = p.getPersistentData();
        d.remove(K_DIM);
        d.remove(K_X);
        d.remove(K_Y);
        d.remove(K_Z);
    }

    /** Devuelve al jugador al sitio desde el que entró (o al punto de reaparición si no se guardó). */
    public static void leave(ServerPlayer p) {
        MinecraftServer server = p.server;
        var d = p.getPersistentData();
        ServerLevel dest = null;
        if (d.contains(K_DIM)) {
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation(d.getString(K_DIM)));
            dest = server.getLevel(key);
        }
        ModNetwork.sendTo(p, new FadePacket(DARK, 8, 10, 30));
        ModNetwork.sendTo(p, com.mundosdeaxelis.network.DungeonMapPacket.off());
        if (dest != null) {
            p.teleportTo(dest, d.getDouble(K_X), d.getDouble(K_Y), d.getDouble(K_Z), p.getYRot(), 0.0F);
        } else {
            BlockPos sp = p.getRespawnPosition();
            ServerLevel o = server.overworld();
            BlockPos at = sp != null ? sp : o.getSharedSpawnPos();
            p.teleportTo(o, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 0.0F, 0.0F);
        }
        p.resetFallDistance();
        d.remove(K_DIM);
    }

    /** Cuenta atrás tras la victoria y regreso de todos. */
    public static void scheduleExit(ServerLevel level) {
        MinecraftServer server = level.getServer();
        int total = 20;
        for (int s = total; s >= 1; s--) {
            final int left = s;
            Scheduler.runLater((total - s) * 20 + 40, () -> {
                if (left == total || left == 15 || left == 10 || left <= 5) {
                    for (ServerPlayer p : level.players()) {
                        Titles.actionBar(p, "Volverás en " + left + " s...");
                    }
                }
            });
        }
        Scheduler.runLater(total * 20 + 40, () -> {
            for (ServerPlayer p : new ArrayList<>(level.players())) {
                leave(p);
            }
        });
        server.getPlayerList();
    }

    // ------------------------------------------------------------------ seguridad

    /** Si alguien entra al juego dentro de la dungeon y ya no hay jefe ni dungeon en marcha, se le devuelve a su sitio. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !inArena(sp.level())) {
            return;
        }
        Scheduler.runLater(60, () -> {
            if (!inArena(sp.level()) || sp.hasDisconnected() || !(sp.level() instanceof ServerLevel sl)) {
                return;
            }
            boolean alive;
            if (ModDimensions.isIce(sl.dimension())) {
                alive = IceTravel.of(sl).running(sl);
            } else if (ModDimensions.isForge(sl.dimension())) {
                alive = ForgeTravel.running(sl);
            } else {
                alive = DungeonManager.of(sl).isInProgress();
                for (ReyBrote r : sl.getEntitiesOfClass(ReyBrote.class, new net.minecraft.world.phys.AABB(-60, 0, -60, 60, 200, 60))) {
                    alive |= r.isAlive();
                }
            }
            if (!alive) {
                leave(sp);
            }
        });
    }
}

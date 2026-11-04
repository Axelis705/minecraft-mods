package com.mundosdeaxelis.cinematic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.StartCinematicPacket;
import com.mundosdeaxelis.network.StopCinematicPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reproduce cinemáticas para varios jugadores a la vez. El servidor manda la cinemática completa a cada
 * cliente (para que la cámara sea fluida) y mientras tanto ejecuta los "cues" y protege a los jugadores.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class CinematicManager {

    private static final class Active {
        final Cinematic cinematic;
        final ServerLevel level;
        final Vec3 origin;
        final List<UUID> players = new ArrayList<>();
        final Runnable onFinish;
        int tick = 0;
        int nextCue = 0;

        Active(Cinematic cinematic, ServerLevel level, Vec3 origin, Runnable onFinish) {
            this.cinematic = cinematic;
            this.level = level;
            this.origin = origin;
            this.onFinish = onFinish;
        }
    }

    private static final List<Active> ACTIVE = new ArrayList<>();
    private static final Set<UUID> WATCHING = new HashSet<>();

    private CinematicManager() {
    }

    public static boolean isWatching(UUID id) {
        return WATCHING.contains(id);
    }

    /**
     * Empieza la cinemática para los jugadores indicados (los que ya estén viendo otra se ignoran).
     * @param onFinish se ejecuta al terminar (puede ser null)
     */
    public static void play(Cinematic cinematic, ServerLevel level, Vec3 origin,
                            Collection<ServerPlayer> players, Runnable onFinish) {
        Active active = new Active(cinematic, level, origin, onFinish);
        List<Keyframe> frames = new ArrayList<>();
        for (Keyframe k : cinematic.frames()) {
            frames.add(k.offset(origin));
        }
        StartCinematicPacket packet = new StartCinematicPacket(cinematic.duration(), cinematic.fadeOut(),
                cinematic.bars(), frames, cinematic.subtitles());

        for (ServerPlayer p : players) {
            if (WATCHING.contains(p.getUUID())) {
                continue;
            }
            WATCHING.add(p.getUUID());
            active.players.add(p.getUUID());
            ModNetwork.sendTo(p, packet);
        }
        if (active.players.isEmpty()) {
            return;
        }
        ACTIVE.add(active);
    }

    /** Corta la cinemática de golpe para los jugadores indicados. */
    public static void stop(Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            if (WATCHING.remove(p.getUUID())) {
                ModNetwork.sendTo(p, new StopCinematicPacket());
            }
        }
        Iterator<Active> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            a.players.removeIf(id -> !WATCHING.contains(id));
            if (a.players.isEmpty()) {
                it.remove();
            }
        }
    }

    /**
     * Salta la cinemática en la que está este jugador. Como las cinemáticas son compartidas, se saltan para
     * todos los que la ven; lo que debía pasar al terminar (viajar, dar objetos...) pasa de inmediato.
     */
    public static void skip(ServerPlayer player) {
        if (!WATCHING.contains(player.getUUID())) {
            return;
        }
        Iterator<Active> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            if (!a.players.contains(player.getUUID())) {
                continue;
            }
            it.remove();
            WATCHING.removeAll(a.players);
            for (UUID id : a.players) {
                ServerPlayer sp = player.server.getPlayerList().getPlayer(id);
                if (sp != null) {
                    ModNetwork.sendTo(sp, new StopCinematicPacket());
                    ModNetwork.sendTo(sp, new com.mundosdeaxelis.network.FadePacket(0x000000, 0, 2, 12));
                }
            }
            if (a.onFinish != null) {
                try {
                    a.onFinish.run();
                } catch (Exception e) {
                    MundosDeAxelis.LOGGER.error("Error al terminar la cinematica {}", a.cinematic.id(), e);
                }
            }
            return;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Active> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            a.tick++;

            List<Cinematic.Cue> cues = a.cinematic.cues();
            while (a.nextCue < cues.size() && cues.get(a.nextCue).tick() <= a.tick) {
                try {
                    cues.get(a.nextCue).action().run(a.level, a.origin);
                } catch (Exception e) {
                    MundosDeAxelis.LOGGER.error("Error en un cue de la cinematica {}", a.cinematic.id(), e);
                }
                a.nextCue++;
            }

            if (a.tick >= a.cinematic.duration()) {
                WATCHING.removeAll(a.players);
                it.remove();
                if (a.onFinish != null) {
                    try {
                        a.onFinish.run();
                    } catch (Exception e) {
                        MundosDeAxelis.LOGGER.error("Error al terminar la cinematica {}", a.cinematic.id(), e);
                    }
                }
            }
        }
    }

    /** Quien ve una cinemática no recibe daño. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player && WATCHING.contains(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        WATCHING.remove(id);
        for (Active a : ACTIVE) {
            a.players.remove(id);
        }
        ACTIVE.removeIf(a -> a.players.isEmpty());
    }

    /** Para pruebas: corta todas las cinemáticas en curso. */
    public static void stopAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        List<ServerPlayer> all = new ArrayList<>(server.getPlayerList().getPlayers());
        stop(all);
    }
}

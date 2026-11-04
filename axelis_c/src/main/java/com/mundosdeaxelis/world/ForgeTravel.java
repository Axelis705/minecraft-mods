package com.mundosdeaxelis.world;

import com.mundosdeaxelis.entity.GranHerrero;
import com.mundosdeaxelis.network.FadePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Viaje a la dimensión del Gran Herrero (jefe de la etapa 2). Lo llama {@link BossTravel#enter}; el regreso y el
 * guardado del sitio de origen son los mismos que en la etapa 1.
 */
public final class ForgeTravel {
    private static final int DARK = 0x1A0A06;

    private ForgeTravel() {
    }

    public static void enter(MinecraftServer server, List<ServerPlayer> players) {
        ServerLevel level = server.getLevel(ModDimensions.FORGE_COPIES.get(0));
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
            Titles.actionBar(p, "La forja se enciende... un momento.");
        }
        start(server, ids);
    }

    private static void start(MinecraftServer server, List<UUID> ids) {
        if (BossTravel.locked(server)) {
            // otra entrada está construyendo una dungeon: esperar a que termine
            Scheduler.runLater(20, () -> start(server, ids));
            return;
        }
        ServerLevel level = BossTravel.pickCopy(server, 2);
        if (level == null) {
            BossTravel.noCopy(server, ids, DARK);
            return;
        }
        BossTravel.lock(server);
        // cada grupo que pulsa «Comenzar» usa una copia libre y genera en ella una dungeon NUEVA con el número de jugadores que entran
        ForgeArena.ensureBuilt(level, () -> {
            ForgeArena.openGateway(level);
            ForgeManager.of(level).reset(level);
            DungeonLayout lay = DungeonLayout.generate(new java.util.Random().nextLong(), DungeonLayout.clampPlayers(ids.size()), true);
            ForgeDungeon.build(level, lay, () -> {
                ForgeManager.of(level).begin(level, lay);
                finishEnter(server, ids, level);
            });
        });
    }

    private static void finishEnter(MinecraftServer server, List<UUID> ids, ServerLevel level) {
        BossTravel.unlock();
        int i = 0;
        DungeonLayout lay = ForgeManager.of(level).layout();
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                continue;
            }
            if (!BossTravel.inArena(p.level())) {
                BossTravel.saveReturn(p);
            }
            Vec3 s = ForgeManager.of(level).entryPoint(level, i);
            p.teleportTo(level, s.x, s.y, s.z, 180.0F, 0.0F);
            p.resetFallDistance();
            ModNetwork.sendTo(p, new FadePacket(DARK, 0, 5, 40));
            ForgeManager.of(level).sendMap(p);
            Titles.show(p, "Taller del Gran Herrero", lay == null ? "Abre camino sala por sala" : lay.cells.size() + " salas · busca llaves y pruebas");
            i++;
        }
    }

    private static final AABB ALL = new AABB(-60, 0, -60, 60, 200, 60);

    /** Hace aparecer al jefe si no hay uno vivo ya. */
    public static void spawnBoss(ServerLevel level) {
        if (!BossTravel.hasPlayer(level)) {
            return; // nadie dentro: no hay pelea que empezar (p. ej. todos murieron durante la intro)
        }
        for (GranHerrero r : level.getEntitiesOfClass(GranHerrero.class, ALL)) {
            if (r.isAlive()) {
                return;
            }
        }
        GranHerrero boss = ModEntities.GRAN_HERRERO.get().create(level);
        if (boss == null) {
            return;
        }
        Vec3 c = ForgeArena.bossSpot();
        boss.moveTo(c.x, c.y, c.z, 0.0F, 0.0F);
        level.addFreshEntity(boss);
        // pasado un rato, el túnel hacia la dungeon se cierra para que durante el combate no se vea lo de atrás
        Scheduler.runLater(800, () -> sealWhenClear(level, 0));
    }

    private static void sealWhenClear(ServerLevel level, int tries) {
        boolean alive = false;
        for (GranHerrero r : level.getEntitiesOfClass(GranHerrero.class, ALL)) {
            alive |= r.isAlive();
        }
        if (!alive || level.players().isEmpty()) {
            return;
        }
        if (ForgeArena.tunnelBusy(level) && tries < 30) {
            Scheduler.runLater(20, () -> sealWhenClear(level, tries + 1));
            return;
        }
        ForgeArena.sealGateway(level, true);
    }

    /** ¿Hay algo en marcha en la dimensión (dungeon sin terminar o jefe vivo)? Si no, quien aparezca ahí vuelve a casa. */
    public static boolean running(ServerLevel level) {
        boolean alive = ForgeManager.of(level).isInProgress();
        for (GranHerrero r : level.getEntitiesOfClass(GranHerrero.class, ALL)) {
            alive |= r.isAlive();
        }
        return alive;
    }
}

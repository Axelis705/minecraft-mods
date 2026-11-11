package com.autoscoleccion.server;

import com.autoscoleccion.entity.RcCarEntity;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.DriveStartPacket;
import com.autoscoleccion.registry.ModEntities;
import com.autoscoleccion.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Los autos a control remoto que hay en el mundo: uno por jugador como máximo. */
public final class Driving {
    /** Distancia máxima entre el jugador y su auto (bloques). */
    public static final double RANGE = 64.0;
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    private Driving() {
    }

    /** Saca un auto del garaje para manejarlo. */
    public static void start(ServerPlayer sp, int car) {
        if (car < 0 || car >= Cars.COUNT || !AutosData.has(sp, car)) {
            sp.displayClientMessage(Component.literal("§cTodavía no tienes ese auto."), true);
            return;
        }
        stop(sp);
        ServerLevel level = sp.serverLevel();
        RcCarEntity e = ModEntities.RC_CAR.get().create(level);
        if (e == null) {
            return;
        }
        Vec3 look = Vec3.directionFromRotation(0, sp.getYRot());
        Vec3 pos = sp.position().add(look.scale(1.4));
        e.moveTo(pos.x, sp.getY() + 0.2, pos.z, sp.getYRot(), 0);
        e.setCar(car);
        e.setOwner(sp.getUUID());
        level.addFreshEntity(e);
        ACTIVE.put(sp.getUUID(), e.getId());
        level.playSound(null, e.blockPosition(), ModSounds.get("machine_start"), SoundSource.PLAYERS, 0.6F, 1.4F);
        AutosNet.toPlayer(sp, new DriveStartPacket(e.getId()));
    }

    /** Guarda el auto (lo quita del mundo). */
    public static void stop(ServerPlayer sp) {
        Integer id = ACTIVE.remove(sp.getUUID());
        if (id != null) {
            Entity e = sp.serverLevel().getEntity(id);
            if (e instanceof RcCarEntity) {
                e.discard();
            }
        }
    }

    /** El cliente del dueño manda dónde está su auto. */
    public static void move(ServerPlayer sp, int id, double x, double y, double z, float yaw, float steer) {
        Integer mine = ACTIVE.get(sp.getUUID());
        if (mine == null || mine != id) {
            return;
        }
        Entity e = sp.serverLevel().getEntity(id);
        if (!(e instanceof RcCarEntity car)) {
            return;
        }
        // no se aceptan saltos imposibles (más de 3 bloques en un tick) ni posiciones lejos del jugador
        if (e.position().distanceToSqr(x, y, z) > 9.0 || sp.distanceToSqr(x, y, z) > RANGE * RANGE) {
            return;
        }
        car.moveTo(x, y, z, yaw, 0);
        car.setSyncedSteer(steer);
    }

    /** ¿Debe seguir existiendo este auto? */
    public static boolean keep(RcCarEntity e) {
        UUID owner = e.owner();
        if (owner == null) {
            return false;
        }
        Integer id = ACTIVE.get(owner);
        if (id == null || id != e.getId()) {
            return false;
        }
        var p = e.ownerPlayer();
        if (p == null || !p.isAlive() || p.distanceToSqr(e) > RANGE * RANGE) {
            ACTIVE.remove(owner);
            return false;
        }
        return true;
    }

    public static void forget(UUID player) {
        ACTIVE.remove(player);
    }
}

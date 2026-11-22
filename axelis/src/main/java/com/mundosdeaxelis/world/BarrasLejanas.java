package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Quita las barras de jefe que se quedan pegadas. Un mini-jefe quita su barra a quien se aleja, pero solo mientras él se mueve
 * (cuando su zona está «activa»). Si el jugador se va muy rápido (piedra de regreso, perla, /tp) y su zona deja de estar activa,
 * la barra se quedaba en pantalla. Aquí, cada segundo, se revisan todas: fuera de la dimensión o a más de 32 bloques se quita, y
 * si el dueño ya no está en el mundo se borra entera.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class BarrasLejanas {
    private BarrasLejanas() {
    }

    /** Dónde está el dueño de la barra y si ya no existe. */
    private record Owner(java.util.function.Supplier<net.minecraft.world.level.Level> level, java.util.function.Supplier<net.minecraft.world.phys.Vec3> pos,
                         java.util.function.BooleanSupplier gone) {
    }

    private static final Map<ServerBossEvent, Owner> OWNERS = new HashMap<>();
    public static final double RANGE = 32.0D;

    public static synchronized void track(ServerBossEvent ev, Entity owner) {
        OWNERS.put(ev, new Owner(owner::level, owner::position, owner::isRemoved));
    }

    /** Para barras de bloques (el Altar Maldito). */
    public static synchronized void track(ServerBossEvent ev, net.minecraft.world.level.block.entity.BlockEntity be) {
        net.minecraft.world.phys.Vec3 c = net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos());
        OWNERS.put(ev, new Owner(be::getLevel, () -> c, be::isRemoved));
    }

    public static synchronized void untrack(ServerBossEvent ev) {
        OWNERS.remove(ev);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 20 != 3) {
            return;
        }
        sweep();
    }

    /** Revisa todas las barras (también lo llaman las pruebas). */
    public static synchronized void sweep() {
        for (Map.Entry<ServerBossEvent, Owner> en : new ArrayList<>(OWNERS.entrySet())) {
            ServerBossEvent ev = en.getKey();
            Owner o = en.getValue();
            if (o.gone().getAsBoolean() || o.level().get() == null) {
                ev.removeAllPlayers();
                OWNERS.remove(ev);
                continue;
            }
            net.minecraft.world.phys.Vec3 c = o.pos().get();
            for (ServerPlayer p : new ArrayList<>(ev.getPlayers())) {
                if (p.isRemoved() || p.level() != o.level().get() || p.distanceToSqr(c) > RANGE * RANGE) {
                    ev.removePlayer(p);
                }
            }
        }
    }
}

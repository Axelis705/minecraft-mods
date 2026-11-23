package com.pescacoleccion.pesca;

import com.axeliscore.AxelisCore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * (Servidor) El pez que sale volando del agua hacia el pescador. Vuela igual que en el juego normal, pero si se queda corto (cae al
 * agua) o se pasa (cae detrás y lejos), al ratito llega solo a los pies de quien lo pescó: nunca se pierde un pez.
 */
public final class PezVolador {
    /** Ticks que se le dan al vuelo antes de ayudarlo (1,5 s); si ya cayó al suelo, desde los 8 ticks. */
    private static final int VUELO = 30, SUELO = 8, OLVIDAR = 200;
    /** Hasta esta distancia (bloques) el pez busca a su pescador. */
    private static final double LEJOS = 48;
    private static final List<Vuelo> VUELOS = new ArrayList<>();

    private static final class Vuelo {
        final ItemEntity item;
        final UUID dueno;
        int ticks;

        Vuelo(ItemEntity item, UUID dueno) {
            this.item = item;
            this.dueno = dueno;
        }
    }

    private PezVolador() {
    }

    public static void init() {
        AxelisCore.onServerTick(PezVolador::tick);
    }

    /** Sigue a un pez recién lanzado hacia su pescador. */
    static void seguir(ItemEntity it, ServerPlayer p) {
        synchronized (VUELOS) {
            VUELOS.add(new Vuelo(it, p.getUUID()));
        }
    }

    private static void tick(MinecraftServer s) {
        synchronized (VUELOS) {
            if (VUELOS.isEmpty()) {
                return;
            }
            Iterator<Vuelo> i = VUELOS.iterator();
            while (i.hasNext()) {
                Vuelo v = i.next();
                ItemEntity it = v.item;
                v.ticks++;
                if (it.isRemoved() || v.ticks > OLVIDAR) {
                    i.remove();
                    continue;
                }
                if (v.ticks < VUELO && !(it.onGround() && v.ticks >= SUELO)) {
                    continue;
                }
                ServerPlayer p = s.getPlayerList().getPlayer(v.dueno);
                if (p != null && p.level() == it.level() && p.distanceToSqr(it) < LEJOS * LEJOS && p.distanceToSqr(it) > 1.0) {
                    it.setPos(p.getX(), p.getY() + 0.25, p.getZ());
                    it.setDeltaMovement(Vec3.ZERO);
                    it.setNoPickUpDelay();
                }
                i.remove();
            }
        }
    }
}

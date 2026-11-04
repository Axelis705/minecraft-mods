package com.mundosdeaxelis.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * «Zonas de calor»: círculos temporales en el suelo donde el frío no te alcanza (como estar junto a un Brasero Eterno). Las crea
 * el Lobo Ártico durante la ventisca; aparecen en un sitio, duran unos segundos y desaparecen, así que hay que moverse. Dentro
 * de una zona NO te proteges de las guadañas, solo del frío.
 */
public final class HeatZones {
    private HeatZones() {
    }

    private static final class Z {
        ResourceKey<Level> dim;
        Vec3 c;
        double r;
        long from;
        long until;
    }

    private static final List<Z> ZONES = new ArrayList<>();

    /** Crea una zona que protege entre los ticks de juego {@code from} y {@code until}. */
    public static void add(ServerLevel sl, Vec3 center, double radius, long from, long until) {
        Z z = new Z();
        z.dim = sl.dimension();
        z.c = center;
        z.r = radius;
        z.from = from;
        z.until = until;
        ZONES.add(z);
    }

    /** ¿Está el jugador dentro de una zona de calor activa? */
    public static boolean inside(ServerPlayer p) {
        if (ZONES.isEmpty()) {
            return false;
        }
        long now = p.level().getGameTime();
        ResourceKey<Level> dim = p.level().dimension();
        boolean in = false;
        for (Iterator<Z> it = ZONES.iterator(); it.hasNext(); ) {
            Z z = it.next();
            if (now > z.until) {
                it.remove();
                continue;
            }
            if (!in && now >= z.from && z.dim == dim) {
                double dx = p.getX() - z.c.x;
                double dz = p.getZ() - z.c.z;
                double dy = p.getY() - z.c.y;
                if (dx * dx + dz * dz <= z.r * z.r && dy > -2.0 && dy < 3.0) {
                    in = true;
                }
            }
        }
        return in;
    }

    /** Quita las zonas de calor de esa copia de dimensión (las de otras copias no se tocan). */
    public static void clear(ServerLevel sl) {
        ZONES.removeIf(z -> z.dim == sl.dimension());
    }
}

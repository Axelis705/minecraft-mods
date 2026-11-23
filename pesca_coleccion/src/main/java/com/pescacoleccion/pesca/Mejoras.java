package com.pescacoleccion.pesca;

import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Nbt;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pelea.PeleaSim;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mejoras para siempre: se ganan con las estrellas de maestría (3 por pez: pescarlo, uno grande y uno sin errores; 75 en total) y
 * cañas que se desbloquean con las especies descubiertas. Todo lo calcula el servidor con la colección del jugador.
 */
public final class Mejoras {
    /** Una mejora: estrellas que pide y qué da. */
    public record Mejora(String id, int estrellas, float zona, float hilo, float fuerza, int ventana, int suerte) {
    }

    public static final Mejora[] TODAS = {
            new Mejora("mano_firme_1", 5, 0.03F, 0, 0, 0, 0),
            new Mejora("hilo_trenzado_1", 10, 0, 10, 0, 0, 0),
            new Mejora("carrete_suave", 15, 0, 0, 0.08F, 0, 0),
            new Mejora("ojo_pescador", 22, 0, 0, 0, 12, 0),
            new Mejora("mano_firme_2", 30, 0.03F, 0, 0, 0, 0),
            new Mejora("hilo_trenzado_2", 38, 0, 15, 0, 0, 0),
            new Mejora("suerte_rio", 46, 0, 0, 0, 0, 1),
            new Mejora("carrete_maestro", 55, 0, 0, 0.1F, 0, 0),
            new Mejora("ojo_aguila", 64, 0, 0, 0, 12, 0),
            new Mejora("leyenda_agua", 75, 0.02F, 10, 0.05F, 0, 1)};

    /** Cañas que se ganan descubriendo especies (la de bambú se fabrica). */
    public static final int[] CANA_ESPECIES = {0, 5, 10, 16, 22};

    private Mejoras() {
    }

    public static CompoundTag datos(ServerPlayer p) {
        return PlayerStore.of(p, PescaColeccion.MODID);
    }

    /** Especies distintas que ha pescado. */
    public static int especies(CompoundTag datos) {
        CompoundTag peces = Nbt.c(datos, "peces");
        int n = 0;
        for (Especie e : Especies.all()) {
            n += Nbt.i(Nbt.c(peces, e.id()), "n") > 0 ? 1 : 0;
        }
        return n;
    }

    /** Aplica a la pelea las mejoras que ya tiene. */
    public static void aplicar(PeleaSim.Params p, CompoundTag datos) {
        int st = Coleccion.estrellas(datos);
        float zona = 0, hilo = 0, fuerza = 0;
        int ventana = 0;
        for (Mejora m : TODAS) {
            if (st >= m.estrellas()) {
                zona += m.zona();
                hilo += m.hilo();
                fuerza += m.fuerza();
                ventana += m.ventana();
            }
        }
        p.zone = Math.min(0.75F, p.zone + zona);
        p.lineHp += hilo;
        p.damage *= 1 + fuerza;
        p.good += ventana;
        p.perfect += ventana / 3;
    }

    /** Suerte extra por las mejoras. */
    public static int suerte(CompoundTag datos) {
        int st = Coleccion.estrellas(datos), s = 0;
        for (Mejora m : TODAS) {
            if (st >= m.estrellas()) {
                s += m.suerte();
            }
        }
        return s;
    }

    /** ¿Puede reclamar esta caña? (tiene las especies y no la ha reclamado) */
    public static boolean puedeReclamar(CompoundTag datos, Cana c) {
        return c.ordinal() > 0 && especies(datos) >= CANA_ESPECIES[c.ordinal()] && (Nbt.i(datos, "canas") & (1 << c.ordinal())) == 0;
    }
}

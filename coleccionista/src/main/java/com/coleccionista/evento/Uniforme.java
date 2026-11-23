package com.coleccionista.evento;

import com.axeliscore.plat.Ids;
import com.axeliscore.vestir.Vestuario;
import com.coleccionista.Coleccionista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * El uniforme de las piezas: camisa color hueso gastada, pantalón gris y una tela con su número en el pecho y la espalda (con unas
 * manchas rojas distintas en cada quien). Va encima de la skin con el Vestuario del Core: no se quita, no ocupa la armadura y cada
 * quien conserva su cara. Se pone al inscribirse (o al llevarlos a la sala de espera) y se quita al salir del evento.
 */
public final class Uniforme {
    public static final ResourceLocation ID = Ids.rl(Coleccionista.MODID, "uniforme");
    /** El organizador puede apagarlo ({@code /evento uniforme no}). */
    public static boolean activo = true;

    private Uniforme() {
    }

    public static void poner(MinecraftServer s, Pieza p) {
        if (!activo || s == null || p == null) {
            return;
        }
        CompoundTag antes = Vestuario.datos(s, p.id, ID);
        CompoundTag d = new CompoundTag();
        d.putString("numero", p.etiqueta());
        // cuántas pruebas lleva (el uniforme se ve más gastado y manchado con cada una)
        d.putInt("pruebas", antes.getInt("pruebas"));
        if (!antes.equals(d)) {
            Vestuario.poner(s, p.id, ID, d);
        }
    }

    /** Empieza otra prueba: el uniforme de cada pieza viva se gasta un poco más (más mugre, manchas y al final rasgaduras). */
    public static void desgastar(MinecraftServer s) {
        if (!activo || s == null) {
            return;
        }
        for (Pieza p : Evento.get(s).vivos()) {
            if (!Vestuario.tiene(s, p.id, ID)) {
                continue;
            }
            CompoundTag d = Vestuario.datos(s, p.id, ID);
            d.putInt("pruebas", d.getInt("pruebas") + 1);
            Vestuario.poner(s, p.id, ID, d);
        }
    }

    public static void quitar(MinecraftServer s, Pieza p) {
        if (s != null && p != null) {
            Vestuario.quitar(s, p.id, ID);
        }
    }

    public static void quitarTodos(MinecraftServer s) {
        if (s != null) {
            Vestuario.quitarDeTodos(s, ID);
        }
    }

    /** Lo prende o lo apaga para todas las piezas. */
    public static void activar(MinecraftServer s, Evento ev, boolean on) {
        activo = on;
        if (on) {
            for (Pieza p : ev.piezas()) {
                poner(s, p);
            }
        } else {
            quitarTodos(s);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_UNIFORME {}", on ? "puesto" : "quitado");
    }
}

package com.coleccionista.prueba;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/** Todas las pruebas juntas: arrancan, se enteran cuando se acaba el reloj y se borran al reiniciar el evento. */
public final class Pruebas {
    private Pruebas() {
    }

    public static void init() {
        Encadenados.init();
        Compartir.init();
        LasPuertas.init();
        LasCopas.init();
        Sacrificio.init();
        Traidor.init();
        ACiegas.init();
        TresSentidos.init();
        Sentidos.init();
        Mascota.init();
    }

    /** El reloj del evento llegó a cero: le toca a la prueba que esté en marcha. Devuelve si alguna lo atendió. */
    public static boolean alAcabarReloj(MinecraftServer s) {
        return Encadenados.alAcabarReloj(s) || Compartir.alAcabarReloj(s) || LasPuertas.alAcabarReloj(s) || LasCopas.alAcabarReloj(s) || Sacrificio.alAcabarReloj(s)
                || Traidor.alAcabarReloj(s) || ACiegas.alAcabarReloj(s) || TresSentidos.alAcabarReloj(s)
                || Sentidos.alAcabarReloj(s) || Mascota.alAcabarReloj(s);
    }

    /**
     * Antes de preparar una prueba: las demás que quedaron abiertas (por ejemplo, una que terminó y no se limpió) se cierran sin eliminar a
     * nadie, para que no se encimen sus avisos, sus voces ni sus rejas con la nueva.
     */
    public static void cerrarOtras(MinecraftServer s, String nueva) {
        cerrar(s, nueva, "encadenados", Encadenados.abierta(), () -> Encadenados.terminar(s));
        cerrar(s, nueva, "compartir", Compartir.abierta(), () -> Compartir.terminar(s));
        cerrar(s, nueva, "puertas", LasPuertas.abierta(), () -> LasPuertas.terminar(s));
        cerrar(s, nueva, "copas", LasCopas.abierta(), () -> LasCopas.terminar(s));
        cerrar(s, nueva, "sacrificio", Sacrificio.abierta(), () -> Sacrificio.terminar(s));
        cerrar(s, nueva, "traidor", Traidor.abierta(), () -> Traidor.terminar(s));
        cerrar(s, nueva, "ciegas", ACiegas.abierta(), () -> ACiegas.terminar(s));
        cerrar(s, nueva, "tres", TresSentidos.abierta(), () -> TresSentidos.terminar(s));
        cerrar(s, nueva, "sentidos", Sentidos.abierta(), () -> Sentidos.terminar(s));
        cerrar(s, nueva, "mascota", Mascota.abierta(), () -> Mascota.reiniciar(s));
    }

    private static void cerrar(MinecraftServer s, String nueva, String id, boolean abierta, Runnable terminar) {
        if (abierta && !id.equals(nueva)) {
            terminar.run();
            com.coleccionista.Coleccionista.LOGGER.info("COLECCIONISTA_PRUEBAS cerrada {} (se prepara {})", id, nueva);
        }
    }

    /** Los equipos que formaron las piezas en los cubículos (los usa el siguiente "preparar" en lugar de hacerlos al azar). */
    private static List<List<java.util.UUID>> dados;

    public static void darEquipos(List<List<java.util.UUID>> equipos) {
        dados = equipos;
    }

    /** Los equipos dados (solo piezas vivas, sin equipos vacíos), o null si no hay; se usan una sola vez. */
    public static List<List<com.coleccionista.evento.Pieza>> dados(com.coleccionista.evento.Evento ev) {
        if (dados == null) {
            return null;
        }
        List<List<com.coleccionista.evento.Pieza>> out = new ArrayList<>();
        for (List<java.util.UUID> eq : dados) {
            List<com.coleccionista.evento.Pieza> l = new ArrayList<>();
            for (java.util.UUID id : eq) {
                com.coleccionista.evento.Pieza p = ev.pieza(id);
                if (p != null && p.vivo()) {
                    l.add(p);
                }
            }
            if (!l.isEmpty()) {
                out.add(l);
            }
        }
        dados = null;
        return out.isEmpty() ? null : out;
    }

    public static void reiniciar(MinecraftServer s) {
        Encadenados.reiniciar(s);
        Compartir.reiniciar(s);
        LasPuertas.reiniciar(s);
        LasCopas.reiniciar(s);
        Sacrificio.reiniciar(s);
        Traidor.reiniciar(s);
        ACiegas.reiniciar(s);
        TresSentidos.reiniciar(s);
        Sentidos.reiniciar(s);
        Mascota.reiniciar(s);
    }
}

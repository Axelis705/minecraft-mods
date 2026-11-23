package com.coleccionista.prueba;

import net.minecraft.server.MinecraftServer;

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
    }

    /** El reloj del evento llegó a cero: le toca a la prueba que esté en marcha. Devuelve si alguna lo atendió. */
    public static boolean alAcabarReloj(MinecraftServer s) {
        return Encadenados.alAcabarReloj(s) || Compartir.alAcabarReloj(s) || LasPuertas.alAcabarReloj(s) || LasCopas.alAcabarReloj(s) || Sacrificio.alAcabarReloj(s)
                || Traidor.alAcabarReloj(s) || ACiegas.alAcabarReloj(s);
    }

    public static void reiniciar(MinecraftServer s) {
        Encadenados.reiniciar(s);
        Compartir.reiniciar(s);
        LasPuertas.reiniciar(s);
        LasCopas.reiniciar(s);
        Sacrificio.reiniciar(s);
        Traidor.reiniciar(s);
        ACiegas.reiniciar(s);
    }
}

package com.coleccionista.evento;

import net.minecraft.server.MinecraftServer;

/**
 * El chat de voz por cercanía (Simple Voice Chat): los fantasmas van a su propio canal y los vivos no los oyen. Si el mod de voz no está
 * instalado, no hace nada. (La conexión con el mod de voz se agrega en su etapa.)
 */
public final class Voz {
    private Voz() {
    }

    /** Cada vez que alguien se vuelve fantasma o revive. */
    static void actualizar(MinecraftServer s, Evento ev) {
        // se conecta con Simple Voice Chat en la etapa de voz
    }
}

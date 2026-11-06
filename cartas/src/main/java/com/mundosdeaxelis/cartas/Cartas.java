package com.mundosdeaxelis.cartas;

import net.minecraftforge.eventbus.api.IEventBus;

/** Punto de entrada del sistema de cartas. Se llama una sola vez: Cartas.init(bus del mod). */
public final class Cartas {
    public static final String MOD_ID = "mundos_de_axelis";

    private Cartas() {
    }

    public static void init(IEventBus modBus) {
        CartasItems.register(modBus);
        modBus.addListener(AlbumCaps::register);
        CartasNet.init();
    }
}

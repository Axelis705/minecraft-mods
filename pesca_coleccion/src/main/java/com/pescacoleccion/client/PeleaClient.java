package com.pescacoleccion.client;

import com.axeliscore.plat.client.Screens;
import com.axeliscore.ui.Rareza;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pelea.Pelea;
import com.pescacoleccion.pelea.PeleaNet;
import com.pescacoleccion.pelea.PeleaSim;
import net.minecraft.network.chat.Component;

/** (Cliente) Abre la pelea que pidió el servidor y le devuelve las teclas al terminar. */
public final class PeleaClient {
    private PeleaClient() {
    }

    public static void open(int id, String pez, int rareza, PeleaSim.Params params) {
        long t0 = System.nanoTime();
        Screens.open(new PeleaScreen(id, params, Component.translatable(pez), Rareza.of(rareza), teclas -> {
            int endMs = (int) ((System.nanoTime() - t0) / 1_000_000L);
            boolean gaveUp = teclas == null;
            PescaColeccion.NET.toServer(new PeleaNet.Fin(id, gaveUp, endMs, Pelea.events(gaveUp ? new int[0] : teclas)));
        }));
    }
}

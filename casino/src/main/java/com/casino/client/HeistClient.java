package com.casino.client;

import com.casino.heist.HeistGamePacket;
import com.casino.heist.HeistPlanPacket;

/** Lo que llega del servidor para el robo: abrir la pantalla del contacto o un minijuego. */
public final class HeistClient {
    private HeistClient() {
    }

    public static void openPlan(HeistPlanPacket m) {
        ClientHooks.open(new HeistPlanScreen(m));
    }

    public static void openGame(HeistGamePacket m) {
        ClientHooks.open(new HeistGameScreen(m));
    }
}

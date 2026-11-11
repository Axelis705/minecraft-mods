package com.autoscoleccion.client;

import com.autoscoleccion.game.Cars;
import com.autoscoleccion.net.CollectionPacket;
import com.autoscoleccion.net.GameOutcomePacket;
import com.autoscoleccion.net.StartGamePacket;
import com.autoscoleccion.net.UnboxPacket;
import net.minecraft.client.Minecraft;

/** Lo que hace el cliente cuando llega un mensaje del servidor. */
public final class AutosClient {
    /** Cuántas veces tiene cada auto (lo último que mandó el servidor). */
    public static int[] counts = new int[Cars.COUNT];

    private AutosClient() {
    }

    public static void onStart(StartGamePacket m) {
        Minecraft.getInstance().setScreen(new MinigameScreen(m));
    }

    public static void onOutcome(GameOutcomePacket m) {
        if (Minecraft.getInstance().screen instanceof MinigameScreen s) {
            s.outcome(m);
        }
    }

    public static void onUnbox(UnboxPacket m) {
        Minecraft.getInstance().setScreen(new UnboxScreen(m));
    }

    public static void onCollection(CollectionPacket m) {
        int[] c = new int[Cars.COUNT];
        System.arraycopy(m.counts(), 0, c, 0, Math.min(c.length, m.counts().length));
        counts = c;
        if (m.open()) {
            Minecraft.getInstance().setScreen(new GarageScreen());
        }
    }
}

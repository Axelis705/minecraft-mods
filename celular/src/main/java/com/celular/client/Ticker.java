package com.celular.client;

import com.celular.Celular;
import net.minecraft.client.Minecraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Reloj de respaldo del celular: cada 50 ms le pide al hilo del juego que corra lo que no depende de dibujar (teclas, música, alarmas,
 * linterna). Normalmente eso lo hace el HUD en cada cuadro, pero con F1 el juego no dibuja el HUD; así sigue funcionando igual.
 * Usa {@code Minecraft.execute}, que es igual en todas las versiones. También mueve la prueba con fotos ({@link AutoTest}).
 */
public final class Ticker {
    private static boolean started;
    private static volatile boolean pending;
    private static boolean warned;

    private Ticker() {
    }

    static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Celular-Reloj");
            t.setDaemon(true);
            return t;
        });
        ex.scheduleAtFixedRate(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && !pending) {
                pending = true;
                mc.execute(Ticker::run);
            }
        }, 2000, 50, TimeUnit.MILLISECONDS);
    }

    private static void run() {
        pending = false;
        try {
            PhoneHud.fallback();
            AutoTest.tick();
        } catch (Throwable e) {
            if (!warned) {
                warned = true;
                Celular.LOGGER.error("Celular: falló el reloj de respaldo", e);
            }
        }
    }
}

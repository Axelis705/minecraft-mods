package com.axeliscore.fx.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.client.G;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * (Cliente) Cosas propias en la pantalla de juego (marcadores, temporizadores, avisos fijos), igual en todas las versiones. Se dibujan
 * en la capa del Core, encima del HUD de Minecraft y debajo de los títulos grandes:
 * <pre>
 * Hud.add((g, ancho, alto) -&gt; { if (torneoActivo) dibujarMarcador(g, ancho); });
 * </pre>
 */
public final class Hud {
    @FunctionalInterface
    public interface Layer {
        void render(G g, int width, int height);
    }

    private static final List<Layer> LAYERS = new CopyOnWriteArrayList<>();

    private Hud() {
    }

    public static void add(Layer l) {
        LAYERS.add(l);
    }

    static void renderAll(G g, int w, int h) {
        for (Layer l : LAYERS) {
            try {
                l.render(g, w, h);
            } catch (RuntimeException e) {
                AxelisCore.LOGGER.error("Axelis Core: falló una capa del HUD", e);
                LAYERS.remove(l);
            }
        }
    }
}

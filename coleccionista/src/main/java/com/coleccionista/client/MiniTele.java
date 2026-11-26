package com.coleccionista.client;

import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * (Cliente) La mini tele del HUD: cuando el Coleccionista habla y no tienes ninguna pantalla enfrente, aparece arriba a la derecha (debajo
 * del "en juego") una tele chiquita con él en vivo (la misma animación que las pantallas), con su marco, rayas de tele vieja y la luz de
 * "en vivo". Entra deslizándose y prende con un chispazo de estática; cuando deja de hablar se va (y ahí mismo sale la tarjeta de reglas).
 */
public final class MiniTele {
    private static float abierta;
    private static long ultimo;

    private MiniTele() {
    }

    /** Dónde está ahora (para que el subtítulo no se le encime): su orilla izquierda y la de abajo; MAX/0 si no se ve. */
    public static int bordeIzq = Integer.MAX_VALUE, bordeAbajo;

    /** ¿Se está viendo la mini tele? (para la prueba automática) */
    public static boolean visible() {
        return abierta > 0.5F;
    }

    public static void init() {
        com.axeliscore.fx.client.Hud.add(MiniTele::dibujar);
    }

    private static void dibujar(G g, int w, int h) {
        long now = System.currentTimeMillis();
        float dt = ultimo == 0 ? 0 : Math.min(0.1F, (now - ultimo) / 1000F);
        ultimo = now;
        String linea = Subtitulo.linea();
        boolean habla = !linea.isEmpty();
        boolean ver = habla && EventoHud.visible() && !Cine.enPantalla() && !Presentacion.activa() && !Presentacion.reglasVisibles()
                && !Atajos.pantallaEnfrente();
        abierta = Mth.clamp(abierta + (ver ? dt * 3.2F : -dt * 4F), 0F, 1F);
        if (abierta <= 0.01F) {
            bordeIzq = Integer.MAX_VALUE;
            bordeAbajo = 0;
            return;
        }
        float e = abierta * abierta * (3 - 2 * abierta);
        int tw = Math.min(110, Math.max(84, w / 5)), th = tw * 3 / 4;
        int x1 = w - 6 + (int) ((1 - e) * (tw + 14)), x0 = x1 - tw;
        int y0 = Presentacion.arribaDerecha(), y1 = y0 + th;
        bordeIzq = x0 - 4;
        bordeAbajo = y1 + 10;
        // el marco de la tele (plástico oscuro con tornillos) y la pantalla con terciopelo
        g.fill(x0 - 4, y0 - 4, x1 + 4, y1 + 10, 0xF0141214);
        g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 9, 0xF0262326);
        for (int[] c : new int[][] {{x0 - 2, y0 - 2}, {x1 + 1, y0 - 2}, {x0 - 2, y1 + 7}, {x1 + 1, y1 + 7}}) {
            g.fill(c[0], c[1], c[0] + 1, c[1] + 1, 0xFF6A6266);
        }
        g.fillGradient(x0, y0, x1, y1, 0xFF3A0A10, 0xFF180406);
        ColeccionistaTV.dibujarHud(g, x0, y0, tw, th, habla, linea);
        // encima del modelo (más adelante en z): rayas de tele vieja, el chispazo al prender y la luz de "en vivo"
        g.push();
        g.translate(0, 0, 400);
        for (int y = y0; y < y1; y += 2) {
            g.fill(x0, y, x1, y + 1, 0x12000000);
        }
        float prende = Mth.clamp((abierta - 0.55F) / 0.35F, 0F, 1F);
        if (prende < 1F) {
            float a = 1 - prende;
            for (int i = 0; i < 18; i++) {
                int yy = y0 + (int) (Math.random() * th), xx = x0 + (int) (Math.random() * tw), ll = 4 + (int) (Math.random() * 18);
                g.fill(xx, yy, Math.min(x1, xx + ll), yy + 1, Ui.alpha(0xFFD8D8D8, a * 0.8F));
            }
            g.fill(x0, y0, x1, y1, Ui.alpha(0xFF101010, a * 0.6F));
        }
        Font f = Ui.font();
        boolean luz = (now / 500) % 2 == 0;
        g.fill(x0 + 1, y1 + 3, x0 + 4, y1 + 6, luz ? 0xFFE02020 : 0xFF501010);
        String vivo = Component.translatable(Coleccionista.MODID + ".mini.vivo").getString();
        g.push();
        g.translate(x0 + 7, y1 + 2.5F, 0);
        g.scale(0.75F, 0.75F, 1);
        g.drawString(f, vivo, 0, 0, 0xFFB8B0A8, false);
        g.pop();
        g.pop();
    }
}

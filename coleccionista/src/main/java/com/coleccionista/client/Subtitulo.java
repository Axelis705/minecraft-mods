package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Pantallas;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * (Cliente) Los subtítulos del Coleccionista: grandes, con su nombre arriba, y el texto que se va escribiendo letra por letra (como si lo
 * estuviera diciendo). Cada frase entra subiendo un poquito; en las risas el texto tiembla. Los usan el subtítulo normal (abajo) y el de
 * cerca de la tele (en la franja de cine).
 */
public final class Subtitulo {
    /** Letras por segundo que se van escribiendo. */
    private static final float LETRAS_SEG = 34F;
    private static String clave = "";
    private static long desde;

    private Subtitulo() {
    }

    /** La frase que se está diciendo (clave de idioma), o "" si nadie habla. */
    static String linea() {
        CompoundTag t = ClientData.get(Pantallas.KEY);
        if (t == null || !Nbt.b(t, "hablando")) {
            return "";
        }
        String l = Nbt.s(t, "linea");
        if (!l.equals(clave)) {
            clave = l;
            desde = System.currentTimeMillis();
        }
        return l;
    }

    /** ¿Ya terminó de hablar (la pantalla muestra al Coleccionista callado)? */
    static boolean termino() {
        CompoundTag t = ClientData.get(Pantallas.KEY);
        return t != null && "coleccionista".equals(Nbt.s(t, "modo")) && !Nbt.b(t, "hablando");
    }

    /**
     * Dibuja la frase actual centrada en {@code cx}, con su parte de abajo en {@code abajo}, en un ancho máximo {@code ancho} (pixeles de la
     * interfaz) y a escala {@code esc}. {@code caja}: con fondo oscuro y la raya roja (si no, solo letras con sombra).
     */
    static void dibujar(G g, int cx, int abajo, int ancho, float esc, boolean caja) {
        String l = linea();
        if (l.isEmpty()) {
            return;
        }
        Font f = Ui.font();
        String texto = Component.translatable(l).getString();
        long ms = System.currentTimeMillis() - desde;
        boolean risa = l.endsWith(".risa");
        // las líneas ya partidas (así no brincan mientras se escriben)
        List<String> ls = new ArrayList<>();
        for (FormattedText ft : f.getSplitter().splitLines(texto, Math.max(60, (int) (ancho / esc)), Style.EMPTY)) {
            ls.add(ft.getString());
        }
        int maxW = 0;
        for (String s : ls) {
            maxW = Math.max(maxW, f.width(s));
        }
        float entra = Math.min(1F, ms / 220F);
        if (entra < 0.05F) {
            return; // (con alfa casi 0 Minecraft dibuja las letras opacas)
        }
        float sube = (1F - entra) * 6F;
        float altoLinea = 10F * esc;
        float alto = ls.size() * altoLinea;
        String nombre = Component.translatable(Coleccionista.MODID + ".sub.nombre").getString();
        float escNom = Math.max(0.75F, esc * 0.72F);
        float yTexto = abajo - alto + sube;
        float yNombre = yTexto - 9F * escNom - 3;
        int a = (int) (entra * 255);
        if (caja) {
            int w2 = (int) (maxW * esc / 2F) + 10;
            int top = (int) yNombre - 5, bot = (int) (yTexto + alto) + 4;
            g.fill(cx - w2, top, cx + w2, bot, Ui.alpha(0xD0060304, entra));
            g.fill(cx - w2, top, cx - w2 + 2, bot, Ui.alpha(EventoHud.ROJO, entra));
            g.fill(cx - w2, bot - 1, cx + w2, bot, Ui.alpha(0x60B3121A, entra));
        }
        // el nombre de quien habla
        g.push();
        g.translate(cx - maxW * esc / 2F, yNombre, 0);
        g.scale(escNom, escNom, 1);
        g.drawString(f, nombre, 0, 0, Ui.alpha(EventoHud.ROJO_CL, entra), true);
        g.pop();
        // el texto, letra por letra (la última que aparece entra suave)
        float visibles = Math.max(0, (ms - 120) / 1000F * LETRAS_SEG);
        int usadas = 0;
        float y = yTexto;
        for (int i = 0; i < ls.size(); i++) {
            String s = ls.get(i);
            int n = (int) Math.min(s.length(), Math.max(0, visibles - usadas));
            float x0 = cx - f.width(s) * esc / 2F;
            if (n > 0) {
                String vis = s.substring(0, n);
                float jx = risa ? (float) Math.sin(ms / 45.0 + i) * 0.8F : 0F, jy = risa ? (float) Math.cos(ms / 37.0 + i * 2) * 0.6F : 0F;
                g.push();
                g.translate(x0 + jx, y + jy, 0);
                g.scale(esc, esc, 1);
                g.drawString(f, vis, 0, 0, (a << 24) | 0xF4ECE4, true);
                // la letra que va entrando
                if (n < s.length() && (visibles - usadas - n) * entra > 0.05F) {
                    float fr = visibles - usadas - n;
                    g.drawString(f, s.substring(n, n + 1), f.width(vis), 0, Ui.alpha(0xFFF4ECE4, Math.min(1F, fr) * entra), true);
                }
                g.pop();
            }
            usadas += s.length();
            y += altoLinea;
        }
    }
}

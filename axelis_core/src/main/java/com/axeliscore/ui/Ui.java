package com.axeliscore.ui;

import com.axeliscore.plat.client.G;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Pinceles de la interfaz del Core (todo con rectángulos y degradados: se ve igual en 1.20.1, 1.21.1 y 26.3 y no necesita texturas).
 */
public final class Ui {
    private Ui() {
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Mezcla dos colores ARGB (t = 0 el primero, 1 el segundo). */
    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    /** El mismo color con otra opacidad (0..1). */
    public static int alpha(int c, float a) {
        return (int) (Math.max(0, Math.min(1, a)) * (c >>> 24)) << 24 | (c & 0xFFFFFF);
    }

    /** Rectángulo con las esquinas recortadas (se ve redondeado). */
    public static void round(G g, int x, int y, int w, int h, int c) {
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    /**
     * Panel: sombra, borde oscuro, filo claro, fondo con degradado y una franja de color arriba. {@code open} (0..1) es la animación de
     * aparecer (se usa para la opacidad).
     */
    public static void panel(G g, int x, int y, int w, int h, Theme t, float open) {
        float a = Math.max(0, Math.min(1, open));
        round(g, x + 3, y + 4, w, h, alpha(t.shadow, a));
        round(g, x - 1, y - 1, w + 2, h + 2, alpha(t.border, a));
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, alpha(t.bgTop, a), alpha(t.bgBottom, a));
        // filo claro arriba e izquierda, oscuro abajo y derecha (relieve)
        g.fill(x + 1, y + 1, x + w - 1, y + 2, alpha(t.borderLight, a * 0.8F));
        g.fill(x + 1, y + 2, x + 2, y + h - 1, alpha(t.borderLight, a * 0.4F));
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, alpha(0xFF000000, a * 0.35F));
        // franja de acento
        g.fill(x + 4, y + 3, x + w - 4, y + 4, alpha(t.accent, a));
        // remaches en las esquinas
        int r = alpha(t.accent, a * 0.9F);
        g.fill(x + 3, y + 6, x + 5, y + 8, r);
        g.fill(x + w - 5, y + 6, x + w - 3, y + 8, r);
        g.fill(x + 3, y + h - 7, x + 5, y + h - 5, r);
        g.fill(x + w - 5, y + h - 7, x + w - 3, y + h - 5, r);
    }

    /** Un hueco hundido (para listas, retratos, ranuras). */
    public static void inset(G g, int x, int y, int w, int h, Theme t) {
        g.fill(x, y, x + w, y + h, alpha(0xFF000000, 0.35F));
        g.fill(x, y, x + w, y + 1, alpha(0xFF000000, 0.5F));
        g.fill(x, y, x + 1, y + h, alpha(0xFF000000, 0.5F));
        g.fill(x, y + h - 1, x + w, y + h, alpha(t.borderLight, 0.35F));
        g.fill(x + w - 1, y, x + w, y + h, alpha(t.borderLight, 0.35F));
    }

    /** Título centrado con sombra y una línea de adorno a cada lado. */
    public static void title(G g, String s, int cx, int y, Theme t) {
        Font f = font();
        int w = f.width(s);
        g.drawString(f, s, cx - w / 2, y, t.title, true);
        int ly = y + 4;
        g.fill(cx - w / 2 - 22, ly, cx - w / 2 - 6, ly + 1, alpha(t.accent, 0.8F));
        g.fill(cx + w / 2 + 6, ly, cx + w / 2 + 22, ly + 1, alpha(t.accent, 0.8F));
        g.fill(cx - w / 2 - 5, ly - 1, cx - w / 2 - 3, ly + 2, t.accent);
        g.fill(cx + w / 2 + 3, ly - 1, cx + w / 2 + 5, ly + 2, t.accent);
    }

    /** Botón: degradado, brillo al pasar el mouse, se hunde al apretarlo, gris si está apagado. */
    public static void button(G g, int x, int y, int w, int h, String label, float hover, boolean pressed, boolean enabled, Theme t) {
        int base = enabled ? mix(t.button, t.buttonHover, hover) : mix(t.button, 0xFF555555, 0.6F);
        int dy = pressed ? 1 : 0;
        round(g, x, y + 1, w, h, alpha(0xFF000000, 0.45F));
        round(g, x, y + dy, w, h - 1, t.border);
        g.fillGradient(x + 1, y + 1 + dy, x + w - 1, y + h - 2 + dy, mix(base, 0xFFFFFFFF, 0.12F), mix(base, 0xFF000000, 0.18F));
        g.fill(x + 2, y + 1 + dy, x + w - 2, y + 2 + dy, alpha(0xFFFFFFFF, 0.25F + 0.2F * hover));
        if (hover > 0.01F && enabled) {
            g.fill(x + 1, y + h - 3 + dy, x + w - 1, y + h - 2 + dy, alpha(t.accent, hover));
        }
        Font f = font();
        int col = enabled ? t.buttonText : 0xFF9A9A9A;
        g.drawString(f, label, x + (w - f.width(label)) / 2, y + (h - 9) / 2 + dy, col, true);
    }

    /** Barra de progreso con brillo que la recorre; {@code frac} de 0 a 1. */
    public static void bar(G g, int x, int y, int w, int h, float frac, int from, int to, float time) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF22252E);
        int fw = (int) (w * frac);
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, mix(from, 0xFFFFFFFF, 0.15F), to);
            g.fill(x, y, x + fw, y + 1, alpha(0xFFFFFFFF, 0.35F));
            // brillo que pasa
            int sx = x + (int) ((time * 40) % (w + 30)) - 15;
            for (int i = 0; i < 6; i++) {
                int px = sx + i;
                if (px >= x && px < x + fw) {
                    g.fill(px, y, px + 1, y + h, alpha(0xFFFFFFFF, 0.25F - Math.abs(i - 3) * 0.06F));
                }
            }
        }
    }

    /** Corta un texto en renglones de como mucho {@code width} píxeles (por palabras; respeta los saltos \n). */
    public static List<String> wrap(String text, int width) {
        Font f = font();
        List<String> out = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String w : para.split(" ")) {
                String probe = line.length() == 0 ? w : line + " " + w;
                if (line.length() > 0 && f.width(probe) > width) {
                    out.add(line.toString());
                    line = new StringBuilder(w);
                } else {
                    line = new StringBuilder(probe);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    /** Texto en renglones; devuelve la y de abajo. */
    public static int text(G g, String s, int x, int y, int width, int color, int lineH) {
        for (String l : wrap(s, width)) {
            g.drawString(font(), l, x, y, color, false);
            y += lineH;
        }
        return y;
    }

    /** El clic de los botones de Minecraft (con otro tono si se quiere). */
    public static void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    /** Suave: de 0 a 1 con entrada y salida lentas. */
    public static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Rebote al final (para cosas que aparecen). */
    public static float pop(float t) {
        t = Math.max(0, Math.min(1, t));
        float c1 = 1.70158F, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }
}

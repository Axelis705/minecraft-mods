package com.axeliscore.ui;

import com.axeliscore.plat.client.G;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Pinceles de la interfaz del Core, estilo "Minecraft pulido": paneles con fondo de pixel art y relieve, botones con textura que se
 * tiñe del color del tema (brillan y se hunden), ranuras como las del inventario, barras con brillo. Se ve igual en 1.20.1, 1.21.1 y
 * 26.3.
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
    /** Recorta un texto para que quepa en {@code w} píxeles (con "…" al final si se cortó). */
    public static String fit(String s, int w) {
        if (font().width(s) <= w) {
            return s;
        }
        while (s.length() > 1 && font().width(s + "…") > w) {
            s = s.substring(0, s.length() - 1);
        }
        return s.trim() + "…";
    }

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
        if (t.frame != null) {
            // con marco de textura: sombra, fondo (pixel art + degradado), relieve y el marco por fuera (no tapa el contenido)
            round(g, x + 2, y + 5, w + 4, h + 2, alpha(t.shadow, a));
            if (t.fondo != null) {
                fondo(g, t, x - 1, y - 1, w + 2, h + 2, a);
                int top = alpha(t.bgTop, a * t.fondoVelo * 1.6F), bottom = alpha(t.bgBottom, a * Math.min(1, t.fondoVelo * 2.2F));
                g.fillGradient(x - 1, y - 1, x + w + 1, y + h + 1, top, bottom);
                relieve(g, x - 1, y - 1, w + 2, h + 2, a);
            } else {
                g.fillGradient(x - 1, y - 1, x + w + 1, y + h + 1, alpha(t.bgTop, a), alpha(t.bgBottom, a));
            }
            g.fill(x + 4, y + 3, x + w - 4, y + 4, alpha(t.accent, a * 0.85F));
            frame(g, t.frame, x - 5, y - 5, w + 10, h + 10, a);
            return;
        }
        round(g, x + 3, y + 4, w, h, alpha(t.shadow, a));
        round(g, x - 1, y - 1, w + 2, h + 2, alpha(t.border, a));
        if (t.fondo != null) {
            fondo(g, t, x + 1, y + 1, w - 2, h - 2, a);
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, alpha(t.bgTop, a * t.fondoVelo * 1.6F), alpha(t.bgBottom, a * Math.min(1, t.fondoVelo * 2.2F)));
        } else {
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, alpha(t.bgTop, a), alpha(t.bgBottom, a));
        }
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

    /**
     * Marco con textura de 32x32 estirado por partes: esquinas de 10x10 fijas y bordes que se repiten (no se deforman los dibujos).
     * El centro de la textura es transparente.
     */
    public static void frame(G g, net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h, float a) {
        final int s = 32, c = 10, m = s - 2 * c;
        int col = alpha(0xFFFFFFFF, a);
        // esquinas
        g.blitTint(tex, x, y, c, c, 0, 0, c, c, s, s, col);
        g.blitTint(tex, x + w - c, y, c, c, s - c, 0, c, c, s, s, col);
        g.blitTint(tex, x, y + h - c, c, c, 0, s - c, c, c, s, s, col);
        g.blitTint(tex, x + w - c, y + h - c, c, c, s - c, s - c, c, c, s, s, col);
        // bordes de arriba y abajo
        for (int i = c; i < w - c; i += m) {
            int seg = Math.min(m, w - c - i);
            g.blitTint(tex, x + i, y, seg, c, c, 0, seg, c, s, s, col);
            g.blitTint(tex, x + i, y + h - c, seg, c, c, s - c, seg, c, s, s, col);
        }
        // bordes de los lados
        for (int j = c; j < h - c; j += m) {
            int seg = Math.min(m, h - c - j);
            g.blitTint(tex, x, y + j, c, seg, 0, c, c, seg, s, s, col);
            g.blitTint(tex, x + w - c, y + j, c, seg, s - c, c, c, seg, s, s, col);
        }
    }

    /** Segundos desde que arrancó el juego (para las animaciones de la interfaz). */
    public static float tiempo() {
        return (System.nanoTime() / 1_000_000L % 10_000_000L) / 1000F;
    }

    /** El fondo de pixel art del tema, repetido y al doble (como el fondo de tierra de Minecraft), con su animación. */
    public static void fondo(G g, Theme t, int x, int y, int w, int h, float a) {
        if (t.fondo == null) {
            return;
        }
        final int tex = 128, esc = 2, lado = tex * esc;
        int ox = 0, oy = 0;
        float tm = tiempo();
        if (t.fondoAnim == 2) {
            // agua: el dibujo se mueve despacio
            ox = (int) (tm * 6) % lado;
            oy = (int) (tm * 2) % lado;
        }
        g.enableScissor(x, y, x + w, y + h);
        int col = alpha(0xFFFFFFFF, a);
        for (int ty = y - oy; ty < y + h; ty += lado) {
            for (int tx = x - ox; tx < x + w; tx += lado) {
                g.blitTint(t.fondo, tx, ty, lado, lado, 0, 0, tex, tex, tex, tex, col);
            }
        }
        if (t.fondoAnim == 1) {
            // estrellas que titilan (siempre en los mismos lugares)
            for (int i = 0; i < 24; i++) {
                int sx = x + (int) ((i * 97L + 13) % Math.max(1, w)), sy = y + (int) ((i * 61L + 7) % Math.max(1, h));
                float b = 0.5F + 0.5F * (float) Math.sin(tm * (1.3 + (i % 5) * 0.4) + i);
                if (b > 0.55F) {
                    int c = alpha(0xFFFFF6D8, a * (b - 0.5F) * 1.6F);
                    g.fill(sx, sy, sx + 1, sy + 1, c);
                    if (b > 0.9F && i % 3 == 0) {
                        g.fill(sx - 1, sy, sx + 2, sy + 1, alpha(0xFFFFF6D8, a * 0.35F));
                        g.fill(sx, sy - 1, sx + 1, sy + 2, alpha(0xFFFFF6D8, a * 0.35F));
                    }
                }
            }
        }
        g.disableScissor();
    }

    /** Relieve como los paneles de Minecraft: luz arriba e izquierda, sombra abajo y derecha. */
    public static void relieve(G g, int x, int y, int w, int h, float a) {
        g.fill(x, y, x + w, y + 1, alpha(0xFFFFFFFF, a * 0.22F));
        g.fill(x, y + 1, x + 1, y + h, alpha(0xFFFFFFFF, a * 0.14F));
        g.fill(x, y + h - 1, x + w, y + h, alpha(0xFF000000, a * 0.45F));
        g.fill(x + w - 1, y, x + w, y + h - 1, alpha(0xFF000000, a * 0.35F));
    }

    public static final net.minecraft.resources.ResourceLocation BOTON = com.axeliscore.plat.Ids.core("textures/gui/boton.png");
    public static final net.minecraft.resources.ResourceLocation RANURA = com.axeliscore.plat.Ids.core("textures/gui/ranura.png");

    /**
     * Dibuja una textura estirada en nueve partes: las esquinas de {@code c} píxeles quedan iguales, los bordes y el centro se repiten
     * (sin deformar el dibujo). {@code argb} tiñe (0xFFFFFFFF = como está).
     */
    public static void nueve(G g, net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h, int tw, int th, int c, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int cw = Math.min(c, w / 2), ch = Math.min(c, h / 2);
        int mw = tw - 2 * c, mh = th - 2 * c;
        // filas: arriba, en medio (repetido) y abajo
        int[][] filas = {{y, ch, 0}, {y + h - ch, ch, th - ch}};
        for (int[] f : filas) {
            tira(g, tex, x, f[0], w, f[1], f[2], f[1], tw, th, cw, mw, argb);
        }
        for (int yy = y + ch; yy < y + h - ch; yy += mh) {
            int alto = Math.min(mh, y + h - ch - yy);
            tira(g, tex, x, yy, w, alto, c, alto, tw, th, cw, mw, argb);
        }
    }

    private static void tira(G g, net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h, int v, int vh, int tw, int th, int cw,
            int mw, int argb) {
        g.blitTint(tex, x, y, cw, h, 0, v, cw, vh, tw, th, argb);
        g.blitTint(tex, x + w - cw, y, cw, h, tw - cw, v, cw, vh, tw, th, argb);
        for (int xx = x + cw; xx < x + w - cw; xx += mw) {
            int ancho = Math.min(mw, x + w - cw - xx);
            g.blitTint(tex, xx, y, ancho, h, cw, v, ancho, vh, tw, th, argb);
        }
    }

    /** Color para teñir la textura del botón (que es gris medio) y que quede del color pedido. */
    static int tinte(int argb) {
        float k = 255F / 160F;
        int r = Math.min(255, (int) (((argb >> 16) & 255) * k)), gg = Math.min(255, (int) (((argb >> 8) & 255) * k)),
                b = Math.min(255, (int) ((argb & 255) * k));
        return (argb & 0xFF000000) | r << 16 | gg << 8 | b;
    }

    /** Ranura de objeto como la del inventario (teñida un poco con el tema). */
    public static void ranura(G g, int x, int y, Theme t, boolean resaltada) {
        g.blitTint(RANURA, x, y, 18, 18, 0, 0, 18, 18, 18, 18, mix(0xFFFFFFFF, t.bgTop | 0xFF000000, 0.25F));
        if (resaltada) {
            g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
        }
    }

    /** Un hueco hundido (para listas, retratos, ranuras). */
    public static void inset(G g, int x, int y, int w, int h, Theme t) {
        // como las ranuras de Minecraft: hundido con sombra arriba e izquierda y luz abajo y derecha
        g.fill(x, y, x + w, y + h, alpha(0xFF000000, 0.38F));
        g.fill(x, y, x + w, y + 1, alpha(0xFF000000, 0.6F));
        g.fill(x, y + 1, x + 1, y + h, alpha(0xFF000000, 0.6F));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, alpha(0xFF000000, 0.2F));
        g.fill(x + 1, y + h - 1, x + w, y + h, alpha(mix(t.borderLight, 0xFFFFFFFF, 0.3F), 0.5F));
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, alpha(mix(t.borderLight, 0xFFFFFFFF, 0.3F), 0.5F));
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
        boton(g, x, y, w, h, hover, pressed, enabled, t);
        Font f = font();
        int dy = pressed ? 1 : 0;
        int col = enabled ? mix(t.buttonText, 0xFFFFFFA0, hover * 0.35F) : 0xFFA0A0A0;
        String s = fit(label, w - 6);
        g.drawString(f, s, x + (w - f.width(s)) / 2, y + (h - 8) / 2 + dy, col, true);
    }

    /**
     * El cuerpo de un botón: textura de Minecraft teñida del color del tema; al pasar el mouse se aclara, sale el filo blanco y pasa un
     * brillo; al apretarlo se hunde. Sirve también para pestañas y botones con ícono.
     */
    public static void boton(G g, int x, int y, int w, int h, float hover, boolean pressed, boolean enabled, Theme t) {
        int base = enabled ? mix(t.button, t.buttonHover, hover) : mix(t.button, 0xFF5A5A5A, 0.7F);
        if (pressed) {
            base = mix(base, 0xFF000000, 0.18F);
        }
        // sombra pegada abajo (crece un poco con el mouse)
        g.fill(x + 1, y + h, x + w - 1, y + h + 1, alpha(0xFF000000, 0.35F + 0.2F * hover));
        nueve(g, BOTON, x, y, w, h, 200, 20, 3, tinte(base | 0xFF000000));
        if (enabled && hover > 0.02F) {
            int c = alpha(0xFFFFFFFF, hover * 0.9F);
            // filo blanco por dentro del borde negro (como los botones de Minecraft al pasar el mouse)
            g.fill(x + 1, y + 1, x + w - 1, y + 2, c);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c);
            g.fill(x + 1, y + 2, x + 2, y + h - 2, c);
            g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, c);
            // brillo en diagonal que cruza el botón
            float tm = tiempo() * 1.2F % 2.2F;
            int bx = x + (int) (tm * (w + h)) - h;
            g.enableScissor(x + 2, y + 2, x + w - 2, y + h - 2);
            for (int i = 0; i < h; i++) {
                int px = bx + i;
                g.fill(px, y + h - 2 - i, px + 3, y + h - 1 - i, alpha(0xFFFFFFFF, 0.18F * hover));
            }
            g.disableScissor();
        }
    }

    /** Barra de progreso con brillo que la recorre; {@code frac} de 0 a 1. */
    public static void bar(G g, int x, int y, int w, int h, float frac, int from, int to, float time) {
        frac = Math.max(0, Math.min(1, frac));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF22252E);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF2E323C);
        int fw = (int) (w * frac);
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, mix(from, 0xFFFFFFFF, 0.15F), to);
            g.fill(x, y, x + fw, y + 1, alpha(0xFFFFFFFF, 0.35F));
            g.fill(x, y + h - 1, x + fw, y + h, alpha(0xFF000000, 0.3F));
            // marcas cada 10 % (como la barra de experiencia)
            if (w >= 40 && h >= 4) {
                for (int i = 1; i < 10; i++) {
                    int mx = x + w * i / 10;
                    if (mx < x + fw) {
                        g.fill(mx, y + 1, mx + 1, y + h - 1, alpha(0xFF000000, 0.18F));
                    }
                }
            }
            // punta que brilla
            g.fill(x + fw - 1, y, x + fw, y + h, alpha(0xFFFFFFFF, 0.5F));
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

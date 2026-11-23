package com.axeliscore.fx.client;

import com.axeliscore.fx.Fx;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Toasts;
import com.axeliscore.ui.Ui;
import net.minecraft.network.chat.Component;

/** Lo que se ve de los efectos del Core en el cliente: temblor (lo aplica la cámara), destello, viñeta, título grande y avisos. */
public final class FxClient {
    private static float shake;
    private static long shakeStart, shakeEnd;
    private static int flashColor, vigColor, titleColor;
    private static long flashStart, flashEnd, vigStart, vigEnd, titleStart, titleEnd;
    private static Component title = Component.empty(), sub = Component.empty();

    private FxClient() {
    }

    public static void fx(int kind, int color, float strength, int ticks) {
        long now = System.currentTimeMillis();
        long end = now + ticks * 50L;
        switch (kind) {
            case Fx.SHAKE -> {
                // si ya tiembla, se queda con lo más fuerte
                if (now > shakeEnd || strength >= currentShake()) {
                    shake = strength;
                    shakeStart = now;
                    shakeEnd = end;
                }
            }
            case Fx.FLASH -> {
                flashColor = color;
                flashStart = now;
                flashEnd = end;
            }
            case Fx.VIGNETTE -> {
                vigColor = color;
                vigStart = now;
                vigEnd = end;
            }
            default -> {
            }
        }
    }

    public static void title(Component t, Component s, int rgb, int ticks) {
        title = t;
        sub = s;
        titleColor = rgb | 0xFF000000;
        titleStart = System.currentTimeMillis();
        titleEnd = titleStart + ticks * 50L;
    }

    private static float currentShake() {
        long now = System.currentTimeMillis();
        if (now >= shakeEnd) {
            return 0;
        }
        float k = (shakeEnd - now) / (float) Math.max(1, shakeEnd - shakeStart);
        return shake * k * k;
    }

    /** Giro extra de la cámara {yaw, pitch, roll} en grados (lo pide el evento de la cámara cada cuadro). */
    public static float[] cameraShake() {
        float s = currentShake();
        if (s <= 0.001F) {
            return null;
        }
        double t = System.currentTimeMillis() / 1000.0;
        return new float[] {(float) (Math.sin(t * 47) * 1.6 + Math.sin(t * 91) * 0.7) * s, (float) (Math.sin(t * 53 + 1) * 1.4 + Math.sin(t * 83) * 0.6) * s,
                (float) Math.sin(t * 37 + 2) * 1.2F * s};
    }

    /** Dibuja destello, viñeta, título y avisos encima de todo. */
    public static void render(G g, int w, int h) {
        com.axeliscore.ui.client.LookInfo.render(g, w, h);
        long now = System.currentTimeMillis();
        if (now < vigEnd) {
            float k = 1 - (now - vigStart) / (float) Math.max(1, vigEnd - vigStart);
            int c = Ui.alpha(vigColor | 0xFF000000, k * ((vigColor >>> 24) / 255F == 0 ? 0.7F : (vigColor >>> 24) / 255F));
            int band = Math.max(20, h / 5);
            g.fillGradient(0, 0, w, band, c, c & 0xFFFFFF);
            g.fillGradient(0, h - band, w, h, c & 0xFFFFFF, c);
            for (int i = 0; i < band; i += 2) {
                int a = Ui.alpha(c, 1 - i / (float) band);
                g.fill(i, 0, i + 2, h, a);
                g.fill(w - i - 2, 0, w - i, h, a);
            }
        }
        if (now < flashEnd) {
            float k = 1 - (now - flashStart) / (float) Math.max(1, flashEnd - flashStart);
            float a0 = (flashColor >>> 24) == 0 ? 0.85F : (flashColor >>> 24) / 255F;
            g.fill(0, 0, w, h, Ui.alpha(flashColor | 0xFF000000, a0 * k * k));
        }
        if (now < titleEnd) {
            float age = (now - titleStart) / 1000F;
            float left = (titleEnd - now) / 1000F;
            float a = Math.min(1, Math.min(age / 0.25F, left / 0.6F));
            float pop = Ui.pop(Math.min(1, age / 0.35F));
            String ts = title.getString(), ss = sub.getString();
            // franja de cine detrás del texto: oscura al centro y transparente hacia los lados, con dos líneas del color del título
            int cy = (int) (h * 0.32F), top = cy - 24, bot = cy + (ss.isEmpty() ? 20 : 42), half = w / 2;
            float reach = half * (0.55F + 0.45F * pop);
            for (int x = 0; x < w; x += 4) {
                float d = Math.abs(x + 2 - half) / reach;
                if (d >= 1) {
                    continue;
                }
                float k = (1 - d * d) * a;
                int c = Ui.alpha(0xFF000000, k * 0.5F);
                int mid = (top + bot) / 2;
                g.fillGradient(x, top, x + 4, mid, c & 0x00FFFFFF, c);
                g.fillGradient(x, mid, x + 4, bot, c, c & 0x00FFFFFF);
                int line = Ui.alpha(titleColor, (1 - d) * a * 0.85F);
                g.fill(x, top + 2, x + 4, top + 3, line);
                g.fill(x, bot - 3, x + 4, bot - 2, line);
            }
            g.push();
            g.translate(w / 2F, h * 0.32F, 0);
            float sc = 2.6F * (0.7F + 0.3F * pop);
            g.scale(sc, sc, 1);
            int tw = Ui.font().width(ts);
            g.drawString(Ui.font(), ts, -tw / 2, -4, Ui.alpha(titleColor, a), true);
            g.pop();
            if (!ss.isEmpty()) {
                g.push();
                g.translate(w / 2F, h * 0.32F + 22, 0);
                g.scale(1.3F, 1.3F, 1);
                int sw = Ui.font().width(ss);
                g.drawString(Ui.font(), ss, -sw / 2, 0, Ui.alpha(0xFFFFFFFF, a * 0.9F), true);
                g.pop();
            }
        }
        Toasts.render(g, w, h);
    }
}

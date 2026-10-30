package com.kensei.client;

import com.kensei.KenseiConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.Random;

/** Efectos que se dibujan sobre toda la pantalla: líneas de velocidad que salen del centro y destellos de color. */
public final class ScreenFx {
    private static final int LINES = 34;
    private static final float[] ANG = new float[LINES];
    private static final float[] START = new float[LINES];
    private static final float[] LEN = new float[LINES];
    private static final float[] THICK = new float[LINES];

    private static long linesStart = -10000;
    private static long linesDur = 1;
    private static int linesRgb = 0xFFFFFF;

    private static long flashStart = -10000;
    private static long flashDur = 1;
    private static int flashRgb = 0xFFFFFF;
    private static int flashAlpha = 0;

    private static long markStart = -10000;
    private static int markRgb = 0xFFFFFF;
    private static float markScale = 1.0F;

    static {
        reseed();
    }

    private ScreenFx() {
    }

    private static void reseed() {
        Random r = new Random();
        for (int i = 0; i < LINES; i++) {
            ANG[i] = (float) (i / (double) LINES * Math.PI * 2 + (r.nextFloat() - 0.5F) * 0.12F);
            START[i] = 0.55F + r.nextFloat() * 0.3F;
            LEN[i] = 0.18F + r.nextFloat() * 0.3F;
            THICK[i] = 1.0F + r.nextFloat() * 1.6F;
        }
    }

    public static void speedLines(int rgb, long ms) {
        if (!KenseiConfig.SCREEN_FX.get()) {
            return;
        }
        reseed();
        linesStart = System.currentTimeMillis();
        linesDur = ms;
        linesRgb = rgb;
    }

    public static void flash(int rgb, int alpha, long ms) {
        if (!KenseiConfig.SCREEN_FX.get()) {
            return;
        }
        flashStart = System.currentTimeMillis();
        flashDur = ms;
        flashRgb = rgb;
        flashAlpha = alpha;
    }

    /** Cuatro marcas en diagonal alrededor de la mira al conectar un golpe. */
    public static void hitMarker(int rgb, float scale) {
        if (!KenseiConfig.SCREEN_FX.get()) {
            return;
        }
        markStart = System.currentTimeMillis();
        markRgb = rgb;
        markScale = scale;
    }

    public static void render(GuiGraphics g, int w, int h) {
        long now = System.currentTimeMillis();
        long ma = now - markStart;
        if (ma >= 0 && ma < 220) {
            float k = ma / 220.0F;
            int a = (int) (255 * (1.0F - k * k));
            float r0 = (5.0F + 7.0F * k) * markScale;
            float len = (7.0F * markScale) * (1.0F - 0.3F * k);
            for (int i = 0; i < 4; i++) {
                g.pose().pushPose();
                g.pose().translate(w / 2.0F, h / 2.0F, 0);
                g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45.0F + 90.0F * i));
                g.fill((int) r0, -1, (int) (r0 + len), 1, (a << 24) | (markRgb & 0xFFFFFF));
                g.pose().popPose();
            }
        }
        long fa = now - flashStart;
        if (fa >= 0 && fa < flashDur) {
            float k = 1.0F - fa / (float) flashDur;
            int a = (int) (flashAlpha * k * k);
            if (a > 0) {
                g.fill(0, 0, w, h, (a << 24) | (flashRgb & 0xFFFFFF));
            }
        }
        long la = now - linesStart;
        if (la < 0 || la >= linesDur) {
            return;
        }
        float t = la / (float) linesDur;
        float fade = t < 0.25F ? t / 0.25F : 1.0F - (t - 0.25F) / 0.75F;
        float cx = w / 2.0F;
        float cy = h / 2.0F;
        float maxR = (float) Math.sqrt(cx * cx + cy * cy);
        float push = 1.0F + t * 0.35F;
        for (int i = 0; i < LINES; i++) {
            float r0 = maxR * START[i] * push;
            float len = maxR * LEN[i] * (0.6F + 0.8F * Mth.sin(Math.min(1.0F, t * 2.0F) * 1.57F));
            int a = (int) (190 * fade);
            if (a <= 4) {
                continue;
            }
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(ANG[i]));
            // la línea es más fina en la punta de adentro: se dibuja en dos tramos
            int x0 = (int) r0;
            int x1 = (int) (r0 + len);
            int th = Math.max(1, Math.round(THICK[i]));
            int col = linesRgb & 0xFFFFFF;
            g.fill(x0, -th / 2, x1, th - th / 2, (a << 24) | col);
            g.fill(x0 + (x1 - x0) / 2, -th, x1, th, ((a / 3) << 24) | col);
            g.pose().popPose();
        }
    }
}

package com.mundosdeaxelis.client;

/** Estado del fundido de pantalla (color sólido que sube, se mantiene y baja). Solo cliente. */
public final class FadeOverlay {
    private static int rgb = 0xFFFFFF;
    private static int fadeIn = 0;
    private static int hold = 0;
    private static int fadeOut = 0;
    private static int elapsed = -1;

    private FadeOverlay() {
    }

    public static void start(int color, int in, int holdTicks, int out) {
        rgb = color & 0xFFFFFF;
        fadeIn = Math.max(0, in);
        hold = Math.max(0, holdTicks);
        fadeOut = Math.max(0, out);
        elapsed = 0;
    }

    public static void tick() {
        if (elapsed >= 0) {
            elapsed++;
            if (elapsed > fadeIn + hold + fadeOut) {
                elapsed = -1;
            }
        }
    }

    public static int rgb() {
        return rgb;
    }

    /** Opacidad de 0.0 a 1.0 en este momento. */
    public static float alpha(float partialTick) {
        if (elapsed < 0) {
            return 0.0F;
        }
        float t = elapsed + partialTick;
        if (t < fadeIn) {
            return fadeIn == 0 ? 1.0F : t / fadeIn;
        }
        if (t < fadeIn + hold) {
            return 1.0F;
        }
        if (fadeOut == 0) {
            return 0.0F;
        }
        float o = (t - fadeIn - hold) / fadeOut;
        return Math.max(0.0F, 1.0F - o);
    }
}

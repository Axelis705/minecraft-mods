package com.celular.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Imágenes y texturas que cambian entre versiones de Minecraft (formato de los píxeles, texturas dinámicas y capturas). */
public final class Tex {
    private Tex() {
    }

    /** Color ARGB de un píxel. */
    public static int argb(NativeImage n, int x, int y) {
        //#if MC >= 1.21.2
        //$$ return n.getPixel(x, y);
        //#else
        int c = n.getPixelRGBA(x, y); // viene como ABGR
        return (c & 0xFF00FF00) | (c & 0xFF) << 16 | (c >> 16 & 0xFF);
        //#endif
    }

    public static void set(NativeImage n, int x, int y, int argb) {
        //#if MC >= 1.21.2
        //$$ n.setPixel(x, y, argb);
        //#else
        n.setPixelRGBA(x, y, (argb & 0xFF00FF00) | (argb & 0xFF) << 16 | (argb >> 16 & 0xFF));
        //#endif
    }

    public static NativeImage toNative(BufferedImage bi) {
        int w = bi.getWidth(), h = bi.getHeight();
        NativeImage n = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                set(n, x, y, 0xFF000000 | bi.getRGB(x, y));
            }
        }
        return n;
    }

    /**
     * Recorta un cuadrado de la imagen (esquina x0, y0 y lado side) y lo achica a out × out promediando los píxeles (así no sale
     * dentado).
     */
    public static BufferedImage cropSquare(NativeImage n, int x0, int y0, int side, int out) {
        BufferedImage bi = new BufferedImage(out, out, BufferedImage.TYPE_INT_RGB);
        int w = n.getWidth(), h = n.getHeight();
        double step = side / (double) out;
        for (int oy = 0; oy < out; oy++) {
            int sy0 = y0 + (int) (oy * step), sy1 = Math.max(sy0 + 1, y0 + (int) ((oy + 1) * step));
            for (int ox = 0; ox < out; ox++) {
                int sx0 = x0 + (int) (ox * step), sx1 = Math.max(sx0 + 1, x0 + (int) ((ox + 1) * step));
                long r = 0, g = 0, b = 0;
                int k = 0;
                for (int y = sy0; y < sy1; y++) {
                    if (y < 0 || y >= h) {
                        continue;
                    }
                    for (int x = sx0; x < sx1; x++) {
                        if (x < 0 || x >= w) {
                            continue;
                        }
                        int c = argb(n, x, y);
                        r += c >> 16 & 255;
                        g += c >> 8 & 255;
                        b += c & 255;
                        k++;
                    }
                }
                if (k > 0) {
                    bi.setRGB(ox, oy, (int) (r / k) << 16 | (int) (g / k) << 8 | (int) (b / k));
                }
            }
        }
        return bi;
    }

    /** Sube la imagen como textura con ese nombre (si ya había una, la reemplaza). La imagen pasa a ser de la textura. */
    public static ResourceLocation upload(ResourceLocation rl, NativeImage img) {
        //#if MC >= 1.21.5
        //$$ DynamicTexture t = new DynamicTexture(rl::toString, img);
        //#else
        DynamicTexture t = new DynamicTexture(img);
        t.setFilter(true, false);
        //#endif
        Minecraft.getInstance().getTextureManager().register(rl, t);
        return rl;
    }

    public static void release(ResourceLocation rl) {
        Minecraft.getInstance().getTextureManager().release(rl);
    }

    /** Captura lo que se ve en pantalla. En las versiones nuevas llega un poco después; quien la recibe debe cerrarla. */
    public static void screenshot(Consumer<NativeImage> cb) {
        //#if MC >= 26.3
        //$$ Screenshot.takeScreenshot(Minecraft.getInstance().gameRenderer.mainRenderTarget(), cb);
        //#elif MC >= 1.21.5
        //$$ Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), cb);
        //#else
        cb.accept(Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget()));
        //#endif
    }
}

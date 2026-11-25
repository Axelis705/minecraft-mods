package com.coleccionista.client;

import com.coleccionista.Coleccionista;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * (Cliente) La piel de un jugador hecha piedra: se copia su textura (ya cargada en la tarjeta de video), cada pixel pasa a gris piedra según
 * qué tan claro es (así se reconocen la cara, el pelo y la ropa) y se guarda como una textura nueva. Si no se puede (todavía no carga),
 * se usa la piel normal y se vuelve a intentar después.
 */
public final class EstatuaPiel {
    private static final Map<ResourceLocation, ResourceLocation> HECHAS = new HashMap<>();
    private static final Map<ResourceLocation, Long> FALLO = new HashMap<>();
    private static int n;

    private EstatuaPiel() {
    }

    public static ResourceLocation de(ResourceLocation piel) {
        ResourceLocation r = HECHAS.get(piel);
        if (r != null) {
            return r;
        }
        Long f = FALLO.get(piel);
        if (f != null && System.currentTimeMillis() - f < 3000) {
            return piel;
        }
        try {
            r = hacer(piel);
        } catch (RuntimeException e) {
            r = null;
        }
        if (r == null) {
            FALLO.put(piel, System.currentTimeMillis());
            return piel;
        }
        HECHAS.put(piel, r);
        return r;
    }

    private static ResourceLocation hacer(ResourceLocation piel) {
        if (!RenderSystem.isOnRenderThread()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture t = mc.getTextureManager().getTexture(piel);
        if (t == null || t.getId() <= 0) {
            return null;
        }
        RenderSystem.bindTexture(t.getId());
        int w = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int h = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (w < 16 || h < 16 || w > 1024 || h > 1024) {
            return null;
        }
        NativeImage img = new NativeImage(w, h, false);
        img.downloadTexture(0, false);
        // de color a piedra: el brillo de cada pixel decide entre piedra oscura y piedra clara (con un poco más de contraste)
        int[] osc = {58, 56, 55}, cla = {206, 202, 194};
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = img.getPixelRGBA(x, y); // ABGR
                int a = c >>> 24 & 255, b = c >> 16 & 255, g = c >> 8 & 255, rr = c & 255;
                if (a == 0) {
                    continue;
                }
                float l = (0.3F * rr + 0.59F * g + 0.11F * b) / 255F;
                l = Math.max(0F, Math.min(1F, (l - 0.5F) * 1.25F + 0.52F));
                // un granito para que no se vea liso
                float grano = (((x * 73856093) ^ (y * 19349663)) & 15) / 15F * 0.06F - 0.03F;
                l = Math.max(0F, Math.min(1F, l + grano));
                int nr = (int) (osc[0] + (cla[0] - osc[0]) * l), ng = (int) (osc[1] + (cla[1] - osc[1]) * l),
                        nb = (int) (osc[2] + (cla[2] - osc[2]) * l);
                img.setPixelRGBA(x, y, a << 24 | nb << 16 | ng << 8 | nr);
            }
        }
        ResourceLocation r = new ResourceLocation(Coleccionista.MODID, "estatua/piel_" + (n++));
        mc.getTextureManager().register(r, new DynamicTexture(img));
        return r;
    }

    /** Al salir del mundo: se olvidan (las texturas se liberan solas con el administrador de texturas). */
    public static void limpiar() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation r : HECHAS.values()) {
            mc.getTextureManager().release(r);
        }
        HECHAS.clear();
        FALLO.clear();
    }
}

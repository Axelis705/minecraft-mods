package com.navidad.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.arbol.Adornos;
import com.navidad.arbol.PinoBE;
import com.navidad.arbol.PuntosArbol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja el Pino de Navidad: el árbol (modelo del Core), los adornos colgando de sus ramas (cada uno mirando hacia afuera y meciéndose
 * poquito; los nuevos aparecen con un saltito), las luces que titilan por grupos, las guirnaldas, y la estrella o el ángel de la punta
 * girando despacio con su resplandor. Lo mismo sirve para el objeto en el inventario (con los adornos que guarda).
 */
public class PinoRenderer implements BlockEntityRenderer<PinoBE> {
    public static final ResourceLocation GEO = new ResourceLocation(Navidad.MODID, "geo/pino_navidad.geo.json");
    public static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/entity/pino_navidad.png");
    private static final String[] GRUPOS = {"a", "b", "c"};
    private static final Map<String, ItemStack> PILAS = new HashMap<>();
    private static Geo geoVisto;
    private static Pose pose;
    private static GeoRenderer.Frame frame;
    private static int[][] luces, guirnaldas;

    public PinoRenderer(BlockEntityRendererProvider.Context c) {
    }

    @Override
    public void render(PinoBE be, float pt, PoseStack ps, MultiBufferSource buf, int luz, int overlay) {
        Level l = be.getLevel();
        float t = (l == null ? 0 : l.getGameTime()) + pt;
        dibujar(ps, buf, luz, overlay, be.lugares, be.luces, be.guirnaldas, be.punta, t, be.aparecio, l, (int) be.getBlockPos().asLong(), true);
    }

    @Override
    public boolean shouldRenderOffScreen(PinoBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static ItemStack pila(String id) {
        return PILAS.computeIfAbsent(id, k -> {
            Item it = Adornos.item(k);
            return it == null ? ItemStack.EMPTY : new ItemStack(it);
        });
    }

    private static boolean preparar() {
        Geo geo = GeoAssets.geo(GEO);
        if (geo == null || geo.bones.length == 0) {
            return false;
        }
        if (geo != geoVisto) {
            geoVisto = geo;
            pose = new Pose(geo.bones.length);
            frame = new GeoRenderer.Frame(geo);
            luces = new int[4][3];
            guirnaldas = new int[4][1];
            for (int s = 1; s <= 3; s++) {
                for (int g = 0; g < 3; g++) {
                    luces[s][g] = geo.bone("luces_" + s + "_" + GRUPOS[g]);
                }
                guirnaldas[s][0] = geo.bone("guirnalda_" + s);
            }
        }
        return true;
    }

    /**
     * Dibuja el árbol en el espacio de un bloque (0..1, con el piso del árbol en y = 0). {@code mundo}: si está puesto (con resplandor
     * de la punta) o es el objeto del inventario.
     */
    public static void dibujar(PoseStack ps, MultiBufferSource buf, int luz, int overlay, String[] lugares, int nLuces, int nGuirnaldas,
            String punta, float t, long[] aparecio, Level l, int semilla, boolean mundo) {
        if (!preparar()) {
            return;
        }
        Geo geo = geoVisto;
        pose.reset();
        for (int s = 1; s <= 3; s++) {
            for (int g = 0; g < 3; g++) {
                int b = luces[s][g];
                if (b >= 0) {
                    pose.hide(b, s > nLuces);
                }
            }
            if (guirnaldas[s][0] >= 0) {
                pose.hide(guirnaldas[s][0], s > nGuirnaldas);
            }
        }
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.scale(-1F, -1F, 1F);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.compute(geo, pose, ps, frame);
        Draw d = new Draw(buf);
        GeoRenderer.draw(geo, pose, frame, d.cutout(TEX), GeoRenderer.Pass.NORMAL, luz, overlay, 0xFFFFFFFF);
        if (nLuces > 0) {
            // los focos: primero su vidrio (con la luz del lugar) y luego el brillo, que sube y baja por grupos
            GeoRenderer.draw(geo, pose, frame, d.cutout(TEX), GeoRenderer.Pass.GLOW, luz, overlay, 0xFF8A8A8A);
            for (int s = 1; s <= nLuces; s++) {
                for (int g = 0; g < 3; g++) {
                    int b = luces[s][g];
                    if (b < 0) {
                        continue;
                    }
                    float on = 0.5F + 0.5F * Mth.sin(t * 0.12F + g * 2.1F + s * 0.9F);
                    on = on * on;
                    int v = (int) (70 + 185 * on);
                    pose.tinte(b, 0xFF000000 | v << 16 | v << 8 | v);
                }
            }
            GeoRenderer.draw(geo, pose, frame, d.glow(TEX), GeoRenderer.Pass.GLOW, LightTexture.FULL_BRIGHT, overlay, 0xFFFFFFFF);
        }
        ps.popPose();
        // los adornos
        var ir = Minecraft.getInstance().getItemRenderer();
        float[][] lg = PuntosArbol.LUGARES;
        for (int i = 0; i < Math.min(lugares.length, lg.length); i++) {
            String id = lugares[i];
            if (id == null || id.isEmpty()) {
                continue;
            }
            ItemStack st = pila(id);
            Adornos.Def def = Adornos.de(id);
            if (st.isEmpty() || def == null) {
                continue;
            }
            float[] q = lg[i];
            int piso = (int) q[3];
            float esc = new float[]{0.5F, 0.46F, 0.42F, 0.37F, 0.32F}[Math.min(4, piso)];
            esc *= salto(t, aparecio == null ? 0 : aparecio[i]);
            if (esc <= 0.001F) {
                continue;
            }
            float mira = (float) Math.toDegrees(Math.atan2(-q[0], -q[2]));
            float mece = Mth.sin(t * 0.045F + i * 1.37F) * 5F;
            ps.pushPose();
            ps.translate(0.5F + q[0], q[1], 0.5F + q[2]);
            ps.mulPose(Axis.YP.rotationDegrees(mira));
            ps.mulPose(Axis.XP.rotationDegrees(mece));
            ps.mulPose(Axis.ZP.rotationDegrees(mece * 0.6F));
            ps.scale(esc, esc, esc);
            ps.translate(0F, -(def.ancla() / 16F - 0.5F), 0F);
            ir.renderStatic(st, ItemDisplayContext.FIXED, luz, overlay, ps, buf, l, semilla + i);
            ps.popPose();
        }
        // la punta
        if (punta != null && !punta.isEmpty()) {
            ItemStack st = pila(punta);
            if (!st.isEmpty()) {
                float[] p = PuntosArbol.PUNTA;
                float esc = 0.55F * salto(t, aparecio == null ? 0 : aparecio[aparecio.length - 1]);
                ps.pushPose();
                ps.translate(0.5F + p[0], p[1] - 0.06F, 0.5F + p[2]);
                ps.mulPose(Axis.YP.rotationDegrees(t * 0.6F));
                ps.scale(esc, esc, esc);
                ps.translate(0F, 0.5F, 0F);
                ir.renderStatic(st, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, overlay, ps, buf, l, semilla + 99);
                ps.popPose();
                if (mundo) {
                    int color = punta.equals("punta_polar") ? 0xA8DCFF : 0xFFD870;
                    float tam = (punta.equals("punta_polar") ? 1.25F : 0.95F) * (1F + 0.08F * Mth.sin(t * 0.08F));
                    resplandor(ps, buf, 0.5F + p[0], p[1] + 0.26F, 0.5F + p[2], tam, color, 150);
                }
            }
        }
    }

    /** Un adorno recién colgado crece con un saltito (en 12 ticks). */
    private static float salto(float t, long desde) {
        if (desde <= 0) {
            return 1F;
        }
        float k = (t - desde) / 12F;
        if (k >= 1F || k < 0F) {
            return 1F;
        }
        // easeOutBack
        float c1 = 1.9F, c3 = c1 + 1F;
        float x = k - 1F;
        return 1F + c3 * x * x * x + c1 * x * x;
    }

    /** Un resplandor redondo que siempre mira a la cámara (se suma a lo de atrás). */
    static void resplandor(PoseStack ps, MultiBufferSource buf, float x, float y, float z, float tam, int rgb, int alfa) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Draw.camRot());
        ps.scale(tam, tam, tam);
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(Draw.ORBE));
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        r = r * alfa / 255;
        g = g * alfa / 255;
        b = b * alfa / 255;
        float h = 0.5F;
        vc.vertex(m, -h, -h, 0).color(r, g, b, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, h, -h, 0).color(r, g, b, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, h, h, 0).color(r, g, b, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, -h, h, 0).color(r, g, b, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(n, 0, 0, 1).endVertex();
        ps.popPose();
    }
}

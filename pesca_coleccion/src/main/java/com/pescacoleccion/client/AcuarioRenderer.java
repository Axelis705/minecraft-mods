package com.pescacoleccion.client;

import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.CoreBER;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.deco.AcuarioBE;
import com.pescacoleccion.pesca.Especie;
import com.pescacoleccion.pesca.PezItem;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * (Cliente) El agua del acuario (caras translúcidas) y los peces que nadan adentro: cada uno da vueltas a su ritmo, sube y baja, mira
 * hacia donde va y mueve la cola. Burbujas que suben de la arena.
 */
public class AcuarioRenderer extends CoreBER<AcuarioBE> {
    private static final ResourceLocation AGUA = Ids.rl(PescaColeccion.MODID, "textures/entity/acuario_agua.png");
    private static final ResourceLocation BURBUJA = Ids.rl(PescaColeccion.MODID, "textures/entity/burbuja.png");
    private static final ResourceLocation ALGA = Ids.rl(PescaColeccion.MODID, "textures/entity/acuario_alga.png");
    /** Adentro del vidrio: de 1 a 15 píxeles a los lados, de la arena (3) a la superficie (13.5). */
    private static final float A = 1 / 16F, B = 15 / 16F, Y0 = 3 / 16F, Y1 = 13.5F / 16F;

    public AcuarioRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    private static void v(VB vb, PoseStack.Pose p, float x, float y, float z, float u, float w, int light, float nx, float ny, float nz, int a) {
        vb.v(p, x, y, z, 255, 255, 255, a, u, w, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }

    @Override
    protected void draw(AcuarioBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        float t = (be.getLevel().getGameTime() % 24000L + partial) / 20F;
        long seed = be.getBlockPos().asLong();
        // peces
        for (int i = 0; i < be.peces.size(); i++) {
            ItemStack st = be.peces.get(i);
            if (st.getItem() instanceof PezItem pz && pz.especie() != null) {
                pez(ps, d, pz.especie(), t, i + (int) (seed & 7), light);
            }
        }
        // algas que se mecen (dos planos cruzados)
        VB alga = d.cutout(ALGA);
        for (int k = 0; k < 2; k++) {
            ps.pushPose();
            ps.translate(11 / 16F, Y0, 4.5F / 16F);
            ps.mulPose(Axis.YP.rotationDegrees(45 + k * 90));
            PoseStack.Pose pa = ps.last();
            float sw = (float) Math.sin(t * 1.3F + k) * 0.035F, hw = 0.13F, alto = 0.42F;
            v(alga, pa, -hw, 0, 0, 0, 1, light, 0, 0, 1, 255);
            v(alga, pa, hw, 0, 0, 1, 1, light, 0, 0, 1, 255);
            v(alga, pa, hw + sw, alto, sw * 0.5F, 1, 0, light, 0, 0, 1, 255);
            v(alga, pa, -hw + sw, alto, sw * 0.5F, 0, 0, light, 0, 0, 1, 255);
            ps.popPose();
        }
        // burbujas
        VB bu = d.translucent(BURBUJA);
        for (int i = 0; i < 4; i++) {
            float ph = (float) ((seed >> (i * 5)) & 31) / 31F;
            float k = (t * 0.35F + ph) % 1F;
            float x = 0.25F + 0.5F * ((i * 0.37F + ph) % 1F), z = 0.3F + 0.4F * ((i * 0.61F + ph * 0.5F) % 1F);
            float y = Y0 + 0.02F + k * (Y1 - Y0 - 0.06F);
            float r = 0.018F + 0.01F * (i % 2);
            burbuja(bu, ps, x + (float) Math.sin(t * 3 + i) * 0.01F, y, z, r, light, (int) (200 * (1 - k * 0.6F)));
        }
        // agua: los 4 lados y la superficie
        VB ag = d.translucent(AGUA);
        PoseStack.Pose p = ps.last();
        int al = 150;
        // norte (z = A) y sur (z = B)
        v(ag, p, A, Y0, A, 0, 1, light, 0, 0, -1, al);
        v(ag, p, A, Y1, A, 0, 0, light, 0, 0, -1, al);
        v(ag, p, B, Y1, A, 1, 0, light, 0, 0, -1, al);
        v(ag, p, B, Y0, A, 1, 1, light, 0, 0, -1, al);
        v(ag, p, B, Y0, B, 0, 1, light, 0, 0, 1, al);
        v(ag, p, B, Y1, B, 0, 0, light, 0, 0, 1, al);
        v(ag, p, A, Y1, B, 1, 0, light, 0, 0, 1, al);
        v(ag, p, A, Y0, B, 1, 1, light, 0, 0, 1, al);
        // oeste (x = A) y este (x = B)
        v(ag, p, A, Y0, B, 0, 1, light, -1, 0, 0, al);
        v(ag, p, A, Y1, B, 0, 0, light, -1, 0, 0, al);
        v(ag, p, A, Y1, A, 1, 0, light, -1, 0, 0, al);
        v(ag, p, A, Y0, A, 1, 1, light, -1, 0, 0, al);
        v(ag, p, B, Y0, A, 0, 1, light, 1, 0, 0, al);
        v(ag, p, B, Y1, A, 0, 0, light, 1, 0, 0, al);
        v(ag, p, B, Y1, B, 1, 0, light, 1, 0, 0, al);
        v(ag, p, B, Y0, B, 1, 1, light, 1, 0, 0, al);
        // superficie (con una ondita)
        float o = (float) Math.sin(t * 1.7) * 0.004F;
        v(ag, p, A, Y1 + o, A, 0, 0, light, 0, 1, 0, al);
        v(ag, p, A, Y1 - o, B, 0, 1, light, 0, 1, 0, al);
        v(ag, p, B, Y1 + o, B, 1, 1, light, 0, 1, 0, al);
        v(ag, p, B, Y1 - o, A, 1, 0, light, 0, 1, 0, al);
    }

    private static void burbuja(VB vb, PoseStack ps, float x, float y, float z, float r, int light, int a) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Draw.camRot());
        PoseStack.Pose p = ps.last();
        v(vb, p, -r, -r, 0, 0, 1, light, 0, 0, 1, a);
        v(vb, p, r, -r, 0, 1, 1, light, 0, 0, 1, a);
        v(vb, p, r, r, 0, 1, 0, light, 0, 0, 1, a);
        v(vb, p, -r, r, 0, 0, 0, light, 0, 0, 1, a);
        ps.popPose();
    }

    /** Un pez nadando: da vueltas por el tanque (cada uno a su ritmo), mira hacia donde va y mueve la cola. */
    private static void pez(PoseStack ps, Draw d, Especie e, float t, int i, int light) {
        float vel = 0.45F + 0.12F * (i % 3), fase = i * 2.1F;
        float a = t * vel + fase;
        float rx = 0.22F, rz = 0.18F + 0.03F * (i % 2);
        float x = 0.5F + (float) Math.cos(a) * rx, z = 0.5F + (float) Math.sin(a * (i % 2 == 0 ? 1F : -1F)) * rz;
        float y = 0.47F + 0.13F * (float) Math.sin(t * 0.6F + i * 1.3F) + (i % 3 - 1) * 0.08F;
        // dirección: la derivada del camino
        float dx = (float) -Math.sin(a) * rx, dz = (float) Math.cos(a * (i % 2 == 0 ? 1F : -1F)) * rz * (i % 2 == 0 ? 1F : -1F);
        // la cabeza del dibujo mira a +x: girar +x hacia (dx, dz)
        float yaw = (float) Math.toDegrees(Math.atan2(-dz, dx));
        // tamaño según la especie (los grandes, más grandes, sin salirse del tanque)
        float s = Math.min(0.2F, 0.11F + e.cmMax() / 2600F);
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ResourceLocation tex = TrofeoRenderer.arte(e);
        VB vb = d.cutout(tex);
        PoseStack.Pose p = ps.last();
        // cuerpo (del 40 % de la imagen hacia la cabeza) y cola (la parte de atrás, que se mueve)
        float cola = (float) Math.sin(t * 9 + i) * 0.35F;
        float xc = -s * 0.2F;
        float n = 0.4F;
        vb.v(p, s, -s, 0, 255, 255, 255, 255, 1, 1, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        vb.v(p, s, s, 0, 255, 255, 255, 255, 1, 0, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        vb.v(p, xc, s, 0, 255, 255, 255, 255, n, 0, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        vb.v(p, xc, -s, 0, 255, 255, 255, 255, n, 1, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        float tx = xc - (xc + s) * (float) Math.cos(cola), tz = (xc + s) * (float) Math.sin(cola);
        vb.v(p, xc, -s, 0, 255, 255, 255, 255, n, 1, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        vb.v(p, xc, s, 0, 255, 255, 255, 255, n, 0, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        vb.v(p, tx, s, tz, 255, 255, 255, 255, 0, 0, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        vb.v(p, tx, -s, tz, 255, 255, 255, 255, 0, 1, OverlayTexture.NO_OVERLAY, light, 0, 0, 1);
        ps.popPose();
    }
}

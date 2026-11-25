package com.coleccionista.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.client.SimpleGeoRenderer;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.CajaSorpresa;
import com.coleccionista.evento.Complejo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * (Cliente) La caja sorpresa con Don Resorte (se mueve con {@link CajaSorpresa#axPose}). Cuando escupe, los zapatos salen volando de su
 * boca (en arco, girando) hasta su lugar al frente del escenario, donde la prueba pone el bloque.
 */
public class CajaSorpresaRenderer extends SimpleGeoRenderer<CajaSorpresa> {
    private static BlockState zapatos;

    public CajaSorpresaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Coleccionista.MODID, "caja_sorpresa", 0.9F, 1F);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }

    @Override
    public void render(CajaSorpresa c, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(c, yaw, pt, ps, buf, light);
        if (c.estado() != CajaSorpresa.ESCUPE) {
            return;
        }
        float t = c.tickCount + pt - c.desde();
        if (t < 8 || t >= 22) {
            return;
        }
        if (zapatos == null) {
            zapatos = Complejo.estado("coleccionista:zapatos[facing=west]");
            if (zapatos == null) {
                return;
            }
        }
        float x = (t - 8) / 14F;
        Vec3 a = c.boca().subtract(c.position()), b = c.zapatos().subtract(c.position());
        double px = a.x + (b.x - a.x) * x, pz = a.z + (b.z - a.z) * x;
        double py = a.y + (b.y - a.y) * x + 5.0 * 4 * x * (1 - x);
        ps.pushPose();
        ps.translate(px, py, pz);
        ps.mulPose(Axis.XP.rotationDegrees(x * 540F));
        ps.mulPose(Axis.YP.rotationDegrees(x * 200F));
        ps.translate(-0.5, -0.25, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(zapatos, ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}

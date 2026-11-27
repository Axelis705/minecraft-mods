package com.navidad.client;

import com.axeliscore.client.SimpleGeoRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.entidad.Reno;
import com.navidad.entidad.TrineoSanta;
import com.navidad.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * El trineo de Santa en el cielo: el modelo del trineo con Santa (animado: saluda, se ríe, avienta regalos) y, adelante, cuatro renos
 * de verdad (el mismo modelo animado de la criatura) galopando en el aire en dos parejas. Todo se ve iluminado aunque sea de noche
 * (es magia), y el farol y la punta de la estela brillan.
 */
public class TrineoRenderer extends SimpleGeoRenderer<TrineoSanta> {
    private static final float ESCALA = 1.4F, ESCALA_RENOS = 1.2F;
    private final Reno[] renos = new Reno[4];
    private Level nivel;
    private int ultimoTick = -1;
    private boolean renosMalos;

    public TrineoRenderer(EntityRendererProvider.Context c) {
        super(c, Navidad.MODID, "trineo_santa", 0F, ESCALA);
    }

    @Override
    protected int getBlockLightLevel(TrineoSanta e, BlockPos p) {
        return 15;
    }

    @Override
    public void render(TrineoSanta e, float yaw, float pt, com.mojang.blaze3d.vertex.PoseStack ps, MultiBufferSource buf, int luz) {
        super.render(e, yaw, pt, ps, buf, LightTexture.FULL_BRIGHT);
        if (!renosMalos) {
            float rumbo = Mth.rotLerp(pt, e.yRotO, e.getYRot());
            renosVivos(e, rumbo, e.tickCount + pt, pt, ps, buf);
        }
    }

    /** Los renos animados, en pares, jalando el trineo. */
    private void renosVivos(TrineoSanta e, float rumbo, float t, float pt, PoseStack ps, MultiBufferSource buf) {
        int pilas = 0;
        try {
            if (renos[0] == null || nivel != e.level()) {
                nivel = e.level();
                for (int i = 0; i < renos.length; i++) {
                    renos[i] = ModRegistry.RENO.get().create(nivel);
                    if (renos[i] == null) {
                        renosMalos = true;
                        return;
                    }
                }
            }
            boolean nuevoTick = e.tickCount != ultimoTick;
            ultimoTick = e.tickCount;
            var disp = Minecraft.getInstance().getEntityRenderDispatcher();
            ps.pushPose();
            pilas++;
            ps.mulPose(Axis.YP.rotationDegrees(-rumbo + 180));
            for (int i = 0; i < renos.length; i++) {
                Reno r = renos[i];
                r.setPos(e.getX(), e.getY(), e.getZ());
                r.tickCount = e.tickCount + i * 7;
                if (nuevoTick) {
                    r.walkAnimation.update(1.1F, 0.4F);
                }
                r.setYRot(rumbo);
                r.yRotO = rumbo;
                r.setYBodyRot(rumbo);
                r.yBodyRotO = rumbo;
                r.setYHeadRot(rumbo);
                r.yHeadRotO = rumbo;
                EntityRenderer<? super Reno> rr = disp.getRenderer(r);
                ps.pushPose();
                pilas++;
                // en el espacio del trineo (ya girado): -z es hacia adelante
                float galope = Mth.sin(t * 0.45F + i * 1.1F) * 0.1F;
                ps.translate(i % 2 == 0 ? -0.62 : 0.62, 0.15 + galope + (i / 2) * 0.12, -2.35 - (i / 2) * 1.9);
                ps.mulPose(Axis.YP.rotationDegrees(rumbo - 180));
                ps.scale(ESCALA_RENOS, ESCALA_RENOS, ESCALA_RENOS);
                rr.render(r, rumbo, pt, ps, buf, LightTexture.FULL_BRIGHT);
                ps.popPose();
                pilas--;
            }
            ps.popPose();
            pilas--;
        } catch (RuntimeException ex) {
            renosMalos = true;
            Navidad.LOGGER.warn("NAVIDAD_TRINEO renos animados no: {}", ex.toString());
        } finally {
            while (pilas-- > 0) {
                ps.popPose();
            }
        }
    }
}

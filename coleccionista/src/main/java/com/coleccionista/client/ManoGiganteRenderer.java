package com.coleccionista.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.client.SimpleGeoRenderer;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.ManoGigante;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * (Cliente) Una mano gigante de porcelana del salón de té (se mueve con {@link ManoGigante#axPose}). Como la mesa tiene su luz encima,
 * la mano siempre se ve clara (porcelana), aunque el salón esté a oscuras.
 */
public class ManoGiganteRenderer extends SimpleGeoRenderer<ManoGigante> {
    public ManoGiganteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Coleccionista.MODID, "mano_gigante", 0.3F, ManoGigante.ESCALA);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }

    @Override
    public void render(ManoGigante m, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luzMundo) {
        if (m.estado() == ManoGigante.GUARDADA) {
            return;
        }
        int light = LightTexture.pack(Math.max(LightTexture.block(luzMundo), 13), LightTexture.sky(luzMundo));
        super.render(m, yaw, pt, ps, buf, light);
    }
}

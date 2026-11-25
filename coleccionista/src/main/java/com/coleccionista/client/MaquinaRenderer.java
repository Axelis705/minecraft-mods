package com.coleccionista.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.client.SimpleGeoRenderer;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Maquina;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** (Cliente) La máquina de latón de la final (se mueve con {@link Maquina#axPose}); bien iluminada, como la mesa. */
public class MaquinaRenderer extends SimpleGeoRenderer<Maquina> {
    public MaquinaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Coleccionista.MODID, "maquina", 0.4F, 1F);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }

    @Override
    public void render(Maquina m, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luzMundo) {
        int light = LightTexture.pack(Math.max(LightTexture.block(luzMundo), 12), LightTexture.sky(luzMundo));
        super.render(m, yaw, pt, ps, buf, light);
    }
}

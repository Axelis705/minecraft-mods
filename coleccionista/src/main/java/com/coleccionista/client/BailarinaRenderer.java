package com.coleccionista.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.client.SimpleGeoRenderer;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Bailarina;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** (Cliente) La bailarina de la caja de música (se mueve con {@link Bailarina#axPose}); siempre iluminada, como con un reflector. */
public class BailarinaRenderer extends SimpleGeoRenderer<Bailarina> {
    public BailarinaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Coleccionista.MODID, "bailarina", 1.4F, Bailarina.ESCALA);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }

    @Override
    public void render(Bailarina b, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luzMundo) {
        int light = LightTexture.pack(Math.max(LightTexture.block(luzMundo), 13), LightTexture.sky(luzMundo));
        super.render(b, yaw, pt, ps, buf, light);
    }
}

package com.coleccionista.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.client.SimpleGeoRenderer;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Escopeta;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** (Cliente) La escopeta de la caja de música: el mástil con el arma de dos cañones (se mueve con {@link Escopeta#axPose}). */
public class EscopetaRenderer extends SimpleGeoRenderer<Escopeta> {
    public EscopetaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Coleccionista.MODID, "escopeta", 0F, 1F);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }
}

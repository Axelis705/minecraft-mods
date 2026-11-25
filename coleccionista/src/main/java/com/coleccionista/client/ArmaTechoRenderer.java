package com.coleccionista.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.client.SimpleGeoRenderer;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.ArmaTecho;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** (Cliente) El arma de la caja de música: el modelo del mástil con el revólver (se mueve con {@link ArmaTecho#axPose}). */
public class ArmaTechoRenderer extends SimpleGeoRenderer<ArmaTecho> {
    public ArmaTechoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Coleccionista.MODID, "arma_techo", 0F, 1F);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }
}

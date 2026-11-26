package com.navidad.client;

import com.axeliscore.client.PersonaRenderer;
import com.navidad.Navidad;
import com.navidad.entidad.HombreJengibre;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** El hombre de jengibre: la Persona del Core con piel de galleta, a la mitad de tamaño. */
public class JengibreRenderer extends PersonaRenderer<HombreJengibre> {
    private static final ResourceLocation PIEL = new ResourceLocation(Navidad.MODID, "textures/entity/hombre_jengibre.png");

    public JengibreRenderer(EntityRendererProvider.Context c) {
        super(c, e -> PIEL);
        this.shadowRadius = 0.25F;
    }

    @Override
    protected float modelScale(HombreJengibre e) {
        return 0.55F;
    }
}

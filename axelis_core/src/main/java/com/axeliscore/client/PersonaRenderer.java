package com.axeliscore.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.client.GeoEntityRenderer;
import com.axeliscore.character.Persona;
import com.axeliscore.plat.Ids;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Persona con su piel de muestra según la variante. Capas: base (caminar/quieto), cuerpo entero (gestos grandes) y de la cintura arriba. */
public class PersonaRenderer extends GeoEntityRenderer<Persona> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[Persona.SKINS.length];

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = Ids.core("textures/entity/persona/" + Persona.SKINS[i] + ".png");
        }
    }

    public PersonaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Ids.core("geo/persona.geo.json"), 0.5F, Ids.core("animations/persona.animation.json"));
    }

    @Override
    protected ResourceLocation texture(Persona e) {
        return SKINS[Math.floorMod(e.variant(), SKINS.length)];
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
        h.animator().layer("full");
        h.animator().layer("upper").mask(h.geo(), "chest");
    }
}

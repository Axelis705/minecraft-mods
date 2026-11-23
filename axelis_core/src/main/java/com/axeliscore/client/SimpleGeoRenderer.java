package com.axeliscore.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.client.GeoEntityRenderer;
import com.axeliscore.plat.Ids;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Renderizador listo para una criatura del Core: modelo, animaciones y textura con el mismo nombre (geo/N.geo.json,
 * animations/N.animation.json, textures/entity/N.png) y capas "base" (moverse) y "action" (gestos de todo el cuerpo).
 */
public class SimpleGeoRenderer<T extends Entity & Animated> extends GeoEntityRenderer<T> {
    private final ResourceLocation texture;
    private final float scale;

    public SimpleGeoRenderer(EntityRendererProvider.Context ctx, String ns, String name, float shadow, float scale) {
        super(ctx, Ids.rl(ns, "geo/" + name + ".geo.json"), shadow, Ids.rl(ns, "animations/" + name + ".animation.json"));
        this.texture = Ids.rl(ns, "textures/entity/" + name + ".png");
        this.scale = scale;
    }

    @Override
    protected ResourceLocation texture(T e) {
        return texture;
    }

    @Override
    protected float modelScale(T e) {
        return scale;
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
        h.animator().layer("action");
    }
}

package com.axeliscore.combat.client;

import com.axeliscore.client.SimpleGeoRenderer;
import com.axeliscore.combat.Jefe;
import com.axeliscore.plat.Ids;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderizador listo para un {@link Jefe}: como {@link SimpleGeoRenderer}, pero cambia de textura según el aspecto de la fase
 * ({@code textures/entity/N.png}, {@code N_f2.png}, {@code N_f3.png}...).
 */
public class JefeRenderer<T extends Jefe> extends SimpleGeoRenderer<T> {
    private final ResourceLocation[] aspectos;

    /** @param aspectos cuántas texturas tiene (1 = solo la normal; 3 = normal, _f2 y _f3). */
    public JefeRenderer(EntityRendererProvider.Context ctx, String ns, String name, float shadow, float scale, int aspectos) {
        super(ctx, ns, name, shadow, scale);
        this.aspectos = new ResourceLocation[Math.max(1, aspectos)];
        for (int i = 0; i < this.aspectos.length; i++) {
            this.aspectos[i] = Ids.rl(ns, "textures/entity/" + name + (i == 0 ? "" : "_f" + (i + 1)) + ".png");
        }
    }

    @Override
    protected ResourceLocation texture(T e) {
        return aspectos[Math.max(0, Math.min(aspectos.length - 1, e.aspecto()))];
    }
}

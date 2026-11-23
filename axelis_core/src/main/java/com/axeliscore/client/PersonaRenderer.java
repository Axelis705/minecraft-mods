package com.axeliscore.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.client.GeoEntityRenderer;
import com.axeliscore.character.Persona;
import com.axeliscore.plat.Ids;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Dibuja una {@link Persona} (o cualquier personaje que herede de ella) con el modelo "Minecraft mejorado" y sus animaciones. Capas:
 * base (caminar/quieto), cuerpo entero (gestos grandes) y de la cintura arriba.
 * <p>
 * Para un personaje de otro mod con su propia piel (una piel de 64x64 normal de Minecraft sirve):
 * <pre>
 *     event.registerEntityRenderer(PESCADOR.get(), PersonaRenderer.with(Ids.rl("pesca", "textures/entity/nilo.png")));
 * </pre>
 */
public class PersonaRenderer<T extends Persona> extends GeoEntityRenderer<T> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[Persona.SKINS.length];

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = Ids.core("textures/entity/persona/" + Persona.SKINS[i] + ".png");
        }
    }

    private final Function<T, ResourceLocation> skin;

    /** Con las pieles de muestra del Core (según la variante). */
    public PersonaRenderer(EntityRendererProvider.Context ctx) {
        this(ctx, e -> SKINS[Math.floorMod(e.variant(), SKINS.length)]);
    }

    /** Con la piel que diga {@code skin} para cada personaje (por ejemplo, según su variante o su oficio). */
    public PersonaRenderer(EntityRendererProvider.Context ctx, Function<T, ResourceLocation> skin) {
        super(ctx, Ids.core("geo/persona.geo.json"), 0.5F, Ids.core("animations/persona.animation.json"));
        this.skin = skin;
    }

    /** Fábrica para registrar un personaje con una sola piel. */
    public static <T extends Persona> EntityRendererProvider<T> with(ResourceLocation skin) {
        return ctx -> new PersonaRenderer<>(ctx, e -> skin);
    }

    /** Fábrica para registrar un personaje con varias pieles (se elige por la variante). */
    public static <T extends Persona> EntityRendererProvider<T> withSkins(ResourceLocation... skins) {
        return ctx -> new PersonaRenderer<>(ctx, e -> skins[Math.floorMod(e.variant(), skins.length)]);
    }

    @Override
    protected ResourceLocation texture(T e) {
        return skin.apply(e);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
        h.animator().layer("full");
        h.animator().layer("upper").mask(h.geo(), "chest");
    }
}

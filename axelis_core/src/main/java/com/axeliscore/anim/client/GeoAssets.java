package com.axeliscore.anim.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.ClipLoader;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.GeoLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Carga (y guarda) los modelos y animaciones de los paquetes de recursos: {@code assets/<mod>/geo/<nombre>.geo.json} y
 * {@code assets/<mod>/animations/<nombre>.animation.json}. Si un archivo falta o está mal, lo dice una vez en el registro y usa un cubo
 * de repuesto (el juego no se cae).
 */
public final class GeoAssets {
    private static final Map<ResourceLocation, Geo> GEOS = new HashMap<>();
    private static final Map<ResourceLocation, Clip.Library> CLIPS = new HashMap<>();

    private GeoAssets() {
    }

    /** Olvida todo (se vuelve a leer al dibujar; lo llama la recarga de recursos con F3+T). */
    public static void clear() {
        GEOS.clear();
        CLIPS.clear();
    }

    public static Geo geo(ResourceLocation id) {
        return GEOS.computeIfAbsent(id, k -> {
            String txt = read(k);
            if (txt != null) {
                try {
                    return GeoLoader.parse(txt);
                } catch (RuntimeException e) {
                    AxelisCore.LOGGER.error("Axelis Core: modelo {} mal escrito: {}", k, e.getMessage());
                }
            }
            return fallback(k.toString());
        });
    }

    /** Une varias bibliotecas de animaciones (la primera manda si se repite un nombre). */
    public static Clip.Library clips(ResourceLocation... ids) {
        if (ids.length == 1) {
            return clips1(ids[0]);
        }
        Map<String, Clip> all = new LinkedHashMap<>();
        for (ResourceLocation id : ids) {
            for (Clip c : new ArrayList<>(clips1(id).all().values())) {
                all.putIfAbsent(c.name, c);
            }
        }
        return ClipLoader.library(all);
    }

    private static Clip.Library clips1(ResourceLocation id) {
        return CLIPS.computeIfAbsent(id, k -> {
            String txt = read(k);
            if (txt != null) {
                try {
                    return ClipLoader.library(ClipLoader.parse(txt));
                } catch (RuntimeException e) {
                    AxelisCore.LOGGER.error("Axelis Core: animaciones {} mal escritas: {}", k, e.getMessage());
                }
            }
            return ClipLoader.library(new LinkedHashMap<>());
        });
    }

    private static String read(ResourceLocation id) {
        try {
            Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(id);
            if (r.isEmpty()) {
                AxelisCore.LOGGER.error("Axelis Core: no existe {}", id);
                return null;
            }
            try (BufferedReader br = r.get().openAsReader()) {
                return br.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            AxelisCore.LOGGER.error("Axelis Core: no se pudo leer {}: {}", id, e.toString());
            return null;
        }
    }

    /** Un cubo de 8x8x8 para que se vea que falta el modelo. */
    private static Geo fallback(String id) {
        List<Geo.Cube> cubes = new ArrayList<>();
        cubes.add(new Geo.Cube(-4, 16, -4, 8, 8, 8, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, null));
        return new Geo("falta:" + id, 64, 64, new Geo.Bone[] {new Geo.Bone("root", -1, 0, 24, 0, 0, 0, 0, cubes, false)});
    }
}

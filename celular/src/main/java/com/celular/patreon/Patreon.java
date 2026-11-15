package com.celular.patreon;

import com.celular.Celular;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Los que apoyan el mod en Patreon. Se guardan en config/celular-patreon.txt (un nombre por línea; las líneas que empiezan con # se
 * ignoran). Se puede editar el archivo a mano o usar /celular patreon agregar|quitar.
 */
public final class Patreon {
    private static final String[] DEFAULT = {"Odette"};
    private static List<String> cache;
    private static long cacheTime = -1;

    private Patreon() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("celular-patreon.txt");
    }

    public static synchronized List<String> list() {
        Path f = file();
        try {
            if (!Files.exists(f)) {
                write(List.of(DEFAULT));
            }
            long t = Files.getLastModifiedTime(f).toMillis();
            if (cache == null || t != cacheTime) {
                List<String> out = new ArrayList<>();
                for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    String n = line.trim();
                    if (!n.isEmpty() && !n.startsWith("#") && n.length() <= 40 && !out.contains(n)) {
                        out.add(n);
                    }
                }
                cache = out;
                cacheTime = t;
            }
        } catch (IOException ex) {
            Celular.LOGGER.warn("No se pudo leer {}", f, ex);
            if (cache == null) {
                cache = new ArrayList<>(List.of(DEFAULT));
            }
        }
        return new ArrayList<>(cache);
    }

    private static void write(List<String> names) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Los que apoyan el mod en Patreon: un nombre por linea. Salen en la app Patreon del celular.");
        lines.add("# Tambien se puede usar /celular patreon agregar <nombre> y /celular patreon quitar <nombre>");
        lines.addAll(names);
        Files.createDirectories(file().getParent());
        Files.write(file(), lines, StandardCharsets.UTF_8);
        cache = null;
    }

    public static synchronized boolean add(String name) {
        List<String> l = list();
        for (String s : l) {
            if (s.equalsIgnoreCase(name)) {
                return false;
            }
        }
        l.add(name);
        try {
            write(l);
            return true;
        } catch (IOException ex) {
            Celular.LOGGER.warn("No se pudo guardar Patreon", ex);
            return false;
        }
    }

    public static synchronized boolean remove(String name) {
        List<String> l = list();
        boolean gone = l.removeIf(s -> s.equalsIgnoreCase(name));
        if (gone) {
            try {
                write(l);
            } catch (IOException ex) {
                Celular.LOGGER.warn("No se pudo guardar Patreon", ex);
                return false;
            }
        }
        return gone;
    }
}

package com.pescacoleccion.pesca;

import com.axeliscore.plat.Plat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pelea.Niveles;
import com.pescacoleccion.pelea.PeleaSim;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Los 25 peces: dónde y cuándo pican, y cómo pelea cada uno. */
public final class Especies {
    private static final List<Especie> ALL;
    private static final Map<String, Especie> BY_ID = new LinkedHashMap<>();

    static {
        List<Especie> list = new ArrayList<>();
        try (var in = Especies.class.getResourceAsStream("/" + PescaColeccion.MODID + "/peces.json")) {
            List<Map<String, Object>> raw = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<List<Map<String, Object>>>() { }.getType());
            for (Map<String, Object> m : raw) {
                @SuppressWarnings("unchecked") List<Number> cm = (List<Number>) m.get("cm");
                @SuppressWarnings("unchecked") List<String> lugar = (List<String>) m.get("lugar");
                @SuppressWarnings("unchecked") List<String> hora = (List<String>) m.get("hora");
                @SuppressWarnings("unchecked") Map<String, Object> pelea = (Map<String, Object>) m.getOrDefault("pelea", Map.of());
                list.add(new Especie((String) m.get("id"), ((Number) m.get("rareza")).intValue(), lugar, hora, (String) m.get("clima"),
                        (String) m.get("luna"), cm.get(0).floatValue(), cm.get(1).floatValue(), ((Number) m.get("kg")).floatValue(), pelea));
            }
        } catch (Exception e) {
            throw new IllegalStateException("no pude leer los peces de " + PescaColeccion.MODID, e);
        }
        ALL = Collections.unmodifiableList(list);
        for (Especie e : ALL) {
            BY_ID.put(e.id(), e);
        }
    }

    private Especies() {
    }

    public static List<Especie> all() {
        return ALL;
    }

    public static Especie get(String id) {
        return BY_ID.get(id);
    }

    // ------------------------------------------------------------------ dónde y cuándo

    private static final Set<ResourceKey<Biome>> FRIOS = Set.of(Biomes.FROZEN_RIVER, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN,
            Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES, Biomes.SNOWY_BEACH, Biomes.GROVE, Biomes.SNOWY_SLOPES,
            Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS);
    private static final Set<ResourceKey<Biome>> CALIDOS = Set.of(Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);
    private static final Set<ResourceKey<Biome>> OCEANO_FRIO = Set.of(Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN,
            Biomes.DEEP_FROZEN_OCEAN);

    private static boolean in(Holder<Biome> b, Set<ResourceKey<Biome>> keys) {
        for (ResourceKey<Biome> k : keys) {
            if (b.is(k)) {
                return true;
            }
        }
        return false;
    }

    public static boolean lugar(Level level, BlockPos pos, String code) {
        Holder<Biome> b = level.getBiome(pos);
        boolean cielo = level.canSeeSky(pos.above());
        return switch (code) {
            case "cualquiera" -> true;
            case "rio" -> b.is(BiomeTags.IS_RIVER);
            case "pantano" -> b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP);
            case "lago" -> cielo && !b.is(BiomeTags.IS_RIVER) && !b.is(BiomeTags.IS_OCEAN) && !b.is(Biomes.SWAMP) && !b.is(Biomes.MANGROVE_SWAMP);
            case "oceano" -> b.is(BiomeTags.IS_OCEAN);
            case "oceano_calido" -> in(b, CALIDOS);
            case "oceano_frio" -> in(b, OCEANO_FRIO);
            case "oceano_profundo" -> b.is(BiomeTags.IS_DEEP_OCEAN);
            case "playa" -> b.is(BiomeTags.IS_BEACH) || b.is(BiomeTags.IS_OCEAN) && pos.getY() >= 60;
            case "cueva" -> !cielo && pos.getY() < 50;
            case "selva" -> b.is(BiomeTags.IS_JUNGLE);
            case "montana" -> pos.getY() > 110 || b.is(BiomeTags.IS_MOUNTAIN);
            case "frio" -> in(b, FRIOS);
            case "cerezo" -> b.is(Biomes.CHERRY_GROVE);
            default -> false;
        };
    }

    public static boolean hora(Level level, String code) {
        long t = Math.floorMod(Plat.dayTime(level), 24000L);
        return switch (code) {
            case "dia" -> t >= 1000 && t < 12000;
            case "atardecer" -> t >= 11500 && t < 13800;
            case "noche" -> t >= 13800 && t < 22200;
            case "amanecer" -> t >= 22200 || t < 1200;
            default -> true;
        };
    }

    public static boolean clima(Level level, String code) {
        return switch (code) {
            case "lluvia" -> level.isRaining();
            case "tormenta" -> level.isThundering();
            case "despejado" -> !level.isRaining();
            default -> true;
        };
    }

    /** Fase de la luna (0 llena ... 4 nueva). */
    public static int fase(Level level) {
        return (int) Math.floorMod(Plat.dayTime(level) / 24000L, 8L);
    }

    public static boolean luna(Level level, String code) {
        return switch (code) {
            case "llena" -> fase(level) == 0;
            case "nueva" -> fase(level) == 4;
            default -> true;
        };
    }

    /** ¿Puede picar este pez aquí y ahora? */
    public static boolean pica(Especie e, Level level, BlockPos pos) {
        return pica(e, level, pos, false);
    }

    /** {@code luna}: con el cebo de luna, los legendarios no piden luna ni clima. */
    public static boolean pica(Especie e, Level level, BlockPos pos, boolean luna) {
        boolean lugar = false;
        for (String l : e.lugar()) {
            lugar |= lugar(level, pos, l);
        }
        boolean hora = false;
        for (String h : e.hora()) {
            hora |= hora(level, h);
        }
        boolean libre = luna && e.rareza() >= 4;
        return lugar && hora && (libre || clima(level, e.clima()) && luna(level, e.luna()));
    }

    /** El pez que se acerca: se sortea la rareza (con la suerte) y se elige uno que viva aquí a esta hora; si no hay, uno más común. */
    public static Especie elegir(Level level, BlockPos pos, RandomSource r, int suerte, boolean luna) {
        int rz = Captura.rareza(r, suerte);
        for (int k = rz; k >= 0; k--) {
            List<Especie> c = new ArrayList<>();
            for (Especie e : ALL) {
                if (e.rareza() == k && pica(e, level, pos, luna)) {
                    c.add(e);
                }
            }
            if (!c.isEmpty()) {
                return c.get(r.nextInt(c.size()));
            }
        }
        return BY_ID.get("charalito");
    }

    /** Cómo pelea esta especie con esta caña (la base de su rareza más su carácter). */
    public static PeleaSim.Params params(Especie e, long seed, Cana cana, float tamanoRel) {
        PeleaSim.Params p = Niveles.base(e.rareza(), seed);
        p.bpm = Math.max(50, p.bpm + Math.round(e.num("bpm", 0)));
        p.pull = Math.min(1.1F, p.pull * e.num("tiron", 1F));
        p.switchMs = Math.max(500, Math.round(p.switchMs * e.num("cambio", 1F) * (e.rasgo("zigzag") ? 0.55F : 1F)));
        p.jumps = Math.max(0, p.jumps + Math.round(e.num("saltos", 0)));
        p.trapChance = Math.max(0, p.trapChance + e.num("trampas", 0));
        // los grandes tienen más energía (hasta un 35 % más)
        p.stamina = p.stamina * e.num("energia", 1F) * (0.85F + 0.5F * tamanoRel);
        if (e.rasgo("fantasma")) {
            p.ghostFromMs = 0;
        }
        if (e.rasgo("rafagas")) {
            p.burstFromMs = 0;
        }
        p.inflate = e.rasgo("inflar");
        p.rests = e.rasgo("descansos");
        Niveles.cana(p, cana.zone, cana.control, cana.lineHp, cana.damage);
        return p;
    }
}

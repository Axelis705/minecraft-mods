package com.mundosdeaxelis.crate;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.gear.GearDef;
import com.mundosdeaxelis.gear.GearSet;
import com.mundosdeaxelis.table.Mats;
import com.mundosdeaxelis.gear.GearDefs;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Qué puede salir en cada crate. Todo premio (de Minecraft o del mod: herramientas, armaduras, mascotas, reliquias, materiales,
 * decoración, llaves) tiene un NIVEL DE CALIDAD del 1 al 9, y cada caja solo da los niveles de su "ventana" (ver
 * {@link CrateKind#levels()}): Naturaleza 1 · Forja 1-2 · Hielo 2-3 · Fuego 3-5 · Magia 5-6 · Dragón 6-7 · Cielo 7-8 · Abismo 8-9, y cada nivel tiene su rareza (ver LEVEL_TIER): así Naturaleza da Común, Forja Común y Poco común, Hielo Poco común y Rara, Fuego Rara y Épica, Magia Épica y Legendaria, Dragón Legendaria y Mítica, Cielo Mítica y Estelar y Abismo Estelar y Celestial (la más alta).
 * Así cada caja es mejor que la anterior en TODO el botín, no solo en el equipo. La lista se arma una sola vez (el cliente y el
 * servidor ven lo mismo, requisito para que la ruleta coincida con el premio real). La rareza (color) de un premio de Minecraft
 * sale de su nivel; la de un objeto del mod es la suya propia.
 * <p>
 * PROBABILIDADES (etapa 137): cada objeto pesa según su IMPORTANCIA y su rareza. La importancia es lo útil o exclusivo que es: los
 * materiales sencillos de Minecraft son lo más común; las armas, herramientas y armaduras del set salen bastante menos; la
 * decoración menos; las capas (exclusivas) menos aún, y las mascotas son lo más difícil (ver {@code BASE_*}). Encima de eso, la rareza y el
 * nivel bajan el peso (ver {@link #TIER_WEIGHT}). {@link #verify(CrateKind)} comprueba que dentro de cada grupo lo mejor salga menos.
 * Cada objeto de Minecraft, mascota o reliquia sale en una sola caja
 * (ver {@code owners()}), para que los sets no se pierdan entre cientos de premios.
 */
public final class CratePools {
    private CratePools() {
    }

    /** Un premio posible: qué objeto, cuántos y su peso absoluto (millonésimas de la probabilidad de que toque en la caja). */
    public record Prize(Item item, int min, int max, int weight, int pity, String group) {
    }

    private static final Map<CrateKind, Map<Tier, List<Prize>>> CACHE = new EnumMap<>(CrateKind.class);

    // ------------------------------------------------------------------ niveles

    /** Rareza (color) de un premio de Minecraft según su nivel. */
    private static final Tier[] LEVEL_TIER = {Tier.COMMON, Tier.COMMON, Tier.UNCOMMON, Tier.RARE, Tier.RARE, Tier.EPIC, Tier.LEGENDARY, Tier.MYTHIC, Tier.STELLAR, Tier.CELESTIAL};

    /** Nivel de un objeto del mod con rareza propia: cada rareza tiene su nivel (la Rara reparte entre 3 y 4). */
    public static int levelOf(Tier t, String id) {
        int h = Math.abs(id.hashCode()) % 2;
        return switch (t) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3 + h;
            case EPIC -> 5;
            case LEGENDARY -> 6;
            case MYTHIC -> 7;
            case STELLAR -> 8;
            case CELESTIAL -> 9;
        };
    }

    /** Nivel de un material de mob según la Esencia que da. */
    private static int matLevel(Mats m) {
        return m.essence <= 3 ? 1 : m.essence <= 5 ? 2 : m.essence <= 6 ? 3 : m.essence <= 10 ? 4 : 5;
    }

    private static Tier matTier(Mats m) {
        return m.essence <= 3 ? Tier.COMMON : m.essence <= 5 ? Tier.UNCOMMON : m.essence <= 6 ? Tier.RARE : Tier.EPIC;
    }

    // ------------------------------------------------------------------ contenido de cada caja

    /** Familias de premios (solo sirven para agrupar y para el comando de pruebas; el peso sale de la rareza y el nivel). */
    private enum Cat {
        GEAR, VANILLA, PETS, RELICS, MATS, KEYS, ARMOR, DECOR, CAPES
    }

    // ------------------------------------------------------------------ probabilidades

    /**
     * Peso base de UN objeto según su rareza. Regla de oro: lo mejor siempre sale menos. Cada rareza pesa menos que la anterior
     * y, dentro de una misma rareza, lo de nivel más alto pesa un poco menos. No importa de qué familia sea el objeto
     * (herramienta, armadura, mascota, reliquia, capa, decoración o un objeto de Minecraft): solo cuenta lo bueno que es.
     * Los números están calibrados con los dos sets por caja para que la rareza más alta de cada caja sea más rara cuanto más alta
     * es la caja (Forja 45 % · Hielo 38 · Fuego 31 · Magia 25 · Dragón 19 · Cielo 14 · Abismo 10).
     */
    private static final double[] TIER_WEIGHT = {1.00, 0.68, 0.50, 0.385, 0.185, 0.055, 0.0087, 0.0009};
    /** Nivel (1..9) de cada rareza: COMMON 1, UNCOMMON 2, RARE 3-4, EPIC 5, LEGENDARY 6, MYTHIC 7, STELLAR 8, CELESTIAL 9. */
    private static final int[] TIER_FIRST_LEVEL = {1, 2, 3, 5, 6, 7, 8, 9};
    /** Cuánto se parece el nivel dentro de una rareza (cada nivel de más resta peso: peso x e^(-DECAY x fracción)). */
    private static final double LEVEL_SLOPE = 0.05;
    private static final double DECAY = 1.2;
    /** Fracción extra de las reliquias: e^(-1,2 x 0,15) = peso x0,835. */
    private static final double RELIC_PENALTY = 0.15;

    // Importancia de cada tipo de premio: peso de UN objeto común de Minecraft de nivel 1 (madera, piedra...) = 1,0.
    private static final double BASE_VANILLA = 1.0;
    /** Armas y herramientas del set, según lo útil que es cada una. */
    private static final double BASE_SWORD = 0.30, BASE_PICK = 0.30, BASE_AXE = 0.26, BASE_SHOVEL = 0.22;
    /** Piezas de armadura (el peto protege más). */
    private static final double BASE_HELMET = 0.24, BASE_CHEST = 0.30, BASE_LEGS = 0.26, BASE_BOOTS = 0.22;
    /** Decoración: solo adorna, así que sale menos que lo útil. */
    private static final double BASE_DECOR = 0.16;
    /** Capas: exclusivas y decorativas, salen menos que casi todo. */
    private static final double BASE_CAPE = 0.08;
    /** Mascotas: se conseguen con mucha suerte; la mascota "estrella" de cada caja pesa todavía menos. */
    private static final double BASE_PET = 0.07, PET_STAR = 0.35;
    /** Segundo set de una caja (el B): un poco menos probable que el A, aunque tengan la misma rareza. */
    private static final double SET_B_FRAC = 0.15;
    private static final double BASE_OTHER = 0.15;

    // Reparto de la caja por tipo de premio (en %). Lo de Minecraft ocupa hasta VANILLA_MAX (menos si la caja tiene pocos objetos de Minecraft,
    // para que ninguno salga demasiado seguido); el resto se reparte entre equipo, armadura, decoración y capas con estas proporciones.
    private static final double VANILLA_MAX = 50.0, VANILLA_PER_ITEM = 4.5;
    private static final double SHARE_GEAR = 18.0, SHARE_ARMOR = 17.0, SHARE_DECOR = 9.0, SHARE_CAPES = 1.6, SHARE_OTHER = 3.0;
    /** Parte de la caja que ocupan TODAS las mascotas juntas (%), de Naturaleza a Abismo: muy poco, y menos en las cajas altas (que tienen pocas mascotas). */
    private static final double[] PET_SHARE = {3.5, 3.5, 3.0, 3.0, 3.0, 2.5, 1.2, 1.0};

    private static double petShare(CrateKind k) {
        return PET_SHARE[Math.max(0, Math.min(PET_SHARE.length - 1, k.rank() - 1))];
    }

    private static double baseShare(Cat c) {
        return switch (c) {
            case GEAR -> SHARE_GEAR;
            case ARMOR -> SHARE_ARMOR;
            case DECOR -> SHARE_DECOR;
            case CAPES -> SHARE_CAPES;
            default -> SHARE_OTHER;
        };
    }

    private static double baseOf(Cat c) {
        return switch (c) {
            case VANILLA -> BASE_VANILLA;
            case PETS -> BASE_PET;
            case DECOR -> BASE_DECOR;
            case CAPES -> BASE_CAPE;
            default -> BASE_OTHER;
        };
    }

    /** Peso de un premio cuando la garantía obliga a una rareza: casi sin importancia, para que el premio garantizado sea del set y no un material. */
    private static double pityBase(Cat c) {
        return switch (c) {
            case VANILLA -> 0.30;
            case PETS -> 0.50;
            default -> 1.0;
        };
    }

    private static double fracOf(Tier t, int level) {
        return LEVEL_SLOPE * Math.max(0, level - TIER_FIRST_LEVEL[t.ordinal()]);
    }

    /** Peso (sin normalizar) de un premio. */
    private static double rawWeight(Tier t, double frac) {
        return TIER_WEIGHT[t.ordinal()] * Math.exp(-DECAY * frac);
    }

    private static double rawWeight(Entry e) {
        return e.base * rawWeight(e.tier, e.frac);
    }

    /** {@code unit}: las piezas de un mismo conjunto (armadura o herramientas de un set) comparten rareza; null = el objeto va solo. */
    private record Entry(Tier tier, double frac, Cat cat, Item item, int min, int max, double base, String group, String unit) {
    }

    /** Botín de Minecraft: una sola escalera para todas las cajas (cada objeto tiene un único nivel). */
    private record V(int level, Item item, int min, int max) {
    }

    private static final List<V> LADDER = new ArrayList<>();

    private static void v(int level, Item it, int min, int max) {
        LADDER.add(new V(level, it, min, max));
    }

    static {
        // Nivel 1: madera, piedra y comida sencilla
        v(1, Items.OAK_LOG, 16, 32); v(1, Items.SPRUCE_LOG, 16, 32); v(1, Items.BIRCH_LOG, 16, 32); v(1, Items.STICK, 24, 48);
        v(1, Items.OAK_PLANKS, 32, 64); v(1, Items.APPLE, 4, 8); v(1, Items.OAK_SAPLING, 4, 8); v(1, Items.BONE_MEAL, 8, 16);
        v(1, Items.BREAD, 6, 12); v(1, Items.TORCH, 16, 32); v(1, Items.COBBLESTONE, 48, 64); v(1, Items.COAL, 12, 20);
        v(1, Items.FURNACE, 1, 2); v(1, Items.WHITE_WOOL, 8, 16); v(1, Items.IRON_NUGGET, 16, 32);
        // Nivel 2: hierro, cobre, redstone y comida cocinada
        v(2, Items.DARK_OAK_LOG, 16, 32); v(2, Items.JUNGLE_LOG, 16, 32); v(2, Items.ACACIA_LOG, 16, 32); v(2, Items.CHEST, 2, 4);
        v(2, Items.BARREL, 2, 4); v(2, Items.LADDER, 16, 32); v(2, Items.COOKED_CHICKEN, 8, 16); v(2, Items.HONEY_BOTTLE, 3, 5);
        v(2, Items.LEATHER, 4, 8); v(2, Items.IRON_INGOT, 4, 8); v(2, Items.RAW_IRON, 8, 16); v(2, Items.COPPER_INGOT, 8, 16);
        v(2, Items.REDSTONE, 16, 32); v(2, Items.LAPIS_LAZULI, 8, 16); v(2, Items.COOKED_SALMON, 6, 12); v(2, Items.BUCKET, 1, 1);
        v(2, Items.GLASS, 16, 32); v(2, Items.STONE_BRICKS, 16, 32); v(2, Items.SMOKER, 1, 1);
        // Nivel 3: oro, bloques de hierro, experiencia y curiosidades útiles
        v(3, Items.GOLD_INGOT, 4, 8); v(3, Items.IRON_BLOCK, 1, 2); v(3, Items.SHIELD, 1, 1); v(3, Items.HOPPER, 2, 4);
        v(3, Items.EXPERIENCE_BOTTLE, 8, 16); v(3, Items.GOLDEN_CARROT, 6, 10); v(3, Items.NAME_TAG, 1, 2); v(3, Items.SADDLE, 1, 1);
        v(3, Items.BOOKSHELF, 1, 3); v(3, Items.PRISMARINE_CRYSTALS, 4, 8); v(3, Items.BLUE_ICE, 3, 5); v(3, Items.SEA_LANTERN, 2, 4);
        v(3, Items.BLAZE_ROD, 2, 4); v(3, Items.QUARTZ, 4, 8); v(3, Items.GLOWSTONE, 4, 8); v(3, Items.OBSIDIAN, 4, 8);
        v(3, Items.GOLDEN_APPLE, 1, 1); v(3, Items.EMERALD, 2, 4); v(3, Items.BLAST_FURNACE, 1, 1); v(3, Items.ENDER_PEARL, 1, 2);
        // Nivel 4: diamantes y tesoros de aventura
        v(4, Items.DIAMOND, 1, 2); v(4, Items.GOLD_BLOCK, 2, 4); v(4, Items.ENDER_EYE, 2, 4); v(4, Items.GHAST_TEAR, 2, 3);
        v(4, Items.ENCHANTING_TABLE, 1, 1); v(4, Items.ANVIL, 1, 1); v(4, Items.BREWING_STAND, 1, 1); v(4, Items.CRYING_OBSIDIAN, 4, 8);
        v(4, Items.LAVA_BUCKET, 1, 1); v(4, Items.NAUTILUS_SHELL, 1, 2); v(4, Items.AMETHYST_CLUSTER, 1, 2); v(4, Items.EMERALD_BLOCK, 1, 2);
        v(4, Items.SPYGLASS, 1, 1); v(4, Items.TURTLE_HELMET, 1, 1);
        // Nivel 5: bloques de diamante, restos antiguos y tesoros raros
        v(5, Items.DIAMOND_BLOCK, 1, 1); v(5, Items.ANCIENT_DEBRIS, 1, 2); v(5, Items.HEART_OF_THE_SEA, 1, 1); v(5, Items.TRIDENT, 1, 1);
        v(5, Items.WITHER_SKELETON_SKULL, 1, 1); v(5, Items.ENDER_CHEST, 1, 1); v(5, Items.END_CRYSTAL, 1, 2);
        // Nivel 6: netherita en fragmentos y botín del Fin
        v(6, Items.NETHERITE_SCRAP, 2, 3); v(6, Items.TOTEM_OF_UNDYING, 1, 1); v(6, Items.SHULKER_SHELL, 2, 3); v(6, Items.ECHO_SHARD, 2, 4);
        v(6, Items.SCULK_SHRIEKER, 1, 2); v(6, Items.MUSIC_DISC_PIGSTEP, 1, 1);
        // Nivel 7: netherita y manzana encantada
        v(7, Items.NETHERITE_INGOT, 1, 2); v(7, Items.ENCHANTED_GOLDEN_APPLE, 1, 2); v(7, Items.BEACON, 1, 1); v(7, Items.SHULKER_BOX, 1, 1);
        // Nivel 8: objetos únicos
        v(8, Items.ELYTRA, 1, 1); v(8, Items.DRAGON_HEAD, 1, 1); v(8, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, 2); v(8, Items.CONDUIT, 1, 1);
        // Nivel 9: lo mejor que existe
        v(9, Items.NETHER_STAR, 1, 2); v(9, Items.DRAGON_EGG, 1, 1); v(9, Items.NETHERITE_BLOCK, 1, 1);
    }

    private static final class B {
        final CrateKind kind;
        final int[] win;
        final List<Entry> list = new ArrayList<>();

        B(CrateKind kind) {
            this.kind = kind;
            this.win = kind.levels();
        }

        boolean has(int level) {
            for (int l : win) {
                if (l == level) {
                    return true;
                }
            }
            return false;
        }

        /** Nivel de la ventana más cercano al pedido. */
        int near(int level) {
            int best = win[0];
            for (int l : win) {
                if (Math.abs(l - level) < Math.abs(best - level)) {
                    best = l;
                }
            }
            return best;
        }

        /** Botín de Minecraft: los objetos de la escalera cuyo nivel está en la ventana y que le tocan a esta caja (ver {@link #owners()}). */
        B vanilla() {
            Map<String, CrateKind> own = owners();
            for (V x : LADDER) {
                if (has(x.level) && own.get("v:" + keyOf(x.item)) == kind) {
                    list.add(new Entry(LEVEL_TIER[x.level], fracOf(LEVEL_TIER[x.level], x.level), Cat.VANILLA, x.item, x.min, x.max, BASE_VANILLA, "vanilla", null));
                }
            }
            return this;
        }

        /** Objeto del mod con rareza propia; solo entra si su nivel cae en la ventana de la caja. */
        B mod(Cat c, Tier t, int level, String id, int min, int max) {
            return mod(c, t, level, id, min, max, baseOf(c));
        }

        B mod(Cat c, Tier t, int level, String id, int min, int max, double base) {
            if (!has(level)) {
                return this;
            }
            Item it = byName(id);
            if (it != null) {
                // las reliquias salen algo menos mientras se pulen (peso x0,835); no rompe el orden entre rarezas
                list.add(new Entry(t, fracOf(t, level) + (c == Cat.RELICS ? RELIC_PENALTY : 0), c, it, min, max, base,
                        c == Cat.PETS && base < BASE_PET ? "pet_star" : c.name().toLowerCase(), null));
            }
            return this;
        }

        /** Objeto del mod que sale siempre en esta caja (equipo, armadura, capas, decoración), con una ligera ventaja o desventaja {@code frac}. */
        B any(Cat c, Tier t, double frac, String id, int min, int max) {
            return any(c, t, frac, id, min, max, baseOf(c), c.name().toLowerCase());
        }

        /** Igual, pero con importancia propia ({@code base}) y un grupo para comprobar el orden (p. ej. "gear_sword"). */
        B any(Cat c, Tier t, double frac, String id, int min, int max, double base, String group) {
            return any(c, t, frac, id, min, max, base, group, null);
        }

        /** Igual, con la "unidad" del conjunto al que pertenece (armadura o herramientas de un set): todas sus piezas tendrán la misma rareza. */
        B any(Cat c, Tier t, double frac, String id, int min, int max, double base, String group, String unit) {
            Item it = byName(id);
            if (it != null) {
                list.add(new Entry(t, frac, c, it, min, max, base, group, unit));
            }
            return this;
        }

        /** Llave de la caja {@code rank} (1..8): vale como el techo de la caja anterior (la llave 1 vale 1). */
        B key(int rank) {
            if (rank >= 1 && rank <= 8) {
                CrateKind k = CrateKind.byIndex(rank);
                if (k != CrateKind.PETS) {
                    int lv = rank == 1 ? 1 : CrateKind.byIndex(rank - 1).highLevel();
                    if (has(lv)) {
                        list.add(new Entry(LEVEL_TIER[lv], lv, Cat.KEYS, ModItems.KEYS.get(k).get(), 1, 1, BASE_OTHER, "keys", null));
                    }
                }
            }
            return this;
        }

        /** Material de mob; el nivel sale de la Esencia que da. */
        B mat(Mats m) {
            int[] q = switch (matTier(m)) {
                case COMMON -> new int[]{3, 6};
                case UNCOMMON -> new int[]{2, 4};
                case RARE -> new int[]{2, 3};
                default -> new int[]{1, 2};
            };
            return mod(Cat.MATS, matTier(m), matLevel(m), m.itemId(), q[0], q[1]);
        }

        Map<Tier, List<Prize>> build() {
            Map<Tier, List<Prize>> m = new EnumMap<>(Tier.class);
            for (Tier t : Tier.values()) {
                m.put(t, new ArrayList<>());
            }
            java.util.Set<Item> seen = new java.util.HashSet<>();
            List<Entry> es = new ArrayList<>();
            double total = 0;
            double[] catW = new double[Cat.values().length];
            int[] catN = new int[Cat.values().length];
            for (Entry e : list) {
                if (seen.add(e.item)) {
                    es.add(e);
                    total += rawWeight(e);
                    catW[e.cat.ordinal()] += rawWeight(e);
                    catN[e.cat.ordinal()]++;
                }
            }
            // cuánto de la caja ocupa cada tipo de premio (fracción de 1)
            double[] share = new double[Cat.values().length];
            double vs = Math.min(VANILLA_MAX, VANILLA_PER_ITEM * catN[Cat.VANILLA.ordinal()]);
            double ps = catN[Cat.PETS.ordinal()] > 0 ? petShare(kind) : 0.0;
            double others = 0;
            for (Cat c : Cat.values()) {
                if (c != Cat.VANILLA && c != Cat.PETS && catN[c.ordinal()] > 0) {
                    others += baseShare(c);
                }
            }
            for (Cat c : Cat.values()) {
                if (catN[c.ordinal()] == 0) {
                    continue;
                }
                share[c.ordinal()] = (c == Cat.VANILLA ? vs : c == Cat.PETS ? ps : baseShare(c) * (100.0 - vs - ps) / others) / 100.0;
            }
            int[] ws = new int[es.size()];
            int[] pws = new int[es.size()];
            for (int i = 0; i < es.size(); i++) {
                Entry e = es.get(i);
                int ci = e.cat.ordinal();
                ws[i] = Math.max(1, (int) Math.round(share[ci] * rawWeight(e) / catW[ci] * 1_000_000.0));
                // peso para la garantía: casi sin la importancia (los premios de esa rareza compiten casi por igual)
                pws[i] = Math.max(1, (int) Math.round(pityBase(e.cat) * (e.base / baseOf(e.cat)) * rawWeight(e.tier, e.frac) * 1_000_000.0));
            }
            // la rareza que se ve en la caja sale de lo seguido que toca cada premio (los pesos y los premios no cambian)
            equalizeSets(es, ws);
            Tier[] shown = byFrequency(kind, es, ws);
            for (int i = 0; i < es.size(); i++) {
                Entry e = es.get(i);
                m.get(shown[i]).add(new Prize(e.item, e.min, e.max, ws[i], pws[i], e.group));
            }
            return m;
        }
    }


    // ------------------------------------------------------------------ rareza según lo seguido que sale

    /** Cajas 1 y 2 (Naturaleza, Forja): rarezas de Común a Legendaria; cajas 3 a 8: de Común a Mítica. */
    private static int tiersOf(CrateKind k) {
        return k.rank() <= 2 ? 5 : 6;
    }

    /**
     * Las piezas de un mismo conjunto salen igual de seguido (cada una el promedio del conjunto): así el conjunto entero lleva la misma rareza y
     * ninguna pieza de una rareza mejor sale más que una de una rareza peor. El total que ocupa el conjunto en la caja no cambia.
     */
    private static void equalizeSets(List<Entry> es, int[] w) {
        Map<String, List<Integer>> units = new java.util.HashMap<>();
        for (int i = 0; i < es.size(); i++) {
            if (es.get(i).unit != null) {
                units.computeIfAbsent(es.get(i).unit, x -> new ArrayList<>()).add(i);
            }
        }
        for (List<Integer> idx : units.values()) {
            long sum = 0;
            for (int i : idx) {
                sum += w[i];
            }
            int avg = (int) Math.max(1, Math.round((double) sum / idx.size()));
            for (int i : idx) {
                w[i] = avg;
            }
        }
    }

    /** Parte acumulada de la caja (%) que ocupa cada rareza, de la más común a la más rara. */
    private static final double[] CUM_5 = {50, 78, 92, 98, 100};
    private static final double[] CUM_6 = {50, 77, 90, 96, 99, 100};

    /**
     * Reparte las rarezas de una caja según la probabilidad de cada premio: lo que más sale es Común y lo que menos sale es la rareza más alta
     * de la caja. Las piezas de un mismo conjunto (armadura o herramientas de un set) llevan siempre la misma rareza, y los premios que salen
     * igual de seguido comparten rareza. Los pesos no se tocan: solo cambia la etiqueta.
     */
    private static Tier[] byFrequency(CrateKind kind, List<Entry> es, int[] w) {
        int n = es.size();
        Tier[] out = new Tier[n];
        // unidades: un conjunto es una sola unidad; cada otro premio es la suya
        Map<String, List<Integer>> units = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            String u = es.get(i).unit != null ? es.get(i).unit : "#" + i;
            units.computeIfAbsent(u, x -> new ArrayList<>()).add(i);
        }
        List<List<Integer>> list = new ArrayList<>(units.values());
        double[] rep = new double[list.size()];
        double[] mass = new double[list.size()];
        double total = 0;
        for (int u = 0; u < list.size(); u++) {
            long s = 0;
            for (int i : list.get(u)) {
                s += w[i];
            }
            rep[u] = (double) s / list.get(u).size();
            mass[u] = s;
            total += s;
        }
        Integer[] order = new Integer[list.size()];
        for (int u = 0; u < order.length; u++) {
            order[u] = u;
        }
        java.util.Arrays.sort(order, (a, b) -> Double.compare(rep[b], rep[a]));
        // niveles: unidades que salen igual de seguido (diferencia menor al 0,5 %) no se separan
        List<List<Integer>> levels = new ArrayList<>();
        double last = -1;
        for (int u : order) {
            if (levels.isEmpty() || Math.abs(last - rep[u]) > 0.005 * last) {
                levels.add(new ArrayList<>());
                last = rep[u];
            }
            levels.get(levels.size() - 1).add(u);
        }
        int tiers = Math.min(tiersOf(kind), levels.size());
        double[] target = tiersOf(kind) == 5 ? CUM_5 : CUM_6;
        double[] cum = new double[levels.size()];
        double c = 0;
        for (int l = 0; l < levels.size(); l++) {
            for (int u : levels.get(l)) {
                c += mass[u];
            }
            cum[l] = 100.0 * c / total;
        }
        int[] end = new int[tiers];
        int prev = 0;
        for (int t = 0; t < tiers - 1; t++) {
            int best = prev + 1;
            double bestD = Double.MAX_VALUE;
            for (int j = prev + 1; j <= levels.size() - (tiers - 1 - t); j++) {
                double d = Math.abs(cum[j - 1] - target[t]);
                if (d < bestD) {
                    bestD = d;
                    best = j;
                }
            }
            end[t] = best;
            prev = best;
        }
        end[tiers - 1] = levels.size();
        int start = 0;
        for (int t = 0; t < tiers; t++) {
            for (int l = start; l < end[t]; l++) {
                for (int u : levels.get(l)) {
                    for (int i : list.get(u)) {
                        out[i] = Tier.byIndex(t);
                    }
                }
            }
            start = end[t];
        }
        return out;
    }

    private static Map<String, Tier> HOME;

    /**
     * Rareza con la que se ve un objeto del mod que sale en las cajas (la de la primera caja que lo da, de la más sencilla a la mejor), o null
     * si no sale en ninguna. Solo es la etiqueta y el color: el poder del objeto sigue saliendo de su rareza interna ({@link Tiers#of}).
     */
    public static synchronized Tier home(String path) {
        if (HOME == null) {
            Map<String, Tier> h = new java.util.HashMap<>();
            for (CrateKind k : CrateKind.values()) {
                if (k == CrateKind.PETS) {
                    continue;
                }
                for (Map.Entry<Tier, List<Prize>> e : pool(k).entrySet()) {
                    for (Prize z : e.getValue()) {
                        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(z.item());
                        if (rl != null && rl.getNamespace().equals(MundosDeAxelis.MODID)) {
                            h.putIfAbsent(rl.getPath(), e.getKey());
                        }
                    }
                }
            }
            HOME = h;
        }
        return HOME.get(path);
    }

    private static String keyOf(Item it) {
        ResourceLocation k = ForgeRegistries.ITEMS.getKey(it);
        return k == null ? it.toString() : k.toString();
    }

    private static Map<String, CrateKind> OWNERS;

    /**
     * A qué caja le toca cada objeto de Minecraft, mascota y reliquia. Cada uno sale en UNA sola caja de las que tienen su nivel
     * (se reparten por turnos, en orden alfabético): así ninguna caja se llena de cientos de objetos y los premios del set tienen
     * una probabilidad decente. Se calcula una vez; cliente y servidor obtienen el mismo reparto.
     */
    private static synchronized Map<String, CrateKind> owners() {
        if (OWNERS != null) {
            return OWNERS;
        }
        Map<String, CrateKind> out = new java.util.HashMap<>();
        Map<String, List<String>> byGroup = new TreeMap<>();
        for (V x : LADDER) {
            byGroup.computeIfAbsent("v" + x.level, k -> new ArrayList<>()).add("v:" + keyOf(x.item));
        }
        for (Map.Entry<String, Tier> e : new TreeMap<>(Tiers.all()).entrySet()) {
            String id = e.getKey();
            boolean pet = id.startsWith("comp_");
            if (Tiers.craftOnly(id)) {
                continue;
            }
            if (!pet && (id.startsWith("gear_") || GearDefs.get(id) != null || com.mundosdeaxelis.gear.ArmorDefs.isArmorId(id))) {
                continue;
            }
            byGroup.computeIfAbsent((pet ? "p" : "r") + levelOf(e.getValue(), id), k -> new ArrayList<>()).add((pet ? "p:" : "r:") + id);
        }
        for (Map.Entry<String, List<String>> g : byGroup.entrySet()) {
            int level = Integer.parseInt(g.getKey().substring(1));
            List<CrateKind> holders = new ArrayList<>();
            for (CrateKind k : CrateKind.values()) {
                if (k != CrateKind.PETS && Arrays.stream(k.levels()).anyMatch(l -> l == level)) {
                    holders.add(k);
                }
            }
            if (holders.isEmpty()) {
                continue;
            }
            List<String> names = new ArrayList<>(g.getValue());
            java.util.Collections.sort(names);
            for (int i = 0; i < names.size(); i++) {
                out.put(names.get(i), holders.get(i % holders.size()));
            }
        }
        OWNERS = out;
        return out;
    }

    private static Item byName(String id) {
        ResourceLocation rl = new ResourceLocation(MundosDeAxelis.MODID, id);
        if (!ForgeRegistries.ITEMS.containsKey(rl)) {
            return null;
        }
        Item it = ForgeRegistries.ITEMS.getValue(rl);
        return it == Items.AIR ? null : it;
    }

    /**
     * Herramientas de la caja: su set (espada, pico, hacha y pala) en las rarezas que tiene, cada una con su nivel, más las
     * especiales que le tocan (con el nivel de la ventana más parecido al de su rareza).
     */
    private static void gear(B b, String... specials) {
        for (GearSet set : b.kind.sets) {
            for (GearDef d : GearDefs.of(set)) {
                double base = switch (d.family()) {
                    case SWORD -> BASE_SWORD;
                    case PICK -> BASE_PICK;
                    case AXE -> BASE_AXE;
                    case SHOVEL -> BASE_SHOVEL;
                };
                b.any(Cat.GEAR, d.tier(), setFrac(set), d.id(), 1, 1, base, "gear_" + d.family().id, "gear:" + set.name());
            }
        }
        for (String s : specials) {
            Tier t = Tiers.of(s);
            b.any(Cat.GEAR, t, 0, s, 1, 1, BASE_SWORD, "gear_special");
        }
    }

    /** El set B de una caja (puesto par) sale un poco menos que el A. */
    private static double setFrac(GearSet set) {
        return set.rank % 2 == 0 ? SET_B_FRAC : 0.0;
    }

    /** Armadura de la caja: las cuatro piezas del set que da esa caja (todas con la rareza del set). */
    private static void armor(B b) {
        for (GearSet set : b.kind.sets) {
            for (com.mundosdeaxelis.gear.ArmorDefs.Piece p : com.mundosdeaxelis.gear.ArmorDefs.Piece.values()) {
                double base = switch (p) {
                    case HELMET -> BASE_HELMET;
                    case CHESTPLATE -> BASE_CHEST;
                    case LEGGINGS -> BASE_LEGS;
                    case BOOTS -> BASE_BOOTS;
                };
                b.any(Cat.ARMOR, com.mundosdeaxelis.gear.ArmorDefs.tier(set), setFrac(set),
                        com.mundosdeaxelis.gear.ArmorDefs.id(set, p), 1, 1, base, "armor_" + p.id, "armor:" + set.name());
            }
        }
    }

    /** Decoración: cada objeto sale solo en las cajas que le tocan (campo crates de DecorDefs); las épicas o mejores, en el nivel alto. */
    private static void decor(B b) {
        for (com.mundosdeaxelis.block.DecorDefs.Def d : com.mundosdeaxelis.block.DecorDefs.ALL) {
            if (d.dropsIn(b.kind.id)) {
                b.any(Cat.DECOR, d.tier(), 0, d.id(), d.min(), d.max());
            }
        }
    }

    /** La mascota "estrella" de una caja: la de mayor rareza que le toca (si hay empate, la última por orden alfabético). */
    private static String starOf(CrateKind kind) {
        String star = null;
        Tier starTier = null;
        for (Map.Entry<String, Tier> e : new TreeMap<>(Tiers.all()).entrySet()) {
            if (e.getKey().startsWith("comp_") && owners().get("p:" + e.getKey()) == kind
                    && new B(kind).has(levelOf(e.getValue(), e.getKey())) && (starTier == null || e.getValue().ordinal() >= starTier.ordinal())) {
                star = e.getKey();
                starTier = e.getValue();
            }
        }
        return star;
    }

    private static java.util.Set<String> STARS;

    /** Puesto (1..8) de la caja que da esa mascota; 0 si no sale de ninguna caja de colección. */
    private static int petRank(String id) {
        CrateKind k = owners().get("p:" + id);
        return k == null ? 0 : k.rank();
    }

    /**
     * Armadura (puntos enteros) que da esa mascota equipada, según la caja de la que sale: en la caja de puesto N las mascotas
     * dan 2N-1 y la estrella (su mascota más rara) da 2N. Así Naturaleza = 1 y 2, Forja = 3 y 4, Hielo = 5 y 6... Las tres
     * Celestiales del Abismo (puesto 8) dan lo mismo, 16.
     */
    public static synchronized int petArmor(String id) {
        // mascotas exclusivas de los jefes: dan lo mismo que la estrella de la caja de su mazmorra (Naturaleza 2, Forja 4)
        if (id.equals("comp_sprout_king")) {
            return 2;
        }
        if (id.equals("comp_ember_anvil")) {
            return 4;
        }
        int r = petRank(id);
        if (r <= 0) {
            return 1;
        }
        if (r >= 8) {
            return 2 * r;
        }
        if (STARS == null) {
            java.util.Set<String> out = new java.util.HashSet<>();
            for (CrateKind k : CrateKind.values()) {
                if (k != CrateKind.PETS) {
                    String s = starOf(k);
                    if (s != null) {
                        out.add(s);
                    }
                }
            }
            STARS = out;
        }
        return STARS.contains(id) ? 2 * r : 2 * r - 1;
    }

    /** Armadura de mascotas del jugador de referencia de una etapa: las {@code slots} mejores mascotas de las cajas hasta esa etapa. */
    public static synchronized float bestPetArmor(int stage, int slots) {
        Float cached = BEST_PET.get(stage * 100 + slots);
        if (cached != null) {
            return cached;
        }
        java.util.List<Integer> v = new java.util.ArrayList<>();
        for (String id : Tiers.all().keySet()) {
            if (id.startsWith("comp_")) {
                int r = petRank(id);
                if (r > 0 && r <= stage) {
                    v.add(petArmor(id));
                }
            }
        }
        v.sort(java.util.Comparator.reverseOrder());
        float sum = 0.0F;
        for (int i = 0; i < Math.min(slots, v.size()); i++) {
            sum += v.get(i);
        }
        BEST_PET.put(stage * 100 + slots, sum);
        return sum;
    }

    private static final Map<Integer, Float> BEST_PET = new java.util.HashMap<>();

    /** Mascotas: el nivel sale de su rareza (las más raras, solo en las cajas altas). */
    private static void pets(B b) {
        // la mascota "estrella" pesa bastante menos que las demás, así cada caja tiene una mascota claramente más valiosa
        String star = starOf(b.kind);
        for (Map.Entry<String, Tier> e : new TreeMap<>(Tiers.all()).entrySet()) {
            if (e.getKey().startsWith("comp_")) {
                if (owners().get("p:" + e.getKey()) == b.kind) {
                    b.mod(Cat.PETS, e.getValue(), levelOf(e.getValue(), e.getKey()), e.getKey(), 1, 1,
                            e.getKey().equals(star) ? BASE_PET * PET_STAR : BASE_PET);
                }
            }
        }
    }

    /** Reliquias: igual, el nivel sale de su rareza. */
    private static void relics(B b) {
        for (Map.Entry<String, Tier> e : new TreeMap<>(Tiers.all()).entrySet()) {
            String id = e.getKey();
            if (Tiers.craftOnly(id) || id.startsWith("comp_") || id.startsWith("gear_") || GearDefs.get(id) != null || com.mundosdeaxelis.gear.ArmorDefs.isArmorId(id)) {
                continue;
            }
            if (owners().get("r:" + id) == b.kind) {
                b.mod(Cat.RELICS, e.getValue(), levelOf(e.getValue(), id), id, 1, 1);
            }
        }
    }

    /** Capas: la capa de la caja, con su rareza; sale en el nivel más alto de la ventana de la caja (es un premio especial). */
    private static void capes(B b) {
        for (com.mundosdeaxelis.cape.CapeDefs.Def d : com.mundosdeaxelis.cape.CapeDefs.of(b.kind)) {
            b.any(Cat.CAPES, d.tier(), 0, d.id(), 1, 1);
        }
    }

    /** Llaves: la de la propia caja (nivel bajo) y la de la siguiente (nivel alto). */
    private static void keys(B b) {
        int r = b.kind.rank();
        b.key(r).key(r + 1);
    }

    /** Pesos de rareza de la caja de Mascotas (solo creativo), en milésimas: C U R E L M. */
    private static final int[] PET_W = {400, 280, 180, 90, 40, 10, 4, 2};

    private static Map<Tier, List<Prize>> build(CrateKind k) {
        if (k == CrateKind.PETS) {
            // la caja de Mascotas (solo creativo) da únicamente mascotas, con la rareza de cada una
            Map<Tier, List<Prize>> m = new EnumMap<>(Tier.class);
            Map<Tier, List<Item>> by = new EnumMap<>(Tier.class);
            for (Tier t : Tier.values()) {
                m.put(t, new ArrayList<>());
                by.put(t, new ArrayList<>());
            }
            for (Map.Entry<String, Tier> e : new TreeMap<>(Tiers.all()).entrySet()) {
                Item it = e.getKey().startsWith("comp_") ? byName(e.getKey()) : null;
                if (it != null) {
                    by.get(e.getValue()).add(it);
                }
            }
            int total = 0;
            for (Tier t : Tier.values()) {
                if (!by.get(t).isEmpty()) {
                    total += PET_W[t.ordinal()];
                }
            }
            for (Tier t : Tier.values()) {
                List<Item> l = by.get(t);
                for (Item it : l) {
                    m.get(t).add(new Prize(it, 1, 1, Math.max(1, (int) Math.round(PET_W[t.ordinal()] * 1000.0 * 1000.0 / Math.max(1, total) / l.size())), 1, "pet"));
                }
            }
            return m;
        }
        B b = new B(k);
        b.vanilla();
        switch (k) {
            case FOREST -> {
                gear(b);
            }
            case FORGE -> {
                gear(b);
            }
            case ICE -> {
                gear(b);
            }
            case MAGMA -> {
                gear(b);
            }
            case MAGIC -> {
                gear(b);
            }
            case RUBY -> {
                gear(b);
            }
            case SKY -> {
                gear(b);
            }
            case ABYSS -> {
                gear(b);
            }
            default -> {
            }
        }
        pets(b);
        // etapa 134: las cajas solo dan mascotas, armas/herramientas, armadura, capas, decoración y botín de Minecraft (sin reliquias, materiales ni llaves)
        armor(b);
        decor(b);
        capes(b);
        return b.build();
    }

    public static synchronized Map<Tier, List<Prize>> pool(CrateKind k) {
        Map<Tier, List<Prize>> m = CACHE.get(k);
        if (m == null) {
            m = build(k);
            CACHE.put(k, m);
        }
        return m;
    }

    // ------------------------------------------------------------------ sorteo

    /** La rareza pedida, o la inmediata inferior que tenga premios (y si no hay, la más baja que tenga). */
    private static Tier fallback(CrateKind k, Tier pick) {
        Map<Tier, List<Prize>> p = pool(k);
        int i = pick.ordinal();
        while (i > 0 && p.get(Tier.byIndex(i)).isEmpty()) {
            i--;
        }
        if (p.get(Tier.byIndex(i)).isEmpty()) {
            for (Tier c : Tier.values()) {
                if (!p.get(c).isEmpty()) {
                    return c;
                }
            }
        }
        return Tier.byIndex(i);
    }

    /**
     * Comprueba la regla "lo mejor sale menos": ningún objeto de una rareza superior puede pesar más que uno de una rareza inferior
     * de la misma caja. Devuelve las infracciones (vacío = todo bien).
     */
    public static List<String> verify(CrateKind k) {
        List<String> bad = new ArrayList<>();
        Map<Tier, List<Prize>> p = pool(k);
        for (Tier lo : Tier.values()) {
            for (Prize a : p.get(lo)) {
                for (Tier hi : Tier.values()) {
                    if (hi.ordinal() <= lo.ordinal()) {
                        continue;
                    }
                    for (Prize b : p.get(hi)) {
                        // solo se compara dentro del mismo grupo (espadas con espadas, petos con petos...): una madera no es una hacha
                        if (a.group().equals(b.group()) && b.weight() > a.weight()) {
                            bad.add(keyOf(b.item()) + " (" + hi.id + ") pesa más que " + keyOf(a.item()) + " (" + lo.id + ")");
                        }
                    }
                }
            }
        }
        return bad;
    }

    /** Un sorteo ya resuelto: el objeto y su rareza. */
    public record Result(Item item, Tier tier) {
    }

    /** Sortea un premio con las probabilidades reales de la caja; {@code forced} (la garantía) obliga a que sea de esa rareza. */
    public static Result draw(CrateKind k, RandomSource r, Tier forced) {
        Map<Tier, List<Prize>> p = pool(k);
        if (forced != null) {
            Tier t = fallback(k, forced);
            return pick(p.get(t), t, r);
        }
        long total = 0;
        for (List<Prize> l : p.values()) {
            for (Prize z : l) {
                total += z.weight();
            }
        }
        if (total <= 0) {
            return new Result(Items.AIR, Tier.COMMON);
        }
        long n = (long) (r.nextDouble() * total);
        for (Tier t : Tier.values()) {
            for (Prize z : p.get(t)) {
                n -= z.weight();
                if (n < 0) {
                    return new Result(z.item(), t);
                }
            }
        }
        return new Result(Items.AIR, Tier.COMMON);
    }

    private static Result pick(List<Prize> l, Tier t, RandomSource r) {
        if (l.isEmpty()) {
            return new Result(Items.AIR, t);
        }
        long total = 0;
        for (Prize z : l) {
            total += z.pity();
        }
        long n = (long) (r.nextDouble() * total);
        for (Prize z : l) {
            n -= z.pity();
            if (n < 0) {
                return new Result(z.item(), t);
            }
        }
        return new Result(l.get(0).item(), t);
    }

    public static Item roll(CrateKind k, RandomSource r) {
        return draw(k, r, null).item();
    }

    /** Una línea del catálogo: premio, rareza, cantidad y probabilidad real (%) de que toque. */
    public record Chance(Item item, Tier tier, int min, int max, double percent, String group) {
    }

    /** Probabilidad real de cada rareza (%), sumando los premios que tiene. */
    public static double[] tierChances(CrateKind k) {
        double[] out = new double[Tier.values().length];
        Map<Tier, List<Prize>> p = pool(k);
        long total = 0;
        for (List<Prize> l : p.values()) {
            for (Prize z : l) {
                total += z.weight();
            }
        }
        if (total <= 0) {
            return out;
        }
        for (Tier t : Tier.values()) {
            long s = 0;
            for (Prize z : p.get(t)) {
                s += z.weight();
            }
            out[t.ordinal()] = 100.0 * s / total;
        }
        return out;
    }

    /** Catálogo completo de la caja: lo común arriba y lo más raro (mítico) al final. */
    public static List<Chance> chances(CrateKind k) {
        List<Chance> out = new ArrayList<>();
        Map<Tier, List<Prize>> p = pool(k);
        long total = 0;
        for (List<Prize> l : p.values()) {
            for (Prize z : l) {
                total += z.weight();
            }
        }
        if (total <= 0) {
            return out;
        }
        for (Tier t : Tier.values()) {
            List<Chance> row = new ArrayList<>();
            for (Prize z : p.get(t)) {
                row.add(new Chance(z.item(), t, z.min(), z.max(), 100.0 * z.weight() / total, z.group()));
            }
            row.sort((a, c) -> Double.compare(c.percent(), a.percent()));
            out.addAll(row);
        }
        return out;
    }

    /** Lista de relleno de la ruleta: el premio ocupa {@code winIdx}; el resto sale con las mismas probabilidades. */
    public static Item[] strip(CrateKind k, long seed, int len, int winIdx, Item winner) {
        Item[] out = new Item[len];
        RandomSource rs = RandomSource.create(seed ^ 0x5DEECE66DL);
        for (int i = 0; i < len; i++) {
            out[i] = i == winIdx ? winner : roll(k, rs);
        }
        // que no haya dos iguales seguidos junto al premio (se ve mejor)
        for (int i = Math.max(1, winIdx - 2); i <= Math.min(len - 1, winIdx + 2); i++) {
            if (i != winIdx && out[i] == out[i - 1]) {
                out[i] = roll(k, rs);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ rareza y cantidad de un premio

    /** Rareza del objeto en este crate (si no está en ninguna lista, la general). */
    public static Tier tierOf(CrateKind k, Item it) {
        Map<Tier, List<Prize>> p = pool(k);
        for (Tier t : Tier.values()) {
            for (Prize z : p.get(t)) {
                if (z.item() == it) {
                    return t;
                }
            }
        }
        return Tiers.of(it);
    }

    /** Cantidad del premio: se calcula con la semilla de la apertura, así el cliente y el servidor coinciden. */
    public static int count(CrateKind k, Item it, long seed) {
        Map<Tier, List<Prize>> p = pool(k);
        for (Tier t : Tier.values()) {
            for (Prize z : p.get(t)) {
                if (z.item() == it) {
                    if (z.max() <= z.min()) {
                        return z.min();
                    }
                    return z.min() + new Random(seed * 31L + 7L).nextInt(z.max() - z.min() + 1);
                }
            }
        }
        return 1;
    }

    public static ItemStack stack(CrateKind k, Item it, long seed) {
        return new ItemStack(it, count(k, it, seed));
    }

    /** Nombre del premio con su cantidad ("Diamante ×3"). */
    public static net.minecraft.network.chat.Component label(ItemStack st) {
        net.minecraft.network.chat.MutableComponent c = st.getHoverName().copy();
        if (st.getCount() > 1) {
            c.append(" ×" + st.getCount());
        }
        return c;
    }
}

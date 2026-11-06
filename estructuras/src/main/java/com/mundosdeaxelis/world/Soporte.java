package com.mundosdeaxelis.world;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Modelo aproximado de {@code Block.canSurvive} de Minecraft 1.20.1 para los bloques que usan las estructuras (antorchas, farolas, alfombras,
 * flores, velas, enredaderas, estandartes, nieve...). Sin nada de Minecraft: trabaja sobre el texto del plano ({@link StructBp}).
 * {@link #prune} quita del plano lo que el juego rompería solo (y soltaría como objeto tirado); {@link Estructuras} hace lo mismo con
 * {@code canSurvive} real como red de seguridad. Así lo que se revisa fuera del juego es lo que de verdad se construye.
 */
final class Soporte {
    private static final String[] DIRN = {"north", "south", "west", "east", "up", "down"};
    private static final int[][] DIRV = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

    private static int di(String d) {
        for (int i = 0; i < 6; i++) {
            if (DIRN[i].equals(d)) {
                return i;
            }
        }
        return 0;
    }

    private static String opp(String d) {
        return switch (d) {
            case "north" -> "south";
            case "south" -> "north";
            case "west" -> "east";
            case "east" -> "west";
            case "up" -> "down";
            default -> "up";
        };
    }

    /** Bloque ya separado en nombre y propiedades. */
    private static final class B {
        final String n;
        final Map<String, String> p;

        B(String n, Map<String, String> p) {
            this.n = n;
            this.p = p;
        }

        String prop(String k, String def) {
            String v = p.get(k);
            return v == null ? def : v;
        }
    }

    private static final B AIR = new B("air", Map.of());
    private static final Map<String, B> CACHE = new HashMap<>();

    private static B parse(String s) {
        B c = CACHE.get(s);
        if (c != null) {
            return c;
        }
        String t = s.startsWith("minecraft:") ? s.substring(10) : s;
        String name = t;
        Map<String, String> pr = new HashMap<>();
        int k = t.indexOf('[');
        if (k >= 0) {
            name = t.substring(0, k);
            for (String kv : t.substring(k + 1, t.length() - 1).split(",")) {
                int e = kv.indexOf('=');
                if (e > 0) {
                    pr.put(kv.substring(0, e).trim(), kv.substring(e + 1).trim());
                }
            }
        }
        c = new B(name, pr);
        CACHE.put(s, c);
        return c;
    }

    private static final Set<String> NOT_FULL = new HashSet<>(Arrays.asList(
            "chest", "trapped_chest", "ender_chest", "enchanting_table", "lectern", "brewing_stand", "cauldron", "water_cauldron", "lava_cauldron",
            "powder_snow_cauldron", "campfire", "soul_campfire", "anvil", "chipped_anvil", "damaged_anvil", "bell", "grindstone", "composter", "hopper",
            "lightning_rod", "end_rod", "chain", "lantern", "soul_lantern", "iron_bars", "cobweb", "lily_pad", "cake", "stonecutter", "sculk_sensor",
            "dragon_egg", "daylight_detector", "conduit", "end_portal_frame", "farmland", "dirt_path", "soul_sand", "barrier", "light", "water", "lava",
            "air", "cave_air", "void_air", "scaffolding", "big_dripleaf", "small_dripleaf", "ladder", "vine", "glow_lichen", "snow", "sea_pickle",
            "flower_pot", "turtle_egg", "pointed_dripstone", "amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud",
            "bamboo", "cocoa", "tripwire", "tripwire_hook", "lever", "rail", "powered_rail", "detector_rail", "activator_rail", "redstone_wire",
            "azalea", "flowering_azalea", "fern", "grass", "tall_grass", "large_fern", "dead_bush", "sugar_cane", "kelp", "seagrass", "torch", "wall_torch",
            "soul_torch", "soul_wall_torch", "redstone_torch", "redstone_wall_torch", "moss_carpet", "skeleton_skull", "skeleton_wall_skull",
            "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head", "hanging_roots", "spore_blossom", "candle",
            "brown_mushroom", "red_mushroom", "sculk_vein", "sculk_shrieker", "sculk_catalyst", "bubble_column", "fire", "soul_fire", "item_frame",
            "decorated_pot", "mud", "honey_block", "slime_block", "cave_vines", "cave_vines_plant", "weeping_vines", "twisting_vines", "piston_head",
            "moving_piston", "glow_item_frame"));
    private static final String[] NOT_FULL_SUFFIX = {"_stairs", "_slab", "_wall", "_fence", "_fence_gate", "_pane", "_carpet", "_candle", "_banner",
            "_wall_banner", "_door", "_trapdoor", "_button", "_pressure_plate", "_sign", "_wall_sign", "_hanging_sign", "_wall_hanging_sign", "_bed",
            "_shulker_box", "_candle_cake", "_sapling", "_tulip", "_head", "_wall_head", "_skull", "_wall_skull", "_leaves", "_fungus", "_roots",
            "_coral", "_coral_fan", "_coral_wall_fan", "_pot", "_bush", "_stem", "_vine", "_vines"};
    private static final Set<String> FLOWERS = new HashSet<>(Arrays.asList("dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip",
            "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose", "torchflower", "sunflower",
            "lilac", "rose_bush", "peony", "pitcher_plant"));
    private static final Set<String> TALL = new HashSet<>(Arrays.asList("tall_grass", "large_fern", "sunflower", "lilac", "rose_bush", "peony"));
    private static final Set<String> DIRT_TAG = new HashSet<>(Arrays.asList("dirt", "grass_block", "podzol", "coarse_dirt", "mycelium", "rooted_dirt",
            "moss_block", "mud", "muddy_mangrove_roots"));

    private static boolean isBush(String n) {
        return FLOWERS.contains(n) || TALL.contains(n) || n.equals("fern") || n.equals("grass") || n.equals("azalea") || n.equals("flowering_azalea")
                || n.equals("sweet_berry_bush");
    }

    private static boolean isFull(String n) {
        if (NOT_FULL.contains(n) || n.startsWith("potted_")) {
            return false;
        }
        for (String s : NOT_FULL_SUFFIX) {
            if (n.endsWith(s)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sturdy(B c, String face) {
        if (c.n.endsWith("_stairs")) {
            String half = c.prop("half", "bottom");
            if (face.equals("down")) {
                return half.equals("bottom");
            }
            if (face.equals("up")) {
                return half.equals("top");
            }
            return c.prop("facing", "north").equals(face) && c.prop("shape", "straight").equals("straight");
        }
        if (c.n.endsWith("_slab")) {
            String t = c.prop("type", "bottom");
            if (t.equals("double")) {
                return true;
            }
            return (t.equals("bottom") && face.equals("down")) || (t.equals("top") && face.equals("up"));
        }
        return isFull(c.n);
    }

    private static boolean center(B c, String face) {
        if (sturdy(c, face)) {
            return true;
        }
        String n = c.n;
        if (n.equals("chain")) {
            // solo la cadena vertical llega hasta el borde del bloque; una cadena acostada (axis=x o z) no sostiene nada
            return c.prop("axis", "y").equals("y") && (face.equals("up") || face.equals("down"));
        }
        if (n.endsWith("_wall") || n.endsWith("_fence") || n.equals("iron_bars") || n.endsWith("_pane") || n.endsWith("_fence_gate")) {
            return face.equals("up") || face.equals("down");
        }
        if (n.equals("lantern") || n.equals("soul_lantern")) {
            return face.equals("up") && !c.prop("hanging", "false").equals("true");
        }
        return (n.equals("end_rod") || n.equals("lightning_rod")) && (face.equals("up") || face.equals("down"));
    }

    private static boolean collisionFull(B c, String face) {
        return c.n.endsWith("_leaves") || sturdy(c, face);
    }

    private static boolean solidMaterial(B c) {
        String n = c.n;
        return !(n.endsWith("_carpet") || n.endsWith("_candle") || n.endsWith("_banner") || n.endsWith("_sign") || isBush(n) || n.equals("torch")
                || n.equals("wall_torch") || n.equals("vine") || n.equals("lantern") || n.equals("chain") || n.equals("moss_carpet"));
    }

    private final StructBp b;

    private Soporte(StructBp b) {
        this.b = b;
    }

    private B at(int x, int y, int z) {
        String s = b.get(x, y, z);
        if (s != null) {
            return parse(s);
        }
        if (y == -1) {
            return parse("grass_block");
        }
        if (y < -1) {
            return parse(y >= -4 ? "dirt" : "stone");
        }
        return AIR;
    }

    private B nb(int x, int y, int z, String dir) {
        int[] v = DIRV[di(dir)];
        return at(x + v[0], y + v[1], z + v[2]);
    }

    private boolean survives(int x, int y, int z, B c) {
        String n = c.n;
        B below = at(x, y - 1, z);
        B above = at(x, y + 1, z);
        switch (n) {
            case "torch", "soul_torch", "redstone_torch":
                return center(below, "up");
            case "wall_torch", "soul_wall_torch", "redstone_wall_torch": {
                String f = c.prop("facing", "north");
                return sturdy(nb(x, y, z, opp(f)), f);
            }
            case "lantern", "soul_lantern":
                return c.prop("hanging", "false").equals("true") ? center(above, "down") : center(below, "up");
            case "lily_pad":
                return below.n.equals("water") || below.n.equals("ice") || below.n.equals("frosted_ice");
            case "snow":
                if (below.n.equals("ice") || below.n.equals("packed_ice") || below.n.equals("barrier")) {
                    return false;
                }
                return collisionFull(below, "up");
            case "vine": {
                for (String f : new String[]{"north", "south", "east", "west", "up"}) {
                    if (c.prop(f, "false").equals("true")) {
                        if (collisionFull(nb(x, y, z, f), opp(f))) {
                            return true;
                        }
                        if (!f.equals("up") && above.n.equals("vine") && above.prop(f, "false").equals("true")) {
                            return true;
                        }
                    }
                }
                return false;
            }
            case "amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud": {
                String f = c.prop("facing", "up");
                return sturdy(nb(x, y, z, opp(f)), f);
            }
            case "ladder": {
                String f = c.prop("facing", "north");
                return sturdy(nb(x, y, z, opp(f)), f);
            }
            case "cake":
                return !below.n.equals("air") && !below.n.equals("water");
            case "brown_mushroom", "red_mushroom":
                return false; // necesitan oscuridad o podzol/micelio: no se pueden predecir, no se usan
            default:
                break;
        }
        if (n.endsWith("_carpet") || n.equals("moss_carpet")) {
            return !below.n.equals("air");
        }
        if (isBush(n)) {
            if (TALL.contains(n) && c.prop("half", "lower").equals("upper")) {
                return below.n.equals(n);
            }
            if (n.equals("azalea") || n.equals("flowering_azalea")) {
                return DIRT_TAG.contains(below.n) || below.n.equals("clay");
            }
            return DIRT_TAG.contains(below.n);
        }
        if (n.endsWith("_candle")) {
            return center(below, "up");
        }
        if (n.endsWith("_wall_banner")) {
            B back = nb(x, y, z, opp(c.prop("facing", "north")));
            return !back.n.equals("air") && !back.n.equals("water") && !back.n.equals("lava") && solidMaterial(back);
        }
        if (n.endsWith("_door")) {
            return c.prop("half", "lower").equals("upper") ? below.n.equals(n) : sturdy(below, "up");
        }
        if (n.endsWith("_pressure_plate")) {
            return center(below, "up");
        }
        if (n.endsWith("_button") || n.equals("lever")) {
            String face = c.prop("face", "wall");
            if (face.equals("floor")) {
                return sturdy(below, "up");
            }
            if (face.equals("ceiling")) {
                return sturdy(above, "down");
            }
            String f = c.prop("facing", "north");
            return sturdy(nb(x, y, z, opp(f)), f);
        }
        return true;
    }

    private static final String[] HOR = {"north", "south", "west", "east"};

    /**
     * Arregla en el plano lo que se puede arreglar sin cambiar el diseño: antorchas y estandartes sin pared detrás se giran hacia una
     * pared que sí exista (o la antorcha pasa a ir de pie), las plantas sobre piedra reciben una mancha de musgo debajo, las velas sobre
     * losas pasan a losa doble, las farolas se giran (colgando o de pie) y las enredaderas se pegan a la pared que tengan al lado.
     * Lo que no tenga arreglo lo quita {@link #prune}.
     */
    static void fix(StructBp bp) {
        Soporte s = new Soporte(bp);
        for (Map.Entry<Long, String> e : List.copyOf(bp.m.entrySet())) {
            String st = e.getValue();
            if (st.equals("air") || st.equals("water")) {
                continue;
            }
            long k = e.getKey();
            int x = StructBp.kx(k);
            int y = StructBp.ky(k);
            int z = StructBp.kz(k);
            B c = parse(st);
            if (s.survives(x, y, z, c)) {
                continue;
            }
            String n = c.n;
            if (n.equals("wall_torch") || n.equals("soul_wall_torch") || n.endsWith("_wall_banner")) {
                boolean torch = !n.endsWith("_banner");
                String cur = c.prop("facing", "north");
                for (String f : HOR) {
                    if (f.equals(cur)) {
                        continue;
                    }
                    B back = s.nb(x, y, z, opp(f));
                    if (torch ? sturdy(back, f) : (!back.n.equals("air") && !back.n.equals("water") && solidMaterial(back) && isFull(back.n))) {
                        bp.m.put(k, n + "[facing=" + f + "]");
                        break;
                    }
                }
                if (torch && s.survives(x, y, z, parse(bp.m.get(k))) == false && center(s.at(x, y - 1, z), "up")) {
                    bp.m.put(k, n.startsWith("soul") ? "soul_torch" : "torch");
                }
            } else if (isBush(n) && !(TALL.contains(n) && c.prop("half", "lower").equals("upper"))) {
                B below = s.at(x, y - 1, z);
                if (isFull(below.n) && !below.n.equals("air") && !below.n.equals("water") && !below.n.endsWith("_leaves")) {
                    bp.m.put(StructBp.key(x, y - 1, z), "moss_block");
                }
            } else if (n.endsWith("_candle")) {
                String below = bp.get(x, y - 1, z);
                if (below != null && below.contains("_slab") && !below.contains("double")) {
                    String nb2 = below.contains("[") ? below.replaceAll("type=(top|bottom)", "type=double") : below + "[type=double]";
                    if (!nb2.contains("type=")) {
                        nb2 = below.substring(0, below.indexOf('[')) + "[type=double," + below.substring(below.indexOf('[') + 1);
                    }
                    bp.m.put(StructBp.key(x, y - 1, z), nb2);
                }
            } else if (n.equals("lantern") || n.equals("soul_lantern")) {
                boolean hanging = c.prop("hanging", "false").equals("true");
                if (!hanging && center(s.at(x, y + 1, z), "down")) {
                    bp.m.put(k, n + "[hanging=true]");
                } else if (hanging && center(s.at(x, y - 1, z), "up")) {
                    bp.m.put(k, n + "[hanging=false]");
                }
            } else if (n.equals("vine")) {
                for (String f : HOR) {
                    if (collisionFull(s.nb(x, y, z, f), opp(f))) {
                        bp.m.put(k, "vine[" + f + "=true]");
                        break;
                    }
                }
            }
        }
        // cadenas que cuelgan sin llegar al techo: se alargan hasta él si hay uno cerca
        for (Map.Entry<Long, String> e : List.copyOf(bp.m.entrySet())) {
            if (!e.getValue().startsWith("chain") || e.getValue().contains("axis=x") || e.getValue().contains("axis=z")) {
                continue;
            }
            long k = e.getKey();
            int x = StructBp.kx(k);
            int y = StructBp.ky(k);
            int z = StructBp.kz(k);
            if (!s.at(x, y + 1, z).n.equals("air") || !s.at(x, y - 1, z).n.equals("lantern") && !s.at(x, y - 1, z).n.equals("soul_lantern")
                    && !s.at(x, y - 1, z).n.equals("chain")) {
                continue;
            }
            for (int up = 1; up <= 12; up++) {
                B t = s.at(x, y + up, z);
                if (!t.n.equals("air")) {
                    if (sturdy(t, "down") || center(t, "down")) {
                        for (int u = 1; u < up; u++) {
                            bp.m.put(StructBp.key(x, y + u, z), "chain[axis=y]");
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Quita del plano los bloques que no pueden sostenerse (repite por si al quitar uno otro se queda sin apoyo).
     * Devuelve cuántos se quitaron por tipo.
     */
    static Map<String, Integer> prune(StructBp bp) {
        Soporte s = new Soporte(bp);
        Map<String, Integer> by = new TreeMap<>();
        for (int pass = 0; pass < 6; pass++) {
            int changed = 0;
            for (Map.Entry<Long, String> e : List.copyOf(bp.m.entrySet())) {
                String st = e.getValue();
                if (st.equals("air") || st.equals("water")) {
                    continue;
                }
                long k = e.getKey();
                B c = parse(st);
                if (!s.survives(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k), c)) {
                    bp.m.put(k, "air");
                    by.merge(c.n, 1, Integer::sum);
                    changed++;
                }
            }
            if (changed == 0) {
                break;
            }
        }
        return by;
    }
}

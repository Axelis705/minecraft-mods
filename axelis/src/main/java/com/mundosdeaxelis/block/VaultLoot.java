package com.mundosdeaxelis.block;

import com.mundosdeaxelis.crate.CrateForge;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Botín de los cofres del santuario y de las estructuras (uno distinto para cada jugador que lo abre). Todo escala con la etapa:
 * los materiales de mobs y las llaves son los de la caja de esa etapa, y cada etapa tiene su propio sabor (etapa 268):
 * <ul>
 *     <li>1, bosque: miel, bayas, manzanas, polvo de hueso, bayas luminosas, regeneración y esmeraldas;</li>
 *     <li>2, forja: hierro, oro, carbón, velocidad y asados;</li>
 *     <li>3, hielo: lapislázuli, diamantes, salmón y pastel de calabaza, visión nocturna y catalejo;</li>
 *     <li>4, volcán: resistencia al fuego, blaze, crema de magma, oro, obsidiana y restos antiguos;</li>
 *     <li>5, magia: libros encantados, amatista, perlas de ender, regeneración, fuerza y algún tótem;</li>
 *     <li>6 a 8 (aún sin estructuras): diamantes, perlas, chatarra de netherita y manzanas encantadas.</li>
 * </ul>
 * Todos pueden traer <b>fragmentos de llave</b> de la caja de la etapa (cuatro se forjan en una llave en la Mesa del Artífice) y
 * <b>libros encantados</b>. No lleva nada que dependa de la suerte de otro jugador.
 */
public final class VaultLoot {
    private VaultLoot() {
    }

    public enum Kind {
        /** Provisiones de la antesala (antes del jefe) o de la entrada de una estructura. */
        ANTE,
        /** Premio de una prueba (saltos o runas) o de una sala apartada. */
        TRIAL,
        /** Cripta sellada (necesita llave): el mejor botín. */
        CRYPT,
        /** Cofre con cerradura (minijuego de ganzúa): mejor cuanto más difícil era la cerradura. */
        LOCKED,
        /** Gran tesoro del final de una estructura (etapa 268): casi tan bueno como la cripta, sin llave. */
        TREASURE
    }

    /** Fragmentos de llave que hacen falta para forjar una llave en la Mesa del Artífice. */
    public static final int FRAGMENTS_PER_KEY = 4;

    /** Esencia que recibe quien abre el cofre por primera vez. */
    public static int essence(Kind kind, int stage) {
        int base = switch (kind) {
            case ANTE -> 15;
            case TRIAL -> 30;
            case CRYPT -> 70;
            case LOCKED -> 40;
            case TREASURE -> 60;
        };
        return Math.round(base * (1.0F + 0.5F * (Math.max(1, stage) - 1)));
    }

    // ------------------------------------------------------------------ piezas sueltas

    private static ItemStack of(Item it, int n) {
        return new ItemStack(it, Math.max(1, n));
    }

    private static ItemStack potion(Potion p) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), p);
    }

    /** Con esa probabilidad (%), añade una decoración del mod que sale en la caja de la etapa (premio raro para decorar). */
    private static void addDecor(List<ItemStack> out, CrateKind crate, RandomSource r, int percent) {
        if (r.nextInt(100) >= percent) {
            return;
        }
        List<com.mundosdeaxelis.block.DecorDefs.Def> pool = new ArrayList<>();
        for (com.mundosdeaxelis.block.DecorDefs.Def d : com.mundosdeaxelis.block.DecorDefs.ALL) {
            if (d.dropsIn(crate.id)) {
                pool.add(d);
            }
        }
        if (pool.isEmpty()) {
            return;
        }
        com.mundosdeaxelis.block.DecorDefs.Def d = pool.get(r.nextInt(pool.size()));
        var item = ModItems.DECOR.get(d.id());
        if (item != null) {
            out.add(new ItemStack(item.get(), d.min() + r.nextInt(d.max() - d.min() + 1)));
        }
    }

    /** Objeto que repara el equipo de la caja de la etapa: la Bellota (bosque) o el Remache Ardiente (de la Forja en adelante). */
    private static ItemStack mender(int st, int n) {
        return of((st >= 2 ? ModItems.EMBER_RIVET : ModItems.ACORN).get(), n);
    }

    private static ItemStack goldenMender(int st) {
        return of((st >= 2 ? ModItems.GOLDEN_RIVET : ModItems.GOLDEN_ACORN).get(), 1);
    }

    /** Comida propia de la dungeon: Baya del Santuario (bosque) o Asado del Herrero (de la Forja en adelante). */
    private static ItemStack food(int st, int n) {
        return of((st >= 2 ? ModItems.FORGE_ROAST : ModItems.SANCTUARY_BERRY).get(), n);
    }

    /** Gelatina Brillante: el material que suelta el slime (lo que se pide a los cofres importantes). */
    private static ItemStack jelly(int n) {
        return of(ModItems.MATS.get(com.mundosdeaxelis.table.Mats.SHINY_JELLY).get(), n);
    }

    /** Fragmentos de llave de la caja de la etapa. */
    private static ItemStack fragments(CrateKind crate, int n) {
        return of(ModItems.KEY_FRAGMENTS.get(crate).get(), n);
    }

    /** Un libro encantado: más fuerte cuanto más alta la etapa (desde el volcán puede traer encantamientos de tesoro). */
    private static ItemStack book(RandomSource r, int st, int bonus) {
        int lvl = Math.max(1, Math.min(30, 3 + 3 * st + bonus + r.nextInt(5)));
        return EnchantmentHelper.enchantItem(r, new ItemStack(Items.BOOK), lvl, st >= 4);
    }

    /** Las «monedas» de la etapa: esmeraldas; en la Forja y el volcán, oro (la mitad de lingotes). */
    private static ItemStack coins(int st, int n) {
        return st == 2 || st == 4 ? of(Items.GOLD_INGOT, Math.max(1, n / 2)) : of(Items.EMERALD, n);
    }

    /** Comida buena de viaje: zanahorias doradas; en la Forja y el volcán, cerdo asado. */
    private static ItemStack travel(int st, int n) {
        return st == 2 || st == 4 ? of(Items.COOKED_PORKCHOP, n) : of(Items.GOLDEN_CARROT, n);
    }

    /** Manzanas doradas; en la Forja y el volcán, el Asado del Herrero. */
    private static ItemStack apples(int st, int n) {
        return st == 2 || st == 4 ? food(st, n) : of(Items.GOLDEN_APPLE, n);
    }

    /** La carne de las provisiones de cada etapa. */
    private static ItemStack meal(int st, int n) {
        return switch (st) {
            case 1 -> of(Items.COOKED_BEEF, n);
            case 2, 4 -> of(Items.COOKED_PORKCHOP, n);
            case 3 -> of(Items.COOKED_SALMON, n);
            case 5 -> of(Items.COOKED_CHICKEN, n);
            default -> of(Items.COOKED_BEEF, n);
        };
    }

    // ------------------------------------------------------------------ reparto

    public static List<ItemStack> roll(Kind kind, int stage, RandomSource r) {
        return roll(kind, stage, r, 1);
    }

    public static List<ItemStack> roll(Kind kind, int stage, RandomSource r, int lockLevel) {
        return scatter(rollRaw(kind, stage, r, lockLevel), r);
    }

    /**
     * Reparte el botín: las pilas grandes se parten en varias más pequeñas (así el cofre se ve lleno y variado en vez de traer
     * todo junto en una pila). Nunca pasa de 24 pilas (el cofre tiene 27 huecos).
     */
    private static List<ItemStack> scatter(List<ItemStack> in, RandomSource r) {
        List<ItemStack> out = new ArrayList<>(in);
        for (ItemStack st : in) {
            if (out.size() >= 24 || st.getMaxStackSize() <= 1 || st.getCount() < 2) {
                continue;
            }
            int parts = Math.min(st.getCount(), st.getCount() >= 8 ? 3 : 2);
            out.remove(st);
            int left = st.getCount();
            for (int i = 0; i < parts; i++) {
                int n = i == parts - 1 ? left : Math.max(1, Math.min(left - (parts - 1 - i), left / (parts - i) + r.nextInt(3) - 1));
                ItemStack piece = st.copy();
                piece.setCount(n);
                out.add(piece);
                left -= n;
            }
        }
        while (out.size() > 27) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private static List<ItemStack> rollRaw(Kind kind, int stage, RandomSource r, int lockLevel) {
        int lv = Math.max(1, Math.min(5, lockLevel));
        int st = Math.max(1, Math.min(8, stage));
        CrateKind crate = CrateKind.byIndex(st);
        List<ItemStack> out = new ArrayList<>();
        switch (kind) {
            case ANTE -> {
                out.add(apples(st, 2));
                out.add(meal(st, 12));
                out.add(food(st, 2));
                out.add(of(Items.EXPERIENCE_BOTTLE, 8));
                out.add(potion(Potions.STRONG_HEALING));
                out.add(of(Items.TORCH, 12 + r.nextInt(9)));
                out.add(mender(st, 1 + r.nextInt(2)));
                if (r.nextInt(100) < 25) {
                    out.add(fragments(crate, 1));
                }
            }
            case TRIAL -> {
                for (CrateForge.Need n : CrateForge.mats(crate)) {
                    out.add(of(ModItems.MATS.get(n.mat()).get(), 1 + r.nextInt(2)));
                }
                out.add(coins(st, 4 + 2 * st + r.nextInt(4)));
                out.add(of(Items.EXPERIENCE_BOTTLE, 6));
                out.add(apples(st, 1));
                out.add(food(st, 1));
                out.add(travel(st, 4 + r.nextInt(3)));
                if (r.nextInt(100) < 30) {
                    out.add(mender(st, 1));
                }
                if (r.nextInt(100) < 50) {
                    out.add(fragments(crate, 1));
                }
                if (r.nextInt(100) < 25) {
                    out.add(book(r, st, 0));
                }
                addDecor(out, crate, r, 6);
            }
            case LOCKED -> {
                for (CrateForge.Need n : CrateForge.mats(crate)) {
                    out.add(of(ModItems.MATS.get(n.mat()).get(), 1 + (lv >= 3 ? 1 : 0) + (lv >= 5 && r.nextBoolean() ? 1 : 0)));
                }
                out.add(travel(st, 3 + 2 * lv));
                out.add(coins(st, 4 * lv + r.nextInt(5)));
                out.add(apples(st, 1 + lv / 3));
                out.add(food(st, 1));
                out.add(jelly(1 + r.nextInt(2) + lv / 2));
                if (r.nextInt(100) < 8 * lv) {
                    out.add(mender(st, 1));
                }
                // cuanto más difícil la cerradura, mejor premio: reparador dorado desde el nivel 4 y decoración rara del mod
                if (lv >= 4 && r.nextInt(100) < 16 * (lv - 3)) {
                    out.add(goldenMender(st));
                }
                addDecor(out, crate, r, 4 * lv);
                out.add(of(Items.EXPERIENCE_BOTTLE, 4 + 2 * lv));
                // fragmentos de llave: uno seguro desde el nivel 3, dos en el 5; a veces una llave entera
                int frag = lv >= 5 ? 2 : lv >= 3 ? 1 : r.nextInt(100) < 30 * lv ? 1 : 0;
                if (frag > 0) {
                    out.add(fragments(crate, frag));
                }
                if (r.nextInt(100) < 12 * lv) {
                    out.add(book(r, st, 2 * lv));
                }
                if (r.nextInt(100) < 6 * lv) {
                    out.add(of(ModItems.KEYS.get(crate).get(), 1));
                }
            }
            case TREASURE -> {
                for (CrateForge.Need n : CrateForge.mats(crate)) {
                    out.add(of(ModItems.MATS.get(n.mat()).get(), 2 + r.nextInt(2)));
                }
                out.add(fragments(crate, 2 + (r.nextInt(100) < 35 ? 1 : 0)));
                out.add(coins(st, 8 + 4 * st + r.nextInt(5)));
                out.add(travel(st, 6 + r.nextInt(3)));
                out.add(apples(st, 2));
                out.add(of(Items.EXPERIENCE_BOTTLE, 12));
                out.add(jelly(2 + r.nextInt(3)));
                out.add(mender(st, 1 + r.nextInt(2)));
                out.add(book(r, st, 8));
                if (r.nextInt(100) < 40) {
                    out.add(book(r, st, 4));
                }
                if (r.nextInt(100) < 45) {
                    out.add(goldenMender(st));
                }
                if (r.nextInt(100) < 20) {
                    out.add(of(ModItems.KEYS.get(crate).get(), 1));
                }
                if (st >= 4 && r.nextInt(100) < 4 * st) {
                    out.add(of(Items.ENCHANTED_GOLDEN_APPLE, 1));
                }
                addDecor(out, crate, r, 35);
            }
            case CRYPT -> {
                out.add(of(ModItems.KEYS.get(crate).get(), 1));
                for (CrateForge.Need n : CrateForge.mats(crate)) {
                    out.add(of(ModItems.MATS.get(n.mat()).get(), 2 + r.nextInt(2)));
                }
                out.add(fragments(crate, 1 + r.nextInt(2)));
                out.add(coins(st, 8 + 4 * st + r.nextInt(5)));
                out.add(travel(st, 8));
                out.add(apples(st, 2));
                out.add(of(Items.EXPERIENCE_BOTTLE, 14));
                out.add(jelly(3 + r.nextInt(3)));
                out.add(mender(st, 1 + r.nextInt(2)));
                out.add(book(r, st, 6));
                if (r.nextInt(100) < 55) {
                    out.add(goldenMender(st));
                }
                addDecor(out, crate, r, 45);
            }
        }
        flavor(out, kind, st, lv, r);
        return out;
    }

    // ------------------------------------------------------------------ sabor de cada etapa

    /** Cuánto «pesa» el cofre para el sabor: 0 provisiones, 1 prueba, 1..3 cerradura (según el nivel), 3 tesoro y cripta. */
    private static int weight(Kind kind, int lv) {
        return switch (kind) {
            case ANTE -> 0;
            case TRIAL -> 1;
            case LOCKED -> lv >= 5 ? 3 : lv >= 3 ? 2 : 1;
            case TREASURE, CRYPT -> 3;
        };
    }

    private static void flavor(List<ItemStack> out, Kind kind, int st, int lv, RandomSource r) {
        int w = weight(kind, lv);
        switch (st) {
            case 1 -> forest(out, kind, w, r);
            case 2 -> forge(out, kind, w, lv, r);
            case 3 -> ice(out, kind, w, r);
            case 4 -> volcano(out, kind, lv, r);
            case 5 -> magic(out, kind, w, r);
            default -> high(out, kind, w, st, r);
        }
    }

    /** Bosque (etapa 1): miel, bayas dulces, manzanas, polvo de hueso, bayas luminosas, regeneración y algún bloque de esmeralda. */
    private static void forest(List<ItemStack> out, Kind kind, int w, RandomSource r) {
        if (w == 0) {
            out.add(of(Items.HONEY_BOTTLE, 2));
            out.add(of(Items.SWEET_BERRIES, 6));
            return;
        }
        out.add(of(Items.SWEET_BERRIES, 4 + 2 * w));
        out.add(of(Items.APPLE, 2 + w + r.nextInt(3)));
        out.add(of(Items.BONE_MEAL, 4 + 2 * w));
        if (r.nextInt(100) < 50) {
            out.add(of(Items.HONEY_BOTTLE, 1 + w / 2));
        }
        if (w >= 2) {
            out.add(of(Items.GLOW_BERRIES, 4 + r.nextInt(5)));
            out.add(potion(w >= 3 ? Potions.LONG_REGENERATION : Potions.REGENERATION));
        }
        if (w >= 3 && r.nextInt(100) < 35) {
            out.add(of(Items.EMERALD_BLOCK, 1));
        }
    }

    /** Forja (etapa 2): metal, carbón, poción de velocidad. */
    private static void forge(List<ItemStack> out, Kind kind, int w, int lv, RandomSource r) {
        switch (kind) {
            case ANTE -> {
                out.add(potion(Potions.LONG_SWIFTNESS));
                out.add(of(Items.COAL, 8));
            }
            case TRIAL -> {
                out.add(of(Items.IRON_INGOT, 4 + r.nextInt(5)));
                if (r.nextInt(100) < 30) {
                    out.add(of(Items.GOLD_INGOT, 1 + r.nextInt(2)));
                }
            }
            case LOCKED -> {
                out.add(of(Items.IRON_INGOT, 2 * lv + r.nextInt(4)));
                if (lv >= 3) {
                    out.add(of(Items.GOLD_INGOT, lv - 1 + r.nextInt(3)));
                }
                if (lv >= 4 && r.nextInt(100) < 35) {
                    out.add(potion(Potions.LONG_SWIFTNESS));
                }
            }
            case TREASURE, CRYPT -> {
                out.add(of(Items.IRON_INGOT, 10 + r.nextInt(7)));
                out.add(of(Items.GOLD_INGOT, 2 + r.nextInt(3)));
                out.add(of(Items.COAL_BLOCK, 1 + r.nextInt(2)));
                out.add(potion(Potions.LONG_SWIFTNESS));
                if (kind == Kind.TREASURE && r.nextInt(100) < 40) {
                    out.add(of(Items.IRON_BLOCK, 1));
                }
            }
        }
    }

    /** Hielo (etapa 3): lapislázuli, diamantes, pastel de calabaza, visión nocturna (las mazmorras son oscuras) y catalejo. */
    private static void ice(List<ItemStack> out, Kind kind, int w, RandomSource r) {
        if (w == 0) {
            out.add(potion(Potions.LONG_NIGHT_VISION));
            out.add(of(Items.PUMPKIN_PIE, 4));
            return;
        }
        out.add(of(Items.LAPIS_LAZULI, 4 + 3 * w + r.nextInt(4)));
        int dia = w == 1 ? (r.nextInt(100) < 40 ? 1 : 0) : w == 2 ? 1 + r.nextInt(2) : 2 + r.nextInt(2);
        if (dia > 0) {
            out.add(of(Items.DIAMOND, dia));
        }
        out.add(of(Items.PUMPKIN_PIE, 2 + w));
        if (w >= 2) {
            out.add(potion(Potions.LONG_NIGHT_VISION));
            out.add(of(Items.AMETHYST_SHARD, 3 + r.nextInt(4)));
        }
        if (w >= 3) {
            out.add(of(Items.LAPIS_BLOCK, 1));
            if (r.nextInt(100) < 30) {
                out.add(of(Items.SPYGLASS, 1));
            }
        }
    }

    /** Volcán (etapa 4): resistencia al fuego, polvo de blaze, crema de magma, oro y obsidiana. */
    private static void volcano(List<ItemStack> out, Kind kind, int lv, RandomSource r) {
        switch (kind) {
            case ANTE -> {
                out.add(potion(Potions.LONG_FIRE_RESISTANCE));
                out.add(of(Items.MAGMA_CREAM, 2 + r.nextInt(3)));
            }
            case TRIAL -> {
                out.add(of(Items.BLAZE_POWDER, 3 + r.nextInt(4)));
                out.add(of(Items.GOLD_INGOT, 3 + r.nextInt(4)));
                if (r.nextInt(100) < 40) {
                    out.add(potion(Potions.FIRE_RESISTANCE));
                }
            }
            case LOCKED -> {
                out.add(of(Items.GOLD_INGOT, 2 * lv + r.nextInt(4)));
                out.add(of(Items.OBSIDIAN, 2 + lv));
                if (lv >= 3) {
                    out.add(of(Items.BLAZE_ROD, lv - 1 + r.nextInt(2)));
                }
                if (lv >= 5) {
                    out.add(of(Items.ANCIENT_DEBRIS, 1));
                }
            }
            case TREASURE, CRYPT -> {
                out.add(of(Items.GOLD_BLOCK, 1 + r.nextInt(2)));
                out.add(of(Items.BLAZE_ROD, 3 + r.nextInt(3)));
                out.add(of(Items.MAGMA_CREAM, 4 + r.nextInt(4)));
                out.add(potion(Potions.LONG_FIRE_RESISTANCE));
                if (r.nextInt(100) < 35) {
                    out.add(of(Items.ANCIENT_DEBRIS, 1));
                }
            }
        }
    }

    /** Magia (etapa 5): más libros, amatista, lapislázuli, perlas de ender, galletas de la suerte, regeneración, fuerza y tótems. */
    private static void magic(List<ItemStack> out, Kind kind, int w, RandomSource r) {
        if (w == 0) {
            out.add(potion(Potions.LONG_REGENERATION));
            out.add(of(ModItems.FORTUNE_COOKIE.get(), 2));
            return;
        }
        out.add(of(Items.AMETHYST_SHARD, 4 + 2 * w + r.nextInt(4)));
        out.add(of(Items.LAPIS_LAZULI, 3 + 2 * w));
        out.add(book(r, 5, 2 * w));
        if (r.nextInt(100) < 40 + 20 * w) {
            out.add(of(Items.ENDER_PEARL, w + r.nextInt(2)));
        }
        if (w >= 2) {
            out.add(of(Items.GLOWSTONE_DUST, 6 + r.nextInt(6)));
            out.add(potion(w >= 3 ? Potions.STRONG_STRENGTH : Potions.STRENGTH));
        }
        if (w >= 3) {
            out.add(of(Items.EXPERIENCE_BOTTLE, 8));
            out.add(potion(Potions.LONG_INVISIBILITY));
            if (r.nextInt(100) < 25) {
                out.add(of(Items.TOTEM_OF_UNDYING, 1));
            }
        }
    }

    /** Etapas 6 a 8 (aún sin estructuras propias): diamantes, perlas de ender, chatarra de netherita y manzanas encantadas. */
    private static void high(List<ItemStack> out, Kind kind, int w, int st, RandomSource r) {
        if (w == 0) {
            out.add(potion(Potions.STRONG_REGENERATION));
            out.add(of(Items.GOLDEN_APPLE, 1));
            return;
        }
        out.add(of(Items.DIAMOND, w + r.nextInt(2)));
        out.add(of(Items.ENDER_PEARL, 1 + w));
        if (w >= 2 && r.nextInt(100) < 15 * w + 5 * (st - 6)) {
            out.add(of(Items.NETHERITE_SCRAP, 1));
        }
        if (w >= 3 && r.nextInt(100) < 20) {
            out.add(of(Items.ENCHANTED_GOLDEN_APPLE, 1));
        }
    }
}

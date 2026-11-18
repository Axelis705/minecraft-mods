package com.mundosdeaxelis.block;

import com.mundosdeaxelis.crate.CrateForge;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;

/**
 * Botín de los cofres del santuario (uno distinto para cada jugador que lo abre). Todo escala con la etapa: los materiales
 * (pocos por cofre: la dungeon se repite para farmearlos; ver también las salas despejadas) son los de la caja de esa etapa y las llaves, las de su caja. No lleva nada que dependa de la suerte de otro jugador.
 */
public final class VaultLoot {
    private VaultLoot() {
    }

    public enum Kind {
        /** Provisiones de la antesala (antes del jefe). */
        ANTE,
        /** Premio de una prueba (saltos o runas). */
        TRIAL,
        /** Cripta sellada (necesita llave): el mejor botín. */
        CRYPT,
        /** Cofre con cerradura (minijuego de ganzúa): mejor cuanto más difícil era la cerradura. */
        LOCKED
    }

    /** Esencia que recibe quien abre el cofre por primera vez. */
    public static int essence(Kind kind, int stage) {
        int base = switch (kind) {
            case ANTE -> 15;
            case TRIAL -> 30;
            case CRYPT -> 70;
            case LOCKED -> 40;
        };
        return Math.round(base * (1.0F + 0.5F * (Math.max(1, stage) - 1)));
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

    /** Objeto que repara el equipo de la caja de la etapa: la Bellota (bosque) o el Remache Ardiente (Forja). */
    private static ItemStack mender(int st, int n) {
        return new ItemStack((st == 2 ? ModItems.EMBER_RIVET : ModItems.ACORN).get(), n);
    }

    private static ItemStack goldenMender(int st) {
        return new ItemStack((st == 2 ? ModItems.GOLDEN_RIVET : ModItems.GOLDEN_ACORN).get(), 1);
    }

    /** Comida propia de la dungeon: Baya del Santuario (bosque) o Asado del Herrero (Forja). */
    private static ItemStack food(int st, int n) {
        return new ItemStack((st == 2 ? ModItems.FORGE_ROAST : ModItems.SANCTUARY_BERRY).get(), n);
    }

    /** Gelatina Brillante: el material que suelta el slime (lo que se pide a los cofres importantes). */
    private static ItemStack jelly(int n) {
        return new ItemStack(ModItems.MATS.get(com.mundosdeaxelis.table.Mats.SHINY_JELLY).get(), Math.max(1, n));
    }

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
        return out;
    }


    /**
     * Cambia lo que era del bosque (miel, bayas dulces, zanahorias doradas, esmeraldas, manzanas doradas) por cosas de la Forja:
     * lingotes de oro, cerdo asado y Asado del Herrero.
     */
    private static void forgeSwap(List<ItemStack> out, int st) {
        List<ItemStack> res = new ArrayList<>();
        for (ItemStack it : out) {
            if (it.is(Items.HONEY_BOTTLE) || it.is(Items.SWEET_BERRIES)) {
                continue;
            }
            if (it.is(Items.EMERALD)) {
                res.add(new ItemStack(Items.GOLD_INGOT, Math.max(1, it.getCount() / 2)));
            } else if (it.is(Items.GOLDEN_CARROT)) {
                res.add(new ItemStack(Items.COOKED_PORKCHOP, it.getCount()));
            } else if (it.is(Items.GOLDEN_APPLE)) {
                res.add(food(st, it.getCount()));
            } else {
                res.add(it);
            }
        }
        out.clear();
        out.addAll(res);
    }

    /** Sabor propio de cada etapa (el de la 1 es el bosque: bayas dulces). Etapa 2 (Forja): metal, carbón y poción de velocidad. */
    private static void flavor(List<ItemStack> out, Kind kind, int st, int lv, RandomSource r) {
        if (st != 2) {
            return;
        }
        switch (kind) {
            case ANTE -> {
                out.add(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.LONG_SWIFTNESS));
                out.add(new ItemStack(Items.COAL, 8));
            }
            case TRIAL -> {
                out.add(new ItemStack(Items.IRON_INGOT, 4 + r.nextInt(5)));
                if (r.nextInt(100) < 30) {
                    out.add(new ItemStack(Items.GOLD_INGOT, 1 + r.nextInt(2)));
                }
            }
            case LOCKED -> {
                out.add(new ItemStack(Items.IRON_INGOT, 2 * lv + r.nextInt(4)));
                if (lv >= 3) {
                    out.add(new ItemStack(Items.GOLD_INGOT, lv - 1 + r.nextInt(3)));
                }
                if (lv >= 4 && r.nextInt(100) < 35) {
                    out.add(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.LONG_SWIFTNESS));
                }
            }
            case CRYPT -> {
                out.add(new ItemStack(Items.IRON_INGOT, 10 + r.nextInt(7)));
                out.add(new ItemStack(Items.GOLD_INGOT, 2 + r.nextInt(3)));
                out.add(new ItemStack(Items.COAL_BLOCK, 1 + r.nextInt(2)));
                out.add(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.LONG_SWIFTNESS));
            }
        }
    }

    private static List<ItemStack> rollRaw(Kind kind, int stage, RandomSource r, int lockLevel) {
        int lv = Math.max(1, Math.min(5, lockLevel));
        int st = Math.max(1, Math.min(8, stage));
        CrateKind crate = CrateKind.byIndex(st);
        List<ItemStack> out = new ArrayList<>();
        switch (kind) {
            case ANTE -> {
                out.add(new ItemStack(Items.GOLDEN_APPLE, 2));
                out.add(new ItemStack(Items.COOKED_BEEF, 12));
                out.add(food(st, 2));
                out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 8));
                out.add(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
                out.add(new ItemStack(Items.HONEY_BOTTLE, 2));
                out.add(st == 1 ? new ItemStack(Items.SWEET_BERRIES, 6) : new ItemStack(Items.COOKED_PORKCHOP, 8)); // etapa 2: carne asada
                out.add(mender(st, 1 + r.nextInt(2)));
            }
            case TRIAL -> {
                for (CrateForge.Need n : CrateForge.mats(crate)) {
                    out.add(new ItemStack(ModItems.MATS.get(n.mat()).get(), 1 + r.nextInt(2)));
                }
                out.add(new ItemStack(Items.EMERALD, 4 + 2 * st + r.nextInt(4)));
                out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 6));
                out.add(new ItemStack(Items.GOLDEN_APPLE, 1));
                out.add(food(st, 1));
                if (r.nextInt(100) < 50) {
                    out.add(new ItemStack(Items.HONEY_BOTTLE, 2));
                }
                if (st == 1 && r.nextInt(100) < 50) {
                    out.add(new ItemStack(Items.SWEET_BERRIES, 6));
                }
                out.add(new ItemStack(Items.GOLDEN_CARROT, 4 + r.nextInt(3)));
                if (r.nextInt(100) < 30) {
                    out.add(mender(st, 1));
                }
                addDecor(out, crate, r, 6);
            }
            case LOCKED -> {
                for (CrateForge.Need n : CrateForge.mats(crate)) {
                    out.add(new ItemStack(ModItems.MATS.get(n.mat()).get(), 1 + (lv >= 3 ? 1 : 0) + (lv >= 5 && r.nextBoolean() ? 1 : 0)));
                }
                out.add(new ItemStack(Items.GOLDEN_CARROT, 3 + 2 * lv));
                out.add(new ItemStack(Items.EMERALD, 4 * lv + r.nextInt(5)));
                out.add(new ItemStack(Items.GOLDEN_APPLE, 1 + lv / 3));
                out.add(food(st, 1));
                out.add(jelly(1 + r.nextInt(2) + lv / 2));
                if (r.nextInt(100) < 8 * lv) {
                    out.add(mender(st, 1));
                }
                // cuanto más difícil la cerradura, mejor premio: bellota dorada desde el nivel 4 y decoración rara del mod
                if (lv >= 4 && r.nextInt(100) < 16 * (lv - 3)) {
                    out.add(goldenMender(st));
                }
                addDecor(out, crate, r, 4 * lv);
                out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 4 + 2 * lv));
                if (r.nextInt(100) < 50) {
                    out.add(new ItemStack(Items.HONEY_BOTTLE, 2));
                }
                if (st == 1 && r.nextInt(100) < 50) {
                    out.add(new ItemStack(Items.SWEET_BERRIES, 8));
                }
                if (r.nextInt(100) < 6 * lv) {
                    out.add(new ItemStack(ModItems.KEYS.get(crate).get(), 1));
                }
            }
            case CRYPT -> {
                out.add(new ItemStack(ModItems.KEYS.get(crate).get(), 1));
                for (CrateForge.Need n : CrateForge.mats(crate)) {
                    out.add(new ItemStack(ModItems.MATS.get(n.mat()).get(), 2 + r.nextInt(2)));
                }
                out.add(new ItemStack(Items.EMERALD, 8 + 4 * st + r.nextInt(5)));
                out.add(new ItemStack(Items.GOLDEN_CARROT, 8));
                out.add(new ItemStack(Items.GOLDEN_APPLE, 2));
                out.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 14));
                out.add(jelly(3 + r.nextInt(3)));
                out.add(mender(st, 1 + r.nextInt(2)));
                if (r.nextInt(100) < 55) {
                    out.add(goldenMender(st));
                }
                addDecor(out, crate, r, 45);
            }
        }
        if (st == 2) {
            forgeSwap(out, st);
        }
        flavor(out, kind, st, lv, r);
        return out;
    }
}

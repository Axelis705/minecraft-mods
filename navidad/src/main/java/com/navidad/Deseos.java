package com.navidad;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Lo que Santa entiende de una carta: busca palabras de cosas que se pueden pedir (en español y en inglés, con o sin acentos, en
 * singular o plural) y por cada una prepara un regalo. Lo que es "muy difícil" (élitros, netherita...) no lo trae, pero manda una
 * sorpresa; lo que no cabe en una caja (paz, amor, familia...) lo contesta con cariño y un regalo de los buenos.
 */
public final class Deseos {
    /** Lo más que trae Santa por carta. */
    public static final int MAX = 3;

    public enum Tipo {
        NORMAL, DIFICIL, BONITO
    }

    public record Deseo(String clave, Tipo tipo, List<ItemStack> cosas) {
    }

    private record Entrada(String clave, Tipo tipo, String[][] palabras, Function<RandomSource, List<ItemStack>> dar) {
    }

    private static final List<Entrada> TODAS = new ArrayList<>();

    private Deseos() {
    }

    private static void e(String clave, String llaves, Function<RandomSource, List<ItemStack>> dar) {
        e(clave, Tipo.NORMAL, llaves, dar);
    }

    private static void e(String clave, Tipo tipo, String llaves, Function<RandomSource, List<ItemStack>> dar) {
        String[] ks = llaves.split(",");
        String[][] p = new String[ks.length][];
        for (int i = 0; i < ks.length; i++) {
            p[i] = ks[i].trim().split(" ");
        }
        TODAS.add(new Entrada(clave, tipo, p, dar));
    }

    private static List<ItemStack> uno(Item it, int n) {
        return List.of(new ItemStack(it, n));
    }

    private static Item nv(String id) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Navidad.MODID, id));
        return it == null ? Items.PAPER : it;
    }

    private static List<ItemStack> encantado(Item it, Enchantment en, int nivel) {
        ItemStack s = new ItemStack(it);
        s.enchant(en, nivel);
        return List.of(s);
    }

    static {
        // primero las de varias palabras (para que "manzana dorada" no se vuelva solo "manzana")
        e("manzana_dorada", "manzana dorada, manzanas doradas, golden apple", r -> uno(Items.GOLDEN_APPLE, 2));
        e("fuegos", "fuegos artificiales, cohete, cohetes, firework, fireworks, pirotecnia, juegos artificiales",
                r -> List.of(withCount(com.navidad.AnioNuevo.cohete(r), 16)));
        e("diamante", "diamante, diamond", r -> uno(Items.DIAMOND, 1 + r.nextInt(2)));
        e("esmeralda", "esmeralda, emerald", r -> uno(Items.EMERALD, 6 + r.nextInt(5)));
        e("oro", "oro, gold", r -> uno(Items.GOLD_INGOT, 5 + r.nextInt(4)));
        e("hierro", "hierro, iron", r -> uno(Items.IRON_INGOT, 10 + r.nextInt(7)));
        e("espada", "espada, sword", r -> encantado(Items.IRON_SWORD, Enchantments.SHARPNESS, 2));
        e("pico", "pico, pickaxe, picota", r -> encantado(Items.IRON_PICKAXE, Enchantments.BLOCK_EFFICIENCY, 2));
        e("hacha", "hacha, axe", r -> encantado(Items.IRON_AXE, Enchantments.BLOCK_EFFICIENCY, 1));
        e("pala", "pala, shovel", r -> encantado(Items.IRON_SHOVEL, Enchantments.BLOCK_EFFICIENCY, 2));
        e("arco", "arco, bow", r -> List.of(encantado(Items.BOW, Enchantments.POWER_ARROWS, 1).get(0), new ItemStack(Items.ARROW, 32)));
        e("escudo", "escudo, shield", r -> uno(Items.SHIELD, 1));
        e("armadura", "armadura, armor, armour, peto, pechera, chestplate", r -> encantado(Items.IRON_CHESTPLATE, Enchantments.ALL_DAMAGE_PROTECTION, 1));
        e("casco", "casco, helmet", r -> uno(Items.IRON_HELMET, 1));
        e("botas", "bota, botas, boots", r -> encantado(Items.IRON_BOOTS, Enchantments.FALL_PROTECTION, 2));
        e("pastel", "pastel, torta, tarta, cake", r -> uno(Items.CAKE, 1));
        e("galletas", "galleta, galletita, cookie", r -> uno(nv("galleta_jengibre"), 12));
        e("dulces", "dulce, caramelo, golosina, candy, sweets", r -> uno(nv("dulce"), 16));
        e("chocolate", "chocolate", r -> uno(nv("chocolate_caliente"), 3));
        e("tamales", "tamal, tamale", r -> uno(nv("tamal"), 6));
        e("comida", "comida, food, pavo, turkey", r -> uno(nv("pavo_navideno"), 2));
        e("perro", "perro, perrito, lobo, lobito, dog, puppy, wolf", r -> List.of(new ItemStack(Items.WOLF_SPAWN_EGG), new ItemStack(Items.BONE, 8)));
        e("gato", "gato, gatito, michi, cat, kitten", r -> List.of(new ItemStack(Items.CAT_SPAWN_EGG), new ItemStack(Items.COD, 8)));
        e("caballo", "caballo, caballito, poni, pony, horse", r -> List.of(new ItemStack(Items.HORSE_SPAWN_EGG), new ItemStack(Items.SADDLE)));
        e("reno", "reno, reindeer", r -> uno(nv("huevo_reno"), 1));
        e("trineo", "trineo, sled, sleigh", r -> uno(nv("trineo_madera"), 1));
        e("patines", "patin, patines, skate, skates", r -> uno(nv("patines"), 1));
        e("sueter", "sueter, sweater, jersey", r -> uno(nv(new String[]{"sueter_reno", "sueter_pinos", "sueter_copos"}[r.nextInt(3)]), 1));
        e("gorro", "gorro, hat", r -> uno(nv("gorro_santa"), 1));
        e("disco", "disco, musica, music, villancico, cancion, song", r -> uno(nv(new String[]{"disco_campanas", "disco_navidad", "disco_noche_paz",
                "disco_adornen"}[r.nextInt(4)]), 1));
        e("libro", "libro, book, encantamiento, enchantment", r -> List.of(libro(r)));
        e("nieve", "nieve, snow, bola de nieve, snowball", r -> uno(Items.SNOWBALL, 16));
        e("bengala", "bengala, sparkler", r -> uno(nv("bengala"), 8));
        e("pinata", "pinata", r -> uno(nv("pinata"), 1));
        e("telescopio", "telescopio, catalejo, spyglass", r -> uno(Items.SPYGLASS, 1));
        e("brujula", "brujula, compass", r -> uno(Items.COMPASS, 1));
        e("mapa", "mapa, map", r -> uno(Items.MAP, 1));
        e("flores", "flor, flower, rosa", r -> uno(new Item[]{Items.POPPY, Items.CORNFLOWER, Items.ALLIUM, Items.OXEYE_DAISY}[r.nextInt(4)], 8));
        e("juguete", "juguete, toy, regalo, regalos, sorpresa, gift, present", r -> uno(nv("regalo_misterioso"), 1));
        // muy difícil hasta para los duendes
        e("dificil", Tipo.DIFICIL, "elytra, elitro, elitros, ala, alas, wing, wings, netherite, netherita, tridente, trident, faro, beacon, "
                + "estrella del nether, nether star, dragon, wither, totem", r -> uno(nv("regalo_misterioso"), 2));
        // lo que no cabe en una caja
        e("bonito", Tipo.BONITO, "paz, amor, familia, amigo, amiga, salud, felicidad, peace, love, family, friend, health, happiness",
                r -> uno(nv("regalo_misterioso"), 3));
    }

    private static ItemStack withCount(ItemStack s, int n) {
        s.setCount(n);
        return s;
    }

    private static ItemStack libro(RandomSource r) {
        Enchantment[] es = {Enchantments.UNBREAKING, Enchantments.BLOCK_EFFICIENCY, Enchantments.ALL_DAMAGE_PROTECTION, Enchantments.SHARPNESS,
                Enchantments.BLOCK_FORTUNE, Enchantments.MENDING};
        Enchantment en = r.nextInt(8) == 0 ? Enchantments.MENDING : es[r.nextInt(es.length - 1)];
        int nivel = en == Enchantments.MENDING ? 1 : 1 + r.nextInt(Math.min(3, en.getMaxLevel()));
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(en, nivel));
    }

    /** Las palabras de la carta, sin acentos ni signos, en minúsculas. */
    public static List<String> palabras(String texto) {
        String t = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        t = t.replaceAll("[^a-z0-9]+", " ").trim();
        List<String> out = new ArrayList<>();
        for (String w : t.split(" ")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    private static boolean igual(String w, String k) {
        return w.equals(k) || w.equals(k + "s") || w.equals(k + "es") || (k.endsWith("z") && w.equals(k.substring(0, k.length() - 1) + "ces"));
    }

    /** Los deseos de la carta, en el orden en que aparecen (sin repetir, a lo más {@link #MAX}). */
    public static List<Deseo> buscar(String texto, RandomSource r) {
        List<String> ws = palabras(texto);
        List<Deseo> out = new ArrayList<>();
        List<String> ya = new ArrayList<>();
        for (int i = 0; i < ws.size() && out.size() < MAX; i++) {
            for (Entrada en : TODAS) {
                if (ya.contains(en.clave)) {
                    continue;
                }
                if (coincide(ws, i, en)) {
                    ya.add(en.clave);
                    out.add(new Deseo(en.clave, en.tipo, en.dar.apply(r)));
                    break;
                }
            }
        }
        return out;
    }

    private static boolean coincide(List<String> ws, int i, Entrada en) {
        for (String[] p : en.palabras) {
            if (i + p.length > ws.size()) {
                continue;
            }
            boolean ok = true;
            for (int j = 0; j < p.length && ok; j++) {
                ok = igual(ws.get(i + j), p[j]);
            }
            if (ok) {
                return true;
            }
        }
        return false;
    }
}

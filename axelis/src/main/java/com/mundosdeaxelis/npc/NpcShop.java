package com.mundosdeaxelis.npc;

import com.mundosdeaxelis.cartas.CartasItems;
import com.mundosdeaxelis.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

/**
 * La tienda de los habitantes (etapa 257): es la misma con todos, y se paga con Esencia. Cada cosa pide un mínimo de vecinos
 * viviendo en su casa (como en Terraria: más vecinos, mejores proveedores). Lo bloqueado se ve en gris con lo que falta.
 * Etapa 282: precios un 40 % más altos. Etapa 285: brújulas de uno en uno (cada habitante te lleva al siguiente) y pestañas.
 * <p>
 * Las brújulas no son un objeto aparte: son brújulas normales que apuntan a la estructura más cercana de ese tipo (sin piedra
 * imán) y llevan su nombre.
 */
public final class NpcShop {
    /** Pestañas de la tienda (etapa 285). */
    public static final int DESTINOS = 0, PROVISIONES = 1, ESPECIALES = 2;

    /**
     * {@code compass}: id del tipo de estructura al que apunta (null = objeto normal). {@code cat}: pestaña. {@code siguiente}: es el
     * camino al próximo habitante (se resalta).
     */
    public record Offer(String id, Supplier<ItemStack> item, int price, int needResidents, String compass, int cat, boolean siguiente) {
        public Offer(String id, Supplier<ItemStack> item, int price, int needResidents, String compass) {
            this(id, item, price, needResidents, compass, compass != null ? DESTINOS : PROVISIONES, false);
        }

        public Offer(String id, Supplier<ItemStack> item, int price, int needResidents, int cat) {
            this(id, item, price, needResidents, null, cat, false);
        }

        public ItemStack stack() {
            return item.get();
        }
    }

    private static ItemStack potion(net.minecraft.world.item.alchemy.Potion p) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), p);
    }

    /** Lo que se vende siempre (con los vecinos que pida cada cosa). Las brújulas van aparte: ver {@link #ofertas}. */
    public static final List<Offer> OFFERS = List.of(
            // con un vecino (Ruda)
            new Offer("berries", () -> new ItemStack(ModItems.SANCTUARY_BERRY.get(), 3), 20, 1, PROVISIONES),
            new Offer("healing", () -> potion(Potions.HEALING), 35, 1, PROVISIONES),
            new Offer("regen", () -> potion(Potions.REGENERATION), 55, 1, PROVISIONES),
            new Offer("saplings", () -> new ItemStack(Items.OAK_SAPLING, 4), 15, 1, PROVISIONES),
            new Offer("bonemeal", () -> new ItemStack(Items.BONE_MEAL, 8), 15, 1, PROVISIONES),
            new Offer("acorn", () -> new ItemStack(ModItems.ACORN.get()), 50, 1, ESPECIALES),
            new Offer("pack_basic", () -> new ItemStack(CartasItems.PACKS.get("basico").get()), 85, 1, ESPECIALES),
            // con dos vecinos
            new Offer("fire_res", () -> potion(Potions.LONG_FIRE_RESISTANCE), 85, 2, PROVISIONES),
            new Offer("pack_silver", () -> new ItemStack(CartasItems.PACKS.get("plata").get()), 195, 2, ESPECIALES),
            new Offer("golden_acorn", () -> new ItemStack(ModItems.GOLDEN_ACORN.get()), 280, 2, ESPECIALES),
            // con tres vecinos
            new Offer("golden_apple", () -> new ItemStack(Items.GOLDEN_APPLE), 170, 3, PROVISIONES),
            new Offer("pack_gold", () -> new ItemStack(CartasItems.PACKS.get("oro").get()), 420, 3, ESPECIALES),
            // con cuatro vecinos (etapa 261: Teo trae libros)
            new Offer("exp_bottles", () -> new ItemStack(Items.EXPERIENCE_BOTTLE, 8), 125, 4, PROVISIONES),
            new Offer("book_mending", () -> net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                    new net.minecraft.world.item.enchantment.EnchantmentInstance(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1)), 560, 4, ESPECIALES),
            // con cinco vecinos (etapa 263: Ignacio trae cosas del volcán)
            new Offer("blaze_rods", () -> new ItemStack(Items.BLAZE_ROD, 4), 170, 5, PROVISIONES),
            new Offer("magma_cream", () -> new ItemStack(Items.MAGMA_CREAM, 4), 85, 5, PROVISIONES),
            new Offer("god_apple", () -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1120, 5, ESPECIALES));

    // ------------------------------------------------------------------ brújulas (etapa 285: de uno en uno)

    /** El camino: cada habitante y la estructura donde está preso, en orden de tier. */
    public static final String[][] RUTA = {{"ruda", "santuario"}, {"hilda", "forja"}, {"bruno", "fortin"}, {"ignacio", "volcan"}, {"teo", "torre"}};
    /** Las otras estructuras de cada tier (la del jefe, la pequeña...). */
    public static final String[][] OTRAS = {{"bosque"}, {"fragua"}, {"hielo"}, {}, {"arcana"}};
    /** Precio de la brújula a una estructura de cada tier. */
    public static final int[] PRECIO_BRUJULA = {55, 85, 110, 140, 170};

    private static Offer brujula(String estructura, int tier, boolean siguiente) {
        return new Offer("compass_" + estructura, () -> new ItemStack(Items.COMPASS), PRECIO_BRUJULA[tier - 1], 1, estructura, DESTINOS, siguiente);
    }

    /**
     * La tienda de ese habitante. Cada uno vende la brújula hasta el siguiente habitante (y solo esa, el primero) y las de los
     * tiers anteriores al suyo, para volver: Ruda solo vende el camino a Hilda; Hilda, el camino a Bruno y los sitios del bosque;
     * Bruno, el camino a Ignacio y los sitios del bosque y de la forja; y así. Teo, el último, vende todos.
     */
    public static List<Offer> ofertas(String npc) {
        int k = com.mundosdeaxelis.world.Sellos.npcTier(npc);
        List<Offer> out = new java.util.ArrayList<>();
        if (k >= 1 && k < RUTA.length) {
            out.add(brujula(RUTA[k][1], k + 1, true));
        }
        int hasta = k >= RUTA.length ? RUTA.length : k - 1;
        for (int t = 1; t <= hasta; t++) {
            out.add(brujula(RUTA[t - 1][1], t, false));
            for (String o : OTRAS[t - 1]) {
                out.add(brujula(o, t, false));
            }
        }
        out.addAll(OFFERS);
        return out;
    }

    /** La oferta {@code id} en la tienda de ese habitante (o null). */
    public static Offer buscar(String npc, String id) {
        for (Offer o : ofertas(npc)) {
            if (o.id().equals(id)) {
                return o;
            }
        }
        return null;
    }

    private NpcShop() {
    }
}

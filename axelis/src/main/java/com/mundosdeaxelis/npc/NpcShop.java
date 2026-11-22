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
 * Etapa 282: precios un 40 % más altos.
 * <p>
 * Las brújulas no son un objeto aparte: son brújulas normales que apuntan a la estructura más cercana de ese tipo (sin piedra
 * imán) y llevan su nombre.
 */
public final class NpcShop {
    /** {@code compass}: id del tipo de estructura al que apunta (null = objeto normal). */
    public record Offer(String id, Supplier<ItemStack> item, int price, int needResidents, String compass) {
        public ItemStack stack() {
            return item.get();
        }
    }

    private static ItemStack potion(net.minecraft.world.item.alchemy.Potion p) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), p);
    }

    public static final List<Offer> OFFERS = List.of(
            // con un vecino (Ruda)
            new Offer("berries", () -> new ItemStack(ModItems.SANCTUARY_BERRY.get(), 3), 20, 1, null),
            new Offer("acorn", () -> new ItemStack(ModItems.ACORN.get()), 50, 1, null),
            new Offer("healing", () -> potion(Potions.HEALING), 35, 1, null),
            new Offer("regen", () -> potion(Potions.REGENERATION), 55, 1, null),
            new Offer("saplings", () -> new ItemStack(Items.OAK_SAPLING, 4), 15, 1, null),
            new Offer("bonemeal", () -> new ItemStack(Items.BONE_MEAL, 8), 15, 1, null),
            new Offer("pack_basic", () -> new ItemStack(CartasItems.PACKS.get("basico").get()), 85, 1, null),
            new Offer("compass_bosque", () -> new ItemStack(Items.COMPASS), 55, 1, "bosque"),
            new Offer("compass_volcan", () -> new ItemStack(Items.COMPASS), 170, 1, "volcan"),
            new Offer("compass_hielo", () -> new ItemStack(Items.COMPASS), 110, 1, "hielo"),
            new Offer("compass_forja", () -> new ItemStack(Items.COMPASS), 110, 1, "forja"),
            new Offer("compass_torre", () -> new ItemStack(Items.COMPASS), 140, 1, "torre"),
            // con dos vecinos
            new Offer("pack_silver", () -> new ItemStack(CartasItems.PACKS.get("plata").get()), 195, 2, null),
            new Offer("golden_acorn", () -> new ItemStack(ModItems.GOLDEN_ACORN.get()), 280, 2, null),
            new Offer("fire_res", () -> potion(Potions.LONG_FIRE_RESISTANCE), 85, 2, null),
            // con tres vecinos
            new Offer("pack_gold", () -> new ItemStack(CartasItems.PACKS.get("oro").get()), 420, 3, null),
            new Offer("golden_apple", () -> new ItemStack(Items.GOLDEN_APPLE), 170, 3, null),
            // con cuatro vecinos (etapa 261: Teo trae libros)
            new Offer("exp_bottles", () -> new ItemStack(Items.EXPERIENCE_BOTTLE, 8), 125, 4, null),
            new Offer("book_mending", () -> net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
                    new net.minecraft.world.item.enchantment.EnchantmentInstance(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1)), 560, 4, null),
            // con cinco vecinos (etapa 263: Ignacio trae cosas del volcán)
            new Offer("blaze_rods", () -> new ItemStack(Items.BLAZE_ROD, 4), 170, 5, null),
            new Offer("magma_cream", () -> new ItemStack(Items.MAGMA_CREAM, 4), 85, 5, null),
            new Offer("god_apple", () -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1120, 5, null));

    private NpcShop() {
    }
}

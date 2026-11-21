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
            new Offer("berries", () -> new ItemStack(ModItems.SANCTUARY_BERRY.get(), 3), 15, 1, null),
            new Offer("acorn", () -> new ItemStack(ModItems.ACORN.get()), 35, 1, null),
            new Offer("healing", () -> potion(Potions.HEALING), 25, 1, null),
            new Offer("regen", () -> potion(Potions.REGENERATION), 40, 1, null),
            new Offer("saplings", () -> new ItemStack(Items.OAK_SAPLING, 4), 10, 1, null),
            new Offer("bonemeal", () -> new ItemStack(Items.BONE_MEAL, 8), 10, 1, null),
            new Offer("pack_basic", () -> new ItemStack(CartasItems.PACKS.get("basico").get()), 60, 1, null),
            new Offer("compass_bosque", () -> new ItemStack(Items.COMPASS), 40, 1, "bosque"),
            new Offer("compass_volcan", () -> new ItemStack(Items.COMPASS), 120, 1, "volcan"),
            new Offer("compass_hielo", () -> new ItemStack(Items.COMPASS), 80, 1, "hielo"),
            new Offer("compass_forja", () -> new ItemStack(Items.COMPASS), 80, 1, "forja"),
            // con dos vecinos
            new Offer("pack_silver", () -> new ItemStack(CartasItems.PACKS.get("plata").get()), 140, 2, null),
            new Offer("golden_acorn", () -> new ItemStack(ModItems.GOLDEN_ACORN.get()), 200, 2, null),
            new Offer("fire_res", () -> potion(Potions.LONG_FIRE_RESISTANCE), 60, 2, null),
            // con tres vecinos
            new Offer("pack_gold", () -> new ItemStack(CartasItems.PACKS.get("oro").get()), 300, 3, null),
            new Offer("golden_apple", () -> new ItemStack(Items.GOLDEN_APPLE), 120, 3, null));

    private NpcShop() {
    }
}

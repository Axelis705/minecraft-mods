package com.casino.chip;

import com.casino.item.ChipItem;
import com.casino.registry.ModItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * El dinero del casino: las fichas son objetos de 5 valores (1, 5, 25, 100 y 500). Todos los juegos cobran y pagan con estas funciones,
 * así que el "saldo" del jugador son simplemente las fichas que lleva en el inventario.
 */
public final class Chips {
    /** De mayor a menor. */
    public static final int[] VALUES = {500, 100, 25, 5, 1};

    private Chips() {
    }

    public static RegistryObject<Item> itemFor(int value) {
        return switch (value) {
            case 500 -> ModItems.CHIP_500;
            case 100 -> ModItems.CHIP_100;
            case 25 -> ModItems.CHIP_25;
            case 5 -> ModItems.CHIP_5;
            default -> ModItems.CHIP_1;
        };
    }

    /** Valor de una ficha suelta (0 si no es una ficha). */
    public static int valueOf(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof ChipItem c ? c.value : 0;
    }

    private static List<ItemStack> slots(Player p) {
        Inventory inv = p.getInventory();
        List<ItemStack> all = new ArrayList<>(inv.items);
        all.addAll(inv.offhand);
        return all;
    }

    /** Fichas (en valor) que lleva el jugador. */
    public static int balance(Player p) {
        long total = 0;
        for (ItemStack s : slots(p)) {
            total += (long) valueOf(s) * s.getCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /** Las fichas justas para pagar una cantidad (billetes grandes primero). */
    public static List<ItemStack> stacksFor(int amount) {
        List<ItemStack> out = new ArrayList<>();
        int rest = Math.max(0, amount);
        for (int v : VALUES) {
            int n = rest / v;
            rest -= n * v;
            while (n > 0) {
                int c = Math.min(n, 64);
                out.add(new ItemStack(itemFor(v).get(), c));
                n -= c;
            }
        }
        return out;
    }

    /** Da fichas al jugador (lo que no le quepa cae al suelo a sus pies). */
    public static void give(Player p, int amount) {
        for (ItemStack s : stacksFor(amount)) {
            ItemHandlerHelper.giveItemToPlayer(p, s);
        }
    }

    /** Cobra fichas: devuelve false (sin tocar nada) si no tiene suficientes. Da el cambio con fichas pequeñas. */
    public static boolean take(Player p, int amount) {
        if (amount <= 0) {
            return true;
        }
        int total = balance(p);
        if (total < amount) {
            return false;
        }
        for (ItemStack s : slots(p)) {
            if (valueOf(s) > 0) {
                s.setCount(0); // se quita todo y se devuelve lo que sobra: así siempre hay cambio exacto
            }
        }
        give(p, total - amount);
        return true;
    }
}

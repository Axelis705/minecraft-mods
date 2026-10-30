package com.casino.block;

import com.casino.chip.Chips;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemHandlerHelper;

/** Las operaciones del cajero (se hacen en el servidor). */
public final class Cashier {
    /** Lo que se puede cambiar por fichas. */
    public static final Item[] CURRENCY = {Items.EMERALD, Items.DIAMOND};
    /** Fichas que se reciben por cada objeto. */
    public static final int[] BUY = {10, 80};
    /** Fichas que cuesta recuperar un objeto: el mismo precio que al comprar, así cambiar de ida y vuelta nunca hace perder nada. */
    public static final int[] SELL = BUY;

    private Cashier() {
    }

    public static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void remove(ServerPlayer p, Item item, int amount) {
        int left = amount;
        for (ItemStack s : p.getInventory().items) {
            if (left > 0 && s.is(item)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
    }

    private static void deny(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        p.level().playSound(null, p.blockPosition(), ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    /** Fichas que se pueden canjear como máximo por este objeto con el saldo actual. */
    public static int maxSell(ServerPlayer p, int kind) {
        return Chips.balance(p) / SELL[kind];
    }

    /**
     * Compra o canje de {@code amount} objetos: al comprar, entrega esa cantidad de objetos y recibe fichas;
     * al canjear, paga fichas y recibe esa cantidad de objetos. Si pide más de lo que tiene, se hace con el máximo posible.
     */
    public static void exchange(ServerPlayer p, int kind, boolean buy, int amount) {
        if (kind < 0 || kind >= CURRENCY.length || amount <= 0) {
            return;
        }
        amount = Math.min(amount, 100_000);
        Item item = CURRENCY[kind];
        if (buy) {
            int n = Math.min(amount, count(p, item));
            if (n <= 0) {
                deny(p, "casino.cashier.no_items");
                return;
            }
            remove(p, item, n);
            Chips.give(p, n * BUY[kind]);
            p.displayClientMessage(Component.translatable("casino.cashier.bought", n * BUY[kind]).withStyle(ChatFormatting.GREEN), true);
        } else {
            int n = Math.min(amount, maxSell(p, kind));
            if (n <= 0) {
                deny(p, "casino.cashier.no_chips");
                return;
            }
            Chips.take(p, n * SELL[kind]);
            int left = n;
            while (left > 0) {
                int c = Math.min(left, 64);
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(item, c));
                left -= c;
            }
            p.displayClientMessage(Component.translatable("casino.cashier.sold", n * SELL[kind], n).withStyle(ChatFormatting.GREEN), true);
        }
        p.level().playSound(null, p.blockPosition(), ModSounds.COINS.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
    }
}

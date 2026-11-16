package com.casino.block;

import com.casino.plat.Plat;
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
    /** Fichas que se reciben por cada objeto (los mismos valores de {@link com.casino.game.Exchange}). */
    public static final int[] BUY = {com.casino.game.Exchange.VALUE[com.casino.game.Exchange.EMERALD],
            com.casino.game.Exchange.VALUE[com.casino.game.Exchange.DIAMOND]};
    /** Fichas que cuesta recuperar un objeto: el mismo precio que al comprar, así cambiar de ida y vuelta nunca hace perder nada. */
    public static final int[] SELL = BUY;

    private Cashier() {
    }

    public static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : Plat.mainItems(p.getInventory())) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void remove(ServerPlayer p, Item item, int amount) {
        int left = amount;
        for (ItemStack s : Plat.mainItems(p.getInventory())) {
            if (left > 0 && s.is(item)) {
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
    }

    private static void deny(ServerPlayer p, String key) {
        Plat.msg(p, Component.translatable(key).withStyle(ChatFormatting.RED), true);
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
            Plat.msg(p, Component.translatable("casino.cashier.bought", n * BUY[kind]).withStyle(ChatFormatting.GREEN), true);
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
            Plat.msg(p, Component.translatable("casino.cashier.sold", n * SELL[kind], n).withStyle(ChatFormatting.GREEN), true);
        }
        p.level().playSound(null, p.blockPosition(), ModSounds.COINS.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    // ------------------------------------------------------------------ cambio entre fichas, monedas arcade, boletos y dinero

    /** El objeto de cada moneda de {@link com.casino.game.Exchange} (null = fichas, que se cuentan por valor). */
    public static Item itemOf(int cur) {
        return switch (cur) {
            case com.casino.game.Exchange.COINS -> com.casino.registry.ModItems.ARCADE_COIN.get();
            case com.casino.game.Exchange.TICKETS -> com.casino.registry.ModItems.ARCADE_TICKET.get();
            case com.casino.game.Exchange.EMERALD -> Items.EMERALD;
            case com.casino.game.Exchange.DIAMOND -> Items.DIAMOND;
            default -> null;
        };
    }

    /** Cuánto tiene el jugador de esa moneda (las fichas, por su valor). */
    public static long have(ServerPlayer p, int cur) {
        Item it = itemOf(cur);
        return it == null ? Chips.balance(p) : count(p, it);
    }

    private static void giveCur(ServerPlayer p, int cur, int n) {
        Item it = itemOf(cur);
        if (it == null) {
            Chips.give(p, n);
            return;
        }
        int left = n;
        while (left > 0) {
            int c = Math.min(left, 64);
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(it, c));
            left -= c;
        }
    }

    /** "16 Esmeraldas", "160 Fichas"... (nombre de la moneda del cajero). */
    public static Component amountOf(int cur, long n) {
        return Component.translatable("casino.cashier.cur_n", String.format("%,d", n), Component.translatable((n == 1 ? "casino.cashier.cur1_" : "casino.cashier.cur_") + cur));
    }

    /**
     * Cambia {@code lots} lotes de {@code from} por {@code to} (ver {@link com.casino.game.Exchange}); si pide más de lo que tiene,
     * se hace con lo más que se pueda. Lo que no le quepa en el inventario cae a sus pies.
     */
    public static void convert(ServerPlayer p, int from, int to, int lots) {
        if (!com.casino.game.Exchange.valid(from, to) || lots <= 0) {
            return;
        }
        int n = Math.min(lots, com.casino.game.Exchange.maxLots(from, to, have(p, from)));
        if (n <= 0) {
            deny(p, from == com.casino.game.Exchange.CHIPS ? "casino.cashier.no_chips" : "casino.cashier.no_items");
            return;
        }
        int give = n * com.casino.game.Exchange.give(from, to);
        int get = n * com.casino.game.Exchange.get(from, to);
        Item it = itemOf(from);
        if (it == null) {
            if (!Chips.take(p, give)) {
                deny(p, "casino.cashier.no_chips");
                return;
            }
        } else {
            remove(p, it, give);
        }
        giveCur(p, to, get);
        Plat.msg(p, Component.translatable("casino.cashier.converted", amountOf(from, give), amountOf(to, get)).withStyle(ChatFormatting.GREEN), true);
        p.level().playSound(null, p.blockPosition(), ModSounds.COINS.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    /** Tarjetas VIP: 0 = Plata (zona VIP), 1 = Oro (VIP + penthouse y azotea). */
    public static final int[] VIP_PRICE = {5000, 25000};

    public static void buyVip(ServerPlayer p, int tier) {
        if (tier < 0 || tier > 1) {
            return;
        }
        if (com.casino.item.VipCardItem.tier(p) >= tier + 1) {
            // ya la tiene registrada: si perdió la tarjeta (no lleva ninguna igual o mejor), se le da una copia sin cobrar
            boolean carries = false;
            for (ItemStack s : Plat.mainAndOffhand(p.getInventory())) {
                if (s.getItem() instanceof com.casino.item.VipCardItem c && c.level >= tier + 1) {
                    carries = true;
                }
            }
            if (carries) {
                deny(p, "casino.vip.already");
            } else {
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(tier == 0 ? com.casino.registry.ModItems.VIP_SILVER.get() : com.casino.registry.ModItems.VIP_GOLD.get()));
                Plat.msg(p, Component.translatable("casino.vip.copy").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (!Chips.take(p, VIP_PRICE[tier])) {
            deny(p, "casino.cashier.no_chips");
            return;
        }
        com.casino.item.VipCardItem.grant(p, tier + 1);
        ItemStack card = new ItemStack(tier == 0 ? com.casino.registry.ModItems.VIP_SILVER.get() : com.casino.registry.ModItems.VIP_GOLD.get());
        ItemHandlerHelper.giveItemToPlayer(p, card);
        Plat.msg(p, Component.translatable(tier == 0 ? "casino.vip.bought_silver" : "casino.vip.bought_gold").withStyle(ChatFormatting.GOLD), true);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
    }
}

package com.mundosdeaxelis.cartas;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro de todas las cartas, sobres y el album, y su pestana del creativo. */
public final class CartasItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Cartas.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Cartas.MOD_ID);

    public static final Map<String, RegistryObject<Item>> CARDS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> PACKS = new LinkedHashMap<>();
    public static final RegistryObject<Item> ALBUM;

    static {
        for (CardDef d : Cards.ALL) {
            CARDS.put(d.id, ITEMS.register("carta_" + d.id, () -> new CardItem(d)));
        }
        for (PackDef p : Cards.PACKS) {
            PACKS.put(p.id, ITEMS.register("sobre_" + p.id, () -> new PackItem(p)));
        }
        ALBUM = ITEMS.register("album_cartas", AlbumItem::new);
        TABS.register("cartas", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mundos_de_axelis.cartas"))
                .icon(() -> new ItemStack(ALBUM.get()))
                .displayItems((params, out) -> {
                    out.accept(ALBUM.get());
                    for (PackDef p : Cards.PACKS) {
                        out.accept(PACKS.get(p.id).get());
                    }
                    for (CardDef d : Cards.ALL) {
                        out.accept(stack(d, false, 1));
                    }
                    for (CardDef d : Cards.ALL) {
                        out.accept(stack(d, true, 1));
                    }
                })
                .build());
    }

    private CartasItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    public static ItemStack stack(CardDef d, boolean holo, int n) {
        return CardItem.make(CARDS.get(d.id).get(), holo, n);
    }

    public static ItemStack pack(PackDef p, int n) {
        return new ItemStack(PACKS.get(p.id).get(), n);
    }

    /** Cuantas cartas de ese tipo lleva el jugador encima. */
    public static int countInInventory(Player p, CardDef d, boolean holo) {
        int n = 0;
        Item item = CARDS.get(d.id).get();
        for (ItemStack s : p.getInventory().items) {
            if (s.getItem() == item && CardItem.isHolo(s) == holo) {
                n += s.getCount();
            }
        }
        return n;
    }
}

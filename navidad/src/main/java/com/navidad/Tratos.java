package com.navidad;

import com.navidad.registry.ModRegistry;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

/**
 * Los aldeanos también celebran: el granjero vende galletas de jengibre y bastones de caramelo, el carnicero el pavo navideño, el pastor
 * gorros y suéteres navideños, el bibliotecario el calendario de Adviento, y el comerciante errante trae adornos, arbolitos y, a veces,
 * un regalo misterioso.
 */
@Mod.EventBusSubscriber(modid = Navidad.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Tratos {
    private Tratos() {
    }

    private static VillagerTrades.ItemListing vende(Supplier<? extends Item> que, int cuantos, int esmeraldas, int usos, int xp) {
        return (quien, r) -> new MerchantOffer(new ItemStack(Items.EMERALD, esmeraldas), new ItemStack(que.get(), cuantos), usos, xp, 0.05F);
    }

    @SubscribeEvent
    public static void aldeanos(VillagerTradesEvent e) {
        var t = e.getTrades();
        if (e.getType() == VillagerProfession.FARMER) {
            t.get(1).add(vende(ModRegistry.GALLETA_JENGIBRE, 6, 1, 12, 2));
            t.get(2).add(vende(ModRegistry.BASTON_CARAMELO, 4, 1, 12, 5));
        } else if (e.getType() == VillagerProfession.BUTCHER) {
            t.get(3).add(vende(ModRegistry.PAVO_NAVIDENO, 1, 4, 8, 10));
        } else if (e.getType() == VillagerProfession.SHEPHERD) {
            t.get(2).add(vende(ModRegistry.GORRO_SANTA, 1, 3, 6, 5));
            t.get(3).add(vende(ModRegistry.SUETER_PINOS, 1, 5, 4, 10));
        } else if (e.getType() == VillagerProfession.LIBRARIAN) {
            t.get(2).add(vende(ModRegistry.CALENDARIO_ITEM, 1, 6, 3, 10));
        }
    }

    @SubscribeEvent
    public static void errante(WandererTradesEvent e) {
        e.getGenericTrades().add(vende(ModRegistry.CAJA_ADORNOS, 4, 2, 6, 1));
        e.getGenericTrades().add(vende(ModRegistry.CHOCOLATE_CALIENTE, 3, 1, 8, 1));
        e.getGenericTrades().add(vende(() -> com.navidad.bloque.deco.Deco.block("pino_maceta").asItem(), 1, 4, 3, 1));
        e.getRareTrades().add(vende(ModRegistry.REGALO_MISTERIOSO, 1, 8, 2, 1));
    }
}

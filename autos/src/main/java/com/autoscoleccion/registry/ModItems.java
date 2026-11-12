package com.autoscoleccion.registry;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.item.BoxItem;
import com.autoscoleccion.item.CarItem;
import com.autoscoleccion.item.GarageBookItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AutosMod.MODID);

    public static final RegistryObject<Item> VENDING_MACHINE = ITEMS.register("maquina_expendedora",
            () -> new BlockItem(ModBlocks.VENDING_MACHINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DISPLAY_CASE = ITEMS.register("vitrina",
            () -> new BlockItem(ModBlocks.DISPLAY_CASE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GARAGE_BOOK = ITEMS.register("garaje",
            () -> new GarageBookItem(new Item.Properties().stacksTo(1)));

    /** Las cuatro cajas, en el orden de {@link Boxes}. */
    public static final List<RegistryObject<Item>> BOXES = new ArrayList<>();
    /** Los 40 autos, en el orden de {@link Cars} (índice 0 = auto nº 1). */
    public static final List<RegistryObject<Item>> CARS = new ArrayList<>();

    static {
        for (int b = 0; b < Boxes.COUNT; b++) {
            final int box = b;
            Rarity r = b == 0 ? Rarity.COMMON : b == 1 ? Rarity.UNCOMMON : b == 2 ? Rarity.RARE : Rarity.EPIC;
            BOXES.add(ITEMS.register(Boxes.ID[b], () -> new BoxItem(box, new Item.Properties().stacksTo(16).rarity(r))));
        }
        for (CarDef c : Cars.all()) {
            final int idx = c.index();
            CARS.add(ITEMS.register("auto_" + c.id, () -> new CarItem(idx, new Item.Properties().stacksTo(1))));
        }
    }

    private ModItems() {
    }
}

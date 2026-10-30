package com.kensei.registry;

import com.kensei.Kensei;
import com.kensei.item.KatanaItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Las armas de Kensei. Por ahora hay una sola: la katana. Para agregar otra arma más adelante basta con registrarla con {@link #weapon}
 * (aparece sola en la pestaña creativa) y darle su propio conjunto de movimientos.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Kensei.MODID);
    /** Todas las armas del mod, en orden (la pestaña creativa las muestra así). */
    public static final List<RegistryObject<Item>> WEAPONS = new ArrayList<>();

    public static final RegistryObject<Item> KATANA = weapon("katana",
            () -> new KatanaItem(KenseiTier.KATANA, 2, -1.8F, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Huevo de aparición del rival de práctica. */
    public static final RegistryObject<Item> RIVAL_EGG = ITEMS.register("rival_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RIVAL, 0x23232f, 0xc8323c, new Item.Properties()));

    private ModItems() {
    }

    private static RegistryObject<Item> weapon(String name, Supplier<Item> factory) {
        RegistryObject<Item> o = ITEMS.register(name, factory);
        WEAPONS.add(o);
        return o;
    }
}

package com.casino.registry;

import com.casino.Casino;
import com.casino.item.ChipItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Casino.MODID);

    public static final RegistryObject<Item> CHIP_1 = chip("chip_1", 1);
    public static final RegistryObject<Item> CHIP_5 = chip("chip_5", 5);
    public static final RegistryObject<Item> CHIP_25 = chip("chip_25", 25);
    public static final RegistryObject<Item> CHIP_100 = chip("chip_100", 100);
    public static final RegistryObject<Item> CHIP_500 = chip("chip_500", 500);

    public static final RegistryObject<Item> CASHIER = ITEMS.register("cashier",
            () -> new BlockItem(ModBlocks.CASHIER.get(), new Item.Properties()));
    public static final RegistryObject<Item> LUCKY_WHEEL = ITEMS.register("lucky_wheel",
            () -> new BlockItem(ModBlocks.LUCKY_WHEEL.get(), new Item.Properties()));

    public static final RegistryObject<Item> PLINKO = ITEMS.register("plinko",
            () -> new BlockItem(ModBlocks.PLINKO.get(), new Item.Properties()));
    public static final RegistryObject<Item> SLOT = ITEMS.register("slot",
            () -> new BlockItem(ModBlocks.SLOT.get(), new Item.Properties()));

    public static final RegistryObject<Item> BLACKJACK = ITEMS.register("blackjack",
            () -> new BlockItem(ModBlocks.BLACKJACK.get(), new Item.Properties()));

    private ModItems() {
    }

    private static RegistryObject<Item> chip(String name, int value) {
        return ITEMS.register(name, () -> new ChipItem(new Item.Properties(), value));
    }
}

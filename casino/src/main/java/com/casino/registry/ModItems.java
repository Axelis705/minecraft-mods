package com.casino.registry;

import com.casino.plat.Reg;
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
            () -> new BlockItem(ModBlocks.CASHIER.get(), Reg.blockItem("cashier")));
    public static final RegistryObject<Item> LUCKY_WHEEL = ITEMS.register("lucky_wheel",
            () -> new BlockItem(ModBlocks.LUCKY_WHEEL.get(), Reg.blockItem("lucky_wheel")));

    public static final RegistryObject<Item> PLINKO = ITEMS.register("plinko",
            () -> new BlockItem(ModBlocks.PLINKO.get(), Reg.blockItem("plinko")));
    public static final RegistryObject<Item> SLOT = ITEMS.register("slot",
            () -> new BlockItem(ModBlocks.SLOT.get(), Reg.blockItem("slot")));

    public static final RegistryObject<Item> BLACKJACK = ITEMS.register("blackjack",
            () -> new BlockItem(ModBlocks.BLACKJACK.get(), Reg.blockItem("blackjack")));

    public static final RegistryObject<Item> POKER = ITEMS.register("poker",
            () -> new BlockItem(ModBlocks.POKER.get(), Reg.blockItem("poker")));

    public static final RegistryObject<Item> SCRATCH_MACHINE = ITEMS.register("scratch_machine",
            () -> new BlockItem(ModBlocks.SCRATCH_MACHINE.get(), Reg.blockItem("scratch_machine")));

    public static final RegistryObject<Item> SCRATCH_CARD = ITEMS.register("scratch_card",
            () -> new com.casino.item.ScratchCardItem(Reg.item("scratch_card").stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> CASINO_GUIDE = ITEMS.register("casino_guide",
            () -> new com.casino.item.GuideItem(Reg.item("casino_guide").stacksTo(1)));

    public static final RegistryObject<Item> ROULETTE = ITEMS.register("roulette",
            () -> new BlockItem(ModBlocks.ROULETTE.get(), Reg.blockItem("roulette")));

    private ModItems() {
    }

    private static RegistryObject<Item> chip(String name, int value) {
        return ITEMS.register(name, () -> new ChipItem(Reg.item(name).rarity(value >= 500 ? net.minecraft.world.item.Rarity.EPIC
                : value >= 100 ? net.minecraft.world.item.Rarity.RARE : value >= 25 ? net.minecraft.world.item.Rarity.UNCOMMON : net.minecraft.world.item.Rarity.COMMON), value));
    }
}

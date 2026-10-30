package com.pooltable.registry;

import com.pooltable.PoolTable;
import com.pooltable.item.PoolCueItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PoolTable.MODID);

    public static final RegistryObject<Item> POOL_TABLE = ITEMS.register("pool_table",
            () -> new BlockItem(ModBlocks.POOL_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> POOL_CUE = ITEMS.register("pool_cue",
            () -> new PoolCueItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}

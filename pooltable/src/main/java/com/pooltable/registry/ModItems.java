package com.pooltable.registry;

import com.pooltable.PoolTable;
import com.pooltable.item.BowlingBallItem;
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

    public static final RegistryObject<Item> BOWLING_LANE = ITEMS.register("bowling_lane",
            () -> new BlockItem(ModBlocks.BOWLING_LANE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BOWLING_BALL = ITEMS.register("bowling_ball",
            () -> new BowlingBallItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> AIR_HOCKEY = ITEMS.register("air_hockey",
            () -> new BlockItem(ModBlocks.AIR_HOCKEY.get(), new Item.Properties()));

    public static final RegistryObject<Item> VOLLEY_COURT = ITEMS.register("volley_court",
            () -> new BlockItem(ModBlocks.VOLLEY_COURT.get(), new Item.Properties()));

    private ModItems() {
    }
}

package com.pooltable;

import com.mojang.logging.LogUtils;
import com.pooltable.net.PoolNet;
import com.pooltable.registry.ModBlockEntities;
import com.pooltable.registry.ModBlocks;
import com.pooltable.registry.ModCreativeTabs;
import com.pooltable.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Pool Table: una mesa de billar jugable (taco, bolas con física, 8-ball para dos jugadores y práctica libre). */
@Mod(PoolTable.MODID)
public class PoolTable {
    public static final String MODID = "pooltable";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PoolTable(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.TYPES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PoolNet::register);
    }
}

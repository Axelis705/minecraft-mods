package com.casino;

import com.casino.net.CasinoNet;
import com.casino.registry.ModBlockEntities;
import com.casino.registry.ModBlocks;
import com.casino.registry.ModCreativeTabs;
import com.casino.registry.ModEntities;
import com.casino.registry.ModItems;
import com.casino.registry.ModSounds;
import com.casino.registry.ModStructures;
import com.mojang.logging.LogUtils;
//#if MC >= 26.1
//$$ import net.minecraftforge.eventbus.api.bus.BusGroup;
//#else
import net.minecraftforge.eventbus.api.IEventBus;
//#endif
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Casino: fichas, cajero y la Ruleta de la suerte diaria al estilo GTA (más juegos en las siguientes etapas). */
@Mod(Casino.MODID)
public class Casino {
    public static final String MODID = "casino";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Casino(FMLJavaModLoadingContext context) {
        //#if MC >= 26.1
        //$$ BusGroup bus = context.getModBusGroup();
        //#else
        IEventBus bus = context.getModEventBus();
        //#endif
        com.casino.registry.ModDecor.init();
        com.casino.diamond.DiamondReg.init();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModBlockEntities.TYPES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModStructures.TYPES.register(bus);
        ModStructures.PIECES.register(bus);
        //#if MC >= 26.1
        //$$ FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        //#else
        bus.addListener(this::commonSetup);
        //#endif
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CasinoNet::register);
    }
}

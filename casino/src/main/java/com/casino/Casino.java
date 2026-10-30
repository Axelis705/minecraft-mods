package com.casino;

import com.casino.net.CasinoNet;
import com.casino.registry.ModBlockEntities;
import com.casino.registry.ModBlocks;
import com.casino.registry.ModCreativeTabs;
import com.casino.registry.ModItems;
import com.casino.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
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
        IEventBus bus = context.getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlockEntities.TYPES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CasinoNet::register);
    }
}

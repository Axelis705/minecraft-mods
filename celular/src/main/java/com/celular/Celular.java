package com.celular;

import com.celular.net.CelularNet;
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

/** El celular: Instacraft, cámara con selfies, galería, llamadas (con voz si está Simple Voice Chat), Patreon y ajustes. */
@Mod(Celular.MODID)
public class Celular {
    public static final String MODID = "celular";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Celular(FMLJavaModLoadingContext context) {
        //#if MC >= 26.1
        //$$ BusGroup bus = context.getModBusGroup();
        //#else
        IEventBus bus = context.getModEventBus();
        //#endif
        CelularReg.ITEMS.register(bus);
        CelularReg.BLOCKS.register(bus);
        CelularReg.SOUNDS.register(bus);
        CelularReg.TABS.register(bus);
        //#if MC >= 26.1
        //$$ FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        //#else
        bus.addListener(this::commonSetup);
        //#endif
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CelularNet::register);
    }
}

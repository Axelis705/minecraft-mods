package com.scardax;

import com.mojang.logging.LogUtils;
import com.scardax.net.ScardaxNet;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Scardax: control de mobs desde dentro del juego. El comando /scardax abre una pantalla donde se ve cada mob (de todos los mods)
 * y se le puede bajar o subir el spawneo, o quitarlo del todo. El valor de MODID debe coincidir con mod_id en gradle.properties.
 */
@Mod(Scardax.MODID)
public class Scardax {
    public static final String MODID = "scardax";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Scardax(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ScardaxNet::register);
        LOGGER.info("Scardax: listo.");
    }
}

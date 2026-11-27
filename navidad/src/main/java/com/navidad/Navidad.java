package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Net;
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

/** Navidad de Axelis: Una Navidad enorme para Minecraft: decoraciones, regalos, posadas con piñatas, comida navideña, Santa y sus duendes, el Krampus, villancicos y mucho más. */
@Mod(Navidad.MODID)
public class Navidad {
    public static final String MODID = "navidad";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Canal de mensajes del mod: si cambias qué mensajes hay, sube el número. */
    public static final Net NET = new Net(MODID, "main", 1);

    public Navidad(FMLJavaModLoadingContext context) {
        AxelisCore.use(MODID);
        //#if MC >= 26.1
        //$$ BusGroup bus = context.getModBusGroup();
        //$$ FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        //#else
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::commonSetup);
        //#endif
        com.navidad.registry.ModRegistry.register(bus);
        com.navidad.entidad.Proyectiles.init();
        com.navidad.gente.Gente.init();
        Fiestas.init();
        Invierno.init();
        AnioNuevo.init();
        Cartas.init();
        GuerraNieve.init();
        Calcetines.init();
        GalletasSanta.init();
        DecoViva.init();
        Logros.init();
        AxelisCore.commands(Comandos::registrar);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // aquí se registran los mensajes de red: NET.register(Mensaje.class, Mensaje::write, Mensaje::read, (m, jugador) -> ...);
            NET.done();
        });
    }
}

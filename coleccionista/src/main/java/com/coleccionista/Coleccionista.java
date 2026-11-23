package com.coleccionista;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Net;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
//#if MC >= 26.1
//$$ import net.minecraftforge.eventbus.api.bus.BusGroup;
//#else
import net.minecraftforge.eventbus.api.IEventBus;
//#endif
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Los Juegos del Coleccionista: Evento de pruebas macabras para jugar con amigos: el último en pie gana. */
@Mod(Coleccionista.MODID)
public class Coleccionista {
    public static final String MODID = "coleccionista";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Canal de mensajes del mod: si cambias qué mensajes hay, sube el número. */
    public static final Net NET = new Net(MODID, "main", 1);

    public Coleccionista(FMLJavaModLoadingContext context) {
        AxelisCore.use(MODID);
        //#if MC >= 26.1
        //$$ BusGroup bus = context.getModBusGroup();
        //$$ FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        //#else
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::commonSetup);
        //#endif
        com.coleccionista.registry.ModRegistry.register(bus);
        com.coleccionista.evento.Evento.init();
        com.coleccionista.evento.Fantasmas.init();
        AxelisCore.commands(com.coleccionista.evento.Comandos::registrar);
        AxelisCore.commands(d -> d.register(Commands.literal(MODID).executes(c -> {
            c.getSource().sendSuccess(() -> Component.translatable(MODID + ".hola", AxelisCore.version()), false);
            LOGGER.info("PRUEBA_COLECCIONISTA_OK {}", AxelisCore.version());
            return 1;
        })));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // aquí se registran los mensajes de red: NET.register(Mensaje.class, Mensaje::write, Mensaje::read, (m, jugador) -> ...);
            NET.done();
        });
    }
}

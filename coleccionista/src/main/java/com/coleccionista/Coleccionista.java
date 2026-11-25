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
        com.coleccionista.evento.Panel.init();
        com.coleccionista.evento.Tomas.init();
        com.coleccionista.evento.Complejo.init();
        com.coleccionista.evento.Celdas.init();
        com.coleccionista.evento.Pantallas.init();
        com.coleccionista.evento.Despertar.init();
        com.coleccionista.evento.Chat.init();
        com.coleccionista.evento.Proteccion.init();
        com.coleccionista.evento.Montacargas.init();
        com.coleccionista.evento.Cabina.init();
        com.coleccionista.evento.Capitulo1.init();
        com.coleccionista.evento.Silla.init();
        com.coleccionista.evento.Objetivo.init();
        com.coleccionista.evento.Recorrido.init();
        com.coleccionista.evento.Pistas.init();
        com.coleccionista.prueba.Pruebas.init();
        com.coleccionista.voz.Voz.init();
        com.axeliscore.ui.Teclas.nueva(MODID, "acercarse", 'R', () -> com.coleccionista.client.Atajos.acercarse());
        com.axeliscore.ui.Teclas.nueva(MODID, "panel", 'O', () -> com.coleccionista.client.Atajos.panel());
        com.axeliscore.ui.Teclas.nueva(MODID, "hud", 'K', () -> com.coleccionista.client.Atajos.hud());
        com.axeliscore.ui.Teclas.nueva(MODID, "rugir", 'G', () -> com.coleccionista.client.MascotaCliente.rugir());
        com.axeliscore.ui.Teclas.nueva(MODID, "olfatear", 'H', () -> com.coleccionista.client.MascotaCliente.olfatear());
        AxelisCore.commands(com.coleccionista.evento.Comandos::registrar);
        AxelisCore.commands(com.coleccionista.evento.ComandosMinijuego::registrar);
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

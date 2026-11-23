package com.pescacoleccion;

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

/** Pesca de colección: Peces de colección con tamaños y rarezas, récords y un libro de colección */
@Mod(PescaColeccion.MODID)
public class PescaColeccion {
    public static final String MODID = "pesca_coleccion";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Canal de mensajes del mod: si cambias qué mensajes hay, sube el número. */
    public static final Net NET = new Net(MODID, "main", 1);

    public PescaColeccion(FMLJavaModLoadingContext context) {
        AxelisCore.use(MODID);
        //#if MC >= 26.1
        //$$ BusGroup bus = context.getModBusGroup();
        //$$ FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        //#else
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::commonSetup);
        //#endif
        com.pescacoleccion.registry.ModRegistry.register(bus);
        AxelisCore.commands(d -> d.register(Commands.literal(MODID).executes(c -> {
            c.getSource().sendSuccess(() -> Component.translatable(MODID + ".hola", AxelisCore.version()), false);
            LOGGER.info("PRUEBA_PESCA_COLECCION_OK {}", AxelisCore.version());
            return 1;
        }).then(Commands.literal("pelea").requires(com.axeliscore.plat.Plat::isOp)
                .then(Commands.argument("rareza", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 5))
                        .executes(c -> com.pescacoleccion.pelea.Prueba.pelea(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "rareza"), false))
                        .then(Commands.literal("facil").executes(c -> com.pescacoleccion.pelea.Prueba.pelea(c.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "rareza"), true)))))));
    }

    /** Modo fácil (config/pesca_coleccion.json): peleas más fáciles para quien lo prefiera. */
    public static boolean facil() {
        return com.axeliscore.data.Config.of(MODID).bool("modo_facil", false,
                "Peleas más fáciles: ventanas más grandes, el pez jala menos y quita menos hilo");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.pescacoleccion.pelea.PeleaNet.register(NET);
            NET.done();
        });
    }
}

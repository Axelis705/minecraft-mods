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
        com.axeliscore.data.PlayerStore.syncOnLogin(MODID);
        com.pescacoleccion.pesca.Libro.init();
        com.pescacoleccion.pesca.Lombrices.init();
        com.pescacoleccion.pesca.PezVolador.init();
        com.pescacoleccion.nilo.Encargos.init();
        com.pescacoleccion.nilo.Tienda.init();
        com.pescacoleccion.torneo.Torneo.init();
        // la caña se dibuja sin el hilo que cuelga mientras su corcho está afuera (como la de Minecraft)
        Net.onClient(() -> com.axeliscore.item.client.ItemModelProps.bool(MODID, "lanzada", (stack, quien) -> {
            if (!(quien instanceof net.minecraft.world.entity.player.Player p) || !(stack.getItem() instanceof com.pescacoleccion.pesca.CanaItem)) {
                return false;
            }
            boolean main = p.getMainHandItem() == stack;
            boolean off = p.getOffhandItem() == stack && !(p.getMainHandItem().getItem() instanceof com.pescacoleccion.pesca.CanaItem);
            return (main || off) && com.pescacoleccion.pesca.Anzuelo.lanzada(p);
        }));
        AxelisCore.commands(d -> d.register(Commands.literal(MODID).executes(c -> {
            c.getSource().sendSuccess(() -> Component.translatable(MODID + ".hola", AxelisCore.version()), false);
            LOGGER.info("PRUEBA_PESCA_COLECCION_OK {}", AxelisCore.version());
            return 1;
        }).then(Commands.literal("torneo").requires(com.axeliscore.plat.Plat::isOp)
                .then(Commands.literal("terminar").executes(c -> {
                    com.pescacoleccion.torneo.Torneo.terminar(c.getSource().getServer());
                    return 1;
                })))
        .then(Commands.literal("nilo").requires(com.axeliscore.plat.Plat::isOp)
                .then(Commands.literal("llamar").executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    com.pescacoleccion.nilo.NiloMundo.llamar(p, com.pescacoleccion.nilo.NiloMundo.aguaCerca(p));
                    return 1;
                }))
                .then(Commands.literal("aqui").executes(c -> {
                    com.pescacoleccion.nilo.NiloMundo.aqui(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("prueba").executes(c -> com.pescacoleccion.nilo.Prueba.correr(c.getSource()))))
        .then(Commands.literal("pelea").requires(com.axeliscore.plat.Plat::isOp)
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

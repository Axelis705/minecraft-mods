package com.axeliscore;

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

/**
 * Axelis Core: la base de los mods nuevos de Axelis (personajes con animaciones, interfaz, efectos y herramientas comunes).
 * <p>
 * No se instala aparte: cada mod que lo usa lo lleva dentro (Jar-in-Jar). Si dos mods traen versiones distintas, Forge carga la más
 * nueva, por eso todo lo público debe seguir funcionando igual al subir de versión (solo se agregan cosas, no se quitan).
 */
@Mod(AxelisCore.MODID)
public class AxelisCore {
    public static final String MODID = "axelis_core";
    /** Versión del Core (la misma que la del jar). */
    public static final String VERSION = "1.6.2";
    /**
     * Nivel de la API: sube cada vez que se agrega algo público. Un mod puede comprobar {@code AxelisCore.API >= n} si necesita algo
     * nuevo (Forge ya se encarga de cargar la versión más alta que haya).
     */
    public static final int API = 8;
    public static final Logger LOGGER = LogUtils.getLogger();

    public AxelisCore(FMLJavaModLoadingContext context) {
        //#if MC >= 26.1
        //$$ BusGroup bus = context.getModBusGroup();
        //$$ FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        //#else
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::commonSetup);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CoreCommands::register);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CoreEvents::onLogin);
        // al final de todos: si otro mod protege el bloque (cancela), ya no llega aquí
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, CoreEvents::onBreak);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CoreEvents::onServerTick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CoreEvents::onHurt);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CoreEvents::onDeath);
        //#endif
        com.axeliscore.combat.Estados.init();
        com.axeliscore.combat.CoreProyectiles.init();
        com.axeliscore.npc.Misiones.init();
        com.axeliscore.npc.NpcAcciones.init();
        com.axeliscore.anim.mc.EstudioServidor.init();
        com.axeliscore.vestir.Vestuario.init();
        com.axeliscore.forma.Formas.init();
        if (Ejemplos.activos()) {
            com.axeliscore.npc.CoreNpcs.init();
        }
        com.axeliscore.ui.Teclas.nueva(MODID, "diario", 'N', () -> com.axeliscore.ui.ScreenRegistry.open(com.axeliscore.npc.NpcAcciones.DIARIO));
        com.axeliscore.registry.CoreEntities.ENTITIES.register(bus);
        com.axeliscore.registry.CoreItems.ITEMS.register(bus);
        // los objetos con modelo de Blockbench (en 26.x hay que avisarle a Minecraft antes de cargar los modelos)
        com.axeliscore.plat.Net.onClient(com.axeliscore.item.client.ObjetoModelo::registrar);
        LOGGER.info("Axelis Core {} (API {})", VERSION, API);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(com.axeliscore.net.CoreNet::register);
    }

    /**
     * Cada mod que usa el Core lo llama en su constructor ({@code AxelisCore.use(MODID)}): así el Core sabe quién lo usa (para el
     * comando /axeliscore, la galería y los avisos de errores).
     */
    public static void use(String modid) {
        CoreUsers.add(modid);
        LOGGER.info("{} usa {}", modid, version());
    }

    /**
     * Comandos de un mod: el Core los registra junto con los suyos (en todas las versiones, sin que cada mod tenga que preocuparse por
     * cómo se registran en cada una).
     */
    public static void commands(java.util.function.Consumer<com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>> c) {
        CoreCommands.EXTRA.add(c);
    }

    /** Algo que hacer cada vez que un jugador entra al mundo (en el servidor), igual en todas las versiones. */
    public static void onLogin(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> c) {
        CoreEvents.LOGIN.add(c);
    }

    /** Lo que se hace cuando un jugador rompe un bloque (ver {@link #onBreak}). */
    @FunctionalInterface
    public interface BreakHandler {
        void broke(net.minecraft.server.level.ServerPlayer p, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                net.minecraft.world.level.block.state.BlockState state);
    }

    /**
     * Algo que hacer cuando un jugador rompe un bloque (en el servidor, justo antes de que se quite), igual en todas las versiones. Solo
     * se llama si de verdad se va a romper: si otro mod lo protege, no llega.
     */
    public static void onBreak(BreakHandler h) {
        CoreEvents.BREAK.add(h);
    }

    /** Algo que hacer al final de cada tick del servidor (20 veces por segundo), igual en todas las versiones. */
    public static void onServerTick(java.util.function.Consumer<net.minecraft.server.MinecraftServer> c) {
        CoreEvents.TICK.add(c);
    }

    /** Texto con la versión, para que los mods lo muestren o lo comprueben. */
    public static String version() {
        return "Axelis Core " + VERSION + " (API " + API + ")";
    }
}

package com.coleccionista.client;

import com.axeliscore.test.ClientTest;
import com.coleccionista.Coleccionista;
import com.coleccionista.registry.ModRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Lo del cliente: renderizadores, HUD, escenas, fantasmas y el escenario de la prueba con fotos. */
@Mod.EventBusSubscriber(modid = Coleccionista.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    private ModClient() {
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModRegistry.BOT.get(), BotRenderer::new);
        e.registerEntityRenderer(ModRegistry.CADAVER.get(), CadaverRenderer::new);
        e.registerEntityRenderer(ModRegistry.SILLA.get(), SillaRenderer::new);
        e.registerBlockEntityRenderer(ModRegistry.PANTALLA_BE.get(), PantallaRenderer::new);
        e.registerBlockEntityRenderer(com.coleccionista.bloque.deco.Deco.TEXTO_BE, TextoRenderer::new);
        e.registerBlockEntityRenderer(com.coleccionista.bloque.deco.Deco.REJA_BE, RejaRenderer::new);
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.AddLayers e) {
        DisfrazRender.capas(e);
        RolesCapa.capas(e);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        EventoCliente.init();
        EventoHud.init();
        EscenaAviso.init();
        MontacargasCliente.init();
        BarraVida.init();
        MiniTele.init();
        FantasmaRender.init();
        Camara.init();
        Cine.init();
        Espera.init();
        Filtro.init();
        DisfrazRender.init();
        Presentacion.init();
        EncadenadosCliente.init();
        Gas.init();
        CadenaRender.init();
        InscripcionesCliente.init();
        CandadoScreen.registrar();
        UniformeCliente.registrar();
        CompartirCliente.init();
        PuertasCliente.init();
        CopasCliente.init();
        SacrificioCliente.init();
        TraidorCliente.init();
        CiegasCliente.init();
        SentidosCliente.init();
        ObjetivoCliente.init();
        DesmayoCliente.init();
        MascotaCliente.init();
        ViajeCliente.init();
        CeraCliente.init();
        Ambiente.init();
        com.axeliscore.ui.ScreenRegistry.register(Coleccionista.MODID + ":candado", CandadoScreen::new);
        // prueba con fotos: cliente-core.yml con mod=coleccionista y escenario=coleccionista
        ClientTest.scenario(Coleccionista.MODID, PruebaCliente::escenario);
    }
}

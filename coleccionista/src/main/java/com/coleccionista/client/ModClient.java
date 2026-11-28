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
        e.registerEntityRenderer(ModRegistry.CAJA_SORPRESA.get(), CajaSorpresaRenderer::new);
        e.registerEntityRenderer(ModRegistry.MANO_GIGANTE.get(), ManoGiganteRenderer::new);
        e.registerEntityRenderer(ModRegistry.BAILARINA.get(), BailarinaRenderer::new);
        e.registerEntityRenderer(ModRegistry.MAQUINA.get(), MaquinaRenderer::new);
        e.registerEntityRenderer(ModRegistry.ESCOPETA.get(), EscopetaRenderer::new);
        e.registerEntityRenderer(ModRegistry.PUERTA_HOTEL.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, Coleccionista.MODID,
                "puerta_hotel", 0F, 1F));
        e.registerEntityRenderer(ModRegistry.TRAMPA_ESCOPETA.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, Coleccionista.MODID,
                "trampa_escopeta", 0.3F, 1F));
        e.registerEntityRenderer(ModRegistry.BOTONES.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, Coleccionista.MODID, "botones",
                0.9F, 1F));
        e.registerEntityRenderer(ModRegistry.CARRITO.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, Coleccionista.MODID, "carrito",
                0.6F, 1F));
        e.registerEntityRenderer(ModRegistry.RATA.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, Coleccionista.MODID, "rata", 0.25F,
                1F));
        e.registerEntityRenderer(ModRegistry.MONITOR.get(), MonitorRenderer::new);
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
        DesenlaceCliente.init();
        Rodaje.init();
        Espera.init();
        ChatVisible.init();
        Nombres.init();
        ApagonCliente.init();
        Filtro.init();
        DisfrazRender.init();
        Presentacion.init();
        EncadenadosCliente.init();
        Gas.init();
        CadenaRender.init();
        InscripcionesCliente.init();
        CandadoScreen.registrar();
        UniformeCliente.registrar();
        CajaCliente.prendas();
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
        CajaCliente.init();
        SorpresaCliente.init();
        TeCliente.init();
        PianoCliente.init();
        VeintiunoCliente.init();
        SoloPrimera.init();
        ViajeCliente.init();
        CeraCliente.init();
        Ambiente.init();
        MudoCliente.init();
        com.coleccionista.client.inicio.Inicio.init();
        com.axeliscore.ui.ScreenRegistry.register(Coleccionista.MODID + ":candado", CandadoScreen::new);
        // prueba con fotos: cliente-core.yml con mod=coleccionista y escenario=coleccionista
        ClientTest.scenario(Coleccionista.MODID, PruebaCliente::escenario);
        // solo el hotel: escenario=coleccionista_hotel
        ClientTest.scenario(Coleccionista.MODID + "_hotel", PruebaHotel::escenario);
    }
}

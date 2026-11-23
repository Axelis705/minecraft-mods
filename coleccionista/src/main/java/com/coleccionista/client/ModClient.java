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
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        EventoCliente.init();
        EventoHud.init();
        EscenaAviso.init();
        FantasmaRender.init();
        // prueba con fotos: cliente-core.yml con mod=coleccionista y escenario=coleccionista
        ClientTest.scenario(Coleccionista.MODID, PruebaCliente::escenario);
    }
}

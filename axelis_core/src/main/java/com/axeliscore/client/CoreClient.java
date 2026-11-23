package com.axeliscore.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.registry.CoreEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registro del lado del cliente: renderizadores y el reloj de las pruebas automáticas. */
@Mod.EventBusSubscriber(modid = AxelisCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CoreClient {
    private CoreClient() {
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CoreEntities.PERSONA.get(), PersonaRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        com.axeliscore.test.ClientTest.startIfEnabled();
    }
}

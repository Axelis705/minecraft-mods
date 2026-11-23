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
        String ns = AxelisCore.MODID;
        e.registerEntityRenderer(CoreEntities.BESTIA.get(), c -> new SimpleGeoRenderer<>(c, ns, "bestia", 0.6F, 1F));
        e.registerEntityRenderer(CoreEntities.ALADO.get(), c -> new SimpleGeoRenderer<>(c, ns, "alado", 0.4F, 1F));
        e.registerEntityRenderer(CoreEntities.GELATINA.get(), c -> new SimpleGeoRenderer<>(c, ns, "gelatina", 0.5F, 1F));
        e.registerEntityRenderer(CoreEntities.COLOSO.get(), c -> new SimpleGeoRenderer<>(c, ns, "coloso", 1.2F, 1F));
        e.registerEntityRenderer(CoreEntities.CHISPA.get(), c -> new SimpleGeoRenderer<>(c, ns, "chispa", 0.3F, 1F));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        com.axeliscore.test.ClientTest.startIfEnabled();
    }
}

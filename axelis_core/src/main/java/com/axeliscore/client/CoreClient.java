package com.axeliscore.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.registry.CoreEntities;
import net.minecraftforge.api.distmarker.Dist;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.G;
import net.minecraftforge.client.event.EntityRenderersEvent;
//#if MC >= 1.20.5
//$$ import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
//#else
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
//#endif
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
        e.registerEntityRenderer(CoreEntities.COLOSO.get(), c -> new com.axeliscore.combat.client.JefeRenderer<>(c, ns, "coloso", 1.2F, 1F, 3));
        e.registerEntityRenderer(CoreEntities.CHISPA.get(), c -> new SimpleGeoRenderer<>(c, ns, "chispa", 0.3F, 1F));
        e.registerEntityRenderer(CoreEntities.AVISO.get(), com.axeliscore.combat.client.AvisoRenderer::new);
        e.registerEntityRenderer(CoreEntities.PROYECTIL.get(), com.axeliscore.combat.client.ProyectilRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        CoreClientEvents.register();
        com.axeliscore.combat.client.CombateHud.init();
        com.axeliscore.combat.client.MusicaJefe.init();
        com.axeliscore.ui.ScreenRegistry.register(AxelisCore.MODID + ":demo", com.axeliscore.ui.DemoScreen::new);
        com.axeliscore.test.ClientTest.startIfEnabled();
    }

    /** Capa del Core encima de todo: destellos, viñetas, títulos grandes y avisos. */
    //#if MC >= 1.20.5
    //$$ @SubscribeEvent
    //$$ public static void onOverlays(AddGuiOverlayLayersEvent e) {
    //$$     e.getLayeredDraw().add(Ids.core("fx"), (gg, delta) -> com.axeliscore.fx.client.FxClient.render(new G(gg), gg.guiWidth(), gg.guiHeight()));
    //$$ }
    //#else
    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("fx", (gui, gg, pt, w, h) -> com.axeliscore.fx.client.FxClient.render(new G(gg), w, h));
    }
    //#endif
}

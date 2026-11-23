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
        String ns = AxelisCore.MODID;
        if (com.axeliscore.Ejemplos.activos()) {
            e.registerEntityRenderer(CoreEntities.PERSONA.get(), PersonaRenderer::new);
            e.registerEntityRenderer(CoreEntities.BESTIA.get(), c -> new SimpleGeoRenderer<>(c, ns, "bestia", 0.6F, 1F));
            e.registerEntityRenderer(CoreEntities.ALADO.get(), c -> new SimpleGeoRenderer<>(c, ns, "alado", 0.4F, 1F));
            e.registerEntityRenderer(CoreEntities.GELATINA.get(), c -> new SimpleGeoRenderer<>(c, ns, "gelatina", 0.5F, 1F));
            e.registerEntityRenderer(CoreEntities.COLOSO.get(), c -> new com.axeliscore.combat.client.JefeRenderer<>(c, ns, "coloso", 1.2F, 1F, 3));
            e.registerEntityRenderer(CoreEntities.CHISPA.get(), c -> new SimpleGeoRenderer<>(c, ns, "chispa", 0.3F, 1F));
        }
        e.registerEntityRenderer(CoreEntities.NPC.get(), c -> new PersonaRenderer<>(c, com.axeliscore.npc.client.NpcCliente::piel));
        e.registerEntityRenderer(CoreEntities.AVISO.get(), com.axeliscore.combat.client.AvisoRenderer::new);
        e.registerEntityRenderer(CoreEntities.PROYECTIL.get(), com.axeliscore.combat.client.ProyectilRenderer::new);
    }

    /** La ropa del Vestuario encima de los dos modelos de jugador (brazos normales y delgados). */
    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.AddLayers e) {
        //#if MC >= 26.1
        //$$ for (net.minecraft.world.entity.player.PlayerModelType t : e.getModelTypes()) {
        //$$     net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> r = e.getPlayerRenderer(t);
        //$$     if (r != null) {
        //$$         r.addLayer(new com.axeliscore.vestir.client.CapaVestuario(r));
        //$$     }
        //$$ }
        //#elif MC >= 1.20.2
        //$$ for (net.minecraft.client.resources.PlayerSkin.Model t : e.getSkins()) {
        //$$     net.minecraft.client.renderer.entity.player.PlayerRenderer r = e.getSkin(t);
        //$$     if (r != null) {
        //$$         r.addLayer(new com.axeliscore.vestir.client.CapaVestuario(r));
        //$$     }
        //$$ }
        //#else
        for (String t : e.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer r = e.getSkin(t);
            if (r != null) {
                r.addLayer(new com.axeliscore.vestir.client.CapaVestuario(r));
            }
        }
        //#endif
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        com.axeliscore.vestir.client.VestuarioCliente.init();
        if (com.axeliscore.Ejemplos.activos()) {
            // prendas de muestra (para las pruebas y como ejemplo): un overol tipo piel con número y un sombrero de Blockbench
            com.axeliscore.vestir.client.VestuarioCliente.registrar(Ids.core("overol"), com.axeliscore.vestir.client.Prenda
                    .piel(Ids.core("textures/prenda/overol.png")).sin(com.axeliscore.vestir.client.Prenda.Parte.CABEZA,
                            com.axeliscore.vestir.client.Prenda.Parte.BRAZO_DER, com.axeliscore.vestir.client.Prenda.Parte.BRAZO_IZQ)
                    .decorar((c, l) -> {
                        String n = c.datos().getString("numero");
                        if (!n.isEmpty()) {
                            l.texto(com.axeliscore.vestir.client.Prenda.Parte.CUERPO, n, 0F, 6.5F, -2F - l.inflado() - 0.03F, 0.3F, 0xFFEFE6C8,
                                    false);
                        }
                    }));
            com.axeliscore.vestir.client.VestuarioCliente.registrar(Ids.core("sombrero"), com.axeliscore.vestir.client.Prenda
                    .modelo(Ids.core("geo/sombrero.geo.json"), Ids.core("textures/prenda/sombrero.png")));
        }
        CoreClientEvents.register();
        com.axeliscore.combat.client.CombateHud.init();
        com.axeliscore.combat.client.MusicaJefe.init();
        com.axeliscore.npc.client.NpcCliente.init();
        com.axeliscore.fx.client.EfectosCliente.init();
        com.axeliscore.fx.client.Hud.add((g, w, h) -> com.axeliscore.anim.client.EstudioArchivos.revisar());
        com.axeliscore.ui.ScreenRegistry.register(AxelisCore.MODID + ":demo", com.axeliscore.ui.DemoScreen::new);
        com.axeliscore.ui.ScreenRegistry.register(com.axeliscore.anim.mc.EstudioServidor.PANTALLA, com.axeliscore.anim.client.EstudioScreen::new);
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

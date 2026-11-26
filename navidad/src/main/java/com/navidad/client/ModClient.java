package com.navidad.client;

import com.navidad.Navidad;
import com.axeliscore.test.ClientTest;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Lo del cliente: renderizadores, pantallas y el escenario de la prueba con fotos. */
@Mod.EventBusSubscriber(modid = Navidad.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    private ModClient() {
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        // e.registerEntityRenderer(ModRegistry.ALGO.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, Navidad.MODID, "algo", 0.5F, 1F));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        // prueba con fotos: cliente-core.yml con mod=navidad y escenario=navidad
        ClientTest.scenario(Navidad.MODID, t -> {
            t.add("preparar", 2000, () -> {
                t.cmd("gamemode creative @a");
                t.cmd("reload");
            });
            t.add("comando", 1500, () -> t.cmd(Navidad.MODID));
            t.photo("inicio", 1500, () -> t.lookFrom(0.5, -60, -4.5, 0.5, -59, 0.5));
        });
    }
}

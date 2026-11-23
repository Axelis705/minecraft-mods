package com.pescacoleccion.client;

import com.pescacoleccion.PescaColeccion;
import com.axeliscore.test.ClientTest;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Lo del cliente: renderizadores, pantallas y el escenario de la prueba con fotos. */
@Mod.EventBusSubscriber(modid = PescaColeccion.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    private ModClient() {
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        // e.registerEntityRenderer(ModRegistry.ALGO.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, PescaColeccion.MODID, "algo", 0.5F, 1F));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        // prueba con fotos: cliente-core.yml con mod=pesca_coleccion y escenario=pesca_coleccion
        ClientTest.scenario(PescaColeccion.MODID, t -> {
            t.add("preparar", 2000, () -> {
                t.cmd("gamemode creative @a");
                t.cmd("reload");
                t.hud(true);
                t.cmd("time set 6000");
                t.cmd("weather clear");
            });
            t.add("comando", 1500, () -> t.cmd(PescaColeccion.MODID));
            t.add("se juega sola", 100, () -> PeleaScreen.autoplay = true);
            // pelea de un pez raro: fotos del ritmo, del salto y del final
            t.add("pelea rara", 300, () -> {
                t.lookFrom(0.5, -60, -4.5, 0.5, -60.5, 4.5);
                t.cmd("execute as @p run " + PescaColeccion.MODID + " pelea 2");
            });
            t.waitFor("se abre la pelea", 8000, () -> com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen);
            t.photo("pelea_ritmo", 3200, () -> {
            });
            t.waitFor("salto", 40000, () -> com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen ps
                    && ps.sim().phase == com.pescacoleccion.pelea.PeleaSim.SALTO && ps.sim().jumpProgress() > 0.45F);
            t.add("salto (foto)", 300, () -> t.shot("pelea_salto"));
            t.waitFor("final", 60000, () -> com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen ps
                    && ps.sim().phase == com.pescacoleccion.pelea.PeleaSim.FINAL);
            t.add("final (foto)", 150, () -> t.shot("pelea_final"));
            t.waitFor("termina la pelea rara", 30000, () -> !(com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen));
            t.add("premio raro", 1200, () -> t.check(net.minecraft.client.Minecraft.getInstance().player.getInventory()
                    .contains(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TROPICAL_FISH)), "el pez raro llegó al inventario"));
            t.add("foto del aviso", 100, () -> t.shot("pelea_premio"));
            // legendario: más rápido, trampas y flechas fantasma
            t.add("pelea legendaria", 300, () -> t.cmd("execute as @p run " + PescaColeccion.MODID + " pelea 4"));
            t.waitFor("se abre la legendaria", 8000, () -> com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen);
            t.photo("pelea_legendaria", 4000, () -> {
            });
            t.waitFor("termina la legendaria", 120000, () -> !(com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen));
            t.add("premio legendario", 1500, () -> t.check(net.minecraft.client.Minecraft.getInstance().player.getInventory()
                    .contains(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.HEART_OF_THE_SEA)), "el pez legendario llegó"));
            t.add("título", 100, () -> t.shot("pelea_legendaria_premio"));
            t.add("sin juego solo", 100, () -> PeleaScreen.autoplay = false);
        });
    }
}

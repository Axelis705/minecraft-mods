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
        e.registerEntityRenderer(com.pescacoleccion.registry.ModRegistry.ANZUELO.get(), AnzueloRenderer::new);
    }

    /** El corcho del jugador visto en el cliente (para las pruebas). */
    static com.pescacoleccion.pesca.Anzuelo corcho() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var l = mc.level.getEntitiesOfClass(com.pescacoleccion.pesca.Anzuelo.class, mc.player.getBoundingBox().inflate(32));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Corre algo en el servidor del mundo de prueba (después de los comandos ya pedidos). */
    static void onServer(java.util.function.Consumer<net.minecraft.server.level.ServerLevel> fn) {
        var s = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        s.execute(() -> fn.accept(s.overworld()));
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
            // la caña de verdad: estanque, lanzar, esperar la picada buena, tirar y pelear
            t.add("estanque", 1500, () -> {
                t.cmd("fill -4 -62 3 4 -61 10 water");
                t.cmd("item replace entity @p weapon.mainhand with " + PescaColeccion.MODID + ":cana_bambu");
                t.lookFrom(0.5, -60, -1.5, 0.5, -61.2, 5.5);
            });
            t.add("lanzar", 1800, () -> onServer(l -> com.pescacoleccion.pesca.Anzuelo.lanzar(
                    l.getServer().getPlayerList().getPlayers().get(0), com.pescacoleccion.pesca.Cana.BAMBU)));
            t.photo("corcho_flota", 1200, () -> {
            });
            t.waitFor("picada buena", 60000, () -> corcho() != null && corcho().state() == com.pescacoleccion.pesca.Anzuelo.PICA);
            t.add("foto de la picada", 60, () -> t.shot("corcho_pica"));
            t.add("tirar", 50, () -> onServer(l -> {
                var sp = l.getServer().getPlayerList().getPlayers().get(0);
                var a = com.pescacoleccion.pesca.Anzuelo.activo(sp);
                if (a != null) {
                    a.tirar(sp);
                }
            }));
            t.waitFor("se abre la pelea del corcho", 5000, () -> com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen);
            t.photo("pelea_corcho", 2500, () -> {
            });
            t.waitFor("termina la pelea del corcho", 90000, () -> !(com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen));
            t.add("premio del corcho", 2500, () -> {
                var inv = net.minecraft.client.Minecraft.getInstance().player.getInventory();
                boolean any = false;
                for (var it : new net.minecraft.world.item.Item[] {net.minecraft.world.item.Items.COD, net.minecraft.world.item.Items.SALMON,
                        net.minecraft.world.item.Items.TROPICAL_FISH, net.minecraft.world.item.Items.PUFFERFISH,
                        net.minecraft.world.item.Items.HEART_OF_THE_SEA}) {
                    any |= inv.contains(new net.minecraft.world.item.ItemStack(it));
                }
                t.check(any, "el pez del corcho llegó al inventario");
            });
            t.add("vaciar", 300, () -> t.cmd("clear @p"));
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

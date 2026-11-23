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

    /** ¿Tiene el jugador un pez de la colección (de esa rareza; -1 = cualquiera)? */
    static boolean tienePez(int rareza) {
        var inv = net.minecraft.client.Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof com.pescacoleccion.pesca.PezItem pz && (rareza < 0 || pz.especie().rareza() == rareza)) {
                return true;
            }
        }
        return false;
    }

    /** Lombrices que salieron al cavar en la prueba (-1 = todavía no se sabe). */
    static volatile int lombrices = -1;

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
        com.axeliscore.ui.ScreenRegistry.register(PescaColeccion.MODID + ":libro", LibroScreen::new);
        // estrellas de maestría del jugador (la copia que manda el servidor) para los cuadros de los peces
        com.pescacoleccion.pesca.PezItem.estrellas = id -> {
            var d = com.axeliscore.data.client.ClientData.player(PescaColeccion.MODID);
            var peces = com.axeliscore.plat.Nbt.c(d, "peces");
            return com.axeliscore.plat.Nbt.i(com.axeliscore.plat.Nbt.c(peces, id), "e");
        };
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
                // carnada en la otra mano; en supervivencia para ver que el pez se la come al picar
                t.cmd("item replace entity @p weapon.offhand with " + PescaColeccion.MODID + ":cebo_brillante 5");
                t.cmd("gamemode survival @a");
                t.lookFrom(0.5, -60, -1.5, 0.5, -61.2, 5.5);
            });
            t.add("lanzar", 1800, () -> onServer(l -> com.pescacoleccion.pesca.Anzuelo.lanzar(
                    l.getServer().getPlayerList().getPlayers().get(0), com.pescacoleccion.pesca.Cana.BAMBU)));
            t.add("caña lanzada", 600, () -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                t.check(com.pescacoleccion.pesca.Anzuelo.lanzada(mc.player), "la caña se dibuja lanzada (sin el hilo que cuelga)");
                t.check(mc.player.getOffhandItem().getCount() == 5, "la carnada no se gasta al lanzar");
            });
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
            t.photo("pelea_corcho", 2500, () -> t.check(net.minecraft.client.Minecraft.getInstance().player.getOffhandItem().getCount() == 4,
                    "el pez se comió una carnada al picar"));
            t.waitFor("termina la pelea del corcho", 90000, () -> !(com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen));
            t.waitFor("el pez del corcho llegó al inventario", 10000, () -> tienePez(-1));
            t.add("caña sin lanzar", 300, () -> t.check(!com.pescacoleccion.pesca.Anzuelo.lanzada(net.minecraft.client.Minecraft.getInstance().player),
                    "la caña vuelve a tener su hilo"));
            // lombrices: cavar pasto bajo la lluvia (en supervivencia, con el evento de romper bloques de verdad)
            t.add("lluvia", 2500, () -> t.cmd("weather rain"));
            t.add("cavar", 600, () -> {
                lombrices = -1;
                onServer(l -> {
                    var sp = l.getServer().getPlayerList().getPlayers().get(0);
                    var lombriz = com.pescacoleccion.registry.ModRegistry.CEBOS.get(com.pescacoleccion.pesca.Cebo.LOMBRIZ).get();
                    int antes = sp.getInventory().countItem(lombriz);
                    for (int i = 0; i < 40; i++) {
                        net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(-8 - i % 8, -61, 14 + i / 8);
                        l.setBlockAndUpdate(bp, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
                        sp.gameMode.destroyBlock(bp);
                    }
                    int sueltas = 0;
                    for (var it : l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, sp.getBoundingBox().inflate(40))) {
                        if (it.getItem().getItem() == lombriz) {
                            sueltas += it.getItem().getCount();
                        }
                    }
                    lombrices = sueltas + sp.getInventory().countItem(lombriz) - antes;
                });
            });
            t.add("lombrices", 600, () -> t.check(lombrices > 0, "salen lombrices al cavar pasto bajo la lluvia (" + lombrices + " de 40)"));
            t.add("vaciar", 300, () -> {
                t.cmd("weather clear");
                t.cmd("gamemode creative @a");
                t.cmd("kill @e[type=item]");
                t.cmd("clear @p");
            });
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
            t.add("premio raro", 1200, () -> t.check(tienePez(2), "el pez raro llegó al inventario"));
            t.add("foto del aviso", 100, () -> t.shot("pelea_premio"));
            // legendario: más rápido, trampas y flechas fantasma
            t.add("pelea legendaria", 300, () -> t.cmd("execute as @p run " + PescaColeccion.MODID + " pelea 4"));
            t.waitFor("se abre la legendaria", 8000, () -> com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen);
            t.photo("pelea_legendaria", 4000, () -> {
            });
            t.waitFor("termina la legendaria", 120000, () -> !(com.axeliscore.plat.client.Screens.current() instanceof PeleaScreen));
            t.add("premio legendario", 1500, () -> t.check(tienePez(4), "el pez legendario llegó"));
            t.add("título", 100, () -> t.shot("pelea_legendaria_premio"));
            t.add("sin juego solo", 100, () -> PeleaScreen.autoplay = false);
            // el libro de pesca
            t.photo("libro_peces", 1400, () -> {
                com.axeliscore.plat.client.Screens.open(new LibroScreen());
            });
            t.photo("libro_cuadro", 900, () -> {
                if (com.axeliscore.plat.client.Screens.current() instanceof LibroScreen l) {
                    l.pick("patriarca");
                    com.axeliscore.ui.UiScreen.fakeMouse = l.cellCenter(14);
                }
            });
            t.photo("libro_pista", 900, () -> {
                if (com.axeliscore.plat.client.Screens.current() instanceof LibroScreen l) {
                    com.axeliscore.ui.UiScreen.fakeMouse = l.cellCenter(2);
                }
            });
            t.photo("libro_mejoras", 900, () -> {
                com.axeliscore.ui.UiScreen.fakeMouse = null;
                if (com.axeliscore.plat.client.Screens.current() instanceof LibroScreen l) {
                    l.showTab(1);
                }
            });
            t.photo("libro_carnada", 900, () -> {
                if (com.axeliscore.plat.client.Screens.current() instanceof LibroScreen l) {
                    com.axeliscore.ui.UiScreen.fakeMouse = l.rowCenter(6);
                }
            });
            t.photo("libro_records", 900, () -> {
                com.axeliscore.ui.UiScreen.fakeMouse = null;
                if (com.axeliscore.plat.client.Screens.current() instanceof LibroScreen l) {
                    l.showTab(2);
                }
            });
            t.add("cerrar libro", 300, () -> com.axeliscore.plat.client.Screens.close());
        });
    }
}

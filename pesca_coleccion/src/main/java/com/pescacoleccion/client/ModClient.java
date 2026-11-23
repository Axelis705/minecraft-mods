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
        e.registerBlockEntityRenderer(com.pescacoleccion.registry.ModRegistry.TROFEO_BE.get(), TrofeoRenderer::new);
        e.registerBlockEntityRenderer(com.pescacoleccion.registry.ModRegistry.ACUARIO_BE.get(), AcuarioRenderer::new);
        e.registerEntityRenderer(com.pescacoleccion.registry.ModRegistry.NILO.get(),
                com.axeliscore.client.PersonaRenderer.with(com.pescacoleccion.nilo.NiloMundo.PIEL));
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

    /** Cuántos de ese objeto lleva el jugador (inventario principal y la otra mano). */
    static int cuenta(net.minecraft.world.item.Item it) {
        int n = 0;
        for (var st : com.axeliscore.plat.Plat.mainAndOffhand(net.minecraft.client.Minecraft.getInstance().player.getInventory())) {
            if (st.getItem() == it) {
                n += st.getCount();
            }
        }
        return n;
    }

    static int antes;
    static volatile int trofeosListos;

    /** Nilo visto en el cliente (para las pruebas). */
    static com.pescacoleccion.nilo.Nilo nilo() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var l = mc.level.getEntitiesOfClass(com.pescacoleccion.nilo.Nilo.class, mc.player.getBoundingBox().inflate(48));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Tablones del muelle de Nilo en el estanque de la prueba. */
    static int tablones() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        int n = 0;
        for (int x = -5; x <= 5; x++) {
            for (int z = 2; z <= 11; z++) {
                if (mc.level.getBlockState(new net.minecraft.core.BlockPos(x, -61, z)).is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS)) {
                    n++;
                }
            }
        }
        return n;
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
        com.axeliscore.ui.ScreenRegistry.register(PescaColeccion.MODID + ":nilo", NiloScreen::new);
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
                // sin monstruos: en el mundo plano salen slimes y en supervivencia matarían al jugador a mitad de la prueba
                t.cmd("difficulty peaceful");
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
            // Nilo: llega solo con el primer pez, saluda, arma su muelle y se pone a pescar
            t.waitFor("llega Nilo", 15000, () -> nilo() != null);
            t.waitFor("Nilo saluda", 40000, () -> com.axeliscore.plat.client.Screens.current() instanceof com.axeliscore.ui.DialogScreen);
            t.photo("nilo_saludo", 1800, () -> {
            });
            t.add("seguir", 300, () -> com.axeliscore.plat.client.Screens.close());
            t.waitFor("muelle listo", 20000, () -> tablones() >= 15);
            t.waitFor("Nilo en su muelle", 25000, () -> nilo() != null && nilo().getZ() > 4.5);
            t.photo("nilo_muelle", 2600, () -> t.lookFrom(6.5, -60, 0.5, 0.5, -59.5, 6.5));
            t.add("acercarse", 800, () -> t.cmd("tp @p 0.5 -60 1.5 0 0"));
            t.add("hablar con Nilo", 300, () -> onServer(l -> {
                var sp = l.getServer().getPlayerList().getPlayers().get(0);
                var ns = l.getEntitiesOfClass(com.pescacoleccion.nilo.Nilo.class, sp.getBoundingBox().inflate(16));
                if (!ns.isEmpty()) {
                    com.pescacoleccion.nilo.NiloMundo.hablar(sp, ns.get(0));
                }
            }));
            t.waitFor("se abre la ventana de Nilo", 6000, () -> com.axeliscore.plat.client.Screens.current() instanceof NiloScreen);
            t.photo("nilo_encargos", 1200, () -> {
            });
            // cumplir el primer encargo (entregando peces o con el progreso lleno) y cobrar
            t.add("cumplir encargo", 1500, () -> onServer(l -> {
                var sp = l.getServer().getPlayerList().getPlayers().get(0);
                var q = com.axeliscore.plat.Nbt.at(com.axeliscore.plat.Nbt.list(com.pescacoleccion.nilo.Encargos.datos(sp), "encargos"), 0);
                String tipo = com.axeliscore.plat.Nbt.s(q, "t");
                int n = Math.max(1, com.axeliscore.plat.Nbt.i(q, "n"));
                boolean entrega = tipo.equals("entrega") || tipo.equals("grande");
                if (entrega) {
                    var es = com.pescacoleccion.pesca.Especies.get(com.axeliscore.plat.Nbt.s(q, "e"));
                    for (int i = 0; i < n; i++) {
                        com.axeliscore.plat.Plat.give(sp, com.pescacoleccion.pesca.PezItem.crear(com.pescacoleccion.registry.ModRegistry.PECES.get(es.id()).get(),
                                es, es.cmMax(), "prueba", false));
                    }
                } else {
                    q.putInt("p", n);
                }
                com.pescacoleccion.nilo.Encargos.accion(sp, 0, entrega ? "entregar" : "cobrar");
            }));
            t.add("premio del encargo", 300, () -> t.check(com.pescacoleccion.nilo.Tienda.esmeraldas(net.minecraft.client.Minecraft.getInstance().player) > 0,
                    "Nilo pagó el encargo con esmeraldas"));
            t.photo("nilo_cobrado", 800, () -> {
            });
            // tienda: comprar lombrices y venderle un pez
            t.add("pez para vender", 600, () -> onServer(l -> {
                var sp = l.getServer().getPlayerList().getPlayers().get(0);
                var es = com.pescacoleccion.pesca.Especies.get("pez_espada");
                com.axeliscore.plat.Plat.give(sp, com.pescacoleccion.pesca.PezItem.crear(com.pescacoleccion.registry.ModRegistry.PECES.get(es.id()).get(), es,
                        es.cmMax() * 0.98F, "prueba", false));
            }));
            t.photo("nilo_tienda", 1000, () -> {
                if (com.axeliscore.plat.client.Screens.current() instanceof NiloScreen ns) {
                    ns.showTab(1);
                }
            });
            t.add("comprar", 1000, () -> {
                antes = cuenta(com.pescacoleccion.registry.ModRegistry.CEBOS.get(com.pescacoleccion.pesca.Cebo.LOMBRIZ).get());
                var tag = new net.minecraft.nbt.CompoundTag();
                tag.putInt("oferta", 0);
                com.axeliscore.data.client.ClientData.send(PescaColeccion.MODID + ":nilo_comprar", tag);
            });
            t.add("compró", 300, () -> t.check(cuenta(com.pescacoleccion.registry.ModRegistry.CEBOS.get(com.pescacoleccion.pesca.Cebo.LOMBRIZ).get()) >= antes + 8,
                    "le compró 8 lombrices a Nilo"));
            t.add("vender", 1000, () -> {
                var inv = com.axeliscore.plat.Plat.mainItems(net.minecraft.client.Minecraft.getInstance().player.getInventory());
                for (int i = 0; i < inv.size(); i++) {
                    if (inv.get(i).getItem() instanceof com.pescacoleccion.pesca.PezItem pz && pz.especie().id().equals("pez_espada")) {
                        var tag = new net.minecraft.nbt.CompoundTag();
                        tag.putInt("slot", i);
                        com.axeliscore.data.client.ClientData.send(PescaColeccion.MODID + ":nilo_vender", tag);
                        break;
                    }
                }
            });
            t.add("vendió", 300, () -> t.check(cuenta(com.pescacoleccion.registry.ModRegistry.PECES.get("pez_espada").get()) == 0, "le vendió el pez a Nilo"));
            // amistad hasta el último nivel: su regalo es su caña
            t.add("amistad", 1200, () -> onServer(l -> {
                var sp = l.getServer().getPlayerList().getPlayers().get(0);
                com.pescacoleccion.nilo.Encargos.sumarAmistad(sp, com.pescacoleccion.nilo.Encargos.datos(sp), 60);
                com.axeliscore.data.PlayerStore.save(sp, PescaColeccion.MODID);
            }));
            t.photo("nilo_amistad", 1400, () -> {
                if (com.axeliscore.plat.client.Screens.current() instanceof NiloScreen ns) {
                    ns.showTab(2);
                }
            });
            t.add("caña de Nilo", 300, () -> t.check(cuenta(com.pescacoleccion.registry.ModRegistry.CANAS.get(com.pescacoleccion.pesca.Cana.NILO).get()) > 0,
                    "Nilo regaló su caña"));
            t.add("cerrar", 600, () -> com.axeliscore.plat.client.Screens.close());
            t.photo("nilo_pescando", 3500, () -> t.lookFrom(4.5, -60, 2.5, 0.5, -59, 6.5));
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
            // trofeos en la pared y acuarios con peces que nadan
            t.add("pared", 800, () -> {
                t.cmd("fill 8 -60 -6 16 -57 -6 minecraft:stone_bricks");
                for (int x : new int[] {10, 12, 14}) {
                    t.cmd("setblock " + x + " -58 -5 " + PescaColeccion.MODID + ":trofeo[facing=south]");
                }
                t.cmd("setblock 10 -60 -3 " + PescaColeccion.MODID + ":acuario");
                t.cmd("setblock 14 -60 -3 " + PescaColeccion.MODID + ":acuario");
            });
            t.add("montar y soltar", 1200, () -> onServer(l -> {
                var sp = l.getServer().getPlayerList().getPlayers().get(0);
                String[] trofeos = {"pez_espada", "patriarca", "koi_cerezo"};
                int[] xs = {10, 12, 14};
                for (int i = 0; i < 3; i++) {
                    var es = com.pescacoleccion.pesca.Especies.get(trofeos[i]);
                    var st = com.pescacoleccion.pesca.PezItem.crear(com.pescacoleccion.registry.ModRegistry.PECES.get(es.id()).get(), es,
                            es.cmMin() + (es.cmMax() - es.cmMin()) * 0.8F, "Dev", false);
                    if (l.getBlockEntity(new net.minecraft.core.BlockPos(xs[i], -58, -5)) instanceof com.pescacoleccion.deco.TrofeoBE be) {
                        com.pescacoleccion.deco.TrofeoBlock.montar(be, sp, st);
                    }
                }
                String[][] peces = {{"payaso", "pez_globo", "koi_cerezo"}, {"pez_linterna", "trucha_arcoiris", "mojarra"}};
                int[] ax = {10, 14};
                trofeosListos = 0;
                for (int a = 0; a < 2; a++) {
                    if (l.getBlockEntity(new net.minecraft.core.BlockPos(ax[a], -60, -3)) instanceof com.pescacoleccion.deco.AcuarioBE be) {
                        for (String id : peces[a]) {
                            var es = com.pescacoleccion.pesca.Especies.get(id);
                            var st = com.pescacoleccion.pesca.PezItem.crear(com.pescacoleccion.registry.ModRegistry.PECES.get(es.id()).get(), es,
                                    es.cmMax() * 0.7F, "Dev", false);
                            com.pescacoleccion.deco.AcuarioBlock.soltar(be, sp, st);
                        }
                        trofeosListos += be.peces.size();
                    }
                }
            }));
            t.add("datos en el cliente", 600, () -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                boolean ok = mc.level.getBlockEntity(new net.minecraft.core.BlockPos(12, -58, -5)) instanceof com.pescacoleccion.deco.TrofeoBE be
                        && !be.pez.isEmpty();
                t.check(ok, "el trofeo llegó al cliente con su pez");
                t.check(trofeosListos == 6, "6 peces en los acuarios (" + trofeosListos + ")");
                boolean ac = mc.level.getBlockEntity(new net.minecraft.core.BlockPos(14, -60, -3)) instanceof com.pescacoleccion.deco.AcuarioBE be
                        && be.peces.size() == 3;
                t.check(ac, "el acuario llegó al cliente con sus peces");
            });
            t.photo("trofeos_acuarios", 2000, () -> t.lookFrom(12.5, -60, 1.5, 12.5, -59, -5));
            t.photo("acuario_cerca", 2000, () -> t.lookFrom(14.5, -60, -0.3, 14.5, -60.4, -3));
            t.photo("trofeo_cuadro", 1500, () -> t.lookFrom(12.5, -60, -2.5, 12.5, -57.6, -5.4));
        });
    }
}

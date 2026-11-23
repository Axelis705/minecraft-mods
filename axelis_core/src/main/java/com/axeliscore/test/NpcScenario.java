package com.axeliscore.test;

import com.axeliscore.npc.NpcAcciones;
import com.axeliscore.npc.client.NpcCliente;
import com.axeliscore.npc.client.NpcScreen;

/**
 * Prueba con fotos de los NPC: el "!" sobre la cabeza, la ventana (hablar, misiones, tienda, amistad), aceptar y entregar una misión
 * con los botones, comprar, el rastreador de la derecha al cazar y el diario.
 */
final class NpcScenario {
    private static final double G = CoreScenario.G;

    private NpcScenario() {
    }

    static NpcScreen pantalla() {
        return com.axeliscore.plat.client.Screens.current() instanceof NpcScreen s ? s : null;
    }

    static void conPantalla(java.util.function.Consumer<NpcScreen> fn) {
        NpcScreen s = pantalla();
        if (s == null) {
            throw new AssertionError("no está abierta la ventana del NPC");
        }
        fn.accept(s);
    }

    /** Abre la ventana del NPC más cercano (como si se le hubiera hecho clic). */
    static void hablarCon(String npc) {
        CoreScenario.onServer(l -> {
            var p = l.players().get(0);
            for (var n : l.getEntitiesOfClass(com.axeliscore.npc.Npc.class, p.getBoundingBox().inflate(16))) {
                if (n.defId().equals(npc)) {
                    com.axeliscore.npc.NpcAcciones.abrir(p, n);
                    return;
                }
            }
        });
    }

    static void npcs(ClientTest.Script t) {
        t.add("npc: poner", 1800, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.hud(true);
            t.cmd("kill @e[type=!player]");
            t.cmd("clear @a");
            t.cmd("time set 6000");
            t.cmd("execute as @p run axeliscore npc reiniciar");
            t.cmd("execute positioned 0.5 -60 4.5 run axeliscore npc poner axelis_core:mara");
            t.cmd("execute positioned 3.5 -60 4.5 run axeliscore npc poner axelis_core:iker");
            CoreScenario.onServer(l -> {
                for (var n : l.getEntitiesOfClass(com.axeliscore.npc.Npc.class, new net.minecraft.world.phys.AABB(-8, -62, -2, 10, -55, 12))) {
                    n.setNoAi(true);
                    n.setYRot(180);
                    n.yBodyRot = 180;
                    n.yHeadRot = 180;
                }
            });
            t.lookFrom(2.0, G, -0.5, 2.0, G + 1.7, 4.5);
        });
        t.add("npc en el cliente", 100, () -> {
            var l = CoreScenario.cerca(com.axeliscore.npc.Npc.class);
            t.check(l.size() == 2 && l.stream().allMatch(n -> n.def() != null), "se ven los dos NPC con su definición");
            t.check(NpcCliente.marca(com.axeliscore.npc.CoreNpcs.MARA) == 1, "Mara tiene una misión nueva (!)");
        });
        t.photo("20_npc_marcas", 400, () -> {
        });
        t.photo("21_npc_hablar", 1600, () -> hablarCon("axelis_core:mara"));
        t.add("abrió en misiones", 100, () -> conPantalla(s -> t.check(s.pestana() == 1, "con una misión nueva abre en Misiones")));
        t.photo("21_npc_hablar_saludo", 1400, () -> conPantalla(s -> s.mostrar(0)));
        t.photo("22_npc_misiones", 600, () -> conPantalla(s -> s.elegir("axelis_core:pan", -1)));
        t.add("aceptar", 900, () -> conPantalla(s -> {
            s.mostrar(1);
            t.check(s.pulsar("axelis_core.mision.aceptar"), "se puede aceptar la misión del pan");
        }));
        t.add("aceptada", 100, () -> t.check(NpcCliente.estado(com.axeliscore.npc.Misiones.porId("axelis_core:pan"))
                == com.axeliscore.npc.Misiones.Estado.ACTIVA, "la misión quedó activa"));
        t.add("dar pan", 0, () -> t.cmd("give @a minecraft:bread 6"));
        t.waitFor("la misión está lista", 4000, () -> NpcCliente.estado(com.axeliscore.npc.Misiones.porId("axelis_core:pan"))
                == com.axeliscore.npc.Misiones.Estado.LISTA);
        t.photo("23_npc_mision_lista", 500, () -> {
        });
        t.add("entregar", 100, () -> conPantalla(s -> t.check(s.pulsar("axelis_core.mision.entregar"), "se puede entregar")));
        t.waitFor("premio y regalo", 4000, () -> NpcCliente.contar("minecraft:emerald") == 4 && NpcCliente.contar("minecraft:bread") == 8
                && NpcCliente.puntos("axelis_core:mara") == 10);
        t.photo("24_npc_entregada", 500, () -> {
        });
        t.photo("25_npc_tienda", 700, () -> conPantalla(s -> {
            s.mostrar(2);
            s.elegir("", 0);
            com.axeliscore.ui.UiScreen.fakeMouse = s.fila(5);
        }));
        t.add("comprar", 100, () -> conPantalla(s -> {
            com.axeliscore.ui.UiScreen.fakeMouse = null;
            t.check(s.pulsar("axelis_core.tienda.comprar"), "se puede comprar pan");
        }));
        t.waitFor("compró", 4000, () -> NpcCliente.contar("minecraft:emerald") == 3 && NpcCliente.contar("minecraft:bread") == 12);
        t.photo("25_npc_tienda_comprado", 400, () -> {
        });
        t.photo("26_npc_amistad", 600, () -> conPantalla(s -> s.mostrar(3)));
        // cazar: la misión de Iker con el rastreador a la derecha
        t.add("cazar", 1500, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("execute as @p run axeliscore npc mision axelis_core:zombis");
            t.cmd("summon minecraft:zombie 7.5 -60 9.5 {NoAI:1b}");
            t.cmd("summon minecraft:zombie 8.5 -60 9.5 {NoAI:1b}");
        });
        t.add("derrotar", 1500, () -> t.cmd("execute as @e[type=minecraft:zombie] run damage @s 100 minecraft:player_attack by @p"));
        t.waitFor("cuenta los zombis", 4000, () -> {
            int[] p = NpcCliente.progreso("axelis_core:zombis");
            return p.length == 1 && p[0] == 2;
        });
        t.photo("27_rastreador", 700, () -> t.lookFrom(2.0, G, -0.5, 2.0, G + 1.7, 4.5));
        t.add("tecla del diario", 100, () -> t.check(com.axeliscore.ui.Teclas.TODAS.stream().anyMatch(k -> k.nombre.equals("diario")
                && k.mapping != null), "la tecla del diario está registrada"));
        t.photo("28_diario", 900, () -> com.axeliscore.ui.ScreenRegistry.open(NpcAcciones.DIARIO));
        t.photo("28_diario_hechas", 600, () -> {
            if (com.axeliscore.plat.client.Screens.current() instanceof com.axeliscore.npc.client.DiarioScreen d) {
                d.mostrar(1, "axelis_core:pan");
            }
        });
        t.add("npc: limpiar", 600, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("kill @e[type=!player]");
            t.cmd("execute as @p run axeliscore npc reiniciar");
            t.cmd("clear @a");
            t.hud(false);
        });
    }
}

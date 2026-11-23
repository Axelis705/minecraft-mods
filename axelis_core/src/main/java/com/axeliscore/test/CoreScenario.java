package com.axeliscore.test;

/**
 * La prueba del propio Core ("core"): galería de Personas con gestos, primer plano de la cara hablando, cada gesto por separado,
 * caminar y correr de lado (para ver que los pies no resbalan) y el destello rojo al recibir daño.
 * El mundo de prueba es plano: el suelo está en y = -60.
 */
public final class CoreScenario {
    static final double G = -60;

    private CoreScenario() {
    }

    /** Corre algo en el servidor del mundo de prueba (después de los comandos que ya se pidieron). */
    static void onServer(java.util.function.Consumer<net.minecraft.server.level.ServerLevel> fn) {
        var s = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
        s.execute(() -> fn.accept(s.overworld()));
    }

    static void demoTab(int i) {
        demo(d -> d.showTab(i));
    }

    static void demo(java.util.function.Consumer<com.axeliscore.ui.DemoScreen> fn) {
        if (com.axeliscore.plat.client.Screens.current() instanceof com.axeliscore.ui.DemoScreen d) {
            fn.accept(d);
        }
    }

    /** Pone la cámara de lado junto a la Persona que se mueve (la más cercana), a {@code dist} bloques. */
    static void follow(ClientTest.Script t, double dist) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var list = mc.level.getEntitiesOfClass(com.axeliscore.character.Persona.class, mc.player.getBoundingBox().inflate(40));
        if (list.isEmpty()) {
            t.log("FALLO no veo a la Persona que camina");
            return;
        }
        var e = list.get(0);
        t.lookFrom(e.getX(), G, e.getZ() - dist, e.getX(), G + 0.9, e.getZ());
    }

    static <T extends net.minecraft.world.entity.Entity> java.util.List<T> cerca(Class<T> c) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.level.getEntitiesOfClass(c, mc.player.getBoundingBox().inflate(40));
    }

    static com.axeliscore.combat.Jefe jefe() {
        var l = cerca(com.axeliscore.combat.Jefe.class);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Volar (en creativo) para que las fotos desde arriba no se caigan al suelo. */
    static void volar(boolean v) {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        if (p != null) {
            p.getAbilities().flying = v;
            p.onUpdateAbilities();
        }
    }

    /** Combate: estados, barras de vida, números de daño, avisos, proyectiles y el Coloso por fases (con la interfaz visible). */
    static void combate(ClientTest.Script t) {
        String aqui = "execute positioned 0.5 -60 -2.5 rotated 0 0 run ";
        t.add("combate: estados", 1500, () -> {
            t.hud(true);
            volar(true);
            t.cmd("kill @e[type=!player]");
            t.cmd(aqui + "axeliscore combate estados");
            t.lookFrom(0.5, G, -2.5, 0.5, G + 1.0, 2.5);
        });
        t.waitFor("los estados llegaron al cliente", 5000, () -> {
            var l = cerca(com.axeliscore.character.Bestia.class);
            return l.size() == 5 && l.stream().allMatch(b -> !com.axeliscore.combat.client.EstadosCliente.de(b.getId()).isEmpty());
        });
        t.photo("15_estados_barras_numeros", 280, () -> t.cmd("damage @e[type=axelis_core:bestia] 2"));
        t.photo("15_estados_golpe_grande", 250, () -> t.cmd("damage @e[type=axelis_core:bestia,limit=1,sort=nearest] 9"));
        t.add("estados en mí", 1200, () -> {
            t.cmd("gamemode survival @a");
            t.cmd("effect give @a minecraft:resistance 30 4 true");
            t.cmd("execute as @p run axeliscore combate estado quemado 20");
            t.cmd("execute as @p run axeliscore combate estado congelado 20 2");
            t.cmd("execute as @p run axeliscore combate estado marcado 20 2");
            t.lookFrom(0.5, G, -2.5, 0.5, G + 1.0, 2.5);
        });
        t.add("estados del jugador", 100, () -> t.check(com.axeliscore.combat.client.EstadosCliente.de(
                net.minecraft.client.Minecraft.getInstance().player.getId()).size() == 3, "el jugador ve sus tres estados"));
        t.photo("16_estados_jugador", 500, () -> {
        });
        t.add("sin estados", 400, () -> {
            t.cmd("execute as @p run axeliscore combate limpiar");
            t.cmd("gamemode creative @a");
            t.cmd("effect clear @a");
            t.cmd("kill @e[type=!player]");
        });
        // un piso invisible (barrera) arriba para las fotos desde lo alto (volando a veces se caía)
        t.add("volar", 400, () -> {
            volar(true);
            t.cmd("fill -4 -54 -9 5 -54 2 minecraft:barrier");
        });
        // avisos (sin daño, lentos)
        t.add("combate: avisos", 1500, () -> {
            t.cmd(aqui + "axeliscore combate avisos");
            t.lookFrom(0.5, G + 7, -1.5, 0.5, G, 7.0);
        });
        t.add("avisos en el cliente", 100, () -> t.check(cerca(com.axeliscore.combat.Aviso.class).size() == 5, "se ven los cinco avisos"));
        t.photo("17_avisos", 200, () -> {
        });
        t.photo("17_avisos_casi_llenos", 6000, () -> {
        });
        t.add("limpiar avisos", 300, () -> t.cmd("kill @e[type=!player]"));
        // proyectiles cruzando despacio
        t.add("combate: proyectiles", 1300, () -> {
            t.cmd(aqui + "axeliscore combate proyectiles");
            t.lookFrom(0.5, G, -2.5, 0.5, G + 1.4, 3.5);
        });
        t.add("proyectiles en el cliente", 100, () -> t.check(cerca(com.axeliscore.combat.Proyectil.class).size() >= 3, "se ven los proyectiles"));
        t.photo("18_proyectiles", 200, () -> {
        });
        t.add("limpiar proyectiles", 300, () -> t.cmd("kill @e[type=!player]"));
        // el Coloso por fases
        t.add("combate: jefe", 2200, () -> {
            t.cmd(aqui + "axeliscore combate jefe");
            t.lookFrom(2.5, G + 1, 1.0, 0.5, G + 2.0, 7.5);
        });
        t.add("música de la fase 1", 100, () -> t.check("axelis_core:musica.coloso_1".equals(com.axeliscore.combat.client.MusicaJefe.actual()),
                "suena la música de la fase 1 del Coloso"));
        t.photo("19_jefe_fase1", 200, () -> {
        });
        t.photo("19_jefe_martillazo", 750, () -> {
            t.cmd("execute as @p run axeliscore combate ataque martillazo");
            t.lookFrom(2.5, G + 7, -1.5, 0.5, G, 7.5);
        });
        t.add("fase 2", 1000, () -> {
            t.cmd("axeliscore combate fase 1");
            t.lookFrom(2.5, G + 1, 1.0, 0.5, G + 2.0, 7.5);
        });
        t.add("fase 2 en el cliente", 100, () -> {
            var j = jefe();
            t.check(j != null && j.fase() == 1, "el Coloso pasó a la fase 2");
            t.check("axelis_core:musica.coloso_2".equals(com.axeliscore.combat.client.MusicaJefe.actual()), "cambió la música a la de la fase 2");
        });
        t.photo("19_jefe_fase2_titulo", 100, () -> {
        });
        t.add("termina la entrada", 2200, () -> {
        });
        t.photo("19_jefe_lluvia", 900, () -> {
            t.cmd("execute as @p run axeliscore combate ataque lluvia");
            t.lookFrom(0.5, G + 7, -4.5, 0.5, G + 7, -2.6);
        });
        t.photo("19_jefe_fragmentos", 900, () -> {
            t.cmd("execute as @p run axeliscore combate ataque fragmentos");
            t.lookFrom(2.5, G + 1, 1.0, 0.5, G + 2.0, 7.5);
        });
        t.add("fase 3", 1000, () -> t.cmd("axeliscore combate fase 2"));
        t.photo("19_jefe_fase3_titulo", 100, () -> {
        });
        t.add("termina la entrada 3", 2200, () -> {
        });
        t.photo("19_jefe_onda", 900, () -> {
            t.cmd("execute as @p run axeliscore combate ataque onda");
            t.lookFrom(0.5, G + 7, -8.5, 0.5, G, 7.5);
        });
        t.photo("19_jefe_onda_2", 700, () -> {
        });
        t.photo("19_jefe_escarcha", 1300, () -> {
            t.cmd("execute as @p run axeliscore combate ataque escarcha");
            t.lookFrom(2.5, G + 1, 1.0, 0.5, G + 2.6, 7.5);
        });
        t.add("limpiar jefe", 600, () -> {
            t.cmd("kill @e[type=!player]");
            t.cmd("fill -4 -54 -9 5 -54 2 minecraft:air");
            t.hud(false);
            volar(false);
        });
    }

    static void register() {
        ClientTest.scenario("core", t -> {
            // /reload: en la 1.21.1 con --quickPlaySingleplayer Forge no avisa a los mods al cargar el mundo (sin comandos ni
            // modificadores de botín) hasta que se recargan los datos
            t.add("recargar", 6000, () -> {
                t.cmd("gamemode creative @a");
                t.cmd("gamerule doMobSpawning false");
                t.cmd("kill @e[type=!player]");
                t.cmd("reload");
            });
            t.add("preparar", 2500, () -> {
                t.hud(false);
                t.cmd("axeliscore");
                t.cmd("time set 6000");
                t.cmd("weather clear");
                t.cmd("kill @e[type=axelis_core:persona]");
                t.lookFrom(0.5, G, -6.5, 0.5, G + 1.0, 0.5);
            });
            t.add("galeria", 3500, () -> t.cmd("axeliscore galeria 0.5 -60 0.5 180"));
            for (int i = 1; i <= 3; i++) {
                t.photo("1_galeria_" + i, 450, () -> {
                });
            }
            t.photo("2_galeria_34", 1500, () -> t.lookFrom(6.5, G, -4.5, 0.5, G + 1.0, 0.5));
            t.photo("2_galeria_espalda", 1500, () -> t.lookFrom(0.5, G, 6.5, 0.5, G + 1.0, 0.5));
            // primer plano de quien habla (la segunda, x = -1.5)
            for (int i = 1; i <= 3; i++) {
                final int k = i;
                t.photo("3_cara_habla_" + k, k == 1 ? 1500 : 300, () -> t.lookFrom(2.5, G, -1.4, 2.5, G + 1.6, 0.5));
            }
            // cada gesto por separado: todas lo hacen a la vez, se ve desde 3/4
            String[][] gestos = {{"sit", "900"}, {"point", "900"}, {"think", "1100"}, {"laugh", "700"}, {"attack", "180"}, {"fish_cast", "330"},
                    {"cheer", "450"}, {"nod", "200"}, {"wave", "700"}, {"death", "1600"}};
            for (String[] g : gestos) {
                t.photo("4_gesto_" + g[0], Long.parseLong(g[1]), () -> {
                    t.lookFrom(4.5, G, -4.0, 0.5, G + 0.9, 0.5);
                    t.cmd("axeliscore probar " + g[0]);
                });
            }
            // caminar y correr (de lado)
            t.add("limpiar", 1200, () -> {
                t.cmd("kill @e[type=axelis_core:persona]");
                t.cmd("summon axelis_core:persona -8.5 -60 6.5");
                t.lookFrom(0.5, G, -1.5, 0.5, G + 1.0, 6.5);
            });
            t.add("ir caminando", 2200, () -> t.cmd("axeliscore ir 10.5 -60 6.5 0.6"));
            for (int i = 1; i <= 3; i++) {
                t.photo("5_camina_" + i, 280, () -> follow(t, 3.2));
            }
            t.add("ir corriendo", 1800, () -> t.cmd("axeliscore ir -10.5 -60 6.5 1.6"));
            for (int i = 1; i <= 3; i++) {
                t.photo("6_corre_" + i, 220, () -> follow(t, 3.6));
            }
            t.photo("7_dano", 120, () -> t.cmd("damage @e[type=axelis_core:persona,limit=1] 1"));
            // criaturas: galería y gestos
            t.add("criaturas", 3500, () -> {
                t.cmd("kill @e[type=!player]");
                t.cmd("axeliscore criaturas 0.5 -60 0.5 180");
                t.lookFrom(0.5, G, -9.5, 0.5, G + 1.2, 0.5);
            });
            for (int i = 1; i <= 2; i++) {
                t.photo("8_criaturas_" + i, 500, () -> {
                });
            }
            t.photo("8_criaturas_34", 1500, () -> t.lookFrom(9.0, G + 1, -6.0, 0.5, G + 1.0, 0.5));
            String[][] cg = {{"attack", "350"}, {"sit", "900"}, {"slam", "1050"}, {"slam", "250"}, {"sweep", "750"}, {"sniff", "800"},
                    {"sleep", "1200"}, {"play", "850"}, {"stomp", "550"}, {"phase", "1500"}, {"death", "2600"}};
            int k = 0;
            for (String[] g : cg) {
                final int kk = k++;
                t.photo("9_criaturas_" + kk + "_" + g[0], Long.parseLong(g[1]), () -> {
                    t.lookFrom(-0.5, G + 1.5, -8.5, 0.5, G + 1.0, 0.5);
                    if (kk == 0 || !g[0].equals(cg[kk - 1][0])) {
                        t.cmd("axeliscore probar " + g[0]);
                    }
                });
            }
            // interfaz y efectos (con la interfaz visible)
            t.add("interfaz", 600, () -> {
                t.hud(true);
                t.cmd("kill @e[type=!player]");
                t.cmd("axeliscore galeria 0.5 -60 3.5 180");
                t.lookFrom(0.5, G, -2.5, 0.5, G + 1.4, 3.5);
            });
            t.photo("10_ui_demo_botones", 1500, () -> t.cmd("axeliscore efecto demo"));
            t.photo("10_ui_demo_lista", 600, () -> demoTab(1));
            t.photo("10_ui_demo_barras", 900, () -> demoTab(2));
            // cuadros de información, tarjetas, interruptores, deslizadores y temas con marco (el mouse "falso" hace aparecer los cuadros)
            t.photo("13_ui_tarjetas", 900, () -> demo(d -> {
                d.showTab(3);
                d.pick(8);
                com.axeliscore.ui.UiScreen.fakeMouse = d.cellCenter(9);
            }));
            t.photo("13_ui_lista_cuadro", 800, () -> demo(d -> {
                d.showTab(1);
                com.axeliscore.ui.UiScreen.fakeMouse = d.rowCenter(3);
            }));
            t.photo("13_ui_boton_icono", 800, () -> demo(d -> {
                d.showTab(0);
                com.axeliscore.ui.UiScreen.fakeMouse = d.iconCenter(3);
            }));
            t.photo("13_ui_ajustes", 700, () -> demo(d -> {
                com.axeliscore.ui.UiScreen.fakeMouse = null;
                d.showTab(4);
            }));
            t.photo("13_ui_tema_madera", 900, () -> demo(d -> {
                d.theme(1);
                d.showTab(3);
                d.pick(9);
                com.axeliscore.ui.UiScreen.fakeMouse = d.cellCenter(4);
            }));
            t.photo("13_ui_tema_pergamino", 900, () -> demo(d -> {
                d.theme(2);
                d.showTab(0);
                com.axeliscore.ui.UiScreen.fakeMouse = d.iconCenter(4);
            }));
            t.photo("13_ui_tema_mar", 900, () -> demo(d -> {
                d.theme(3);
                d.showTab(1);
                com.axeliscore.ui.UiScreen.fakeMouse = d.rowCenter(6);
            }));
            t.photo("13_ui_tema_piedra", 900, () -> demo(d -> {
                com.axeliscore.ui.UiScreen.fakeMouse = null;
                d.theme(4);
                d.showTab(5);
            }));
            t.add("sin mouse falso", 200, () -> {
                com.axeliscore.ui.UiScreen.fakeMouse = null;
                demo(d -> d.theme(0));
            });
            t.photo("11_ui_dialogo", 3600, () -> t.cmd("axeliscore efecto dialogo"));
            t.add("cerrar", 600, () -> com.axeliscore.plat.client.Screens.close());
            t.photo("12_fx_titulo_aviso", 700, () -> {
                t.cmd("axeliscore efecto titulo");
                t.cmd("axeliscore efecto aviso");
            });
            t.photo("12_fx_vineta", 500, () -> t.cmd("axeliscore efecto vineta"));
            t.photo("12_fx_destello", 120, () -> t.cmd("axeliscore efecto destello"));
            // la tarjeta al mirar un personaje y un jefe
            t.add("mirar", 1800, () -> {
                com.axeliscore.plat.client.Screens.close();
                t.cmd("kill @e[type=!player]");
                t.cmd("summon axelis_core:persona 0.5 -60 3.5 {NoAI:1b,Rotation:[180f,0f]}");
                onServer(l -> l.getEntitiesOfClass(com.axeliscore.character.Persona.class, new net.minecraft.world.phys.AABB(-4, -62, 0, 5, -56, 8))
                        .forEach(p -> p.setCustomName(net.minecraft.network.chat.Component.literal("Nilo"))));
                t.lookFrom(0.5, G, 0.5, 0.5, G + 1.1, 3.5);
            });
            t.photo("14_mirar_persona", 1000, () -> {
            });
            t.add("coloso", 1200, () -> {
                t.cmd("kill @e[type=!player]");
                t.cmd("summon axelis_core:coloso 0.5 -60 6.5 {NoAI:1b,Rotation:[180f,0f]}");
                t.lookFrom(0.5, G, 0.5, 0.5, G + 2.2, 6.5);
            });
            t.photo("14_mirar_coloso", 1000, () -> {
            });
            t.add("limpiar coloso", 300, () -> t.cmd("kill @e[type=!player]"));
            combate(t);
            NpcScenario.npcs(t);
            EstudioScenario.estudio(t);
            VestuarioScenario.vestuario(t);
            t.add("datos", 1500, () -> t.cmd("axeliscore datos @a"));
            t.add("datos en el cliente", 100, () -> t.check(com.axeliscore.plat.Nbt.i(
                    com.axeliscore.data.client.ClientData.player("axelis_core_prueba"), "visitas") > 0, "los datos del jugador llegaron al cliente"));
            t.add("listo", 300, () -> t.hud(false));
        });
    }
}

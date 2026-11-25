package com.coleccionista.client;

import com.axeliscore.test.ClientTest;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/**
 * La prueba con el juego de verdad y fotos (cliente-core.yml con mod=coleccionista y escenario=coleccionista): HUD, parejas, eliminación
 * (la franja y la escena propia), fantasma, panel del organizador, pausa y ganador. Corre con bots: no hace falta nadie más.
 */
final class PruebaCliente {
    private static final double G = -60;

    private PruebaCliente() {
    }

    private static String yo() {
        return Minecraft.getInstance().player.getGameProfile().getName();
    }

    static void escenario(ClientTest.Script t) {
        t.add("preparar", 2500, () -> {
            SoloPrimera.permitir = true;
            t.hud(true);
            t.cmd("evento reiniciar confirmar");
            t.cmd("kill @e[type=!player]");
            t.cmd("time set 18000");
            t.cmd("gamemode adventure @a");
            t.cmd("evento inscribir @a");
            t.cmd("execute positioned 0.5 " + G + " 0.5 run evento bots 7");
            t.lookFrom(0.5, G, -7.5, 0.5, G + 1, 0.5);
        });
        t.add("inscritos", 100, () -> {
            t.check(EventoCliente.activo(), "llegó el estado del evento");
            t.check(EventoCliente.total() == 8, "hay 8 piezas (yo y 7 bots): " + EventoCliente.total());
            t.check(EventoCliente.yo() != null, "soy una pieza");
        });
        t.photo("01_hud_espera", 800, () -> {
        });
        t.photo("02_reloj", 900, () -> {
            t.cmd("evento prueba Encadenados");
            t.cmd("evento reloj 754");
        });
        t.photo("03_parejas", 900, () -> t.cmd("evento parejas"));
        t.add("parejas", 100, () -> t.check(!EventoCliente.companeros().isEmpty(), "tengo pareja"));
        t.photo("04_eliminado", 900, () -> t.cmd("evento eliminar Bot_02 Encadenados"));
        t.add("eliminado", 100, () -> t.check(EventoCliente.vivos() == 7, "quedan 7: " + EventoCliente.vivos()));
        t.add("esperar franja", 4200, () -> {
        });
        t.photo("06_mi_eliminacion", 2800, () -> t.cmd("evento eliminar " + yo()));
        t.add("soy fantasma", 100, () -> {
            t.check(EventoCliente.esFantasma(Minecraft.getInstance().player.getUUID()), "quedé como fantasma");
            t.check(Minecraft.getInstance().player.isSpectator(), "estoy en modo espectador");
        });
        t.add("esperar escena", 4800, () -> {
        });
        t.photo("07_fantasma_hud", 600, () -> {
        });
        t.photo("08_fantasma_tercera", 1200, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            t.lookFrom(0.5, G + 1, -3.5, 0.5, G + 1, 0.5);
        });
        t.add("volver camara", 300, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        t.add("revivir", 1500, () -> t.cmd("evento revivir " + yo()));
        t.add("vivo otra vez", 100, () -> {
            t.check(!EventoCliente.esFantasma(Minecraft.getInstance().player.getUUID()), "volví al juego");
            t.check(!Minecraft.getInstance().player.isSpectator(), "ya no soy espectador");
            t.lookFrom(0.5, G, -7.5, 0.5, G + 1, 0.5);
        });
        // el panel del organizador (soy operador en la prueba)
        t.photo("11_panel_piezas", 1200, () -> {
            PanelScreen s = new PanelScreen();
            com.axeliscore.plat.client.Screens.open(s);
            s.pestana(0);
        });
        t.photo("12_panel_evento", 900, () -> {
            if (com.axeliscore.plat.client.Screens.current() instanceof PanelScreen s) {
                s.pestana(1);
            }
        });
        t.add("tomas", 900, () -> {
            com.axeliscore.data.client.ClientData.send(com.coleccionista.evento.Panel.ACCION,
                    com.coleccionista.evento.Panel.pedido("toma_guardar", null));
            com.axeliscore.data.client.ClientData.send(com.coleccionista.evento.Panel.ACCION,
                    com.coleccionista.evento.Panel.pedido("toma_guardar", null));
        });
        t.photo("13_panel_camaras", 900, () -> {
            if (com.axeliscore.plat.client.Screens.current() instanceof PanelScreen s) {
                s.pestana(3);
            }
        });
        t.add("cerrar panel", 600, () -> {
            t.check(com.axeliscore.data.client.ClientData.get(com.coleccionista.evento.Tomas.KEY) != null
                    && com.axeliscore.data.client.ClientData.get(com.coleccionista.evento.Tomas.KEY).getList("lista", 10).size() >= 2,
                    "se guardaron las tomas de cámara");
            com.axeliscore.plat.client.Screens.close();
        });
        t.photo("09_pausa", 1200, () -> t.cmd("evento pausar"));
        t.add("reanudar", 800, () -> t.cmd("evento reanudar"));
        // quedo yo solo: salen las 6 franjas y luego la del ganador
        t.add("final", 4600, () -> {
            for (String b : new String[] {"Bot_03", "Bot_04", "Bot_05", "Bot_06", "Bot_07", "Bot_08"}) {
                t.cmd("evento eliminar " + b);
            }
        });
        t.add("esperar franjas", 24000, () -> {
        });
        t.photo("10_ganador", 1500, () -> {
        });
        t.add("hay ganador", 100, () -> t.check(EventoCliente.vivos() == 1 && "FIN".equals(EventoCliente.fase()), "queda una sola pieza"));
        mapa(t);
        t.add("limpiar", 1500, () -> {
            t.cmd("evento reiniciar confirmar");
            t.cmd("gamemode creative @a");
        });
    }

    /** Para la foto: la cámara frente a uno de los números escondidos de la celda 1 (como lo vería alguien buscándolo). */
    private static void mirarPista(ClientTest.Script t, int k) {
        var l = InscripcionesCliente.de(1);
        if (k >= l.size()) {
            return;
        }
        var i = l.get(k);
        t.cmd("gamemode spectator @a");
        net.minecraft.world.phys.Vec3 p = i.pos(), ojo;
        double r = Math.toRadians(i.ang());
        net.minecraft.world.phys.Vec3 n = new net.minecraft.world.phys.Vec3(Math.sin(r), 0, Math.cos(r));
        switch (i.plano()) {
            case com.coleccionista.evento.Pistas.PISO -> ojo = "bajo_catre".equals(i.lugar()) ? p.add(1.9, 0.3, 0) : p.add(n.scale(0.55)).add(0, 0.95, 0);
            case com.coleccionista.evento.Pistas.TECHO -> ojo = p.add(n.scale(0.6)).add(0, -1.45, 0);
            default -> ojo = p.add(n.scale(0.95)).add(0, 0.06, 0);
        }
        t.lookFrom(ojo.x, ojo.y - 1.62, ojo.z, p.x, p.y, p.z);
        com.coleccionista.Coleccionista.LOGGER.info("COLECCIONISTA_PRUEBA_PISTA {} '{}'", i.lugar(), i.texto());
    }

    /** El Ala de celdas: construir, sala de espera, ¡pum!, cinemática, discurso, acercarse a la tele, candado, cuerpos. */
    private static void mapa(ClientTest.Script t) {
        // el mapa queda con el origen en (0, -61, 30): celda 1 en x 7..11 (z 31..35, puerta en z 36), el patio en x 7..53 y z 37..59
        t.add("mapa: construir", 9000, () -> {
            t.cmd("evento reiniciar confirmar");
            t.cmd("kill @e[type=!player]");
            t.cmd("gamemode creative @a");
            t.lookFrom(18, -50, 20, 18, -60, 41);
            t.cmd("execute positioned 0 -60 30 run evento construir");
        });
        t.add("mapa: piezas", 2500, () -> {
            t.cmd("evento inscribir @a");
            t.cmd("execute positioned 18 -60 41 run evento bots 7");
            t.cmd("evento investigar 15");
        });
        t.photo("20_sala_espera", 3000, () -> t.cmd("evento sala"));
        t.add("en la sala", 100, () -> {
            t.check(EventoCliente.sala(), "estamos en la sala de espera");
            t.check(Minecraft.getInstance().player.isSleeping(), "estoy acostado en mi cama");
            t.check(Espera.activa(), "veo la pantalla de espera");
        });
        // el ¡pum! prende las luces y la cinemática empieza segundo y medio después
        t.photo("21_despertar_cine", 3500, () -> t.cmd("evento despertar"));
        t.add("cinematica", 100, () -> t.check(Cine.activo(), "está la cinemática del despertar"));
        t.photo("22_parpadeo", 2900, () -> {
        });
        t.photo("23_celda_luz", 3200, () -> {
        });
        t.add("despierto", 100, () -> {
            t.check(!Minecraft.getInstance().player.isSleeping(), "ya me levanté");
            t.check(!Cine.activo(), "terminó la cinemática");
        });
        // a los 15 segundos de levantarse se acaba el tiempo libre: apagón y, al volver la luz, los tres números escondidos en cada celda
        t.photo("24a_apagon", 13800, () -> {
        });
        t.add("apagon", 100, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(9, -57, 33));
            t.check(st.getBlock() instanceof com.coleccionista.bloque.FocoBlock && !com.coleccionista.bloque.FocoBlock.prendido(st),
                    "se fue la luz de mi celda");
            t.check(InscripcionesCliente.cuantas(0) == 0, "todavía no hay números escritos");
        });
        t.photo("24b_pistas", 5700, () -> {
        });
        t.add("pistas", 100, () -> {
            t.check(InscripcionesCliente.cuantas(1) == 3, "mi celda tiene sus 3 números escondidos (" + InscripcionesCliente.cuantas(1) + ")");
            t.check(InscripcionesCliente.cuantas(0) >= 24, "las demás celdas también (" + InscripcionesCliente.cuantas(0) + ")");
            java.util.Set<String> pos = new java.util.HashSet<>();
            for (var i : InscripcionesCliente.de(1)) {
                pos.add(i.texto().replaceAll("[0-9]", "#"));
            }
            t.check(pos.size() == 3, "cada número dice su lugar (" + pos + ")");
        });
        for (int i = 0; i < 3; i++) {
            final int k = i;
            t.photo("24c_pista_" + (i + 1), 1500, () -> mirarPista(t, k));
        }
        t.add("regresar a la celda", 900, () -> {
            t.cmd("gamemode adventure @a");
            t.lookFrom(9.5, -60, 34.5, 9.5, -57.5, 31);
        });
        // el uniforme de pieza (con su número) visto de frente y de espaldas
        t.photo("24d_uniforme_frente", 900, () -> {
            t.check(!com.axeliscore.vestir.client.VestuarioCliente.de(Minecraft.getInstance().player.getUUID()).isEmpty(),
                    "llevo el uniforme de pieza");
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        t.photo("24e_uniforme_espalda", 900, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        t.add("primera persona", 300, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        t.photo("24_tele_habla", 300, () -> {
        });
        t.add("habla", 100, () -> {
            var st = com.axeliscore.data.client.ClientData.get(com.coleccionista.evento.Pantallas.KEY);
            t.check(st != null && "coleccionista".equals(st.getString("modo")), "las pantallas muestran al Coleccionista");
        });
        t.photo("25_zoom_tele", 2600, Atajos::acercarse);
        t.add("zoom", 100, () -> t.check(Cine.enPantalla(), "la cámara está frente a la tele"));
        t.photo("25b_zoom_subtitulo", 5000, () -> {
        });
        t.add("volver", 1200, Atajos::acercarse);
        t.add("esperar discurso", 64000, () -> {
        });
        // de cerca otra vez cuando ya terminó de hablar: debe decir que con R se cierra
        t.photo("25c_zoom_fin", 1800, Atajos::acercarse);
        t.add("zoom fin", 100, () -> t.check(Subtitulo.termino(), "ya terminó de hablar (aviso de cerrar con R)"));
        t.add("volver 2", 600, Atajos::acercarse);
        // al terminar de hablar: los candados funcionan y corre el reloj de 2 minutos para salir; en el patio no hay pantallas grandes
        t.add("reloj para salir", 100, () -> {
            var l = Minecraft.getInstance().level;
            t.check(!(l.getBlockState(new net.minecraft.core.BlockPos(24, -55, 48)).getBlock() instanceof com.coleccionista.bloque.PantallaBlock),
                    "la torre del patio ya no tiene pantalla grande");
            int r = EventoCliente.resta();
            t.check(r > 60 && r <= 120, "corre el reloj para salir de la celda (" + r + " s)");
        });
        t.photo("24f_reloj_salir", 600, () -> {
        });
        // el candado de la celda 1, de cerca: la mano gira las ruedas (arrastrando) y aprieta el botón
        t.add("abrir candado", 1400, () -> {
            CompoundTag c = new CompoundTag();
            c.putInt("celda", 1);
            c.putInt("x", 10);
            c.putInt("y", -59);
            c.putInt("z", 35);
            c.putString("f", "north");
            c.putString("res", "");
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.evento.Celdas.KEY_CANDADO, c);
            com.axeliscore.plat.client.Screens.open(new CandadoScreen());
        });
        t.add("candado de cerca", 100, () -> {
            t.check(CandadoScreen.actual() != null, "está abierta la vista del candado");
            t.check(Cine.enCandado(), "la cámara está frente al candado");
            CandadoScreen.actual().arrastrarPrueba(0, 0, false);
        });
        t.photo("26_candado", 600, () -> CandadoScreen.actual().arrastrarPrueba(0, 2.0, false));
        t.photo("26e_candado_girando", 500, () -> CandadoScreen.actual().arrastrarPrueba(0, 4.4, false));
        t.add("soltar rueda", 600, () -> {
            CandadoScreen.actual().arrastrarPrueba(0, 4.4, true);
            CandadoScreen.actual().arrastrarPrueba(2, -3.1, true);
        });
        t.add("ruedas", 100, () -> t.check(CandadoScreen.actual().codigo() == 308, "las ruedas quedaron en 3 _ 8 (" + CandadoScreen.actual().codigo() + ")"));
        t.photo("26f_candado_mal", 450, () -> CandadoScreen.actual().probarPrueba());
        t.add("cerrar candado", 700, () -> com.axeliscore.plat.client.Screens.close());
        t.add("candado cerrado", 100, () -> t.check(!Cine.activo(), "la cámara regresó a mis ojos"));
        // la reja de la celda 1: cerrada, corriéndose y abierta (con visión nocturna para que se vea en la foto)
        t.photo("26b_reja_cerrada", 1500, () -> {
            t.cmd("gamemode spectator @a");
            t.cmd("effect give @a night_vision 30 0 true");
            t.lookFrom(9.5, G, 39.3, 9.5, G + 1, 36.5);
        });
        t.add("bajo a 10", 100, () -> {
            int r = EventoCliente.resta();
            t.check(r >= 0 && r <= 10, "salió el último de su celda: el reloj bajó a 10 segundos (" + r + ")");
        });
        t.photo("26c_reja_abriendo", 650, () -> t.cmd("evento celdas abrir 1"));
        t.photo("26d_reja_abierta", 1600, () -> {
        });
        t.add("reja", 100, () -> {
            var l = Minecraft.getInstance().level;
            net.minecraft.core.BlockPos q = new net.minecraft.core.BlockPos(9, (int) G, 36);
            var st = l.getBlockState(q);
            t.check(st.getBlock() instanceof com.coleccionista.bloque.deco.DecoReja && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN),
                    "la reja de la celda 1 está abierta");
            t.check(l.getBlockEntity(q) instanceof com.coleccionista.bloque.deco.RejaBE be && be.avance(1F) == 1F, "la reja terminó de correrse");
            t.cmd("effect clear @a night_vision");
        });
        t.photo("27_pasillo", 2500, () -> {
            t.cmd("evento celdas abrir");
            t.cmd("gamemode spectator @a");
            t.lookFrom(52.5, -59, 48.5, 7, -57.5, 48.5);
            muestrearLamparas(true);
        });
        t.photo("28_cuerpo", 2500, () -> {
            t.cmd("evento eliminar Bot_02 Encadenados");
            t.lookFrom(15.5, -59.4, 39.5, 15.5, -60.6, 34.5);
        });
        t.photo("29_torre_camaras", 2000, () -> t.lookFrom(12.5, -59, 48.5, 24, -55, 48.5));
        t.photo("30_patio", 1500, () -> t.lookFrom(52, -52, 38, 30, -58, 48));
        t.add("lamparas", 100, () -> {
            muestrearLamparas(false);
            t.check(lamparasFallando > 0, "algunas lámparas del pasillo parpadean solas (" + lamparasFallando + " ticks con alguna apagada)");
        });
        // Axelis aparece disfrazado del Coleccionista en el pasillo (visto de frente)
        t.add("disfrazarme", 1500, () -> {
            t.cmd("evento quitar " + yo());
            t.cmd("evento disfraz si @a");
        });
        t.photo("31_disfraz", 1800, () -> {
            t.lookFrom(44.5, -60, 48.5, 30, -58.4, 48.5);
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        t.add("disfrazado", 100, () -> {
            t.check(EventoCliente.disfrazado(Minecraft.getInstance().player.getUUID()), "estoy disfrazado del Coleccionista");
            t.check(Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(54, -60, 48)).isAir(),
                    "al acabarse el reloj, el portón subió solo");
            t.check(!Minecraft.getInstance().player.isSpectator(), "el disfraz me saca de espectador");
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.cmd("evento disfraz no @a");
        });
        capitulo1(t);
        encadenados(t);
    }

    /**
     * Capítulo 1: la pantalla grande sola, el portón que sube, la sala grande con sus ascensores, el viaje que cae, el taller de los
     * sentidos (la mesa, la cadena, los cuartos), la salida en sillas, la galería y la vitrina de cera; al final, la línea de tiempo.
     * Mi equipo (yo y un bot) sale primero; de los dos equipos de bots, el último va a la vitrina.
     */
    private static void capitulo1(ClientTest.Script t) {
        t.add("cap1: preparar", 2800, () -> {
            t.cmd("evento capitulo1 parar");
            t.cmd("evento porton cerrar");
            t.cmd("gamemode adventure @a");
            t.cmd("evento inscribir @a");
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("evento sentidos bots true 25 26");
            t.cmd("evento sentidos tiempos 20 300");
            t.cmd("evento mascota tiempos 6 90");
            t.cmd("forceload add 704 424 784 488");
            t.cmd("forceload add 0 428 200 576");
            t.cmd("forceload add -82 428 -8 492");
            t.lookFrom(46.5, -60, 48.5, 54, -58.5, 48.5);
        });
        t.photo("c1_51_porton", 2600, () -> t.cmd("evento porton abrir"));
        t.add("porton arriba", 100, () -> t.check(Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(54, -60, 48)).isAir(),
                "el portón subió (ya no hay barrotes abajo)"));
        // todos pasan a la sala grande: cuando ya están todos, baja el portón y habla el Coleccionista (aquí sí hay pantalla)
        t.photo("c1_52_sala_grande", 1500, () -> {
            t.cmd("evento capitulo1 sala");
            t.lookFrom(57.5, -59, 48.5, 111, -55, 48.5);
        });
        t.add("todos dentro", 4500, () -> {
        });
        t.add("habla en la sala", 100, () -> {
            var st = com.axeliscore.data.client.ClientData.get(com.coleccionista.evento.Pantallas.KEY);
            String m = st == null ? "" : st.getString("modo");
            t.check("estatica".equals(m) || "coleccionista".equals(m), "la pantalla de la sala grande se prendió (" + m + ")");
        });
        t.photo("c1_52b_equipos", 3500, () -> t.lookFrom(96.5, -59, 48.5, 111, -55, 48.5));
        t.add("porton cerrado", 100, () -> t.check(!Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(54, -60, 48)).isAir(),
                "el portón bajó detrás de todos"));
        t.add("piso", 100, () -> t.check(!Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(80, -60, 48)).is(
                net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("coleccionista", "tapete_rojo"))),
                "el tapete va a ras del piso (no un bloque arriba)"));
        t.add("saltar a formar", 600, () -> t.cmd("evento capitulo1 formar"));
        t.photo("c1_53_ascensores", 4500, () -> t.lookFrom(86.5, -60, 40.5, 86.5, -59, 29));
        t.add("hay objetivo", 100, () -> t.check(ObjetivoCliente.hay(), "el indicador de «ir acá» apunta a un ascensor"));
        t.add("al ascensor", 200, () -> t.lookFrom(86.5, -60, 27.5, 86.5, -59.5, 31));
        t.photo("c1_54_ascensor_listo", 1800, () -> {
        });
        t.add("bajando", 5500, () -> {
        });
        t.add("cae", 100, () -> t.check(ViajeCliente.cayendo(), "voy en el ascensor que cae"));
        t.photo("c1_55_cayendo", 900, () -> {
        });
        t.add("llegar", 12000, () -> {
        });
        // el Coleccionista espera a que salgan todos del ascensor (los bots salen solos)
        t.add("esperando", 100, () -> t.check("ESPERA".equals(SentidosCliente.fase()), "espera a que salgamos del ascensor (" + SentidosCliente.fase() + ")"));
        t.add("salir del ascensor", 1500, () -> t.lookFrom(149.5, -60, 437.5, 149.5, -59.5, 441));
        t.add("habla", 100, () -> t.check("HABLANDO".equals(SentidosCliente.fase()), "ya que estamos todos en el taller, habla (" + SentidosCliente.fase() + ")"));
        t.add("saltar discurso", 1200, () -> t.cmd("evento sentidos saltar"));
        t.add("en la mesa", 100, () -> t.check("ELEGIR".equals(SentidosCliente.fase()), "estamos eligiendo objetos (" + SentidosCliente.fase() + ")"));
        t.photo("c1_56_mesa", 900, () -> {
            t.cmd("evento sentidos toma " + yo() + " 2");
            t.lookFrom(149.5, -60, 436.5, 149.5, -59.5, 441);
        });
        // a los demás les toca lo que sobró y se abre la puerta (así el tiempo de los bots queda fijo)
        t.add("tomar esposas", 4000, () -> t.cmd("evento sentidos saltar"));
        t.add("con esposas", 100, () -> {
            t.check("ESPOSAS".equals(SentidosCliente.miRol()), "me tocaron las esposas (" + SentidosCliente.miRol() + ")");
            t.check("JUEGO".equals(SentidosCliente.fase()), "ya empezó el recorrido (" + SentidosCliente.fase() + ")");
        });
        // mi equipo visto desde un lado (con el HUD del equipo a la izquierda, debajo de la tarjeta)
        t.photo("c1_57_equipo_encadenado", 1500, () -> {
            t.cmd("gamemode spectator @a");
            t.lookFrom(153.5, -59, 440.5, 149.5, -59.5, 437.5);
        });
        t.add("primera", 300, () -> t.cmd("gamemode adventure @a"));
        t.add("mostrar", 600, () -> {
            t.cmd("gamemode spectator @a");
            t.cmd("evento sentidos mostrar");
        });
        t.photo("c1_58_pasillo_munecas", 1200, () -> t.lookFrom(149.5, -59, 449.5, 164, -58.3, 449.5));
        t.photo("c1_59_postes", 1200, () -> t.lookFrom(153.5, -58.5, 467, 161, -59.5, 476));
        t.photo("c1_60_pasarela", 1200, () -> t.lookFrom(181.5, -57, 489.2, 181.5, -61.5, 496));
        t.photo("c1_61_baldosas", 1200, () -> t.lookFrom(167, -55.5, 515, 166.5, -61, 523));
        t.photo("c1_62_casilla", 1200, () -> t.lookFrom(179.5, -59, 546.2, 179.5, -58, 549));
        t.photo("c1_62b_palancas", 1200, () -> t.lookFrom(171.5, -59, 547.5, 176, -59.2, 547.5));
        t.add("a la salida", 2500, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("evento sentidos salida " + yo());
        });
        t.photo("c1_63_salida_sillas", 1200, () -> t.lookFrom(166, -59.5, 565, 166, -60.3, 569));
        t.add("sentarme", 2500, () -> t.cmd("execute as " + yo() + " at @s run ride @s mount @e[type=coleccionista:silla,sort=nearest,limit=1]"));
        t.add("sentado", 100, () -> t.check(Minecraft.getInstance().player.getVehicle() instanceof com.coleccionista.evento.Silla,
                "estoy sentado en la silla de la salida"));
        // cuando terminan todos, los que se salvaron pasan (sentados, sin que se note) a su cuarto del teatro y se abre la puerta
        t.waitFor("al teatro", 70000, () -> Minecraft.getInstance().player.getX() < -40);
        t.photo("c1_64_palco_puerta", 3500, () -> {
        });
        t.add("en el teatro", 100, () -> {
            var p = Minecraft.getInstance().player;
            t.check(p.getVehicle() instanceof com.coleccionista.evento.Silla, "llegué al teatro sentado en mi silla (" + (int) p.getX() + ", "
                    + (int) p.getZ() + ")");
        });
        t.add("levantarme", 800, () -> t.cmd("ride " + yo() + " dismount"));
        t.photo("c1_65_teatro_telon", 1500, () -> {
            t.cmd("gamemode spectator @a");
            t.lookFrom(-53, -59, 457.5, -53, -56.5, 470);
        });
        t.waitFor("telon abierto", 40000, () -> Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(-53, -56, 470)).isAir());
        t.photo("c1_66_vitrinas", 2500, () -> t.lookFrom(-53, -57.5, 462, -53, -58.5, 477));
        // la cera sube en la vitrina del centro (la primera víctima); las vitrinas están en x -60, -52 y -44, z 474
        t.waitFor("cera", 40000, () -> net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(Minecraft.getInstance().level
                .getBlockState(new net.minecraft.core.BlockPos(-52, -60, 474)).getBlock()).getPath().equals("cera"));
        t.photo("c1_67_cera", 3500, () -> t.lookFrom(-56, -58.3, 467.5, -56, -59, 474));
        t.waitFor("caen", 30000, () -> {
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.estatua() && c.cae() > 0) {
                    return true;
                }
            }
            return false;
        });
        t.photo("c1_68_estatuas_caen", 300, () -> t.lookFrom(-56, -58, 465.5, -56, -59.6, 471.5));
        t.photo("c1_68b_estatuas_tiradas", 2200, () -> {
        });
        t.add("estatuas", 100, () -> {
            int n = 0;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.estatua()) {
                    n++;
                }
            }
            t.check(n >= 2, "hay estatuas de piedra tiradas frente a las vitrinas (" + n + ")");
        });
        t.waitFor("salida del teatro", 40000, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(-25, -60, 464));
            return st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
        });
        t.photo("c1_69_salida_teatro", 1500, () -> t.lookFrom(-31, -59, 464.5, -25, -59, 464.5));
        t.add("a la antesala", 1500, () -> {
            t.cmd("gamemode adventure @a");
            t.lookFrom(-19, -60, 464.5, -11, -58, 464.5);
        });
        t.waitFor("antesala habla", 15000, () -> {
            var st = com.axeliscore.data.client.ClientData.get(com.coleccionista.evento.Pantallas.KEY);
            return st != null && st.getBoolean("hablando");
        });
        t.photo("c1_70_antesala", 2500, () -> {
        });
        // lo que ve el último equipo (simulado en mi pantalla): el gas y el despertar en la vitrina con su mensaje
        t.photo("c1_71_gas", 500, () -> {
            CompoundTag g = new CompoundTag();
            g.putString("fase", "gas");
            g.putLong("desde", Minecraft.getInstance().level.getGameTime() - 110);
            g.putInt("dura", 150);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sentidos.KEY_GAS, g);
        });
        t.photo("c1_72_despierta", 500, () -> {
            CompoundTag g = new CompoundTag();
            g.putString("fase", "despierta");
            g.putLong("desde", Minecraft.getInstance().level.getGameTime() - 125);
            g.putInt("dura", 160);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sentidos.KEY_GAS, g);
        });
        t.add("sin gas", 300, () -> com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sentidos.KEY_GAS, new CompoundTag()));
        t.photo("c1_73_linea_tiempo", 1500, () -> {
            LineaScreen s = new LineaScreen();
            com.axeliscore.plat.client.Screens.open(s);
            int[] c = s.nodo(4);
            com.axeliscore.ui.UiScreen.fakeMouse = c;
        });
        t.photo("c1_74_linea_cap2", 1200, () -> {
            com.axeliscore.ui.UiScreen.fakeMouse = null;
            if (com.axeliscore.plat.client.Screens.current() instanceof LineaScreen s) {
                s.capitulo(1);
            }
        });
        // los textos de todas las pantallas del organizador: ninguno se puede quedar cortado (en el idioma del juego)
        t.add("textos: limpiar", 300, () -> {
            com.axeliscore.ui.Ui.RECORTADOS.clear();
            com.axeliscore.ui.Ui.APRETADOS.clear();
        });
        for (int c = 0; c < LineaScreen.capitulos().size(); c++) {
            final int cc = c;
            t.add("textos: linea " + (c + 1), 350, () -> {
                if (com.axeliscore.plat.client.Screens.current() instanceof LineaScreen s) {
                    s.capitulo(cc);
                }
            });
        }
        t.add("textos: panel", 300, () -> com.axeliscore.plat.client.Screens.open(new PanelScreen()));
        for (int p = 0; p < 4; p++) {
            final int pp = p;
            t.add("textos: panel " + (p + 1), 350, () -> {
                if (com.axeliscore.plat.client.Screens.current() instanceof PanelScreen s) {
                    s.pestana(pp);
                }
            });
        }
        t.add("textos: revisar", 100, () -> {
            com.axeliscore.AxelisCore.LOGGER.info("COLECCIONISTA_TEXTOS achicados={} {}", com.axeliscore.ui.Ui.APRETADOS.size(),
                    com.axeliscore.ui.Ui.APRETADOS);
            com.axeliscore.AxelisCore.LOGGER.info("COLECCIONISTA_TEXTOS cortados={} {}", com.axeliscore.ui.Ui.RECORTADOS.size(),
                    com.axeliscore.ui.Ui.RECORTADOS);
            t.check(com.axeliscore.ui.Ui.RECORTADOS.isEmpty(), "ningún texto de las pantallas del organizador se corta: " + com.axeliscore.ui.Ui.RECORTADOS);
        });
        t.add("cerrar linea", 500, () -> com.axeliscore.plat.client.Screens.close());
        capitulo2(t);
        t.add("cap2: limpiar", 500, () -> {
            t.cmd("evento mascota limpiar");
            t.cmd("evento pocos false");
            t.cmd("evento sentidos limpiar");
            t.cmd("evento capitulo1 parar");
            t.cmd("gamemode adventure @a");
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("evento revivir " + yo());
        });
        capitulo3(t);
    }

    /** Mira (sentado) según dónde voy en la góndola: atrás, a la espalda del de adelante; adelante, hacia atrás. */
    private static void mirarEnGondola() {
        var me = Minecraft.getInstance().player;
        if (me == null || me.getVehicle() == null) {
            return;
        }
        float y = me.getVehicle().getYRot();
        boolean frente = "frente".equals(com.axeliscore.plat.Nbt.s(CajaCliente.mio(), "lado"));
        me.setYRot(frente ? y + 180F : y);
        me.setXRot(frente ? 2F : 12F);
        me.yRotO = me.getYRot();
        me.xRotO = me.getXRot();
    }

    /** Mira (sentado en el círculo) hacia un punto. */
    private static void mirarA(double x, double y, double z) {
        var me = Minecraft.getInstance().player;
        if (me == null) {
            return;
        }
        double dx = x - me.getX(), dy = y - me.getEyeY(), dz = z - me.getZ();
        me.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        me.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
        me.yRotO = me.getYRot();
        me.xRotO = me.getXRot();
    }

    private static boolean apuntaA(String nombre) {
        var e = CajaCliente.estado();
        if (!e.hasUUID("apuntado") || !"ESPERA".equals(com.axeliscore.plat.Nbt.s(e, "paso"))) {
            return false;
        }
        var p = EventoCliente.pieza(e.getUUID("apuntado"));
        return p != null && nombre.equalsIgnoreCase(com.axeliscore.plat.Nbt.s(p, "nombre"));
    }

    /**
     * Capítulo 3 · La caja de música: el pasillo, el salón con el carrusel, la carta del papel, la máscara y la tarjeta de la espalda, un
     * apagón, dos turnos en la góndola (atrás y adelante), el círculo con el arma bajando, un balazo a un bot (con su sangre), mi turno
     * (la pantalla de los símbolos), la pantalla del organizador y el final.
     * La caja está en (790, -61, 430): el pasillo en x 791..806 / z 447..456, el salón en x 807..853 / z 430..472, el escenario en
     * x 823..837 / z 444..458 (centro 830, 451).
     */
    private static void capitulo3(ClientTest.Script t) {
        t.add("cap3: preparar", 600, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add 784 424 864 480");
            t.cmd("evento caja tiempos 10 20");
            t.cmd("evento caja esperar true");
            t.cmd("evento pocos true");
            t.cmd("evento caja llegar");
        });
        t.waitFor("cap3: llegando", 15000, () -> "LLEGANDO".equals(CajaCliente.fase()));
        t.photo("c3_01_pasillo", 1500, () -> t.lookFrom(799.5, -60, 451.5, 807, -59, 451.5));
        t.add("puertas de la caja", 100, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(806, -60, 455));
            t.check(st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN),
                    "se abrieron las puertas del pasillo al salón del carrusel");
        });
        t.add("al salon", 300, () -> t.lookFrom(811.5, -60, 451.5, 817, -55, 451.5));
        t.waitFor("cap3: habla", 15000, () -> "HABLANDO".equals(CajaCliente.fase()));
        t.photo("c3_02_carrusel", 5000, () -> t.lookFrom(808.5, -59, 433.5, 830, -56, 451));
        t.photo("c3_02b_pantalla", 2500, () -> t.lookFrom(809.5, -60, 451.5, 816, -53, 451.5));
        t.photo("c3_02c_gondolas", 2000, () -> t.lookFrom(830.5, -60, 432.5, 830.5, -58.5, 438));
        t.photo("c3_03_carta", 1500, () -> {
            t.lookFrom(811.5, -60, 451.5, 817, -59, 451.5);
            t.cmd("evento caja saltar");
        });
        t.add("papel", 100, () -> {
            t.check("ROLES".equals(CajaCliente.fase()), "se repartieron los papeles (" + CajaCliente.fase() + ")");
            t.check(!CajaCliente.rol().isEmpty(), "tengo papel: " + CajaCliente.rol());
            t.check(CajaCliente.anonimo(), "es la parte a ciegas (sin nombres)");
        });
        // sin HUD: un bot de espaldas frente a mí (su tarjeta y su máscara) y yo de frente (mi máscara)
        t.photo("c3_03c_espalda", 1200, () -> {
            Minecraft.getInstance().options.hideGui = true;
            t.cmd("tp @a 811.5 -60 451.5 270 8");
            t.cmd("tp @e[type=coleccionista:bot,limit=1,sort=nearest] 813.3 -60 451.5 270 0");
        });
        t.photo("c3_03b_mascara", 1000, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.add("sin hud", 200, () -> {
            Minecraft.getInstance().options.hideGui = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        t.waitFor("cap3: apagon", 12000, () -> "APAGON".equals(CajaCliente.fase()));
        t.photo("c3_04_apagon", 1200, () -> {
        });
        t.waitFor("cap3: turno 1", 8000, () -> "TURNO".equals(CajaCliente.fase()));
        t.photo("c3_05_gondola", 1500, PruebaCliente::mirarEnGondola);
        t.add("sentado", 100, () -> {
            var me = Minecraft.getInstance().player;
            t.check(me.getVehicle() instanceof com.coleccionista.evento.Silla, "voy sentado en mi góndola");
            t.check(com.axeliscore.plat.Nbt.i(CajaCliente.mio(), "gondola") > 0, "sé en qué góndola voy");
            t.check(Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON, "en primera persona");
            // mi pareja (si no voy solo) está sentada en la misma góndola, a 2 bloques
            int cerca = 0;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.bot.PiezaBot b) {
                    com.axeliscore.AxelisCore.LOGGER.info("COLECCIONISTA_CAJA_BOT {} en {} {} {} vehiculo={} {}", b.getName().getString(),
                            (int) Math.floor(b.getX()), (int) Math.floor(b.getY()), (int) Math.floor(b.getZ()),
                            b.getVehicle() == null ? "-" : b.getVehicle().getClass().getSimpleName(),
                            b.getVehicle() == null ? "" : (int) b.getVehicle().getX() + "," + (int) b.getVehicle().getZ());
                    if (b.distanceTo(me) < 3.5) {
                        cerca++;
                    }
                }
            }
            if (!"solo".equals(com.axeliscore.plat.Nbt.s(CajaCliente.mio(), "lado"))) {
                t.check(cerca >= 1, "mi pareja va sentada conmigo en la góndola (" + cerca + ")");
            }
        });
        t.waitFor("cap3: turno 2", 20000, () -> "TURNO".equals(CajaCliente.fase()) && com.axeliscore.plat.Nbt.i(CajaCliente.estado(), "turno") == 2);
        t.photo("c3_06_gondola2", 1500, PruebaCliente::mirarEnGondola);
        // los demás turnos, rápido
        for (int i = 0; i < 5; i++) {
            t.add("cap3: saltar turno " + (i + 3), 4600, () -> t.cmd("evento caja saltar"));
        }
        t.waitFor("cap3: circulo", 15000, () -> "CIRCULO".equals(CajaCliente.fase()));
        t.add("orden", 100, () -> {
            t.cmd("evento caja primero Bot_03");
            t.cmd("evento caja primero " + yo());
        });
        t.photo("c3_07_circulo", 1500, () -> mirarA(830.5, -55.5, 451.5));
        t.photo("c3_08_arma_baja", 4000, () -> mirarA(830.5, -56.5, 451.5));
        t.photo("c3_08b_tercera", 1000, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        t.add("primera", 200, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        t.waitFor("cap3: apunta al bot", 40000, () -> apuntaA("Bot_03"));
        t.photo("c3_09_apunta_bot", 600, () -> mirarA(830.5, -56.5, 451.5));
        t.photo("c3_10_tension", 1000, () -> t.cmd("evento caja bala"));
        t.photo("c3_11_bang", 1250, () -> {
        });
        t.photo("c3_12_sangre", 2500, () -> {
        });
        t.waitFor("cap3: me apunta", 30000, () -> apuntaA(yo()));
        t.photo("c3_13_elige", 1200, () -> {
        });
        t.add("elige", 300, () -> {
            t.check(Minecraft.getInstance().screen instanceof EligeSimboloScreen, "me abrió la pantalla para marcar mi símbolo");
            CompoundTag c = new CompoundTag();
            c.putString("que", "simbolo");
            c.putInt("s", 1);
            com.axeliscore.data.client.ClientData.send(com.coleccionista.prueba.Caja.ACCION, c);
        });
        t.photo("c3_13b_marcado", 700, () -> {
        });
        // lo que ve el organizador (simulado en mi pantalla)
        t.photo("c3_14_organizador", 900, () -> {
            com.axeliscore.plat.client.Screens.close();
            CompoundTag o = new CompoundTag();
            o.putString("nombre", "Bot_05");
            o.putString("etiqueta", "05");
            o.putString("real", "trebol");
            o.putString("marco", "corazon");
            o.putBoolean("traidor", false);
            o.putString("paso", "ESPERA");
            o.putInt("i", 3);
            o.putInt("de", 8);
            o.putString("fase", "REVISION");
            o.putBoolean("juez", true);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Caja.KEY_ORG, o);
            com.axeliscore.plat.client.Screens.open(new RevisionScreen());
        });
        t.add("fin organizador", 200, () -> {
            com.axeliscore.plat.client.Screens.close();
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Caja.KEY_ORG, new CompoundTag());
            t.cmd("evento caja bien");
            t.cmd("evento caja esperar false");
            t.cmd("evento caja auto true");
        });
        t.waitFor("cap3: fin", 150000, () -> "FIN".equals(CajaCliente.fase()));
        t.photo("c3_15_fin", 3000, () -> mirarA(830.5, -55, 451.5));
        t.add("cap3: limpiar", 1500, () -> {
            t.cmd("evento caja limpiar");
            t.cmd("evento caja auto false");
            t.cmd("evento pocos false");
            t.cmd("gamemode adventure @a");
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("evento revivir " + yo());
        });
    }

    /**
     * Capítulo 2 · La mascota: al terminar la antesala del teatro pasan (sin que se note) al pasillo de la casa; se abren las puertas y,
     * ya en el vestíbulo, habla el Coleccionista (Cascabel ruge detrás de la reja). Luego el minuto para esconderse y yo me convierto en
     * Cascabel: la cámara se va detrás, ruge, olfatea y se come a una pieza; al final vuelve a su jaula y vuelvo a ser yo.
     */
    private static void capitulo2(ClientTest.Script t) {
        // la casa está en (720, -61, 430): el pasillo en x 722..735 y z 448..455, el vestíbulo en x 737..760 y z 440..463, la jaula detrás
        t.waitFor("cap2: a la casa", 30000, () -> !"".equals(MascotaCliente.fase()) && !"NADA".equals(MascotaCliente.fase()));
        t.photo("c2_01_pasillo_casa", 1800, () -> {
            t.cmd("gamemode adventure @a");
            t.lookFrom(724.5, -60, 451.5, 737, -59, 451.5);
        });
        t.add("puertas de la casa", 100, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(736, -60, 455));
            t.check(st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN),
                    "se abrieron las puertas del pasillo al vestíbulo");
            t.check("LLEGANDO".equals(MascotaCliente.fase()) || "HABLANDO".equals(MascotaCliente.fase()), "llegamos a la casa (" + MascotaCliente.fase() + ")");
        });
        t.add("al vestibulo", 200, () -> t.lookFrom(744.5, -60, 451.5, 760, -53, 451.5));
        t.waitFor("cap2: habla", 15000, () -> "HABLANDO".equals(MascotaCliente.fase()));
        t.photo("c2_02_vestibulo", 9000, () -> t.lookFrom(741.5, -60, 451.5, 760, -53.5, 451.5));
        t.photo("c2_03_jaula", 4000, () -> t.lookFrom(756.5, -60, 451.5, 768, -59, 451.5));
        t.photo("c2_04_galeria", 2500, () -> t.lookFrom(739.5, -53, 442.5, 752, -60, 455));
        t.photo("c2_05_esconder", 1800, () -> {
            t.cmd("evento mascota saltar");
            t.lookFrom(744.5, -60, 451.5, 737, -59, 444);
        });
        t.add("esconder", 100, () -> t.check("ESCONDER".equals(MascotaCliente.fase()), "corre el minuto para esconderse (" + MascotaCliente.fase() + ")"));
        // una pieza solo ve en primera persona y, en lugar del inventario, su bolsillo con el logo
        t.add("solo primera", 400, () -> {
            SoloPrimera.permitir = false;
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        t.add("primera persona", 100, () -> t.check(Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON,
                "una pieza no puede ver en tercera persona"));
        t.photo("c2_05b_bolsillo", 900, () -> com.axeliscore.plat.client.Screens.open(
                new net.minecraft.client.gui.screens.inventory.InventoryScreen(Minecraft.getInstance().player)));
        t.add("bolsillo", 300, () -> {
            t.check(Minecraft.getInstance().screen instanceof BolsilloScreen, "en lugar del inventario sale el bolsillo");
            com.axeliscore.plat.client.Screens.close();
            SoloPrimera.permitir = true;
        });
        // yo soy Cascabel
        t.add("cap2: soy cascabel", 300, () -> {
            t.cmd("evento pocos true");
            t.cmd("evento mascota cazador @p");
        });
        t.photo("c2_06_cascabel", 2500, () -> t.cmd("evento mascota soltar"));
        t.add("transformado", 100, () -> {
            var me = Minecraft.getInstance().player;
            t.check(com.axeliscore.forma.client.FormasCliente.transformado(me), "me veo como Cascabel");
            t.check(MascotaCliente.soyCazador(), "soy Cascabel");
            t.check(Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK, "la cámara se fue detrás para verme");
            t.check(me.getBbHeight() > 1.85F, "mi caja es la de Cascabel (" + me.getBbHeight() + ")");
        });
        t.photo("c2_07_cascabel_frente", 1200, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.photo("c2_08_ruge", 700, MascotaCliente::rugir);
        t.photo("c2_09_olfatea", 1500, () -> {
            t.cmd("tp @a 762.5 -60 451.5 90 10");
            MascotaCliente.olfatear();
        });
        // se come a una pieza (la acerco primero para que se vea)
        t.add("presa cerca", 600, () -> t.cmd("tp @e[type=coleccionista:bot] 759.5 -60 451.5"));
        t.photo("c2_10_atrapa", 450, () -> t.cmd("evento mascota comer"));
        t.photo("c2_11_traga", 900, () -> {
        });
        t.add("comio", 2500, () -> {
        });
        t.add("se lo comio", 100, () -> {
            var d = com.axeliscore.data.client.ClientData.get(com.coleccionista.prueba.Mascota.KEY);
            t.check(d != null && d.getInt("comidos") >= 1, "Cascabel se comió a una pieza (" + (d == null ? -1 : d.getInt("comidos")) + ")");
        });
        t.photo("c2_12_hud_cascabel", 900, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        // lo que ve quien se comen (simulado en mi pantalla): los dientes y lo negro
        t.photo("c2_13_comido_dientes", 300, () -> {
            CompoundTag c = new CompoundTag();
            c.putLong("desde", Minecraft.getInstance().level.getGameTime() - 15);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Mascota.KEY_COMIDO, c);
        });
        t.photo("c2_14_comido_negro", 300, () -> {
            CompoundTag c = new CompoundTag();
            c.putLong("desde", Minecraft.getInstance().level.getGameTime() - 34);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Mascota.KEY_COMIDO, c);
        });
        t.add("sin comido", 300, () -> com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Mascota.KEY_COMIDO, new CompoundTag()));
        // se acaba: Cascabel a su jaula, se cierra la reja y vuelvo a ser yo
        t.photo("c2_15_a_su_jaula", 1500, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
            t.cmd("evento mascota terminar");
        });
        t.add("reja cerrada", 100, () -> t.check(!Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(761, -60, 451)).isAir(),
                "se cerró la reja de la jaula"));
        t.add("vuelvo a ser yo", 3500, () -> {
        });
        t.add("normal", 100, () -> {
            t.check(!com.axeliscore.forma.client.FormasCliente.transformado(Minecraft.getInstance().player), "volví a ser yo");
            t.check(Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON, "la cámara regresó a mis ojos");
        });
    }

    private static boolean escuchando, muestreando;
    private static int lamparasFallando;

    /** Cuenta los ticks en los que alguna lámpara del pasillo del Ala está apagada por una falla (las lámparas malas parpadean solas). */
    private static void muestrearLamparas(boolean si) {
        if (!escuchando) {
            escuchando = true;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent e) -> {
                var l = Minecraft.getInstance().level;
                if (!muestreando || e.phase != net.minecraftforge.event.TickEvent.Phase.END || l == null) {
                    return;
                }
                for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(7, -60, 37, 53, -50, 59)) {
                    var s = l.getBlockState(p);
                    if (s.hasProperty(com.coleccionista.bloque.deco.DecoBlock.FALLA) && s.getValue(com.coleccionista.bloque.deco.DecoBlock.FALLA)) {
                        lamparasFallando++;
                        break;
                    }
                }
            });
        }
        muestreando = si;
    }

    /** Mi pareja en Encadenados (su nombre), o "" si no tengo. */
    private static String pareja() {
        java.util.UUID yo = Minecraft.getInstance().player.getUUID();
        for (EncadenadosCliente.Cadena c : EncadenadosCliente.cadenas) {
            java.util.UUID otro = c.a().equals(yo) ? c.b() : c.b().equals(yo) ? c.a() : null;
            if (otro != null) {
                var p = EventoCliente.pieza(otro);
                return p == null ? "" : com.axeliscore.plat.Nbt.s(p, "nombre");
            }
        }
        return "";
    }

    /**
     * Prueba 1, Encadenados: fundido, presentación, la Sala de los grilletes (cadenas a la pared y a la pareja, el aviso de la llave),
     * la compuerta de dos placas, el frente del gas, adentro del gas, el Refugio y el final (el gas se lleva a los bots que se quedaron).
     */
    private static void encadenados(ClientTest.Script t) {
        t.photo("32_fundido", 1600, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("evento inscribir @a");
            t.cmd("evento revivir Bot_02");
            t.cmd("forceload add 48 258 170 306");
            t.cmd("evento encadenados tiempo 30");
            t.cmd("evento encadenados gracia 2");
            t.cmd("evento encadenados preparar");
        });
        t.photo("33_presentacion", 5700, () -> {
        });
        t.add("después del título", 6000, () -> t.check(!Presentacion.reglasVisibles(), "las reglas esperan al Coleccionista (no salen solas)"));
        t.add("encadenado", 100, () -> {
            t.check("LISTA".equals(EncadenadosCliente.fase), "Encadenados está lista");
            t.check(EncadenadosCliente.encadenado(), "estoy encadenado a la pared");
            t.check(!pareja().isEmpty(), "tengo pareja (cadena)");
        });
        t.photo("34_grilletes", 4800, () -> {
        });
        t.photo("35_cadenas_tercera", 1200, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.add("a jugar", 1200, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.cmd("minijuego 1 ya");
            t.cmd("evento encadenados soltar todos");
            t.cmd("evento encadenados ir enc.placa.1.a " + yo());
            t.cmd("evento encadenados ir enc.placa.1.b " + pareja());
        });
        t.photo("33b_reglas", 1600, () -> {
        });
        t.add("reglas", 100, () -> t.check(Presentacion.reglasVisibles(), "al arrancar sale la tarjeta de reglas arriba a la derecha"));
        t.photo("36_compuerta", 900, () -> {
        });
        t.add("compuerta", 100, () -> {
            t.check(!EncadenadosCliente.encadenado(), "ya no estoy encadenado a la pared");
            t.check(EncadenadosCliente.placas.stream().anyMatch(EncadenadosCliente.Placa::abierta), "la compuerta se abrió con las dos placas");
            t.cmd("evento encadenados ir enc.centro " + yo());
        });
        t.add("voltear", 600, () -> Minecraft.getInstance().player.setYRot(90F));
        t.photo("37_gas_frente", 1300, () -> Minecraft.getInstance().player.setYRot(90F));
        t.photo("38_gas_dentro", 2600, () -> {
        });
        t.add("en el gas", 100, () -> {
            t.check(Gas.adentro(Minecraft.getInstance().player.getEyePosition(), 0), "estoy dentro del gas");
            t.check(EncadenadosCliente.aire() < 100, "se me acaba el aire");
            t.cmd("evento encadenados ir enc.salida " + yo());
        });
        t.photo("39_refugio", 1600, () -> {
        });
        t.add("a salvo", 100, () -> t.check(EncadenadosCliente.aSalvo.contains(Minecraft.getInstance().player.getUUID()), "llegué al Refugio"));
        t.add("esperar el gas", 9000, () -> {
        });
        t.photo("40_fin", 600, () -> {
        });
        t.add("fin de la prueba", 100, () -> t.check("FIN".equals(EncadenadosCliente.fase), "terminó Encadenados (el gas se llevó a los demás)"));
        t.photo("41_panel_pruebas", 900, () -> {
            PanelScreen s = new PanelScreen();
            com.axeliscore.plat.client.Screens.open(s);
            s.pestana(2);
        });
        t.add("cerrar", 500, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("evento encadenados terminar");
        });
        compartir(t);
    }

    /** Prueba 3, Compartir o robar: el Salón, la mesa, la pantalla para elegir, la revelación de una mesa y el final. */
    private static void compartir(ClientTest.Script t) {
        // el recorrido: en el elevador a la sala de llegada del Salón, los cubículos para formar equipos y la sala de espera
        t.add("recorrido", 600, () -> {
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("forceload add 24 190 80 212");
            t.cmd("gamemode adventure @a");
            t.cmd("evento recorrido ir compartir");
        });
        t.photo("41b_elevador_llegada", 2500, () -> {
        });
        t.add("rec: equipos", 1500, () -> t.cmd("evento recorrido saltar"));
        t.photo("41c_cubiculos", 1500, () -> t.lookFrom(44.5, -60, 203.5, 40, -58.5, 193.5));
        t.photo("41d_cubiculos_sur", 1200, () -> t.lookFrom(44.5, -60, 199.5, 40, -58.5, 209.5));
        t.photo("41e_sala_espera", 1200, () -> t.lookFrom(60.5, -60, 201.5, 75, -58.5, 201.5));
        t.add("rec: parar", 600, () -> {
            t.cmd("evento recorrido parar");
            t.cmd("evento reloj 0");
        });
        // de espaldas a las pantallas: el Coleccionista habla en la mini tele de arriba a la derecha
        t.add("habla sin tele enfrente", 600, () -> t.cmd("evento pantalla hablar antesala"));
        t.photo("41g_mini_tele", 4200, () -> {
        });
        t.add("mini tele", 100, () -> t.check(MiniTele.visible(), "sale la mini tele con el Coleccionista"));
        t.add("com: preparar", 1600, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add 160 16 208 56");
            t.cmd("evento compartir hablar 8");
            t.cmd("evento compartir tiempo_elegir 12");
            t.cmd("evento compartir ritmo 100");
            t.cmd("evento compartir preparar");
        });
        t.photo("42_salon_presentacion", 5700, () -> {
        });
        t.add("en la mesa", 100, () -> t.check("LISTA".equals(CompartirCliente.fase()), "Compartir o robar está lista"));
        t.photo("43_mesa", 5000, () -> {
        });
        t.photo("44_hablar", 1200, () -> t.cmd("evento compartir empezar ya"));
        t.photo("45_eleccion", 1500, () -> t.cmd("evento compartir elegir"));
        t.add("pantalla de elegir", 100, () -> t.check(com.axeliscore.plat.client.Screens.current() instanceof EleccionScreen,
                "se abrió la pantalla para elegir"));
        t.photo("46_elegido", 900, () -> {
            net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
            c.putBoolean("robar", false);
            com.axeliscore.data.client.ClientData.send(com.coleccionista.prueba.Compartir.ACCION, c);
        });
        t.add("revelar", 1500, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("evento compartir revelar");
        });
        t.photo("47_revelacion", 4000, () -> {
        });
        t.add("revelando", 100, () -> t.check("REVELAR".equals(CompartirCliente.fase()), "se están revelando las mesas"));
        t.add("esperar las mesas", 27500, () -> {
        });
        t.photo("48_compartir_fin", 600, () -> {
        });
        t.add("fin compartir", 100, () -> t.check("FIN".equals(CompartirCliente.fase()), "se revelaron todas las mesas"));
        t.photo("49_panel_pruebas2", 900, () -> {
            PanelScreen s = new PanelScreen();
            com.axeliscore.plat.client.Screens.open(s);
            s.pestana(2);
        });
        t.add("cerrar compartir", 500, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("evento compartir terminar");
        });
        puertas(t);
    }

    /** Prueba 6, Las puertas: el vestíbulo, una ronda con colores y pista, la cabina con las rejas cerradas, la trampa y el final. */
    private static void puertas(ClientTest.Script t) {
        t.add("pue: preparar", 1600, () -> {
            t.cmd("evento revivir " + yo());
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add 0 -24 48 24");
            t.cmd("evento puertas rondas 1");
            t.cmd("evento puertas tiempo 30");
            t.cmd("evento puertas preparar");
        });
        t.photo("50_puertas_presentacion", 5700, () -> {
        });
        t.photo("51_vestibulo", 5000, () -> {
        });
        t.add("puertas lista", 100, () -> t.check("LISTA".equals(PuertasCliente.fase()), "Las puertas está lista"));
        t.photo("52_ronda", 1500, () -> t.cmd("evento puertas ronda"));
        t.add("eligiendo", 100, () -> t.check("ELEGIR".equals(PuertasCliente.fase()), "se abrieron las rejas para elegir"));
        t.photo("53_cabina", 1500, () -> t.cmd("evento puertas cerrar"));
        t.photo("54_trampa", 3000, () -> {
        });
        t.add("trampa", 100, () -> t.check("CAE".equals(PuertasCliente.fase()) || "FIN".equals(PuertasCliente.fase()), "se abrió el piso de una cabina"));
        t.add("esperar fin", 5200, () -> {
        });
        t.add("fin puertas", 100, () -> {
            t.check("FIN".equals(PuertasCliente.fase()), "terminó Las puertas");
            t.cmd("evento puertas terminar");
        });
        copas(t);
    }

    /** La final, Las copas: yo y dos bots en la Cámara final; turnos hasta que queda una sola pieza. */
    private static void copas(ClientTest.Script t) {
        t.add("cop: preparar", 1600, () -> {
            t.cmd("evento revivir " + yo());
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            for (int i = 4; i <= 8; i++) {
                t.cmd("evento eliminar Bot_0" + i);
            }
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add 0 -56 40 -20");
            t.cmd("evento copas tiempo 20");
            t.cmd("evento copas preparar");
        });
        t.photo("55_copas_presentacion", 6200, () -> {
        });
        t.photo("56_mesa_final", 5000, () -> {
        });
        t.add("final lista", 100, () -> t.check("LISTA".equals(CopasCliente.fase()), "la final está lista"));
        t.photo("57_turno", 1500, () -> t.cmd("evento copas empezar ya"));
        t.add("turno", 100, () -> t.check("TURNO".equals(CopasCliente.fase()), "alguien tiene el turno"));
        t.photo("58_bebe", 1500, () -> t.cmd("evento copas beber"));
        for (int i = 0; i < 12; i++) {
            t.add("beber " + i, 4300, () -> t.cmd("evento copas beber"));
        }
        t.photo("59_ganador", 1200, () -> {
        });
        t.add("hay ganador", 100, () -> {
            t.check("FIN".equals(CopasCliente.fase()), "terminó la final");
            t.check(EventoCliente.vivos() == 1, "queda una sola pieza (la ganadora)");
            t.cmd("evento copas terminar");
        });
        sacrificio(t);
    }

    /** Prueba 5, El sacrificio: el cuarto, ofrecerse (nadie), votar en secreto, "¡salgan!" y el final. */
    private static void sacrificio(ClientTest.Script t) {
        t.add("sac: preparar", 1600, () -> {
            t.cmd("evento revivir " + yo());
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add -16 64 64 96");
            t.cmd("evento sacrificio tamano 4");
            t.cmd("evento sacrificio tiempo 12 15");
            t.cmd("evento sacrificio bots_ofrecen no");
            t.cmd("evento sacrificio preparar");
        });
        t.photo("60_sacrificio_presentacion", 5700, () -> {
        });
        t.photo("61_cuarto", 5000, () -> {
        });
        t.add("sacrificio lista", 100, () -> t.check("LISTA".equals(SacrificioCliente.fase()), "El sacrificio está lista"));
        t.photo("62_ofrecer", 1500, () -> t.cmd("evento sacrificio empezar ya"));
        t.photo("63_voto", 12000, () -> {
        });
        t.add("pantalla de voto", 100, () -> t.check(com.axeliscore.plat.client.Screens.current() instanceof VotoScreen,
                "se abrió la pantalla para votar"));
        t.photo("64_votado", 900, () -> {
            var c = SacrificioCliente.miCuarto();
            if (c != null) {
                for (var y : c.getList("miembros", 10)) {
                    var m = (net.minecraft.nbt.CompoundTag) y;
                    if (!m.getUUID("id").equals(Minecraft.getInstance().player.getUUID())) {
                        net.minecraft.nbt.CompoundTag v = new net.minecraft.nbt.CompoundTag();
                        v.putUUID("id", m.getUUID("id"));
                        com.axeliscore.data.client.ClientData.send(com.coleccionista.prueba.Sacrificio.ACCION, v);
                        SacrificioCliente.miVoto = m.getUUID("id");
                        break;
                    }
                }
            }
        });
        t.add("cerrar voto", 13000, () -> com.axeliscore.plat.client.Screens.close());
        t.photo("65_salgan", 600, () -> {
        });
        t.add("esperar sacrificio", 10000, () -> {
        });
        t.photo("66_sacrificio_fin", 600, () -> {
        });
        t.add("fin sacrificio", 100, () -> {
            t.check("FIN".equals(SacrificioCliente.fase()), "terminó El sacrificio (cada cuarto perdió una pieza)");
            t.cmd("evento sacrificio terminar");
        });
        traidor(t);
    }

    /** Prueba 2, El traidor: la sala, la carta secreta del papel, la nota, el teclado de la bóveda y el final con los traidores. */
    private static void traidor(ClientTest.Script t) {
        t.add("tra: preparar", 1600, () -> {
            t.cmd("evento revivir " + yo());
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add -16 88 64 120");
            t.cmd("evento traidor tamano 4");
            t.cmd("evento traidor tiempo 90");
            t.cmd("evento traidor preparar");
        });
        t.photo("67_traidor_presentacion", 5700, () -> {
        });
        t.photo("68_sala_archivo", 5000, () -> {
        });
        t.add("traidor lista", 100, () -> t.check("LISTA".equals(TraidorCliente.fase()), "El traidor está lista"));
        t.photo("69_papel", 2500, () -> t.cmd("evento traidor empezar ya"));
        t.add("papel", 100, () -> t.check(!TraidorCliente.rol().isEmpty(), "me tocó un papel (leal o traidor)"));
        t.photo("70_nota", 6000, () -> {
            var inv = Minecraft.getInstance().player.getInventory();
            for (int i = 0; i < 9; i++) {
                var st = inv.getItem(i);
                if (st.hasTag() && st.getTag().getBoolean("coleccionista_nota")) {
                    inv.selected = i;
                }
            }
        });
        t.add("buscando", 100, () -> t.check("BUSCAR".equals(TraidorCliente.fase()), "empezó la búsqueda del código"));
        t.photo("71_teclado", 900, () -> com.axeliscore.plat.client.Screens.open(new TecladoScreen()));
        t.add("cortar", 3000, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("evento traidor cortar");
        });
        t.photo("72_castigo", 600, () -> {
        });
        t.add("esperar traidor", 22000, () -> {
        });
        t.photo("73_traidor_fin", 600, () -> {
        });
        t.add("fin traidor", 100, () -> {
            t.check("FIN".equals(TraidorCliente.fase()), "terminó El traidor");
            com.axeliscore.plat.client.Screens.close();
            t.cmd("evento traidor terminar");
        });
        ciegas(t);
    }

    /** Prueba 4, A ciegas: primero camino con la venda (casi no veo nada), luego soy el guía y veo el laberinto con las trampas en rojo. */
    private static void ciegas(ClientTest.Script t) {
        t.add("cie: preparar", 1600, () -> {
            t.cmd("evento revivir " + yo());
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add -160 24 -16 64");
            t.cmd("evento ciegas tamano 2");
            t.cmd("evento ciegas tiempo 120");
            t.cmd("evento ciegas trampas 10");
            t.cmd("evento ciegas preparar");
        });
        t.photo("74_ciegas_presentacion", 5700, () -> {
        });
        t.add("ciegas lista", 100, () -> {
            t.check("LISTA".equals(CiegasCliente.fase()), "A ciegas está lista");
            // que me toque caminar: el guía de mi equipo será mi compañero
            String otro = CiegasCliente.companero();
            if (otro != null) {
                t.cmd("evento ciegas guia " + otro);
            }
        });
        t.add("cie: esperar la presentación", 5000, () -> {
        });
        t.add("cie: empezar", 1500, () -> t.cmd("evento ciegas empezar ya"));
        t.photo("75_vendado", 2500, () -> Minecraft.getInstance().player.setXRot(8));
        t.add("vendado", 100, () -> t.check(CiegasCliente.vendado(), "camino con los ojos vendados"));
        t.add("cie: otra vez", 1500, () -> {
            t.cmd("evento ciegas terminar");
            t.cmd("evento ciegas preparar");
        });
        t.add("cie: ser guía", 11000, () -> t.cmd("evento ciegas guia " + yo()));
        t.add("cie: empezar 2", 1500, () -> t.cmd("evento ciegas empezar ya"));
        t.photo("76_guia", 2500, () -> Minecraft.getInstance().player.setXRot(62));
        t.add("guía", 100, () -> t.check(CiegasCliente.soyElGuia() && !CiegasCliente.vendado(), "soy el guía: veo todo y las trampas en rojo"));
        t.add("cie: cortar", 1500, () -> t.cmd("evento ciegas cortar"));
        t.photo("77_ciegas_fin", 1500, () -> {
        });
        t.add("fin ciegas", 100, () -> {
            t.check("FIN".equals(CiegasCliente.fase()), "terminó A ciegas");
            t.cmd("evento ciegas terminar");
        });
        tres(t);
    }

    /** Prueba extra, Encadenados de tres: la sala de la mesa, elegir las esposas, la cadena entre el trío y la salida. */
    private static void tres(ClientTest.Script t) {
        t.add("tres: preparar", 1600, () -> {
            t.cmd("evento revivir " + yo());
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add -112 64 -32 128");
            t.cmd("evento tres bots_salen no");
            t.cmd("evento tres tiempo 60 120");
            t.cmd("evento tres preparar");
        });
        t.photo("78_tres_presentacion", 5700, () -> {
        });
        t.photo("79_tres_mesa", 9000, () -> Minecraft.getInstance().player.setXRot(25));
        t.add("tres lista", 100, () -> t.check("LISTA".equals(SentidosCliente.fase()), "Encadenados de tres está lista"));
        t.photo("80_tres_elegir", 2000, () -> t.cmd("evento tres empezar ya"));
        t.add("tres: tomar esposas", 1500, () -> t.cmd("evento tres toma " + yo() + " 2"));
        t.add("tres: rol", 100, () -> t.check("ESPOSAS".equals(SentidosCliente.miRol()), "me puse las esposas"));
        t.add("tres: arrancar", 3500, () -> t.cmd("evento tres arrancar"));
        t.photo("81_tres_recorrido", 1500, () -> Minecraft.getInstance().player.setXRot(10));
        t.add("tres: juego", 100, () -> t.check("JUEGO".equals(SentidosCliente.fase()), "se abrieron las rejas: a cruzar encadenados"));
        t.photo("82_tres_cadena", 1500, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.add("tres: sacar", 1500, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.cmd("evento tres sacar " + yo());
        });
        t.photo("83_tres_fin", 3000, () -> {
        });
        t.add("fin tres", 100, () -> {
            t.check("FIN".equals(SentidosCliente.fase()), "terminó Encadenados de tres");
            t.cmd("evento tres terminar");
        });
    }
}

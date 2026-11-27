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

    /** Una foto (y en el rodaje del tráiler, si esa foto tiene toma, la toma: el mundo se congela y la cámara se mueve). */
    private static void foto(ClientTest.Script t, String nombre, long espera, Runnable preparar) {
        t.photo(nombre, espera, preparar);
        if (Rodaje.hay(nombre)) {
            t.add(nombre + " (rodar)", 0, () -> Rodaje.empezar(nombre));
            t.waitFor(nombre + " (rodada)", 900000, Rodaje::listo);
        }
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
        foto(t, "01_hud_espera", 800, () -> {
        });
        foto(t, "02_reloj", 900, () -> {
            t.cmd("evento prueba Encadenados");
            t.cmd("evento reloj 754");
        });
        foto(t, "03_parejas", 900, () -> t.cmd("evento parejas"));
        t.add("parejas", 100, () -> t.check(!EventoCliente.companeros().isEmpty(), "tengo pareja"));
        foto(t, "04_eliminado", 900, () -> t.cmd("evento eliminar Bot_02 Encadenados"));
        t.add("eliminado", 100, () -> t.check(EventoCliente.vivos() == 7, "quedan 7: " + EventoCliente.vivos()));
        t.add("esperar franja", 4200, () -> {
        });
        foto(t, "06_mi_eliminacion", 2800, () -> t.cmd("evento eliminar " + yo()));
        t.add("soy fantasma", 100, () -> {
            t.check(EventoCliente.esFantasma(Minecraft.getInstance().player.getUUID()), "quedé como fantasma");
            t.check(Minecraft.getInstance().player.isSpectator(), "estoy en modo espectador");
        });
        t.add("esperar escena", 4800, () -> {
        });
        foto(t, "07_fantasma_hud", 600, () -> {
        });
        foto(t, "08_fantasma_tercera", 1200, () -> {
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
        foto(t, "11_panel_piezas", 1200, () -> {
            PanelScreen s = new PanelScreen();
            com.axeliscore.plat.client.Screens.open(s);
            s.pestana(0);
        });
        foto(t, "12_panel_evento", 900, () -> {
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
        foto(t, "13_panel_camaras", 900, () -> {
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
        foto(t, "09_pausa", 1200, () -> t.cmd("evento pausar"));
        t.add("reanudar", 800, () -> t.cmd("evento reanudar"));
        // quedo yo solo: salen las 6 franjas y luego la del ganador
        t.add("final", 4600, () -> {
            for (String b : new String[] {"Bot_03", "Bot_04", "Bot_05", "Bot_06", "Bot_07", "Bot_08"}) {
                t.cmd("evento eliminar " + b);
            }
        });
        t.add("esperar franjas", 24000, () -> {
        });
        foto(t, "10_ganador", 1500, () -> {
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
        foto(t, "20_sala_espera", 3000, () -> t.cmd("evento sala"));
        t.add("en la sala", 100, () -> {
            t.check(EventoCliente.sala(), "estamos en la sala de espera");
            t.check(Minecraft.getInstance().player.isSleeping(), "estoy acostado en mi cama");
            t.check(Espera.activa(), "veo la pantalla de espera");
        });
        // el ¡pum! prende las luces y la cinemática empieza segundo y medio después
        foto(t, "21_despertar_cine", 3500, () -> t.cmd("evento despertar"));
        t.add("cinematica", 100, () -> t.check(Cine.activo(), "está la cinemática del despertar"));
        foto(t, "22_parpadeo", 2900, () -> {
        });
        foto(t, "23_celda_luz", 3200, () -> {
        });
        t.add("despierto", 100, () -> {
            t.check(!Minecraft.getInstance().player.isSleeping(), "ya me levanté");
            t.check(!Cine.activo(), "terminó la cinemática");
        });
        // a los 15 segundos de levantarse se acaba el tiempo libre: apagón y, al volver la luz, los tres números escondidos en cada celda
        foto(t, "24a_apagon", 13800, () -> {
        });
        t.add("apagon", 100, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(9, -57, 33));
            t.check(st.getBlock() instanceof com.coleccionista.bloque.FocoBlock && !com.coleccionista.bloque.FocoBlock.prendido(st),
                    "se fue la luz de mi celda");
            t.check(InscripcionesCliente.cuantas(0) == 0, "todavía no hay números escritos");
        });
        foto(t, "24b_pistas", 5700, () -> {
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
            foto(t, "24c_pista_" + (i + 1), 1500, () -> mirarPista(t, k));
        }
        t.add("regresar a la celda", 900, () -> {
            t.cmd("gamemode adventure @a");
            t.lookFrom(9.5, -60, 34.5, 9.5, -57.5, 31);
        });
        // el uniforme de pieza (con su número) visto de frente y de espaldas
        foto(t, "24d_uniforme_frente", 900, () -> {
            t.check(!com.axeliscore.vestir.client.VestuarioCliente.de(Minecraft.getInstance().player.getUUID()).isEmpty(),
                    "llevo el uniforme de pieza");
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        foto(t, "24e_uniforme_espalda", 900, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        t.add("primera persona", 300, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        foto(t, "24_tele_habla", 300, () -> {
        });
        t.add("habla", 100, () -> {
            var st = com.axeliscore.data.client.ClientData.get(com.coleccionista.evento.Pantallas.KEY);
            t.check(st != null && "coleccionista".equals(st.getString("modo")), "las pantallas muestran al Coleccionista");
        });
        foto(t, "25_zoom_tele", 2600, Atajos::acercarse);
        t.add("zoom", 100, () -> t.check(Cine.enPantalla(), "la cámara está frente a la tele"));
        foto(t, "25b_zoom_subtitulo", 5000, () -> {
        });
        t.add("volver", 1200, Atajos::acercarse);
        t.add("esperar discurso", 64000, () -> {
        });
        // de cerca otra vez cuando ya terminó de hablar: debe decir que con R se cierra
        foto(t, "25c_zoom_fin", 1800, Atajos::acercarse);
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
        foto(t, "24f_reloj_salir", 600, () -> {
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
        foto(t, "26_candado", 600, () -> CandadoScreen.actual().arrastrarPrueba(0, 2.0, false));
        foto(t, "26e_candado_girando", 500, () -> CandadoScreen.actual().arrastrarPrueba(0, 4.4, false));
        t.add("soltar rueda", 600, () -> {
            CandadoScreen.actual().arrastrarPrueba(0, 4.4, true);
            CandadoScreen.actual().arrastrarPrueba(2, -3.1, true);
        });
        t.add("ruedas", 100, () -> t.check(CandadoScreen.actual().codigo() == 308, "las ruedas quedaron en 3 _ 8 (" + CandadoScreen.actual().codigo() + ")"));
        foto(t, "26f_candado_mal", 450, () -> CandadoScreen.actual().probarPrueba());
        t.add("cerrar candado", 700, () -> com.axeliscore.plat.client.Screens.close());
        t.add("candado cerrado", 100, () -> t.check(!Cine.activo(), "la cámara regresó a mis ojos"));
        // la reja de la celda 1: cerrada, corriéndose y abierta (con visión nocturna para que se vea en la foto)
        foto(t, "26b_reja_cerrada", 1500, () -> {
            t.cmd("gamemode spectator @a");
            t.cmd("effect give @a night_vision 30 0 true");
            t.lookFrom(9.5, G, 39.3, 9.5, G + 1, 36.5);
        });
        t.add("bajo a 10", 100, () -> {
            int r = EventoCliente.resta();
            t.check(r >= 0 && r <= 10, "salió el último de su celda: el reloj bajó a 10 segundos (" + r + ")");
        });
        foto(t, "26c_reja_abriendo", 650, () -> t.cmd("evento celdas abrir 1"));
        foto(t, "26d_reja_abierta", 1600, () -> {
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
        foto(t, "27_pasillo", 2500, () -> {
            t.cmd("evento celdas abrir");
            t.cmd("gamemode spectator @a");
            t.lookFrom(52.5, -59, 48.5, 7, -57.5, 48.5);
            muestrearLamparas(true);
        });
        foto(t, "28_cuerpo", 2500, () -> {
            t.cmd("evento eliminar Bot_02 Encadenados");
            t.lookFrom(15.5, -59.4, 39.5, 15.5, -60.6, 34.5);
        });
        foto(t, "29_torre_camaras", 2000, () -> t.lookFrom(12.5, -59, 48.5, 24, -55, 48.5));
        foto(t, "30_patio", 1500, () -> t.lookFrom(52, -52, 38, 30, -58, 48));
        t.add("lamparas", 100, () -> {
            muestrearLamparas(false);
            t.check(lamparasFallando > 0, "algunas lámparas del pasillo parpadean solas (" + lamparasFallando + " ticks con alguna apagada)");
        });
        // Axelis aparece disfrazado del Coleccionista en el pasillo (visto de frente)
        t.add("disfrazarme", 1500, () -> {
            t.cmd("evento quitar " + yo());
            t.cmd("evento disfraz si @a");
        });
        foto(t, "31_disfraz", 1800, () -> {
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
        foto(t, "c1_51_porton", 2600, () -> t.cmd("evento porton abrir"));
        t.add("porton arriba", 100, () -> t.check(Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(54, -60, 48)).isAir(),
                "el portón subió (ya no hay barrotes abajo)"));
        // todos pasan a la sala grande: cuando ya están todos, baja el portón y habla el Coleccionista (aquí sí hay pantalla)
        foto(t, "c1_52_sala_grande", 1500, () -> {
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
        foto(t, "c1_52b_equipos", 3500, () -> t.lookFrom(96.5, -59, 48.5, 111, -55, 48.5));
        t.add("porton cerrado", 100, () -> t.check(!Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(54, -60, 48)).isAir(),
                "el portón bajó detrás de todos"));
        t.add("piso", 100, () -> t.check(!Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(80, -60, 48)).is(
                net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("coleccionista", "tapete_rojo"))),
                "el tapete va a ras del piso (no un bloque arriba)"));
        t.add("saltar a formar", 600, () -> t.cmd("evento capitulo1 formar"));
        foto(t, "c1_53_ascensores", 4500, () -> t.lookFrom(86.5, -60, 40.5, 86.5, -59, 29));
        t.add("hay objetivo", 100, () -> t.check(ObjetivoCliente.hay(), "el indicador de «ir acá» apunta a un ascensor"));
        t.add("al ascensor", 200, () -> t.lookFrom(86.5, -60, 27.5, 86.5, -59.5, 31));
        foto(t, "c1_54_ascensor_listo", 1800, () -> {
        });
        t.add("bajando", 5500, () -> {
        });
        t.add("cae", 100, () -> t.check(ViajeCliente.cayendo(), "voy en el ascensor que cae"));
        foto(t, "c1_55_cayendo", 900, () -> {
        });
        t.add("llegar", 12000, () -> {
        });
        // el Coleccionista espera a que salgan todos del ascensor (los bots salen solos)
        t.add("esperando", 100, () -> t.check("ESPERA".equals(SentidosCliente.fase()), "espera a que salgamos del ascensor (" + SentidosCliente.fase() + ")"));
        t.add("salir del ascensor", 1500, () -> t.lookFrom(149.5, -60, 437.5, 149.5, -59.5, 441));
        t.add("habla", 100, () -> t.check("HABLANDO".equals(SentidosCliente.fase()), "ya que estamos todos en el taller, habla (" + SentidosCliente.fase() + ")"));
        t.add("saltar discurso", 1200, () -> t.cmd("evento sentidos saltar"));
        t.add("en la mesa", 100, () -> t.check("ELEGIR".equals(SentidosCliente.fase()), "estamos eligiendo objetos (" + SentidosCliente.fase() + ")"));
        foto(t, "c1_56_mesa", 900, () -> {
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
        foto(t, "c1_57_equipo_encadenado", 1500, () -> {
            t.cmd("gamemode spectator @a");
            t.lookFrom(153.5, -59, 440.5, 149.5, -59.5, 437.5);
        });
        t.add("primera", 300, () -> t.cmd("gamemode adventure @a"));
        t.add("mostrar", 600, () -> {
            t.cmd("gamemode spectator @a");
            t.cmd("evento sentidos mostrar");
        });
        foto(t, "c1_58_pasillo_munecas", 1200, () -> t.lookFrom(149.5, -59, 449.5, 164, -58.3, 449.5));
        foto(t, "c1_59_postes", 1200, () -> t.lookFrom(153.5, -58.5, 467, 161, -59.5, 476));
        foto(t, "c1_60_pasarela", 1200, () -> t.lookFrom(181.5, -57, 489.2, 181.5, -61.5, 496));
        foto(t, "c1_61_baldosas", 1200, () -> t.lookFrom(167, -55.5, 515, 166.5, -61, 523));
        foto(t, "c1_62_casilla", 1200, () -> t.lookFrom(179.5, -59, 546.2, 179.5, -58, 549));
        foto(t, "c1_62b_palancas", 1200, () -> t.lookFrom(171.5, -59, 547.5, 176, -59.2, 547.5));
        t.add("a la salida", 2500, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("evento sentidos salida " + yo());
        });
        foto(t, "c1_63_salida_sillas", 1200, () -> t.lookFrom(166, -59.5, 565, 166, -60.3, 569));
        t.add("sentarme", 2500, () -> t.cmd("execute as " + yo() + " at @s run ride @s mount @e[type=coleccionista:silla,sort=nearest,limit=1]"));
        t.add("sentado", 100, () -> t.check(Minecraft.getInstance().player.getVehicle() instanceof com.coleccionista.evento.Silla,
                "estoy sentado en la silla de la salida"));
        // cuando terminan todos, los que se salvaron pasan (sentados, sin que se note) a su cuarto del teatro y se abre la puerta
        t.waitFor("al teatro", 70000, () -> Minecraft.getInstance().player.getX() < -40);
        foto(t, "c1_64_palco_puerta", 3500, () -> {
        });
        t.add("en el teatro", 100, () -> {
            var p = Minecraft.getInstance().player;
            t.check(p.getVehicle() instanceof com.coleccionista.evento.Silla, "llegué al teatro sentado en mi silla (" + (int) p.getX() + ", "
                    + (int) p.getZ() + ")");
        });
        t.add("levantarme", 800, () -> t.cmd("ride " + yo() + " dismount"));
        foto(t, "c1_65_teatro_telon", 1500, () -> {
            t.cmd("gamemode spectator @a");
            t.lookFrom(-53, -59, 457.5, -53, -56.5, 470);
        });
        t.waitFor("telon abierto", 40000, () -> Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(-53, -56, 470)).isAir());
        foto(t, "c1_66_vitrinas", 2500, () -> t.lookFrom(-53, -57.5, 462, -53, -58.5, 477));
        // la cera sube en la vitrina del centro (la primera víctima); las vitrinas están en x -60, -52 y -44, z 474
        t.waitFor("cera", 40000, () -> net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(Minecraft.getInstance().level
                .getBlockState(new net.minecraft.core.BlockPos(-52, -60, 474)).getBlock()).getPath().equals("cera"));
        foto(t, "c1_67_cera", 3500, () -> t.lookFrom(-56, -58.3, 467.5, -56, -59, 474));
        t.waitFor("caen", 30000, () -> {
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.estatua() && c.cae() > 0) {
                    return true;
                }
            }
            return false;
        });
        foto(t, "c1_68_estatuas_caen", 300, () -> t.lookFrom(-56, -58, 465.5, -56, -59.6, 471.5));
        foto(t, "c1_68b_estatuas_tiradas", 2200, () -> {
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
        foto(t, "c1_69_salida_teatro", 1500, () -> t.lookFrom(-31, -59, 464.5, -25, -59, 464.5));
        t.add("a la antesala", 1500, () -> {
            t.cmd("gamemode adventure @a");
            t.lookFrom(-19, -60, 464.5, -11, -58, 464.5);
        });
        t.waitFor("antesala habla", 15000, () -> {
            var st = com.axeliscore.data.client.ClientData.get(com.coleccionista.evento.Pantallas.KEY);
            return st != null && st.getBoolean("hablando");
        });
        foto(t, "c1_70_antesala", 2500, () -> {
        });
        // lo que ve el último equipo (simulado en mi pantalla): el gas y el despertar en la vitrina con su mensaje
        foto(t, "c1_71_gas", 500, () -> {
            CompoundTag g = new CompoundTag();
            g.putString("fase", "gas");
            g.putLong("desde", Minecraft.getInstance().level.getGameTime() - 110);
            g.putInt("dura", 150);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sentidos.KEY_GAS, g);
        });
        foto(t, "c1_72_despierta", 500, () -> {
            CompoundTag g = new CompoundTag();
            g.putString("fase", "despierta");
            g.putLong("desde", Minecraft.getInstance().level.getGameTime() - 125);
            g.putInt("dura", 160);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sentidos.KEY_GAS, g);
        });
        t.add("sin gas", 300, () -> com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sentidos.KEY_GAS, new CompoundTag()));
        foto(t, "c1_73_linea_tiempo", 1500, () -> {
            LineaScreen s = new LineaScreen();
            com.axeliscore.plat.client.Screens.open(s);
            int[] c = s.nodo(4);
            com.axeliscore.ui.UiScreen.fakeMouse = c;
        });
        foto(t, "c1_74_linea_cap2", 1200, () -> {
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
        capitulo4(t);
        capitulo5(t);
        capitulo6(t);
        capitulo7(t);
        menus(t);
    }

    /**
     * Los menús del evento (en la prueba no reemplazan a los de Minecraft, así que se abren a mano): la pantalla de inicio (recién abierta
     * con la tele que se prende, y luego con el ratón encima de "Salir", que lo hace reír), Configurar y la pausa.
     */
    private static void menus(ClientTest.Script t) {
        t.add("menus: inicio", 300, () -> {
            com.coleccionista.client.inicio.Inicio.organizador = false;
            Minecraft.getInstance().setScreen(new com.coleccionista.client.inicio.InicioScreen());
        });
        foto(t, "m_01_inicio_prende", 700, () -> {
        });
        foto(t, "m_02_inicio", 3200, () -> {
        });
        foto(t, "m_02b_inicio_salir", 1500, () -> {
            // el ratón sobre "Salir" (se ríe) — se mueve el cursor de la ventana
            Minecraft mc = Minecraft.getInstance();
            double k = mc.getWindow().getGuiScale();
            int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
            double x = Math.max(14, (int) (w * 0.07F)) + 30, y = (int) (h * 0.47F) + 2 * 20 + 16 + 8;
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x * k, y * k);
        });
        t.add("menus: hay pantalla", 100, () -> t.check(Minecraft.getInstance().screen instanceof com.coleccionista.client.inicio.InicioScreen,
                "se abrió la pantalla de inicio"));
        foto(t, "m_03_configurar", 1200, () -> Minecraft.getInstance().setScreen(
                new com.coleccionista.client.inicio.ConfigurarScreen(Minecraft.getInstance().screen)));
        foto(t, "m_04_pausa", 1200, () -> Minecraft.getInstance().setScreen(new com.coleccionista.client.inicio.PausaScreen()));
        t.add("menus: cerrar", 300, () -> Minecraft.getInstance().setScreen(null));
    }

    /**
     * Capítulo 5 · La hora del té (yo miro como fantasma, para poder mover la cámara): el pasillo, el salón con los sillones (los bots se
     * sientan en pareja), las jaulas colgadas, el primer duelo (las manos se sacuden y muestran la jugada, el que pierde bebe, el veneno lo
     * pone verde y se va de espaldas con la silla), el cambio (el muerto en una jaula) y las pantallas de elegir jugada y cáliz (simuladas).
     * El salón está en (950, -61, 430): el pasillo x 952..965 / z 448..455, el salón x 967..1011 / z 430..472, la mesa x 990..998 /
     * z 450..452 (centro 994.5, 451.5; el mantel arriba en y -59.03), las sillas en x 989 y 999, las jaulas en un anillo de radio 10.
     */
    private static void capitulo5(ClientTest.Script t) {
        t.add("cap5: preparar", 800, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add 944 424 1016 480");
            t.cmd("evento pocos true");
            t.cmd("evento te encadenar false");
            t.cmd("evento piano encadenar false");
            t.cmd("evento te rapido true");
            t.cmd("evento te veneno 1");
            t.cmd("evento eliminar " + yo());
        });
        t.add("cap5: llegar", 600, () -> t.cmd("evento te llegar"));
        t.waitFor("cap5: llegando", 15000, () -> "LLEGANDO".equals(TeCliente.fase()));
        foto(t, "c5_01_pasillo", 1500, () -> t.lookFrom(958.5, -60, 451.5, 967, -59, 451.5));
        foto(t, "c5_02_salon", 3500, () -> {
            t.cmd("evento te saltar");
            t.lookFrom(969.5, -59, 451.5, 994.5, -58, 451.5);
        });
        foto(t, "c5_03_sillones", 2500, () -> t.lookFrom(986.5, -58.5, 451.5, 975, -59.5, 444));
        t.add("cap5: formar parejas", 400, () -> t.cmd("evento te saltar"));
        t.waitFor("cap5: reglas", 15000, () -> "REGLAS".equals(TeCliente.fase()));
        foto(t, "c5_04_jaulas", 2000, () -> t.lookFrom(1008.5, -53, 467.5, 994.5, -56.5, 451.5));
        foto(t, "c5_05_mesa", 1500, () -> t.lookFrom(994.5, -57.5, 444.5, 994.5, -58.8, 451.5));
        foto(t, "c5_05b_desde_jaula", 1200, () -> t.lookFrom(1004.5, -55, 454.5, 994.5, -59, 451.5));
        // mientras habla la tele: las pantallas (simuladas en la mía): elegir jugada y elegir cáliz con la mano
        foto(t, "c5_13_jugada", 900, () -> {
            CompoundTag m = new CompoundTag();
            m.putInt("lado", 0);
            m.putBoolean("jugar", true);
            m.putInt("segundos", 12);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Te.KEY_MIO, m);
        });
        t.add("jugada", 300, () -> {
            t.check(Minecraft.getInstance().screen instanceof JugadaScreen, "salió la pantalla de piedra, papel o tijera");
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Te.KEY_MIO, new CompoundTag());
            com.axeliscore.plat.client.Screens.close();
        });
        t.add("caliz", 1500, () -> {
            CompoundTag m = new CompoundTag();
            m.putInt("lado", 0);
            m.putBoolean("caliz", true);
            m.putInt("segundos", 20);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Te.KEY_MIO, m);
        });
        foto(t, "c5_14_caliz", 1200, () -> {
            if (CalizScreen.actual() != null) {
                CalizScreen.actual().senalarPrueba(2);
            }
        });
        t.add("caliz cerrar", 300, () -> {
            t.check(Minecraft.getInstance().screen instanceof CalizScreen, "salió la pantalla para elegir el cáliz con la mano");
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Te.KEY_MIO, new CompoundTag());
            com.axeliscore.plat.client.Screens.close();
        });
        t.add("cap5: duelo", 300, () -> t.cmd("evento te saltar"));
        t.waitFor("cap5: manos", 15000, () -> "MANOS".equals(TeCliente.fase()));
        foto(t, "c5_06_manos", 700, () -> t.lookFrom(994.5, -59.8, 447.0, 994.5, -58.3, 451.5));
        foto(t, "c5_07_jugadas", 1400, () -> {
        });
        t.waitFor("cap5: bebe", 30000, () -> "BEBE".equals(TeCliente.fase()));
        t.add("cap5: mirar al que bebe", 50, () -> {
            int l = com.axeliscore.plat.Nbt.i(TeCliente.estado(), "perdedor");
            double[] s = TeCliente.silla(l);
            if (s != null) {
                // desde un lado de la mesa (al norte), parado en el piso, mirando al que bebe
                double r = Math.toRadians(s[3]);
                double fx = -Math.sin(r), fz = Math.cos(r);
                t.lookFrom(s[0] + fx * 1.2 + fz * 2.6, s[1], s[2] + fz * 1.2 - fx * 2.6, s[0], s[1] + 1.25, s[2]);
            }
        });
        foto(t, "c5_08_bebe", 1400, () -> {
        });
        foto(t, "c5_09_veneno", 2600, () -> {
        });
        t.waitFor("cap5: muere", 15000, () -> "MUERE".equals(TeCliente.fase()));
        foto(t, "c5_10_cae", 1200, () -> {
        });
        t.waitFor("cap5: cambio", 15000, () -> "CAMBIO".equals(TeCliente.fase()));
        t.waitFor("cap5: siguiente duelo", 15000, () -> !"CAMBIO".equals(TeCliente.fase()));
        t.add("cap5: el muerto en su jaula", 400, () -> {
            com.coleccionista.evento.Cadaver muerto = null;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.enJaula()) {
                    muerto = c;
                }
            }
            t.check(muerto != null, "el envenenado quedó en una jaula");
            if (muerto != null) {
                double dx = 994.5 - muerto.getX(), dz = 451.5 - muerto.getZ();
                double d = Math.hypot(dx, dz);
                t.lookFrom(muerto.getX() + dx / d * 3.2, muerto.getY() - 0.6, muerto.getZ() + dz / d * 3.2, muerto.getX(), muerto.getY() + 0.5,
                        muerto.getZ());
            }
        });
        foto(t, "c5_11_jaula_muerto", 1500, () -> {
        });
        foto(t, "c5_12_cambio", 1200, () -> t.lookFrom(994.5, -57.5, 444.5, 994.5, -58.8, 451.5));
        t.add("cap5: terminar", 1500, () -> t.cmd("evento te terminar"));
        t.waitFor("cap5: fin", 20000, () -> "FIN".equals(TeCliente.fase()));
        foto(t, "c5_15_fin", 9000, () -> t.lookFrom(1008.5, -53, 467.5, 994.5, -56.5, 451.5));
        t.add("cap5: limpiar", 1500, () -> {
            t.cmd("evento te limpiar");
            t.cmd("evento te rapido false");
            t.cmd("evento te veneno 0");
            t.cmd("evento te encadenar true");
            t.cmd("evento pocos false");
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("evento revivir " + yo());
            t.cmd("gamemode adventure @a");
        });
    }

    /** Dónde me paro en mi piano (el centro del tapete): x, y, z; null si no toco. */
    private static double[] miTapete() {
        CompoundTag m = PianoCliente.mio();
        int[] p = m == null ? new int[0] : m.getIntArray("pos");
        return p.length < 3 ? null : new double[] {p[0] + 1.0, p[1] + 1.0, p[2] + 1.0};
    }

    /**
     * Capítulo 6 · El piano gigante (yo toco, con tres bots): el pasillo, el salón de baile (el escenario con la bailarina, los cuatro
     * pianos de piso con sus cortinas), la bailarina, mi piano mientras ella toca (la tecla de cada nota brilla con su columna de luz), yo
     * tocando (con /evento piano bien), el desempate (dos bots se equivocan a propósito y yo me salvo: lo veo desde arriba), el piso que se
     * abre y el foso. El salón está en (1030, -61, 430): el pasillo x 1032..1045 / z 448..455, el salón x 1047..1091 / z 430..472, el
     * escenario x 1057..1081 / z 431..439 (la bailarina en 1069.5, 435.5), los pianos (tapete) en x 1056, 1065, 1074, 1083 / z 452 con las
     * teclas en y -58.
     */
    private static void capitulo6(ClientTest.Script t) {
        t.add("cap6: preparar", 800, () -> {
            t.cmd("forceload add 1024 424 1096 480");
            t.cmd("evento pocos true");
            t.cmd("evento piano encadenar false");
            t.cmd("evento veintiuno encadenar false");
            t.cmd("evento piano rapido true");
            t.cmd("evento piano caen 1");
            for (String b : new String[] {"Bot_01", "Bot_05", "Bot_06", "Bot_07", "Bot_08"}) {
                t.cmd("evento eliminar " + b);
            }
            t.cmd("evento piano fallar Bot_02");
            t.cmd("evento piano fallar Bot_03");
            t.cmd("gamemode spectator @a");
        });
        t.add("cap6: llegar", 600, () -> t.cmd("evento piano llegar"));
        t.waitFor("cap6: llegando", 15000, () -> "LLEGANDO".equals(PianoCliente.fase()));
        foto(t, "c6_01_pasillo", 1500, () -> t.lookFrom(1038.5, -60, 451.5, 1047, -59, 451.5));
        foto(t, "c6_02_salon", 3500, () -> {
            t.cmd("evento piano saltar");
            t.lookFrom(1050.5, -53, 471.5, 1068, -58, 445);
        });
        foto(t, "c6_03_bailarina", 2500, () -> t.lookFrom(1069.5, -59, 442.5, 1069.5, -56, 435.5));
        t.add("cap6: a los pianos", 400, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("evento piano saltar");
        });
        // cada quien sube a su pista por su escalera (con su número); los bots suben solos
        t.waitFor("cap6: subiendo", 8000, () -> "LUGARES".equals(PianoCliente.fase()));
        foto(t, "c6_03b_pistas", 2500, () -> t.lookFrom(1069.5, -57.5, 469.5, 1069.5, -58.5, 455.5));
        foto(t, "c6_03c_escalera", 1200, () -> t.lookFrom(1059.5, -59.5, 463.5, 1057, -58.5, 456));
        t.add("cap6: me falta subir", 100, () -> {
            CompoundTag m = PianoCliente.mio();
            t.check(m != null && !m.getBoolean("arriba"), "todavía no subo a mi pista (la caja no me sube)");
        });
        t.add("cap6: que suba la caja", 300, () -> t.cmd("evento piano saltar"));
        t.waitFor("cap6: muestra", 20000, () -> "MUESTRA".equals(PianoCliente.fase()));
        t.add("cap6: mi piano", 1700, () -> {
            double[] c = miTapete();
            t.check(c != null, "me tocó un piano");
            t.check(PianoCliente.estado().getIntArray("melodia").length == 3, "la melodía de la primera ronda tiene 3 notas");
            if (c != null) {
                t.lookFrom(c[0], c[1], c[2], c[0], c[1] - 1, c[2] - 2.3);
            }
        });
        foto(t, "c6_04_muestra", 200, () -> {
        });
        t.waitFor("cap6: toca", 15000, () -> "TOCA".equals(PianoCliente.fase()));
        foto(t, "c6_05_toca", 500, () -> {
        });
        t.add("cap6: nota 1", 250, () -> t.cmd("evento piano bien " + yo()));
        foto(t, "c6_06_bien", 150, () -> {
        });
        t.add("cap6: nota 2", 400, () -> t.cmd("evento piano bien " + yo()));
        t.add("cap6: nota 3", 600, () -> t.cmd("evento piano bien " + yo()));
        t.add("cap6: terminé", 200, () -> {
            CompoundTag m = PianoCliente.mio();
            t.check(m != null && m.getBoolean("listo"), "toqué las 3 notas bien");
        });
        t.waitFor("cap6: revisa", 15000, () -> "REVISA".equals(PianoCliente.fase()));
        foto(t, "c6_07_desempate", 900, () -> {
            t.check("desempate".equals(PianoCliente.estado().getString("resultado")), "dos se equivocaron: desempate");
        });
        t.add("cap6: desde arriba", 300, () -> t.cmd("gamemode spectator @a"));
        t.waitFor("cap6: segunda ronda", 15000, () -> "MUESTRA".equals(PianoCliente.fase()));
        foto(t, "c6_08_desde_arriba", 2200, () -> {
            t.cmd("evento piano fallar Bot_02");
            t.lookFrom(1069.5, -49, 466.5, 1069.5, -58, 452.5);
        });
        t.waitFor("cap6: cae", 20000, () -> "CAE".equals(PianoCliente.fase()));
        foto(t, "c6_09_se_abre", 500, () -> {
        });
        t.waitFor("cap6: el cuerpo", 8000, () -> {
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.getX() > 1030 && c.getX() < 1094) {
                    return true;
                }
            }
            return false;
        });
        t.add("cap6: el foso", 1200, () -> {
            com.coleccionista.evento.Cadaver muerto = null;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.getX() > 1030 && c.getX() < 1094) {
                    muerto = c;
                }
            }
            t.check(muerto != null, "el que se equivocó quedó en el foso");
            if (muerto != null) {
                t.lookFrom(muerto.getX() + 0.5, -57.5, muerto.getZ() + 3.2, muerto.getX(), -63, muerto.getZ());
            }
        });
        foto(t, "c6_10_foso", 900, () -> {
        });
        t.waitFor("cap6: fin", 20000, () -> "FIN".equals(PianoCliente.fase()));
        foto(t, "c6_11_fin", 6000, () -> t.lookFrom(1050.5, -53, 471.5, 1068, -58, 445));
        t.add("cap6: limpiar", 1500, () -> {
            t.cmd("evento piano limpiar");
            t.cmd("evento piano rapido false");
            t.cmd("evento piano caen 0");
            t.cmd("evento piano encadenar true");
            t.cmd("evento pocos false");
            for (int i = 1; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("evento revivir " + yo());
            t.cmd("gamemode adventure @a");
        });
    }

    /** De qué lado de la mesa estoy en la final (0 oeste, 1 este; -1 si no estoy). */
    private static int miLadoV21() {
        var mc = Minecraft.getInstance();
        CompoundTag e = VeintiunoCliente.estado();
        for (int l = 0; l < 2; l++) {
            if (e.hasUUID("id" + l) && mc.player != null && e.getUUID("id" + l).equals(mc.player.getUUID())) {
                return l;
            }
        }
        return -1;
    }

    /** El mazo para que la ronda salga como queremos: yo (tapada, visible), el bot (tapada, visible) y lo que sigue. */
    private static String mazoV21(int yoH, int yoV, int botH, int botV, String resto) {
        int l = miLadoV21();
        String m = l == 1 ? botH + " " + yoH + " " + botV + " " + yoV : yoH + " " + botH + " " + yoV + " " + botV;
        return "evento veintiuno mazo " + m + " " + resto;
    }

    /** Mientras no se volteen las cartas: si es mi turno, me planto. */
    private static boolean plantarmeHasta(String fase) {
        if (Minecraft.getInstance().screen instanceof VeintiunoScreen v) {
            v.elegirPrueba(1);
        }
        return fase.equals(VeintiunoCliente.fase());
    }

    /**
     * Capítulo 7 · Veintiuno (la final): yo contra un bot. El pasillo, el cuarto (la mesa con las dos máquinas y la lámpara), sentado en
     * primera persona (mi brazo metido en la máquina, mis cartas y las del otro), el reparto, mi turno (la pantalla), voltear las cartas,
     * la máquina que me aprieta, la vista en tercera persona, y la última ronda: ¡pum! al bot. Con dos rondas perdidas se corta (para que
     * sea corto) y el mazo puesto para que pierda yo la primera y el bot las otras dos.
     * El cuarto está en (1110, -61, 430): el pasillo x 1112..1125 / z 448..455, el cuarto x 1127..1157 / z 436..466, la mesa x 1141..1144 /
     * z 450..452 (centro 1143.0, 451.5).
     */
    private static void capitulo7(ClientTest.Script t) {
        t.add("cap7: preparar", 800, () -> {
            t.cmd("forceload add 1104 424 1168 480");
            t.cmd("evento pocos true");
            t.cmd("evento veintiuno encadenar false");
            t.cmd("evento veintiuno rapido true");
            t.cmd("evento veintiuno golpes 2");
            for (int i = 1; i <= 8; i++) {
                if (i != 2) {
                    t.cmd("evento eliminar Bot_0" + i);
                }
            }
            t.cmd("evento revivir Bot_02");
            t.cmd("evento revivir " + yo());
            t.cmd("gamemode spectator @a");
        });
        t.add("cap7: llegar", 600, () -> t.cmd("evento veintiuno llegar"));
        t.waitFor("cap7: llegando", 15000, () -> "LLEGANDO".equals(VeintiunoCliente.fase()));
        foto(t, "c7_01_pasillo", 1500, () -> t.lookFrom(1118.5, -60, 451.5, 1127, -59, 451.5));
        foto(t, "c7_02_cuarto", 3500, () -> {
            t.cmd("evento veintiuno saltar");
            t.lookFrom(1129.5, -57, 462.5, 1143, -59.5, 451.5);
        });
        foto(t, "c7_03_mesa", 1500, () -> t.lookFrom(1143.0, -58.2, 447.6, 1143.0, -59.2, 451.5));
        t.add("cap7: sentarse", 400, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("evento veintiuno saltar");
        });
        t.waitFor("cap7: reglas", 20000, () -> "REGLAS".equals(VeintiunoCliente.fase()));
        t.add("cap7: el mazo de la ronda 1", 800, () -> {
            t.check(miLadoV21() >= 0, "estoy sentado en la final");
            // la primera la pierdo yo (5 contra 19)
            t.cmd(mazoV21(2, 3, 10, 9, "1 4 5"));
        });
        foto(t, "c7_04_sentado", 1500, () -> {
        });
        t.add("cap7: empezar", 300, () -> t.cmd("evento veintiuno saltar"));
        t.waitFor("cap7: reparte", 15000, () -> "REPARTE".equals(VeintiunoCliente.fase()));
        foto(t, "c7_05_reparte", 1200, () -> {
        });
        t.waitFor("cap7: mi turno", 20000, () -> Minecraft.getInstance().screen instanceof VeintiunoScreen);
        foto(t, "c7_06_mi_turno", 800, () -> {
        });
        t.waitFor("cap7: revela", 20000, () -> plantarmeHasta("REVELA"));
        foto(t, "c7_07_revela", 2200, () -> {
            CompoundTag e = VeintiunoCliente.estado();
            t.check(e.getInt("perdedor") == miLadoV21(), "la primera ronda la pierdo yo");
        });
        t.add("cap7: el mazo de las otras", 200, () -> t.cmd(mazoV21(10, 11, 1, 2, "3 4 9")));
        t.waitFor("cap7: castigo", 15000, () -> "CASTIGO".equals(VeintiunoCliente.fase()));
        foto(t, "c7_08_aprieta", 900, () -> {
        });
        t.waitFor("cap7: ronda 2", 20000, () -> "TURNO".equals(VeintiunoCliente.fase()) && VeintiunoCliente.estado().getInt("ronda") == 2);
        foto(t, "c7_09_tercera", 900, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        t.add("cap7: primera persona", 200, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        t.waitFor("cap7: revela 2", 30000, () -> plantarmeHasta("REVELA"));
        t.waitFor("cap7: ronda 3", 30000, () -> "TURNO".equals(VeintiunoCliente.fase()) && VeintiunoCliente.estado().getInt("ronda") == 3);
        t.waitFor("cap7: castigo 3", 30000, () -> plantarmeHasta("CASTIGO"));
        foto(t, "c7_10_pum", 900, () -> {
            t.check(VeintiunoCliente.estado().getBoolean("cortado"), "a la segunda ronda perdida, la máquina corta");
        });
        foto(t, "c7_11_sangre", 1500, () -> {
        });
        t.waitFor("cap7: fin", 20000, () -> "FIN".equals(VeintiunoCliente.fase()));
        foto(t, "c7_12_fin", 4000, () -> {
        });
        t.add("cap7: el cuerpo", 600, () -> {
            boolean manco = false;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.manco()) {
                    manco = true;
                }
            }
            t.check(manco, "el que perdió se quedó en su silla, sin el brazo");
            t.cmd("gamemode spectator @a");
            t.lookFrom(1143.0, -58.0, 447.2, 1143.0, -59.3, 451.5);
        });
        foto(t, "c7_13_la_mesa_al_final", 1500, () -> {
        });
        desenlace(t);
        t.add("cap7: limpiar", 1500, () -> {
            t.cmd("evento veintiuno limpiar");
            t.cmd("evento veintiuno rapido false");
            t.cmd("evento veintiuno golpes 3");
            t.cmd("evento veintiuno mazo azar");
            t.cmd("evento veintiuno encadenar true");
            t.cmd("evento pocos false");
            for (int i = 1; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("evento revivir " + yo());
            t.cmd("gamemode adventure @a");
        });
    }

    /**
     * El desenlace (yo gané la final): se abre la puerta del este y me voy por el pasillo (la casa, el sótano, el yeso cada vez más claro)
     * hasta la luz; los créditos (el título, la colección, el ganador, Axelis); la vitrina junto al trono ya con mi estatua y la pantalla
     * hablando; el FIN. El pasillo está en (1160, -61, 430): se camina por z 452.0 (el centro), la luz empieza en x 1238.
     */
    private static void desenlace(ClientTest.Script t) {
        t.add("des: preparar", 400, () -> t.cmd("forceload add 1156 440 1262 464"));
        t.waitFor("des: camina", 90000, () -> "CAMINA".equals(DesenlaceCliente.fase()));
        foto(t, "d_01_puerta", 2500, () -> t.lookFrom(1152.5, -60, 452.0, 1162, -59, 452.0));
        foto(t, "d_02_casa", 1500, () -> t.lookFrom(1165.5, -60, 452.0, 1180, -58.8, 452.0));
        foto(t, "d_03_sotano", 1500, () -> t.lookFrom(1190.5, -60, 452.0, 1206, -58.8, 452.0));
        foto(t, "d_04_yeso", 1500, () -> t.lookFrom(1226.5, -60, 452.0, 1245, -58.0, 452.0));
        t.add("des: a la luz", 300, () -> t.lookFrom(1243.5, -60, 452.0, 1255, -58, 452.0));
        t.waitFor("des: luz", 15000, () -> "LUZ".equals(DesenlaceCliente.fase()));
        foto(t, "d_05_luz", 1600, () -> {
        });
        t.waitFor("des: créditos", 15000, () -> Minecraft.getInstance().screen instanceof Creditos);
        foto(t, "d_06_creditos_titulo", 8500, () -> {
        });
        foto(t, "d_07_creditos_coleccion", 6500, () -> {
        });
        foto(t, "d_08_creditos_ganador", 7500, () -> {
        });
        foto(t, "d_09_creditos_axelis", 9000, () -> {
        });
        t.waitFor("des: la vitrina", 60000, () -> "POST".equals(DesenlaceCliente.fase()));
        foto(t, "d_10_vitrina", 6000, () -> {
            boolean hay = false;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.estatua() && Minecraft.getInstance().player.getUUID().equals(c.dueno())) {
                    hay = true;
                }
            }
            t.check(hay, "en la vitrina está mi estatua");
            t.check(DesenlaceCliente.camara(), "la cámara está frente a la vitrina");
        });
        t.waitFor("des: fin", 60000, () -> "FIN".equals(DesenlaceCliente.fase()));
        foto(t, "d_11_fin", 2600, () -> {
        });
        t.waitFor("des: se acabó", 20000, () -> "NADA".equals(DesenlaceCliente.fase()) && !DesenlaceCliente.camara());
    }

    /**
     * Capítulo 4 · La caja sorpresa: el pasillo, el teatrito (la herradura de gradas y el escenario con la caja), el apagón y todos
     * sentados, el turno de un bot (su silla gira y lo levanta, va a la manivela, le da 2 vueltas) y Resortes: salta, lo agarra, lo
     * muerde (y a los demás les da el susto), mastica y escupe los zapatos al centro; la pantalla para elegir las vueltas (obligado a 3)
     * y el final.
     * El teatrito está en (870, -61, 430): el pasillo x 872..885 / z 448..455, el salón x 887..931 / z 430..472, el centro de la herradura
     * (905, 451), el escenario x 916..928 (se pisa en y -58) y la caja en (922, 451).
     */
    private static void capitulo4(ClientTest.Script t) {
        t.add("cap4: preparar", 600, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add 864 424 936 480");
            t.cmd("evento pocos true");
            t.cmd("evento sorpresa comer 1");
            t.cmd("evento sorpresa primero Bot_03");
            t.cmd("evento sorpresa secreto 2");
            t.cmd("evento sorpresa llegar");
        });
        t.waitFor("cap4: llegando", 15000, () -> "LLEGANDO".equals(SorpresaCliente.fase()));
        foto(t, "c4_01_pasillo", 1500, () -> t.lookFrom(878.5, -60, 451.5, 887, -59, 451.5));
        foto(t, "c4_02_teatro", 3000, () -> {
            t.cmd("evento sorpresa saltar");
            t.lookFrom(889.5, -59, 451.5, 922, -56.5, 451.5);
        });
        foto(t, "c4_02b_caja", 2500, () -> t.lookFrom(912.5, -57.5, 451.5, 922, -56.5, 451.5));
        foto(t, "c4_02c_gradas", 1500, () -> t.lookFrom(914.5, -57.5, 444.5, 900, -59, 451.5));
        t.add("cap4: a las gradas", 200, () -> t.cmd("evento sorpresa saltar"));
        t.waitFor("cap4: turno", 15000, () -> "IR".equals(SorpresaCliente.fase()));
        foto(t, "c4_03_sentados", 1500, () -> mirarA(922.5, -56.5, 451.5));
        t.add("sentado en la grada", 100, () -> t.check(Minecraft.getInstance().player.getVehicle() instanceof com.coleccionista.evento.Silla,
                "voy sentado en mi silla de la grada"));
        t.waitFor("cap4: el bot elige", 25000, () -> "ELIGE".equals(SorpresaCliente.fase()));
        foto(t, "c4_04_manivela", 400, () -> {
            t.cmd("evento sorpresa vueltas 2");
            mirarA(921.5, -56.5, 451.5);
            Minecraft.getInstance().options.fov().set(40);
        });
        t.waitFor("cap4: salta", 8000, () -> "SALTO".equals(SorpresaCliente.fase()));
        // con zoom (menos campo de visión) para ver bien que Resortes sale hacia arriba
        foto(t, "c4_05_salta", 900, () -> {
            mirarA(922.5, -55, 451.5);
            Minecraft.getInstance().options.fov().set(32);
        });
        t.waitFor("cap4: agarra", 5000, () -> "AGARRA".equals(SorpresaCliente.fase()));
        foto(t, "c4_06_agarra", 900, () -> mirarA(922.5, -55, 451.5));
        t.add("sin zoom", 50, () -> Minecraft.getInstance().options.fov().set(70));
        t.waitFor("cap4: muerde", 5000, () -> "MUERDE".equals(SorpresaCliente.fase()));
        foto(t, "c4_07_mordida", 150, () -> {
        });
        foto(t, "c4_08_mastica", 2200, () -> {
        });
        t.waitFor("cap4: escupe", 8000, () -> "ESCUPE".equals(SorpresaCliente.fase()));
        foto(t, "c4_09_escupe", 700, () -> {
        });
        foto(t, "c4_10_zapatos", 2600, () -> mirarA(905.5, -60.5, 451.5));
        t.add("zapatos", 100, () -> {
            boolean hay = false;
            var lvl = Minecraft.getInstance().level;
            for (int x = 901; x <= 909 && !hay; x++) {
                for (int z = 447; z <= 455 && !hay; z++) {
                    var st = lvl.getBlockState(new net.minecraft.core.BlockPos(x, -60, z));
                    hay = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath().equals("zapatos");
                }
            }
            t.check(hay, "los zapatos cayeron en el piso del centro, en medio de todos");
        });
        // la pantalla de las vueltas (simulada en mi pantalla), obligado a dar 3
        foto(t, "c4_11_vueltas", 900, () -> {
            CompoundTag m = new CompoundTag();
            m.putBoolean("turno", true);
            m.putBoolean("elige", true);
            m.putBoolean("obligado", true);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sorpresa.KEY_MIO, m);
        });
        t.add("vueltas", 300, () -> {
            t.check(Minecraft.getInstance().screen instanceof VueltasScreen, "salió la pantalla para elegir las vueltas");
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Sorpresa.KEY_MIO, new CompoundTag());
            com.axeliscore.plat.client.Screens.close();
        });
        t.waitFor("cap4: fin", 40000, () -> "FIN".equals(SorpresaCliente.fase()));
        foto(t, "c4_12_fin", 3000, () -> mirarA(922.5, -56, 451.5));
        t.add("cap4: limpiar", 1500, () -> {
            t.cmd("evento sorpresa limpiar");
            t.cmd("evento sorpresa comer 0");
            t.cmd("evento pocos false");
            t.cmd("gamemode adventure @a");
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
            t.cmd("evento revivir " + yo());
        });
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
     * Capítulo 3 · La caja de música: el pasillo, el salón con las góndolas, buscar asiento (me siento yo solo con clic derecho en el
     * sillón; los bots caminan y se sientan), el apagón y la carta del papel, la máscara, dos turnos en la góndola (atrás y adelante), el
     * círculo con la compuerta que se abre y la escopeta que sube, un balazo a un bot (la tensión, la carga, el disparo y la silla que se va
     * de espaldas), mi turno (la pantalla de los símbolos y el clic en vacío), la pantalla del organizador y el final.
     * La caja está en (790, -61, 430): el pasillo en x 792..805 / z 448..455, el salón en x 807..849 / z 430..472, el escenario al centro
     * (828, 451) con la compuerta en x 827..829 / z 450..452; las góndolas 1..4 al norte (entradas en z 434, centros x 822..834).
     */
    /** Fotos de un pasillo hacia adelante: el tramo largo desde el primer codo y el último, hacia la puerta de atrás de la sala siguiente. */
    private static void fotosPaso(ClientTest.Script t, String nombre, int dx, int ancho, int zin) {
        double cx = dx + ancho / 2 + 0.5;
        foto(t, nombre + "_1", 1600, () -> t.lookFrom(cx, -60, 430 + zin + 0.5, cx, -59.4, 451.5));
        foto(t, nombre + "_2", 1200, () -> t.lookFrom(cx, -60, 451.9, dx + ancho + 1.5, -59.4, 451.9));
    }

    private static boolean puertaAbierta(int x, int y, int z) {
        var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
        return st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
    }

    private static void capitulo3(ClientTest.Script t) {
        t.add("cap3: preparar", 600, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("forceload add 784 424 864 480");
            t.cmd("evento caja tiempos 10 20");
            t.cmd("evento caja sentarse 60");
            t.cmd("evento caja esperar true");
            t.cmd("evento pocos true");
        });
        // los pasillos hacia adelante entre capítulos (zonas paso_*): fotos de cada uno antes de recorrer el de la caja
        fotosPaso(t, "c3_00c_sorpresa", 852, 18, 8);
        fotosPaso(t, "c3_00d_te", 934, 16, 39);
        fotosPaso(t, "c3_00e_piano", 1014, 16, 4);
        fotosPaso(t, "c3_00f_veintiuno", 1094, 16, 37);
        // de la mansión a la caja caminando: se abre la puerta de salida de la mansión (773, 438) y la de atrás del pasillo de la caja
        // (791, 451); al entrar todos, se cierran las dos y, sin apagón, se abren las del salón
        t.add("cap3: llamar", 600, () -> t.cmd("evento caja llamar"));
        t.waitFor("cap3: camino", 8000, () -> "PASILLO".equals(CajaCliente.fase()));
        foto(t, "c3_00a_salida_mansion", 1800, () -> t.lookFrom(767.5, -60, 439, 774, -59.6, 439));
        t.add("se abre la salida", 100, () -> {
            t.check(puertaAbierta(773, -60, 438), "se abrió la puerta de salida de la mansión");
            t.check(puertaAbierta(791, -60, 451), "se abrió la puerta de atrás del pasillo de la caja");
        });
        fotosPaso(t, "c3_00b_caja", 775, 15, 8);
        t.add("cap3: entrar al pasillo", 400, () -> t.cmd("tp @a 794.5 -60 451.5 90 0"));
        t.waitFor("cap3: llegando", 15000, () -> "LLEGANDO".equals(CajaCliente.fase()));
        t.add("se cierra detrás", 2500, () -> {
            t.check(!puertaAbierta(773, -60, 438), "se cerró la salida de la mansión");
            t.check(!puertaAbierta(791, -60, 451), "se cerró la puerta de atrás del pasillo");
        });
        foto(t, "c3_01_pasillo", 1500, () -> t.lookFrom(799.5, -60, 451.5, 807, -59, 451.5));
        t.add("puertas de la caja", 100, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(806, -60, 455));
            t.check(st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN),
                    "se abrieron las puertas del pasillo al salón");
        });
        t.add("al salon", 300, () -> t.lookFrom(811.5, -60, 451.5, 817, -55, 451.5));
        t.waitFor("cap3: habla", 15000, () -> "HABLANDO".equals(CajaCliente.fase()));
        foto(t, "c3_02_salon", 5000, () -> t.lookFrom(809.5, -58, 434.5, 830, -57, 451));
        foto(t, "c3_02b_pantalla", 2500, () -> t.lookFrom(811.5, -60, 451.5, 849, -52, 451.5));
        foto(t, "c3_02c_gondolas", 2000, () -> t.lookFrom(828.5, -60, 444.5, 828.5, -58.5, 433));
        // a sentarse: yo, con clic derecho en el sillón de la góndola 3 (entrada en 830, 434; el sillón en 830, 433)
        foto(t, "c3_03_sentarse", 1500, () -> {
            t.cmd("evento caja saltar");
            t.cmd("tp @a 830.5 -60 434.6 180 35");
        });
        t.add("cap3: buscar asiento", 200, () -> t.check("SENTARSE".equals(CajaCliente.fase()), "toca buscar asiento (" + CajaCliente.fase() + ")"));
        t.add("cap3: me siento", 600, () -> {
            var mc = Minecraft.getInstance();
            var pos = new net.minecraft.core.BlockPos(830, -60, 433);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.3, 0.5), net.minecraft.core.Direction.SOUTH, pos, false));
        });
        t.add("sentado yo", 100, () -> t.check(Minecraft.getInstance().player.getVehicle() instanceof com.coleccionista.evento.Silla,
                "me senté solo en la góndola (clic derecho en el sillón)"));
        foto(t, "c3_03b_sentado", 1200, () -> {
            var me = Minecraft.getInstance().player;
            me.setYRot(180F);
            me.setXRot(8F);
            me.yRotO = me.getYRot();
            me.xRotO = me.getXRot();
        });
        foto(t, "c3_03c_salon_bots", 3500, () -> {
            var me = Minecraft.getInstance().player;
            me.setYRot(0F);
            me.setXRot(10F);
            me.yRotO = me.getYRot();
            me.xRotO = me.getXRot();
        });
        t.waitFor("cap3: todos sentados", 40000, () -> "APAGON".equals(CajaCliente.fase()));
        foto(t, "c3_04_apagon", 1200, () -> {
        });
        t.waitFor("cap3: papeles", 8000, () -> "ROLES".equals(CajaCliente.fase()));
        foto(t, "c3_05_carta", 1500, () -> {
        });
        t.add("papel", 100, () -> {
            t.check(!CajaCliente.rol().isEmpty(), "tengo papel: " + CajaCliente.rol());
            t.check(CajaCliente.anonimo(), "es la parte a ciegas (sin nombres)");
            t.check(Minecraft.getInstance().player.getVehicle() instanceof com.coleccionista.evento.Silla, "sigo sentado en una góndola");
        });
        foto(t, "c3_05b_mascara", 6500, () -> {
            Minecraft.getInstance().options.hideGui = true;
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        t.add("sin hud", 200, () -> {
            Minecraft.getInstance().options.hideGui = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        t.waitFor("cap3: turno 1", 8000, () -> "TURNO".equals(CajaCliente.fase()));
        foto(t, "c3_06_gondola", 1500, PruebaCliente::mirarEnGondola);
        t.add("sentado", 100, () -> {
            var me = Minecraft.getInstance().player;
            t.check(me.getVehicle() instanceof com.coleccionista.evento.Silla, "voy sentado en mi góndola");
            t.check(com.axeliscore.plat.Nbt.i(CajaCliente.mio(), "gondola") > 0, "sé en qué góndola voy");
            t.check(Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON, "en primera persona");
            // mi pareja (si no voy solo) está sentada en la misma góndola, a 2 bloques (y se ve donde está su asiento)
            int cerca = 0, lejos = 0;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.bot.PiezaBot b) {
                    com.axeliscore.AxelisCore.LOGGER.info("COLECCIONISTA_CAJA_BOT {} en {} {} {} vehiculo={} {}", b.getName().getString(),
                            (int) Math.floor(b.getX()), (int) Math.floor(b.getY()), (int) Math.floor(b.getZ()),
                            b.getVehicle() == null ? "-" : b.getVehicle().getClass().getSimpleName(),
                            b.getVehicle() == null ? "" : (int) b.getVehicle().getX() + "," + (int) b.getVehicle().getZ());
                    if (b.distanceTo(me) < 3.5) {
                        cerca++;
                    }
                    if (b.getVehicle() != null && b.distanceToSqr(b.getVehicle()) > 4) {
                        lejos++;
                    }
                }
            }
            if (!"solo".equals(com.axeliscore.plat.Nbt.s(CajaCliente.mio(), "lado"))) {
                t.check(cerca >= 1, "mi pareja va sentada conmigo en la góndola (" + cerca + ")");
            }
            t.check(lejos == 0, "todos los bots sentados se ven en su asiento (" + lejos + " lejos)");
        });
        t.waitFor("cap3: turno 2", 20000, () -> "TURNO".equals(CajaCliente.fase()) && com.axeliscore.plat.Nbt.i(CajaCliente.estado(), "turno") == 2);
        foto(t, "c3_07_gondola2", 1500, PruebaCliente::mirarEnGondola);
        // me intento dar la vuelta: el de adelante no puede (la silla lo limita); el de atrás ve la cortina cerrada de la entrada
        foto(t, "c3_07b_vuelta", 1200, () -> {
            var me = Minecraft.getInstance().player;
            if (me != null && me.getVehicle() != null) {
                me.setYRot(me.getVehicle().getYRot() + 180F);
                me.setXRot(5F);
                me.yRotO = me.getYRot();
                me.xRotO = me.getXRot();
            }
        });
        t.add("no me doy la vuelta", 100, () -> {
            var me = Minecraft.getInstance().player;
            String lado = com.axeliscore.plat.Nbt.s(CajaCliente.mio(), "lado");
            if (me != null && me.getVehicle() != null && "frente".equals(lado)) {
                float d = Math.abs(net.minecraft.util.Mth.wrapDegrees(me.getYRot() - me.getVehicle().getYRot()));
                t.check(d <= com.coleccionista.prueba.Caja.LIMITE_FRENTE + 1F, "adelante no me puedo dar la vuelta (giro " + (int) d + "°)");
            }
            // la cortina de mi góndola (a 2 bloques detrás del asiento de atrás) está puesta
            if (me != null && me.getVehicle() != null) {
                var v = me.getVehicle();
                double r = Math.toRadians(v.getYRot());
                double fx = -Math.sin(r), fz = Math.cos(r);
                double atras = "frente".equals(lado) ? 4.0 : 2.0;
                var bp = net.minecraft.core.BlockPos.containing(v.getX() - fx * atras, v.getY() + 0.5, v.getZ() - fz * atras);
                var st = Minecraft.getInstance().level.getBlockState(bp);
                com.axeliscore.AxelisCore.LOGGER.info("COLECCIONISTA_CAJA_CORTINA {} {} {} -> {}", bp.getX(), bp.getY(), bp.getZ(), st);
                t.check(st.getBlock().getDescriptionId().contains("cortina"), "la cortina de la góndola está cerrada (" + st.getBlock().getDescriptionId() + ")");
            }
        });
        // los demás turnos, rápido
        for (int i = 0; i < 5; i++) {
            t.add("cap3: saltar turno " + (i + 3), 4600, () -> t.cmd("evento caja saltar"));
        }
        t.waitFor("cap3: circulo", 15000, () -> "CIRCULO".equals(CajaCliente.fase()));
        t.add("orden", 100, () -> {
            t.cmd("evento caja primero Bot_03");
            t.cmd("evento caja primero " + yo());
        });
        foto(t, "c3_08_circulo", 2000, () -> mirarA(828.5, -55.5, 451.5));
        foto(t, "c3_09_compuerta", 2200, () -> mirarA(828.5, -60.5, 451.5));
        t.add("compuerta", 100, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(827, -61, 451));
            t.check(st.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock
                    && st.getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN), "se abrió la compuerta del piso");
        });
        foto(t, "c3_09b_sube", 3500, () -> mirarA(828.5, -58.5, 451.5));
        foto(t, "c3_09c_arriba", 3500, () -> mirarA(828.5, -57, 451.5));
        foto(t, "c3_09d_tercera", 1000, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK));
        t.add("primera", 200, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        t.waitFor("cap3: apunta al bot", 60000, () -> apuntaA("Bot_03"));
        foto(t, "c3_10_apunta_bot", 600, () -> mirarA(828.5, -57, 451.5));
        // la tensión (igual para Bien y para Bala), la carga a los 3.2 s y el disparo a los 5.2 s
        foto(t, "c3_11_tension", 2500, () -> t.cmd("evento caja bala"));
        foto(t, "c3_11b_carga", 900, () -> {
        });
        foto(t, "c3_12_bang", 1850, () -> {
        });
        foto(t, "c3_12b_silla_cae", 700, () -> {
        });
        t.add("silla cae", 100, () -> {
            int sillas = 0;
            for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (e instanceof com.coleccionista.evento.Cadaver c && c.enSilla() && c.cae() > 0) {
                    sillas++;
                }
            }
            t.check(sillas >= 1, "la silla se fue de espaldas con el bot (" + sillas + ")");
        });
        foto(t, "c3_12c_sangre", 2000, () -> {
        });
        t.waitFor("cap3: me apunta", 60000, () -> apuntaA(yo()));
        foto(t, "c3_13_elige", 1200, () -> {
        });
        t.add("elige", 300, () -> {
            t.check(Minecraft.getInstance().screen instanceof EligeSimboloScreen, "me abrió la pantalla para marcar mi símbolo");
            CompoundTag c = new CompoundTag();
            c.putString("que", "simbolo");
            c.putInt("s", 1);
            com.axeliscore.data.client.ClientData.send(com.coleccionista.prueba.Caja.ACCION, c);
        });
        foto(t, "c3_13b_marcado", 700, () -> {
        });
        // lo que ve el organizador (simulado en mi pantalla)
        foto(t, "c3_14_organizador", 900, () -> {
            com.axeliscore.plat.client.Screens.close();
            CompoundTag o = new CompoundTag();
            o.putString("nombre", "Bot_05");
            o.putString("etiqueta", "05");
            o.putString("real", "cuadrado");
            o.putString("marco", "circulo");
            o.putBoolean("traidor", false);
            o.putString("paso", "ESPERA");
            o.putInt("i", 3);
            o.putInt("de", 8);
            o.putString("fase", "REVISION");
            o.putBoolean("juez", true);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Caja.KEY_ORG, o);
            com.axeliscore.plat.client.Screens.open(new RevisionScreen());
        });
        foto(t, "c3_14b_me_salvo", 5600, () -> {
            com.axeliscore.plat.client.Screens.close();
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Caja.KEY_ORG, new CompoundTag());
            t.cmd("evento caja bien");
            mirarA(828.5, -57, 451.5);
        });
        t.add("sigo vivo", 100, () -> t.check(!Minecraft.getInstance().player.isSpectator(), "con Bien: clic en vacío y sigo vivo"));
        t.add("auto", 200, () -> {
            t.cmd("evento caja esperar false");
            t.cmd("evento caja auto true");
        });
        t.waitFor("cap3: fin", 240000, () -> "FIN".equals(CajaCliente.fase()));
        foto(t, "c3_15_fin", 5000, () -> mirarA(828.5, -55, 451.5));
        t.add("cap3: limpiar", 1500, () -> {
            t.cmd("evento caja limpiar");
            t.cmd("evento caja auto false");
            t.cmd("evento caja sentarse 75");
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
        // la casa está en (720, -61, 430) (local + eso = mundo): el pasillo de la cripta en x 743..750 y z 368..381 (sus puertas en z 382),
        // el vestíbulo en x 737..756 y z 431..452 (pies en y -53), la jaula al fondo (reja en z 447) y la pantalla arriba de ella
        t.waitFor("cap2: a la casa", 30000, () -> !"".equals(MascotaCliente.fase()) && !"NADA".equals(MascotaCliente.fase()));
        foto(t, "c2_01_pasillo_casa", 1800, () -> {
            t.cmd("gamemode adventure @a");
            t.lookFrom(747.0, -60, 370.5, 747.0, -59, 382);
        });
        t.add("puertas de la casa", 100, () -> {
            var st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(743, -60, 382));
            t.check(st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN),
                    "se abrieron las puertas del pasillo al vestíbulo");
            t.check("LLEGANDO".equals(MascotaCliente.fase()) || "HABLANDO".equals(MascotaCliente.fase()), "llegamos a la casa (" + MascotaCliente.fase() + ")");
        });
        // la llegada: de la cripta al jardín y la casa al fondo
        foto(t, "c2_01b_cripta", 900, () -> t.lookFrom(747.0, -60, 384.5, 747.0, -58, 393));
        foto(t, "c2_01c_llegada_casa", 1500, () -> t.lookFrom(747.0, -53, 400.5, 747.0, -44, 430));
        foto(t, "c2_01d_fachada", 1200, () -> t.lookFrom(747.0, -52, 417.5, 747.0, -40, 430));
        t.add("al vestibulo", 200, () -> t.lookFrom(747.0, -53, 438.5, 747.0, -43.5, 446.9));
        t.waitFor("cap2: habla", 15000, () -> "HABLANDO".equals(MascotaCliente.fase()));
        foto(t, "c2_02_vestibulo", 9000, () -> t.lookFrom(747.0, -53, 436.5, 747.0, -43.5, 446.9));
        foto(t, "c2_03_jaula", 4000, () -> t.lookFrom(747.0, -53, 442.5, 747.0, -51, 447));
        foto(t, "c2_04_galeria", 2500, () -> t.lookFrom(747.0, -45, 432.5, 747.0, -45, 446.9));
        foto(t, "c2_05_esconder", 1800, () -> {
            t.cmd("evento mascota saltar");
            t.lookFrom(744.5, -53, 442.5, 736, -52, 442.5);
        });
        t.add("esconder", 100, () -> t.check("ESCONDER".equals(MascotaCliente.fase()), "corre el minuto para esconderse (" + MascotaCliente.fase() + ")"));
        // una pieza solo ve en primera persona y, en lugar del inventario, su bolsillo con el logo
        t.add("solo primera", 400, () -> {
            SoloPrimera.permitir = false;
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        t.add("primera persona", 100, () -> t.check(Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON,
                "una pieza no puede ver en tercera persona"));
        foto(t, "c2_05b_bolsillo", 900, () -> com.axeliscore.plat.client.Screens.open(
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
        foto(t, "c2_06_cascabel", 2500, () -> t.cmd("evento mascota soltar"));
        t.add("transformado", 100, () -> {
            var me = Minecraft.getInstance().player;
            t.check(com.axeliscore.forma.client.FormasCliente.transformado(me), "me veo como Cascabel");
            t.check(MascotaCliente.soyCazador(), "soy Cascabel");
            t.check(Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK, "la cámara se fue detrás para verme");
            t.check(me.getBbHeight() > 1.85F, "mi caja es la de Cascabel (" + me.getBbHeight() + ")");
        });
        foto(t, "c2_07_cascabel_frente", 1200, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        foto(t, "c2_08_ruge", 700, MascotaCliente::rugir);
        foto(t, "c2_09_olfatea", 1500, () -> {
            t.cmd("tp @a 747.0 -53 444.5 180 10");
            MascotaCliente.olfatear();
        });
        // se come a una pieza (la acerco primero para que se vea)
        t.add("presa cerca", 600, () -> t.cmd("tp @e[type=coleccionista:bot] 747.0 -53 441.5"));
        foto(t, "c2_10_atrapa", 450, () -> t.cmd("evento mascota comer"));
        foto(t, "c2_11_traga", 900, () -> {
        });
        t.add("comio", 2500, () -> {
        });
        t.add("se lo comio", 100, () -> {
            var d = com.axeliscore.data.client.ClientData.get(com.coleccionista.prueba.Mascota.KEY);
            t.check(d != null && d.getInt("comidos") >= 1, "Cascabel se comió a una pieza (" + (d == null ? -1 : d.getInt("comidos")) + ")");
        });
        foto(t, "c2_12_hud_cascabel", 900, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        // lo que ve quien se comen (simulado en mi pantalla): los dientes y lo negro
        foto(t, "c2_13_comido_dientes", 300, () -> {
            CompoundTag c = new CompoundTag();
            c.putLong("desde", Minecraft.getInstance().level.getGameTime() - 15);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Mascota.KEY_COMIDO, c);
        });
        foto(t, "c2_14_comido_negro", 300, () -> {
            CompoundTag c = new CompoundTag();
            c.putLong("desde", Minecraft.getInstance().level.getGameTime() - 34);
            com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Mascota.KEY_COMIDO, c);
        });
        t.add("sin comido", 300, () -> com.axeliscore.data.client.ClientData.receive(com.coleccionista.prueba.Mascota.KEY_COMIDO, new CompoundTag()));
        // se acaba: Cascabel a su jaula, se cierra la reja y vuelvo a ser yo
        foto(t, "c2_15_a_su_jaula", 1500, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
            t.cmd("evento mascota terminar");
        });
        t.add("reja cerrada", 100, () -> t.check(!Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(746, -53, 447)).isAir(),
                "se cerró la reja de la jaula"));
        t.add("vuelvo a ser yo", 3500, () -> {
        });
        t.add("normal", 100, () -> {
            t.check(!com.axeliscore.forma.client.FormasCliente.transformado(Minecraft.getInstance().player), "volví a ser yo");
            t.check(Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON, "la cámara regresó a mis ojos");
        });
        // un recorrido por la mansión gótica (como espectador, para no chocar con los muebles ni caer de lo alto); al final, para no atrasar
        // el discurso y el minuto para esconderse. Local + (720, -61, 430) = mundo.
        t.add("recorrido: espectador", 300, () -> t.cmd("gamemode spectator @a"));
        double[][] fotos = {
                // planta baja (pies en y -53)
                {734.5, -51, 443.5, 727, -53, 437}, {734.5, -51, 451.5, 728, -53, 448}, {759.5, -51, 443.5, 764, -53, 434},
                {728, -44, 455, 750, -52, 461}, {737.5, -51, 479.5, 730, -53, 474}, {747, -51, 470.5, 747, -53, 478},
                {756.5, -51, 470.5, 762, -53, 479},
                // planta alta (pies en y -45)
                {727.5, -43, 441.5, 735, -45, 436}, {734.5, -43, 445.5, 729, -45, 450}, {766.5, -43, 441.5, 760, -45, 434},
                {737.5, -43, 470.5, 730, -45, 476}, {747, -43, 470.5, 747, -45, 477},
                // ático, una torre y el sótano
                {746.5, -36, 456.5, 732, -38, 440}, {728.5, -28.4, 430.5, 747, -53, 410}, {761.5, -58, 443.5, 755, -59, 436},
                {737.5, -58, 470.5, 731, -59, 476}, {760.5, -58, 460.5, 730, -60, 461},
                // el jardín: la fuente, el laberinto, el cementerio, atrás (invernadero y estanque) y la casa desde arriba
                {747, -52, 422.5, 747, -50, 412}, {730, -41, 402, 730, -54, 382}, {763, -49, 400, 764, -54, 380},
                {747, -47, 486, 747, -54, 510}, {790, -20, 395, 747, -44, 455}};
        String[] nombres = {"c2_20_biblioteca", "c2_21_musica", "c2_22_comedor", "c2_23_salon_baile", "c2_24_billar", "c2_25_trofeos",
                "c2_26_cocina", "c2_27_recamara", "c2_28_bebe", "c2_29_estudio", "c2_30_costura", "c2_31_bano", "c2_32_atico",
                "c2_33_torre", "c2_34_calderas", "c2_35_munecas", "c2_36_bodega", "c2_37_fuente", "c2_38_laberinto", "c2_39_cementerio",
                "c2_40_jardin_atras", "c2_41_casa_arriba"};
        for (int i = 0; i < fotos.length; i++) {
            double[] f = fotos[i];
            foto(t, nombres[i], 1100, () -> t.lookFrom(f[0], f[1], f[2], f[3], f[4], f[5]));
        }
        t.add("recorrido: aventura", 300, () -> t.cmd("gamemode adventure @a"));
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
        foto(t, "32_fundido", 1600, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("evento inscribir @a");
            t.cmd("evento revivir Bot_02");
            t.cmd("forceload add 48 258 170 306");
            t.cmd("evento encadenados tiempo 30");
            t.cmd("evento encadenados gracia 2");
            t.cmd("evento encadenados preparar");
        });
        foto(t, "33_presentacion", 5700, () -> {
        });
        t.add("después del título", 6000, () -> t.check(!Presentacion.reglasVisibles(), "las reglas esperan al Coleccionista (no salen solas)"));
        t.add("encadenado", 100, () -> {
            t.check("LISTA".equals(EncadenadosCliente.fase), "Encadenados está lista");
            t.check(EncadenadosCliente.encadenado(), "estoy encadenado a la pared");
            t.check(!pareja().isEmpty(), "tengo pareja (cadena)");
        });
        foto(t, "34_grilletes", 4800, () -> {
        });
        foto(t, "35_cadenas_tercera", 1200, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.add("a jugar", 1200, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.cmd("minijuego 1 ya");
            t.cmd("evento encadenados soltar todos");
            t.cmd("evento encadenados ir enc.placa.1.a " + yo());
            t.cmd("evento encadenados ir enc.placa.1.b " + pareja());
        });
        foto(t, "33b_reglas", 1600, () -> {
        });
        t.add("reglas", 100, () -> t.check(Presentacion.reglasVisibles(), "al arrancar sale la tarjeta de reglas arriba a la derecha"));
        foto(t, "36_compuerta", 900, () -> {
        });
        t.add("compuerta", 100, () -> {
            t.check(!EncadenadosCliente.encadenado(), "ya no estoy encadenado a la pared");
            t.check(EncadenadosCliente.placas.stream().anyMatch(EncadenadosCliente.Placa::abierta), "la compuerta se abrió con las dos placas");
            t.cmd("evento encadenados ir enc.centro " + yo());
        });
        t.add("voltear", 600, () -> Minecraft.getInstance().player.setYRot(90F));
        foto(t, "37_gas_frente", 1300, () -> Minecraft.getInstance().player.setYRot(90F));
        foto(t, "38_gas_dentro", 2600, () -> {
        });
        t.add("en el gas", 100, () -> {
            t.check(Gas.adentro(Minecraft.getInstance().player.getEyePosition(), 0), "estoy dentro del gas");
            t.check(EncadenadosCliente.aire() < 100, "se me acaba el aire");
            t.cmd("evento encadenados ir enc.salida " + yo());
        });
        foto(t, "39_refugio", 1600, () -> {
        });
        t.add("a salvo", 100, () -> t.check(EncadenadosCliente.aSalvo.contains(Minecraft.getInstance().player.getUUID()), "llegué al Refugio"));
        t.add("esperar el gas", 9000, () -> {
        });
        foto(t, "40_fin", 600, () -> {
        });
        t.add("fin de la prueba", 100, () -> t.check("FIN".equals(EncadenadosCliente.fase), "terminó Encadenados (el gas se llevó a los demás)"));
        foto(t, "41_panel_pruebas", 900, () -> {
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
        foto(t, "41b_elevador_llegada", 2500, () -> {
        });
        t.add("rec: equipos", 1500, () -> t.cmd("evento recorrido saltar"));
        foto(t, "41c_cubiculos", 1500, () -> t.lookFrom(44.5, -60, 203.5, 40, -58.5, 193.5));
        foto(t, "41d_cubiculos_sur", 1200, () -> t.lookFrom(44.5, -60, 199.5, 40, -58.5, 209.5));
        foto(t, "41e_sala_espera", 1200, () -> t.lookFrom(60.5, -60, 201.5, 75, -58.5, 201.5));
        t.add("rec: parar", 600, () -> {
            t.cmd("evento recorrido parar");
            t.cmd("evento reloj 0");
        });
        // de espaldas a las pantallas: el Coleccionista habla en la mini tele de arriba a la derecha
        t.add("habla sin tele enfrente", 600, () -> t.cmd("evento pantalla hablar antesala"));
        foto(t, "41g_mini_tele", 4200, () -> {
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
        foto(t, "42_salon_presentacion", 5700, () -> {
        });
        t.add("en la mesa", 100, () -> t.check("LISTA".equals(CompartirCliente.fase()), "Compartir o robar está lista"));
        foto(t, "43_mesa", 5000, () -> {
        });
        foto(t, "44_hablar", 1200, () -> t.cmd("evento compartir empezar ya"));
        foto(t, "45_eleccion", 1500, () -> t.cmd("evento compartir elegir"));
        t.add("pantalla de elegir", 100, () -> t.check(com.axeliscore.plat.client.Screens.current() instanceof EleccionScreen,
                "se abrió la pantalla para elegir"));
        foto(t, "46_elegido", 900, () -> {
            net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
            c.putBoolean("robar", false);
            com.axeliscore.data.client.ClientData.send(com.coleccionista.prueba.Compartir.ACCION, c);
        });
        t.add("revelar", 1500, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("evento compartir revelar");
        });
        foto(t, "47_revelacion", 4000, () -> {
        });
        t.add("revelando", 100, () -> t.check("REVELAR".equals(CompartirCliente.fase()), "se están revelando las mesas"));
        t.add("esperar las mesas", 27500, () -> {
        });
        foto(t, "48_compartir_fin", 600, () -> {
        });
        t.add("fin compartir", 100, () -> t.check("FIN".equals(CompartirCliente.fase()), "se revelaron todas las mesas"));
        foto(t, "49_panel_pruebas2", 900, () -> {
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
        foto(t, "50_puertas_presentacion", 5700, () -> {
        });
        foto(t, "51_vestibulo", 5000, () -> {
        });
        t.add("puertas lista", 100, () -> t.check("LISTA".equals(PuertasCliente.fase()), "Las puertas está lista"));
        foto(t, "52_ronda", 1500, () -> t.cmd("evento puertas ronda"));
        t.add("eligiendo", 100, () -> t.check("ELEGIR".equals(PuertasCliente.fase()), "se abrieron las rejas para elegir"));
        foto(t, "53_cabina", 1500, () -> t.cmd("evento puertas cerrar"));
        foto(t, "54_trampa", 3000, () -> {
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
        foto(t, "55_copas_presentacion", 6200, () -> {
        });
        foto(t, "56_mesa_final", 5000, () -> {
        });
        t.add("final lista", 100, () -> t.check("LISTA".equals(CopasCliente.fase()), "la final está lista"));
        foto(t, "57_turno", 1500, () -> t.cmd("evento copas empezar ya"));
        t.add("turno", 100, () -> t.check("TURNO".equals(CopasCliente.fase()), "alguien tiene el turno"));
        foto(t, "58_bebe", 1500, () -> t.cmd("evento copas beber"));
        for (int i = 0; i < 12; i++) {
            t.add("beber " + i, 4300, () -> t.cmd("evento copas beber"));
        }
        foto(t, "59_ganador", 1200, () -> {
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
        foto(t, "60_sacrificio_presentacion", 5700, () -> {
        });
        foto(t, "61_cuarto", 5000, () -> {
        });
        t.add("sacrificio lista", 100, () -> t.check("LISTA".equals(SacrificioCliente.fase()), "El sacrificio está lista"));
        foto(t, "62_ofrecer", 1500, () -> t.cmd("evento sacrificio empezar ya"));
        foto(t, "63_voto", 12000, () -> {
        });
        t.add("pantalla de voto", 100, () -> t.check(com.axeliscore.plat.client.Screens.current() instanceof VotoScreen,
                "se abrió la pantalla para votar"));
        foto(t, "64_votado", 900, () -> {
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
        foto(t, "65_salgan", 600, () -> {
        });
        t.add("esperar sacrificio", 10000, () -> {
        });
        foto(t, "66_sacrificio_fin", 600, () -> {
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
        foto(t, "67_traidor_presentacion", 5700, () -> {
        });
        foto(t, "68_sala_archivo", 5000, () -> {
        });
        t.add("traidor lista", 100, () -> t.check("LISTA".equals(TraidorCliente.fase()), "El traidor está lista"));
        foto(t, "69_papel", 2500, () -> t.cmd("evento traidor empezar ya"));
        t.add("papel", 100, () -> t.check(!TraidorCliente.rol().isEmpty(), "me tocó un papel (leal o traidor)"));
        foto(t, "70_nota", 6000, () -> {
            var inv = Minecraft.getInstance().player.getInventory();
            for (int i = 0; i < 9; i++) {
                var st = inv.getItem(i);
                if (st.hasTag() && st.getTag().getBoolean("coleccionista_nota")) {
                    inv.selected = i;
                }
            }
        });
        t.add("buscando", 100, () -> t.check("BUSCAR".equals(TraidorCliente.fase()), "empezó la búsqueda del código"));
        foto(t, "71_teclado", 900, () -> com.axeliscore.plat.client.Screens.open(new TecladoScreen()));
        t.add("cortar", 3000, () -> {
            com.axeliscore.plat.client.Screens.close();
            t.cmd("evento traidor cortar");
        });
        foto(t, "72_castigo", 600, () -> {
        });
        t.add("esperar traidor", 22000, () -> {
        });
        foto(t, "73_traidor_fin", 600, () -> {
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
        foto(t, "74_ciegas_presentacion", 5700, () -> {
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
        foto(t, "75_vendado", 2500, () -> Minecraft.getInstance().player.setXRot(8));
        t.add("vendado", 100, () -> t.check(CiegasCliente.vendado(), "camino con los ojos vendados"));
        t.add("cie: otra vez", 1500, () -> {
            t.cmd("evento ciegas terminar");
            t.cmd("evento ciegas preparar");
        });
        t.add("cie: ser guía", 11000, () -> t.cmd("evento ciegas guia " + yo()));
        t.add("cie: empezar 2", 1500, () -> t.cmd("evento ciegas empezar ya"));
        foto(t, "76_guia", 2500, () -> Minecraft.getInstance().player.setXRot(62));
        t.add("guía", 100, () -> t.check(CiegasCliente.soyElGuia() && !CiegasCliente.vendado(), "soy el guía: veo todo y las trampas en rojo"));
        t.add("cie: cortar", 1500, () -> t.cmd("evento ciegas cortar"));
        foto(t, "77_ciegas_fin", 1500, () -> {
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
        foto(t, "78_tres_presentacion", 5700, () -> {
        });
        foto(t, "79_tres_mesa", 9000, () -> Minecraft.getInstance().player.setXRot(25));
        t.add("tres lista", 100, () -> t.check("LISTA".equals(SentidosCliente.fase()), "Encadenados de tres está lista"));
        foto(t, "80_tres_elegir", 2000, () -> t.cmd("evento tres empezar ya"));
        t.add("tres: tomar esposas", 1500, () -> t.cmd("evento tres toma " + yo() + " 2"));
        t.add("tres: rol", 100, () -> t.check("ESPOSAS".equals(SentidosCliente.miRol()), "me puse las esposas"));
        t.add("tres: arrancar", 3500, () -> t.cmd("evento tres arrancar"));
        foto(t, "81_tres_recorrido", 1500, () -> Minecraft.getInstance().player.setXRot(10));
        t.add("tres: juego", 100, () -> t.check("JUEGO".equals(SentidosCliente.fase()), "se abrieron las rejas: a cruzar encadenados"));
        foto(t, "82_tres_cadena", 1500, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.add("tres: sacar", 1500, () -> {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.cmd("evento tres sacar " + yo());
        });
        foto(t, "83_tres_fin", 3000, () -> {
        });
        t.add("fin tres", 100, () -> {
            t.check("FIN".equals(SentidosCliente.fase()), "terminó Encadenados de tres");
            t.cmd("evento tres terminar");
        });
    }
}

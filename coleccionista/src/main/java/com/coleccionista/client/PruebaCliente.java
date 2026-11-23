package com.coleccionista.client;

import com.axeliscore.test.ClientTest;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

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

    /** El Ala de celdas: construir, sala de espera, ¡pum!, cinemática, discurso, acercarse a la tele, candado, cuerpos. */
    private static void mapa(ClientTest.Script t) {
        // el mapa queda con el origen en (0, -61, 30): celda 1 en (3, -60, 34), pasillo en z 41
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
            t.cmd("evento investigar 22");
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
        // a los 22 segundos se prende la tele y habla el Coleccionista
        t.photo("24_tele_habla", 17000, () -> {
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
        t.add("esperar discurso", 26000, () -> {
        });
        t.photo("26_candado", 900, () -> com.axeliscore.plat.client.Screens.open(new CandadoScreen()));
        t.add("cerrar candado", 500, () -> com.axeliscore.plat.client.Screens.close());
        t.photo("27_pasillo", 2500, () -> {
            t.cmd("evento celdas abrir");
            t.cmd("gamemode spectator @a");
            t.lookFrom(-4.5, -60, 41.5, 40, -59.2, 41.5);
        });
        t.photo("28_cuerpo", 2500, () -> {
            t.cmd("evento eliminar Bot_02 Encadenados");
            t.lookFrom(9.5, -59.4, 39.5, 9.5, -60.6, 34.5);
        });
        t.photo("29_pantalla_grande", 2000, () -> t.lookFrom(10.5, -57, 41.5, -7, -56, 41.5));
        t.photo("30_pasarela", 1500, () -> t.lookFrom(35.5, -54.4, 38, 0, -55, 38));
        // Axelis aparece disfrazado del Coleccionista en el pasillo (visto de frente)
        t.add("disfrazarme", 1500, () -> {
            t.cmd("evento quitar " + yo());
            t.cmd("evento disfraz si @a");
        });
        t.photo("31_disfraz", 1800, () -> {
            t.lookFrom(14.5, -60, 41.5, 0, -58.4, 41.5);
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        t.add("disfrazado", 100, () -> {
            t.check(EventoCliente.disfrazado(Minecraft.getInstance().player.getUUID()), "estoy disfrazado del Coleccionista");
            t.check(!Minecraft.getInstance().player.isSpectator(), "el disfraz me saca de espectador");
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.cmd("evento disfraz no @a");
        });
        encadenados(t);
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
            t.cmd("forceload add 32 0 176 80");
            t.cmd("evento encadenados tiempo 30");
            t.cmd("evento encadenados gracia 2");
            t.cmd("evento encadenados preparar");
        });
        t.photo("33_presentacion", 5700, () -> {
        });
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
            t.cmd("evento encadenados empezar ya");
            t.cmd("evento encadenados soltar todos");
            t.cmd("evento encadenados ir enc.placa.1.a " + yo());
            t.cmd("evento encadenados ir enc.placa.1.b " + pareja());
        });
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
        t.add("com: preparar", 1600, () -> {
            for (int i = 2; i <= 8; i++) {
                t.cmd("evento revivir Bot_0" + i);
            }
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
            t.cmd("forceload add -112 32 -16 64");
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
    }
}

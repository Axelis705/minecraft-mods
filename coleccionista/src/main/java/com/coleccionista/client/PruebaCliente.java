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
                s.pestana(2);
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
        t.photo("21_despertar_cine", 2000, () -> t.cmd("evento despertar"));
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
            t.cmd("evento disfraz si");
        });
        t.photo("31_disfraz", 1800, () -> {
            t.lookFrom(14.5, -60, 41.5, 0, -58.4, 41.5);
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        t.add("disfrazado", 100, () -> {
            t.check(EventoCliente.disfrazado(Minecraft.getInstance().player.getUUID()), "estoy disfrazado del Coleccionista");
            t.check(!Minecraft.getInstance().player.isSpectator(), "el disfraz me saca de espectador");
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            t.cmd("evento disfraz no");
        });
    }
}

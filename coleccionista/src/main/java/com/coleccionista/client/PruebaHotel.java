package com.coleccionista.client;

import com.axeliscore.test.ClientTest;
import com.coleccionista.evento.Complejo;
import com.coleccionista.hotel.Botones;
import com.coleccionista.hotel.JuegoHotel;
import com.coleccionista.hotel.Monitor;
import com.coleccionista.hotel.PuertaHotel;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

/**
 * La prueba con fotos del hotel (cliente-core.yml con mod=coleccionista y escenario=coleccionista_hotel): la torre de la sala grande, el
 * ascensor de una persona por dentro, la bajada con la explicación, la llegada, las tres clases de puerta (el hilo de la trampa, la luz de
 * la salida), el Botones con su carrito, las ratas, las pistas en la pared, el tubo, la trampa que vuela la cabeza (a un bot), la caída por
 * el tobogán, la sala de espera con los monitores y el regreso a la sala grande.
 * <p>
 * El mapa se construye con el origen en (0, -61, 30): la sala grande empieza en (55, -61, 30), el hotel en (300, -61, -100) y la sala de
 * espera en (372, -61, -88). El cliente y el servidor están en el mismo juego: las posiciones se leen de las marcas.
 */
public final class PruebaHotel {
    private static final double G = -60;

    private PruebaHotel() {
    }

    private static MinecraftServer srv() {
        return Minecraft.getInstance().getSingleplayerServer();
    }

    private static BlockPos m(String marca) {
        return Complejo.pos(srv(), marca);
    }

    private static String yo() {
        return Minecraft.getInstance().player.getGameProfile().getName();
    }

    /** Mirar una puerta de servicio desde el pasillo (de frente, un poco de lado y a la altura de los ojos). */
    private static void mirarPuerta(ClientTest.Script t, int k, double lado, double lejos) {
        BlockPos p = m("hotel.serv." + k);
        if (p == null) {
            return;
        }
        double r = Math.toRadians(Complejo.yaw("hotel.serv." + k));
        double fx = Math.sin(r), fz = -Math.cos(r);           // hacia el pasillo
        double lx = Math.cos(r), lz = Math.sin(r);            // de lado
        double cx = p.getX() + 0.5, cz = p.getZ() + 0.5;
        t.lookFrom(cx + fx * lejos + lx * lado, p.getY() - 0.2, cz + fz * lejos + lz * lado, cx, p.getY() + 0.7, cz);
    }

    static void escenario(ClientTest.Script t) {
        t.add("hotel: construir", 3000, () -> {
            t.hud(false);
            t.cmd("evento reiniciar confirmar");
            t.cmd("kill @e[type=!player]");
            t.cmd("time set 18000");
            t.cmd("gamemode creative @a");
            t.cmd("execute positioned 0 -60 30 run evento construir");
            t.lookFrom(85, -40, 20, 85, -60, 47);
        });
        t.waitFor("hotel: construido", 600000, () -> srv() != null && Complejo.construido(srv(), "hotel")
                && Complejo.construido(srv(), "hotel_espera") && !Complejo.construyendo());
        t.add("hotel: piezas", 2500, () -> {
            t.cmd("forceload add 290 -110 400 -40");
            t.cmd("forceload add 55 30 115 70");
            t.cmd("evento inscribir @a");
            t.cmd("execute positioned 100 -60 47 run evento bots 4");
            t.cmd("evento hotel usar true");
            t.cmd("evento hotel tiempos 600 30 200");
            t.cmd("gamemode adventure @a");
        });
        // la torre de los 32 ascensores, en medio de la sala grande
        t.photo("h01_torre", 1500, () -> t.lookFrom(106, -59, 47.5, 85, -58, 47.5));
        t.photo("h02_torre_puertas", 1200, () -> t.lookFrom(81, -60, 63, 81, -58.6, 56));
        // empieza: el Coleccionista habla (se salta) y se abren los ascensores; yo me meto al 1
        t.add("hotel: empezar", 1500, () -> {
            t.cmd("evento hotel empezar");
        });
        t.add("hotel: preparado", 100, () -> {
            t.check(JuegoHotel.fase() == JuegoHotel.Fase.HABLANDO, "el Coleccionista presenta el hotel: " + JuegoHotel.fase());
            t.check(!JuegoHotel.salida().isEmpty(), "ya hay salida: " + JuegoHotel.salida());
            t.check(InscripcionesCliente.delHotel() > 10, "llegaron las pistas del hotel: " + InscripcionesCliente.delHotel());
        });
        t.add("hotel: ascensores", 1500, () -> t.cmd("evento hotel ascensores"));
        t.photo("h03_dentro_ascensor", 3500, () -> {
            BlockPos o = m("hotel.cab.0");
            // el ascensor 1 de la torre: el bloque de adentro está en (77, -60, 39); se mira hacia la puerta (al norte)
            t.lookFrom(77.5, G, 39.5, 77.5, G + 1.5, 36);
        });
        t.add("hotel: bajar", 1000, () -> t.cmd("evento hotel bajar"));
        t.photo("h04_bajando", 4500, () -> {
        });
        t.waitFor("hotel: llegamos", 30000, () -> JuegoHotel.fase() == JuegoHotel.Fase.JUEGO);
        t.photo("h05_llegada", 2500, () -> {
        });
        t.add("hotel: juego", 100, () -> {
            t.check(Minecraft.getInstance().player.getY() > -61 && Minecraft.getInstance().player.getX() > 290, "llegué al hotel");
            int puertas = 0;
            for (var e : srv().overworld().getAllEntities()) {
                if (e instanceof PuertaHotel) {
                    puertas++;
                }
            }
            t.check(puertas == 20, "hay 20 puertas de servicio: " + puertas);
        });
        // las tres clases de puerta
        t.photo("h06_puerta_trampa", 1500, () -> {
            t.cmd("gamemode spectator @a");
            mirarPuerta(t, JuegoHotel.puertaDe(true), 0.6, 1.6);
        });
        t.photo("h07_puerta_salida", 1500, () -> mirarPuerta(t, JuegoHotel.puertaDe(false), -0.5, 1.8));
        t.photo("h08_puerta_normal", 1500, () -> {
            int k = 0;
            while (k == JuegoHotel.puertaDe(false) || JuegoHotel.trampas().contains(com.coleccionista.mapa.Hotel.codigo(k))) {
                k++;
            }
            mirarPuerta(t, k, 0.8, 2.6);
        });
        // el Botones con su carrito (uno de exhibición en el vestíbulo, quieto) y una rata
        t.photo("h09_botones", 2500, () -> {
            t.cmd("summon coleccionista:botones 325.5 -60 -84.5 {Rotation:[0f,0f]}");
            t.cmd("summon coleccionista:rata 323.5 -60 -80.5 {NoAI:1b,Rotation:[200f,0f]}");
            t.lookFrom(325.5, -60, -77.5, 325.5, -58.2, -84.5);
        });
        t.photo("h10_botones_cerca", 1500, () -> t.lookFrom(327.5, -59.3, -81.5, 325.5, -57.6, -84.5));
        t.add("hotel: botones de verdad", 100, () -> {
            int b = 0;
            for (var e : srv().overworld().getAllEntities()) {
                if (e instanceof Botones x && x.base != null) {
                    b++;
                }
            }
            t.check(b == 2, "hay dos Botones rondando: " + b);
        });
        // una pista en la pared
        t.photo("h11_pista", 1500, () -> {
            var l = InscripcionesCliente.hotel();
            if (!l.isEmpty()) {
                var i = l.get(0);
                double r = Math.toRadians(i.ang());
                var p = i.pos();
                var ojo = i.plano() == com.coleccionista.evento.Pistas.PISO ? p.add(0, 1.3, 0.6) : p.add(Math.sin(r) * 1.3, 0, Math.cos(r) * 1.3);
                t.lookFrom(ojo.x, ojo.y - 1.62, ojo.z, p.x, p.y, p.z);
                com.coleccionista.Coleccionista.LOGGER.info("COLECCIONISTA_PRUEBA_HOTEL pista '{}'", i.texto());
            }
        });
        // el tubo en el piso y en la mano
        t.photo("h12_tubo", 1500, () -> {
            BlockPos p = m("hotel.tubo.0");
            if (p != null) {
                t.lookFrom(p.getX() + 0.5, p.getY(), p.getZ() + 2.2, p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            }
        });
        t.photo("h13_tubo_mano", 1500, () -> {
            t.hud(true);
            t.cmd("gamemode adventure @a");
            t.cmd("give @a coleccionista:tubo_suelto");
        });
        t.add("sin interfaz", 200, () -> t.hud(false));
        // la trampa: un bot abre una puerta con trampa y yo lo veo de lado
        t.add("hotel: mirar la trampa", 1200, () -> {
            t.cmd("gamemode spectator @a");
            int k = JuegoHotel.puertaDe(true);
            mirarPuerta(t, k, 2.6, 1.0);
        });
        t.photo("h14_disparo", 900, () -> t.cmd("evento hotel abrir trampa Bot_02"));
        t.photo("h15_sin_cabeza", 1800, () -> {
        });
        t.add("hotel: eliminado", 100, () -> {
            boolean hay = false;
            for (var e : srv().overworld().getAllEntities()) {
                hay |= e instanceof com.coleccionista.evento.Cadaver c && c.sinCabeza();
            }
            t.check(hay, "quedó el cuerpo sin cabeza");
            t.check(EventoCliente.vivos() == 4, "quedan 4 vivos (yo y 3 bots): " + EventoCliente.vivos());
        });
        t.photo("h16_trampa_adentro", 1200, () -> mirarPuerta(t, JuegoHotel.puertaDe(true), 0.2, 1.2));
        // la salida: abro y caigo por el tobogán
        t.add("hotel: abrir salida", 2200, () -> {
            t.cmd("gamemode adventure @a");
            t.cmd("evento hotel abrir salida " + yo());
        });
        t.photo("h17_salida_abierta", 400, () -> {
        });
        t.photo("h18_tobogan", 1300, () -> t.cmd("evento hotel salir " + yo()));
        t.photo("h19_colchon", 3500, () -> {
        });
        t.add("hotel: salí", 100, () -> t.check(JuegoHotel.salieron() == 1, "salí del hotel: " + JuegoHotel.salieron()));
        // la sala de espera y los monitores
        t.photo("h20_sala_espera", 1500, () -> t.lookFrom(386.5, G, -75.5, 386.5, G + 2.5, -87));
        t.photo("h21_monitores", 1500, () -> t.lookFrom(386.5, G, -84, 388, G + 2.2, -87));
        t.add("hotel: monitores", 100, () -> {
            int n = 0;
            for (var e : srv().overworld().getAllEntities()) {
                if (e instanceof Monitor) {
                    n++;
                }
            }
            t.check(n == com.coleccionista.mapa.Hotel.MONITORES, "hay 12 monitores: " + n);
        });
        t.photo("h22_tercera", 1200, () -> Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.add("primera persona", 300, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
        // se acaba: los que siguen adentro se quedan; los demás regresan dormidos a la sala
        t.add("hotel: salen bots", 800, () -> t.cmd("evento hotel salir Bot_03"));
        t.photo("h23_fin", 3000, () -> t.cmd("evento hotel terminar"));
        t.waitFor("hotel: regreso", 60000, () -> JuegoHotel.fase() == JuegoHotel.Fase.NADA);
        t.photo("h24_sala_regreso", 4000, () -> {
        });
        t.add("hotel: equipos", 100, () -> {
            t.check(com.coleccionista.evento.Capitulo1.fase() == com.coleccionista.evento.Capitulo1.Fase.HABLANDO
                    || com.coleccionista.evento.Capitulo1.fase() == com.coleccionista.evento.Capitulo1.Fase.FORMAR,
                    "después del hotel se forman los equipos: " + com.coleccionista.evento.Capitulo1.fase());
            t.check(EventoCliente.vivos() == 2, "quedamos 2 (yo y Bot_03): " + EventoCliente.vivos());
        });
        t.add("hotel: limpiar", 1000, () -> {
            t.cmd("evento capitulo1 parar");
            t.cmd("evento hotel limpiar");
            t.cmd("gamemode creative @a");
        });
    }
}

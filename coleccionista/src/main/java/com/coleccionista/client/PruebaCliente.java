package com.coleccionista.client;

import com.axeliscore.test.ClientTest;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * La prueba con el juego de verdad y fotos (cliente-core.yml con mod=coleccionista y escenario=coleccionista): HUD, parejas, eliminación
 * (la franja y la escena propia), ficha de vida, fantasma, pausa y ganador. Corre con bots: no hace falta nadie más.
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
        t.photo("04_eliminado", 900, () -> t.cmd("evento eliminar Bot_02 forzar"));
        t.add("esperar franja", 4200, () -> {
        });
        t.photo("05_ficha", 1000, () -> t.cmd("evento eliminar Bot_03 Encadenados"));
        t.add("ficha usada", 100, () -> t.check(EventoCliente.vivos() == 7, "con la ficha se salvó: quedan " + EventoCliente.vivos()));
        t.add("esperar ficha", 3500, () -> {
        });
        t.photo("06_mi_eliminacion", 2800, () -> t.cmd("evento eliminar " + yo() + " forzar"));
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
        t.photo("09_pausa", 1200, () -> t.cmd("evento pausar"));
        t.add("reanudar", 800, () -> t.cmd("evento reanudar"));
        // quedo yo solo: salen las 6 franjas y luego la del ganador
        t.add("final", 4600, () -> {
            for (String b : new String[] {"Bot_03", "Bot_04", "Bot_05", "Bot_06", "Bot_07", "Bot_08"}) {
                t.cmd("evento eliminar " + b + " forzar");
            }
        });
        t.add("esperar franjas", 24000, () -> {
        });
        t.photo("10_ganador", 1500, () -> {
        });
        t.add("hay ganador", 100, () -> t.check(EventoCliente.vivos() == 1 && "FIN".equals(EventoCliente.fase()), "queda una sola pieza"));
        t.add("limpiar", 1500, () -> {
            t.cmd("evento reiniciar confirmar");
            t.cmd("gamemode creative @a");
        });
    }
}

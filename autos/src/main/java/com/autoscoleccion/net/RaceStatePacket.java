package com.autoscoleccion.net;

import java.util.List;

/**
 * Servidor -> cliente: cómo va mi carrera. phase: 0 cuenta regresiva, 1 corriendo, 2 terminada, -1 ya no estoy en carrera. timer:
 * ticks que faltan para salir (fase 0) o para cerrar los resultados (fase 2).
 */
public record RaceStatePacket(int phase, int timer, int lap, int laps, int place, int total, int timeMs, boolean finished, List<String> board,
        int me) {
}

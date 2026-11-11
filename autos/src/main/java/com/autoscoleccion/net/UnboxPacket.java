package com.autoscoleccion.net;

/** Servidor -> cliente: abre la animación de la caja; el auto ya está elegido. */
public record UnboxPacket(int box, int car, boolean isNew) {
}

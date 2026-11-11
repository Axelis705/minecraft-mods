package com.autoscoleccion.net;

/** Cliente -> servidor: posición, giro y dirección de las llantas del auto que manejo. */
public record CarMovePacket(int entityId, double x, double y, double z, float yaw, float steer) {
}

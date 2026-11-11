package com.autoscoleccion.net;

/** Cliente -> servidor: sacar un auto del garaje para manejarlo (car >= 0) o guardarlo (car = -1). */
public record DrivePacket(int car) {
}

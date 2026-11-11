package com.autoscoleccion.net;

/** Servidor -> cliente: cuántas veces tiene cada auto; si open es true, abre el garaje. */
public record CollectionPacket(int[] counts, boolean open) {
}

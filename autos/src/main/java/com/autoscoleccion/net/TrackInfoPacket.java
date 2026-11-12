package com.autoscoleccion.net;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Servidor -> cliente: cómo está la mini pista (construida, vueltas, bots, quién se anotó). Si open es true, abre su pantalla. */
public record TrackInfoPacket(BlockPos pos, boolean built, int laps, int bots, boolean running, List<String> names, boolean open) {
}

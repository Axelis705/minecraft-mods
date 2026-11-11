package com.autoscoleccion.net;

import net.minecraft.core.BlockPos;

/** Servidor -> cliente: empieza una prueba de habilidad (cuál, con qué semilla y en qué máquina). */
public record StartGamePacket(int session, int type, long seed, BlockPos pos) {
}

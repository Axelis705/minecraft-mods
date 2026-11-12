package com.autoscoleccion.net;

import net.minecraft.core.BlockPos;

/** Cliente -> servidor: algo que el jugador hizo en la pantalla de la mini pista. */
public record RaceActionPacket(BlockPos pos, int action, int arg) {
    public static final int BUILD = 0, JOIN = 1, LEAVE = 2, LAPS = 3, BOTS = 4, START = 5, CANCEL = 6, THEME = 7;
}

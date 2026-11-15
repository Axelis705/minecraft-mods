package com.casino.client;

import com.casino.net.LeaderboardPacket;

/** Lo último que mandó el servidor para las pantallas de récords (null mientras no llega nada). */
public final class Leaderboard {
    private static LeaderboardPacket data;
    /** Cuándo llegó (reloj del cliente), para animar el cambio. */
    private static long at;

    private Leaderboard() {
    }

    public static void set(LeaderboardPacket m) {
        data = m;
        at = System.currentTimeMillis();
    }

    public static LeaderboardPacket get() {
        return data;
    }

    public static long receivedAt() {
        return at;
    }
}

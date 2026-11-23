package com.axeliscore.ui;

import com.axeliscore.net.CoreNet;
import net.minecraft.server.level.ServerPlayer;

/** Desde el servidor: abrir una pantalla registrada en el cliente con {@link ScreenRegistry}. */
public final class CoreScreens {
    private CoreScreens() {
    }

    public static void open(ServerPlayer p, String id) {
        CoreNet.NET.toPlayer(p, new CoreNet.OpenMsg(id));
    }
}

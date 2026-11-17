package com.celular.map;

import com.celular.CelularReg;
import com.celular.net.AppNet;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Mapa con amigos: quién comparte su ubicación y dónde está (solo se ven los de tu misma dimensión que tengan celular). */
public final class Friends {
    /** Los que apagaron "compartir ubicación". */
    private static final Set<UUID> HIDDEN = new HashSet<>();

    private Friends() {
    }

    public static void share(ServerPlayer p, boolean on) {
        if (on) {
            HIDDEN.remove(p.getUUID());
        } else {
            HIDDEN.add(p.getUUID());
        }
        send(p);
    }

    public static void send(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        AppNet.Friends f = new AppNet.Friends();
        f.sharing = !HIDDEN.contains(p.getUUID());
        for (ServerPlayer q : s.getPlayerList().getPlayers()) {
            if (q == p || HIDDEN.contains(q.getUUID()) || q.isSpectator() || q.level().dimension() != p.level().dimension()
                    || CelularReg.phoneColor(q) < 0) {
                continue;
            }
            f.ids.add(q.getUUID());
            f.names.add(q.getScoreboardName());
            f.pos.add(new double[]{q.getX(), q.getY(), q.getZ(), q.getYRot()});
        }
        Net.toPlayer(p, f);
    }

    public static void logout(UUID id) {
        HIDDEN.remove(id);
    }
}

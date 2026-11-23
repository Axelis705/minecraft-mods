package com.pescacoleccion.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.torneo.Torneo;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * (Cliente) El marcador del torneo arriba a la izquierda (arriba al centro va la barra de los jefes y a la derecha los avisos de "¡Lo
 * sacaste!", que lo tapaban): modo, tiempo que queda, los 3 primeros y tu lugar.
 */
public final class TorneoHud {
    private static final String MOD = PescaColeccion.MODID;
    private static long recibido;

    private TorneoHud() {
    }

    public static void init() {
        ClientData.listen(Torneo.KEY, t -> recibido = System.currentTimeMillis());
        com.axeliscore.fx.client.Hud.add(TorneoHud::render);
    }

    /** Segundos que quedan (con el tiempo del mundo, igual que el servidor). */
    public static long resta(CompoundTag t) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && Nbt.l(t, "fin") > 0) {
            return Math.max(0, (Nbt.l(t, "fin") - mc.level.getGameTime()) / 20);
        }
        return Math.max(0, Nbt.l(t, "resta") - (System.currentTimeMillis() - recibido) / 1000);
    }

    private static void render(G g, int w, int h) {
        CompoundTag t = ClientData.get(Torneo.KEY);
        if (t == null || !Nbt.b(t, "activo") || Minecraft.getInstance().player == null) {
            return;
        }
        long s = resta(t);
        String titulo = Component.translatable(MOD + ".torneo.hud", Component.translatable(MOD + ".torneo.modo." + Nbt.s(t, "modo")),
                String.format("%d:%02d", s / 60, s % 60)).getString();
        ListTag l = Nbt.list(t, "tabla");
        UUID yo = Minecraft.getInstance().player.getUUID();
        int filas = Math.min(3, l.size());
        int mio = -1;
        for (int i = 0; i < l.size(); i++) {
            if (yo.equals(Nbt.uuid(Nbt.at(l, i), "id"))) {
                mio = i;
            }
        }
        boolean extra = mio >= 3;
        int pw = Math.max(Ui.font().width(titulo) + 16, 150), ph = 16 + (filas + (extra ? 1 : 0)) * 11 + (filas == 0 ? 11 : 0);
        int x = 4, y = 4;
        g.fill(x, y, x + pw, y + ph, 0xC0101820);
        g.fill(x, y, x + pw, y + 1, 0xFF6FB7FF);
        g.drawString(Ui.font(), titulo, x + (pw - Ui.font().width(titulo)) / 2, y + 4, s <= 10 ? 0xFFFF7A6A : 0xFFE8F4FF, true);
        int yy = y + 16;
        if (filas == 0) {
            String n = Component.translatable(MOD + ".torneo.nadie_aun").getString();
            g.drawString(Ui.font(), n, x + (pw - Ui.font().width(n)) / 2, yy, 0xFF9AA3BC, false);
        }
        int[] col = {0xFFFFD24A, 0xFFD6D9E0, 0xFFE0A070};
        for (int i = 0; i < filas; i++) {
            fila(g, Nbt.at(l, i), i, x, yy, pw, i == mio ? 0xFF7CF29A : col[i]);
            yy += 11;
        }
        if (extra) {
            fila(g, Nbt.at(l, mio), mio, x, yy, pw, 0xFF7CF29A);
        }
    }

    private static void fila(G g, CompoundTag r, int i, int x, int y, int pw, int c) {
        String a = (i + 1) + ". " + Nbt.s(r, "n");
        String b = Nbt.s(r, "p");
        g.drawString(Ui.font(), Ui.fit(a, pw - 20 - Ui.font().width(b)), x + 6, y, c, true);
        g.drawString(Ui.font(), b, x + pw - 6 - Ui.font().width(b), y, c, true);
    }
}

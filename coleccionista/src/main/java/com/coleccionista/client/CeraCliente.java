package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.coleccionista.prueba.Sentidos;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/**
 * (Cliente) Dentro de la vitrina, cuando cae la cera: la pantalla se va llenando de cera color marfil desde abajo, tiembla un poco la orilla (como
 * cera escurriendo) y al final todo se oscurece.
 */
public final class CeraCliente {
    private static CompoundTag cera = new CompoundTag();

    private CeraCliente() {
    }

    public static void init() {
        ClientData.listen(Sentidos.KEY_CERA, t -> cera = t);
        com.axeliscore.fx.client.Hud.add(CeraCliente::dibujar);
    }

    private static void dibujar(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !cera.contains("desde")) {
            return;
        }
        float t = mc.level.getGameTime() - cera.getLong("desde") + mc.getFrameTime();
        int dura = Math.max(1, Nbt.i(cera, "dura"));
        if (t < 0 || t > dura + 80) {
            return;
        }
        float p = Math.min(1F, t / dura);
        // el ámbar sube desde abajo, con la orilla ondulada
        int alto = (int) (h * p);
        long ms = System.currentTimeMillis();
        int pasos = 24;
        for (int i = 0; i < pasos; i++) {
            int x0 = w * i / pasos, x1 = w * (i + 1) / pasos;
            int ola = (int) (6 * Math.sin(ms / 300.0 + i * 0.9) + 4 * Math.sin(ms / 170.0 + i * 2.1));
            int y = h - alto + ola;
            g.fillGradient(x0, y, x1, h, 0x88E4D8B8, 0xE0C8BA92);
            g.fill(x0, y, x1, y + 2, 0xB0F6EEDA);
        }
        // y al final, se oscurece
        if (p > 0.7F) {
            int a = (int) (Math.min(1F, (p - 0.7F) / 0.3F + Math.max(0, t - dura) / 40F) * 230);
            g.fill(0, 0, w, h, Math.min(255, a) << 24 | 0x16130C);
        }
    }
}

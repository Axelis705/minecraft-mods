package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.evento.Apagon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * (Cliente) El apagón corto: las luces parpadean (tres veces, irregular), todo negro mientras "se va la corriente" y la luz regresa poco a
 * poco. Con el zumbido de la corriente al irse y al volver.
 */
public final class ApagonCliente {
    private static int visto = -1;
    private static long desde;
    private static int dura;
    private static boolean volvio;

    private ApagonCliente() {
    }

    public static void init() {
        ClientData.listen(Apagon.KEY, t -> {
            int n = Nbt.i(t, "n");
            if (n == visto) {
                return;
            }
            visto = n;
            desde = System.currentTimeMillis();
            dura = Math.max(20, Nbt.i(t, "dura")) * 50;
            volvio = false;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_DEACTIVATE, 0.6F, 0.6F));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LEVER_CLICK, 0.5F, 0.6F));
        });
        com.axeliscore.fx.client.Hud.add(ApagonCliente::dibujar);
    }

    public static boolean activo() {
        return visto >= 0 && System.currentTimeMillis() - desde < dura + 600;
    }

    private static void dibujar(G g, int w, int h) {
        if (!activo()) {
            return;
        }
        long t = System.currentTimeMillis() - desde;
        float a;
        if (t < 650) {
            // parpadeo: prende, apaga, prende… (irregular)
            int k = (int) (t / 90);
            a = (k == 1 || k == 3 || k >= 5) ? 0.96F : 0.25F;
        } else if (t < dura - 300) {
            a = 1F;
        } else {
            a = Math.max(0F, 1F - (t - (dura - 300)) / 900F);
            if (!volvio) {
                volvio = true;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 0.45F, 0.8F));
            }
        }
        g.push();
        g.translate(0, 0, 400);
        g.fill(0, 0, w, h, Ui.alpha(0xFF000000, a));
        g.pop();
    }
}

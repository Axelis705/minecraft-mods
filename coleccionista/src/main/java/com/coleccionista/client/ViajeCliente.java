package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.coleccionista.evento.Cabina;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.Random;

/**
 * (Cliente) Lo que se ve dentro de un ascensor que cae a toda velocidad ({@link Cabina.Tipo#CAIDA}): la luz parpadea y a ratos se va, una
 * luz roja de emergencia late y por las rendijas de la puerta pasan rayas de luz hacia arriba (los pisos que se quedan atrás).
 */
public final class ViajeCliente {
    private static CompoundTag viaje = new CompoundTag();
    private static final Random R = new Random();
    private static float apagon;
    private static long ultimoCambio;

    private ViajeCliente() {
    }

    public static void init() {
        ClientData.listen(Cabina.KEY_VIAJE, t -> viaje = t);
        com.axeliscore.fx.client.Hud.add(ViajeCliente::dibujar);
    }

    /** ¿Voy en un ascensor que cae? (para la prueba automática) */
    public static boolean cayendo() {
        return t() >= 0;
    }

    /** Ticks desde que arrancó el viaje que cae (-1 si no hay). */
    private static float t() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !"CAIDA".equals(Nbt.s(viaje, "tipo"))) {
            return -1;
        }
        float t = mc.level.getGameTime() - viaje.getLong("desde") + mc.getFrameTime();
        int dura = Nbt.i(viaje, "dura");
        return t < 0 || t > dura + 20 ? -1 : t;
    }

    private static void dibujar(G g, int w, int h) {
        float t = t();
        if (t < 0) {
            return;
        }
        int dura = Math.max(1, Nbt.i(viaje, "dura"));
        long ms = System.currentTimeMillis();
        // fuerza: sube rápido al arrancar y baja al frenar
        float f = Math.min(1F, t / 15F) * Math.min(1F, Math.max(0F, (dura - t) / 25F));
        // las rayas de luz que suben por las orillas (como pisos pasando por las rendijas)
        if (f > 0.05F) {
            int n = 7;
            for (int i = 0; i < n; i++) {
                float vel = 2.6F + (i % 3) * 0.7F;
                float y = (h + 60) - ((ms / 4F * vel + i * 173) % (h + 120));
                int a = (int) (f * (60 + (i % 2) * 40));
                int col = (a << 24) | 0xFFE7B0;
                int grosor = 2 + i % 3;
                g.fillGradient(0, (int) y, w / 9, (int) y + grosor, col, col & 0x00FFFFFF);
                g.fillGradient(w - w / 9, (int) y + 30, w, (int) y + 30 + grosor, col, col & 0x00FFFFFF);
            }
        }
        // la luz que parpadea: a ratos casi negro
        if (ms - ultimoCambio > 60 + R.nextInt(160)) {
            ultimoCambio = ms;
            apagon = R.nextFloat() < 0.35F * f ? 0.55F + R.nextFloat() * 0.35F : R.nextFloat() * 0.15F * f;
        }
        if (apagon > 0.01F) {
            g.fill(0, 0, w, h, ((int) (apagon * 255) << 24));
        }
        // la luz roja de emergencia, latiendo
        float rojo = (float) (0.5 + 0.5 * Math.sin(ms / 180.0)) * 0.22F * f;
        if (rojo > 0.01F) {
            int a = (int) (rojo * 255);
            g.fillGradient(0, 0, w, h / 3, (a << 24) | 0xB00000, 0x00B00000);
            g.fillGradient(0, h - h / 3, w, h, 0x00B00000, (a << 24) | 0xB00000);
        }
        // viñeta oscura alrededor
        int v = (int) (120 * f);
        g.fillGradient(0, 0, w, h / 6, v << 24, 0);
        g.fillGradient(0, h - h / 6, w, h, 0, v << 24);
    }
}

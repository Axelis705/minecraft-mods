package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.coleccionista.evento.Cabina;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.Random;

/**
 * (Cliente) Lo que se ve dentro de un vehículo que viaja ({@link Cabina}):
 * <ul>
 *     <li>El ascensor que cae ({@link Cabina.Tipo#CAIDA}): la luz parpadea y a ratos se va, una luz roja de emergencia late y por las
 *     rendijas de la puerta pasan rayas de luz hacia arriba (los pisos que se quedan atrás).</li>
 *     <li>El autobús ({@link Cabina.Tipo#AUTOBUS}): de noche; cada tanto pasa por las ventanas la luz anaranjada de un farol de la calle
 *     (de adelante hacia atrás), y el foco del techo tiembla con los baches.</li>
 *     <li>El montacargas ({@link Cabina.Tipo#MONTACARGAS}): franjas de luz de cada piso que bajan despacio (la jaula sube) y el foco que se
 *     mece.</li>
 * </ul>
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
        return t("CAIDA") >= 0;
    }

    /** ¿Voy en un viaje de ese tipo ahora mismo? (para la prueba automática) */
    public static boolean viaje(String tipo) {
        return t(tipo) >= 0;
    }

    /** Ticks desde que arrancó el viaje de ese tipo (-1 si no hay). */
    private static float t(String tipo) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !tipo.equals(Nbt.s(viaje, "tipo"))) {
            return -1;
        }
        float t = mc.level.getGameTime() - viaje.getLong("desde") + mc.getFrameTime();
        int dura = Nbt.i(viaje, "dura");
        return t < 0 || t > dura + 20 ? -1 : t;
    }

    private static void dibujar(G g, int w, int h) {
        if (t("AUTOBUS") >= 0) {
            autobus(g, w, h, t("AUTOBUS"));
            return;
        }
        if (t("MONTACARGAS") >= 0) {
            montacargas(g, w, h, t("MONTACARGAS"));
            return;
        }
        float t = t("CAIDA");
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

    /** El autobús de noche: los faroles de la calle pasan por las ventanas (una franja de luz anaranjada que cruza) y la luz tiembla. */
    private static void autobus(G g, int w, int h, float t) {
        int dura = Math.max(1, Nbt.i(viaje, "dura"));
        // se mueve: arranca despacio y frena al final
        float f = Math.min(1F, t / 40F) * Math.min(1F, Math.max(0F, (dura - t) / 30F));
        if (f <= 0.02F) {
            return;
        }
        long ms = System.currentTimeMillis();
        // cada farol: una franja ancha y suave que cruza la pantalla de derecha a izquierda (más rápido a toda velocidad)
        float periodo = 1700F - 500F * f;
        for (int n = 0; n < 2; n++) {
            float fase = ((ms + n * periodo * 0.5F) % periodo) / periodo;
            float cx = w * (1.3F - 1.6F * fase);
            int ancho = w / 3;
            int a = (int) (f * (n == 0 ? 46 : 26));
            int col = (a << 24) | 0xFFB060;
            horizontal(g, (int) (cx - ancho), (int) cx, h, 0x00FFB060, col);
            horizontal(g, (int) cx, (int) (cx + ancho), h, col, 0x00FFB060);
        }
        // el foco del techo: tiembla con los baches (a ratos baja un poco)
        float tiembla = (float) (Math.sin(ms / 70.0) * Math.sin(ms / 230.0));
        if (tiembla > 0.6F) {
            g.fill(0, 0, w, h, ((int) (f * 40 * (tiembla - 0.6F) / 0.4F) << 24));
        }
        // la noche alrededor (las orillas más oscuras)
        int v = (int) (90 * f);
        g.fillGradient(0, 0, w, h / 5, v << 24, 0);
        g.fillGradient(0, h - h / 5, w, h, 0, v << 24);
    }

    /** El montacargas que sube: las franjas de luz de los pisos bajan despacio por la reja y el foco se mece. */
    private static void montacargas(G g, int w, int h, float t) {
        int dura = Math.max(1, Nbt.i(viaje, "dura"));
        float f = Math.min(1F, t / 20F) * Math.min(1F, Math.max(0F, (dura - t) / 20F));
        if (f <= 0.02F) {
            return;
        }
        long ms = System.currentTimeMillis();
        // cada piso que pasa: una franja de luz con la sombra de los barrotes que baja de arriba hacia abajo
        float periodo = 2600F;
        float fase = (ms % (long) periodo) / periodo;
        int y = (int) (-h * 0.3F + fase * h * 1.6F);
        int alto = h / 5, a = (int) (f * 50);
        g.fillGradient(0, y, w, y + alto / 2, 0x00FFD9A0, (a << 24) | 0xFFD9A0);
        g.fillGradient(0, y + alto / 2, w, y + alto, (a << 24) | 0xFFD9A0, 0x00FFD9A0);
        for (int x = 0; x < w; x += Math.max(8, w / 14)) {
            g.fill(x, y, x + Math.max(2, w / 90), y + alto, (int) (f * 70) << 24);
        }
        // entre piso y piso, más oscuro; y el foco que se mece
        float mece = (float) (0.5 + 0.5 * Math.sin(ms / 420.0));
        g.fill(0, 0, w, h, ((int) (f * (35 + 30 * mece)) << 24));
    }

    /** Un degradado horizontal (de izquierda a derecha) en columnas delgadas. */
    private static void horizontal(G g, int x0, int x1, int h, int c0, int c1) {
        if (x1 <= x0) {
            return;
        }
        int paso = Math.max(2, (x1 - x0) / 24);
        for (int x = x0; x < x1; x += paso) {
            float k = (x - x0) / (float) (x1 - x0);
            g.fill(x, 0, Math.min(x1, x + paso), h, mezcla(c0, c1, k));
        }
    }

    private static int mezcla(int a, int b, float k) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = (a >>> s) & 0xFF, cb = (b >>> s) & 0xFF;
            r |= ((int) (ca + (cb - ca) * k) & 0xFF) << s;
        }
        return r;
    }
}

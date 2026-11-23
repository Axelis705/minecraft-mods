package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Montacargas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * (Cliente) Lo que se ve en el viaje del montacargas: el gas (todo se pone verde y la cámara se marea antes de quedar a negro) y, durante
 * el viaje, la carátula de latón con la aguja que baja hacia la siguiente parada.
 */
public final class MontacargasCliente {
    private static final String MOD = Coleccionista.MODID;
    private static String fase = "NADA", zona = "";
    private static long desde;
    private static int dura;

    private MontacargasCliente() {
    }

    public static void init() {
        ClientData.listen(Montacargas.KEY, t -> {
            fase = Nbt.s(t, "fase");
            zona = Nbt.s(t, "zona");
            dura = Nbt.i(t, "dura");
            desde = System.currentTimeMillis();
        });
        com.axeliscore.fx.client.Hud.add(MontacargasCliente::hud);
        MinecraftForge.EVENT_BUS.addListener(MontacargasCliente::camara);
    }

    public static String fase() {
        return fase;
    }

    /** 0..1: qué tanto va de la parte actual. */
    private static float avance() {
        return dura <= 0 ? 1F : Math.min(1F, (System.currentTimeMillis() - desde) / (dura * 50F));
    }

    private static boolean viajo() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && EventoCliente.pieza(mc.player.getUUID()) != null;
    }

    /** El mareo del gas: la cámara se ladea y cabecea, cada vez más. */
    private static void camara(ViewportEvent.ComputeCameraAngles e) {
        if (!"GAS".equals(fase) || !viajo()) {
            return;
        }
        float p = avance();
        double t = System.currentTimeMillis() / 1000.0;
        e.setRoll(e.getRoll() + (float) (Math.sin(t * 2.1) * 7 * p));
        e.setPitch(e.getPitch() + (float) (Math.sin(t * 1.4 + 1) * 3 * p));
        e.setYaw(e.getYaw() + (float) (Math.sin(t * 0.9) * 2 * p));
    }

    private static void hud(G g, int w, int h) {
        if (!viajo()) {
            return;
        }
        if ("GAS".equals(fase)) {
            float p = avance();
            float latido = (float) (0.75 + 0.25 * Math.sin(System.currentTimeMillis() / 170.0));
            g.fill(0, 0, w, h, Ui.alpha(0xFF4E7A1E, 0.42F * p * latido));
            // la vista se nubla por las orillas
            int b = (int) (h * 0.22F * p);
            g.fillGradient(0, 0, w, b, Ui.alpha(0xFF101A06, 0.8F * p), 0x00101A06);
            g.fillGradient(0, h - b, w, h, 0x00101A06, Ui.alpha(0xFF101A06, 0.8F * p));
        } else if ("VIAJE".equals(fase)) {
            caratula(g, w);
        }
    }

    /** La carátula del montacargas: un medio círculo de latón con la aguja que va de la izquierda a la derecha (de arriba a la parada). */
    private static void caratula(G g, int w) {
        Font f = Ui.font();
        float p = avance();
        float ap = Math.min(1F, (System.currentTimeMillis() - desde) / 400F);
        int cx = w / 2, cy = Math.max(EventoHud.abajoHud(), 4) + 36, r = 26;
        int laton = Ui.alpha(0xFFB08A3A, ap), oscuro = Ui.alpha(0xF0140E0A, ap);
        // la carátula (puntos de un medio círculo relleno)
        for (int dy = 0; dy <= r + 3; dy++) {
            int dx = (int) Math.sqrt(Math.max(0, (r + 3) * (r + 3) - dy * dy));
            g.fill(cx - dx, cy - dy, cx + dx + 1, cy - dy + 1, laton);
        }
        for (int dy = 0; dy <= r; dy++) {
            int dx = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
            g.fill(cx - dx, cy - dy, cx + dx + 1, cy - dy + 1, oscuro);
        }
        g.fill(cx - r - 3, cy, cx + r + 4, cy + 3, laton);
        // las marcas de los pisos
        for (int i = 0; i <= 8; i++) {
            double a = Math.PI - i * Math.PI / 8;
            int x = cx + (int) Math.round(Math.cos(a) * (r - 4)), y = cy - (int) Math.round(Math.sin(a) * (r - 4));
            g.fill(x - 1, y - 1, x + 1, y + 1, i == 8 ? Ui.alpha(0xFFE04040, ap) : Ui.alpha(0xFFD8C8A0, ap));
        }
        // la aguja (con un temblor)
        double temblor = Math.sin(System.currentTimeMillis() / 60.0) * 0.03;
        double a = Math.PI - (p * Math.PI) + temblor;
        for (int k = 0; k < r - 6; k++) {
            int x = cx + (int) Math.round(Math.cos(a) * k), y = cy - (int) Math.round(Math.sin(a) * k);
            g.fill(x, y, x + 1, y + 1, Ui.alpha(0xFFE04040, ap));
        }
        g.fill(cx - 2, cy - 2, cx + 3, cy + 1, laton);
        String t = Component.translatable(MOD + ".mon.hud.titulo").getString();
        String s = Component.translatable(MOD + ".mon.hud.parada", Component.translatable(MOD + ".mon.zona." + zona)).getString();
        g.drawCenteredString(f, t, cx, cy + 6, Ui.alpha(0xFFB08A3A, ap));
        g.drawCenteredString(f, s, cx, cy + 17, Ui.alpha(0xFFF0E8E0, ap));
    }
}

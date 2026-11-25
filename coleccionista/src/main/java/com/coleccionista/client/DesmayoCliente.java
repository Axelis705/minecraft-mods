package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Sentidos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * (Cliente) El último equipo, en su cuarto de la salida: el gas (todo se tiñe de verde, la vista se mece, tose, el corazón late cada vez
 * más fuerte, la cabeza se va de lado y se cierran los ojos) y luego el despertar en la vitrina (los párpados se abren despacio, borroso,
 * dos parpadeos) con un mensaje abajo.
 */
public final class DesmayoCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag d = new CompoundTag();
    private static long ultimoSonido;

    private DesmayoCliente() {
    }

    public static void init() {
        ClientData.listen(Sentidos.KEY_GAS, t -> d = t);
        com.axeliscore.fx.client.Hud.add(DesmayoCliente::dibujar);
        MinecraftForge.EVENT_BUS.addListener(DesmayoCliente::camara);
    }

    /** Ticks desde que empezó lo que toca (-1 si nada). */
    private static float t() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !d.contains("desde")) {
            return -1;
        }
        float t = mc.level.getGameTime() - d.getLong("desde") + mc.getFrameTime();
        return t < 0 || t > 420 ? -1 : t;
    }

    private static boolean gas() {
        return "gas".equals(Nbt.s(d, "fase"));
    }

    /** Para la prueba automática: ¿estoy desmayado o despertando? */
    public static boolean activo() {
        return t() >= 0;
    }

    private static void camara(ViewportEvent.ComputeCameraAngles e) {
        float t = t();
        if (t < 0) {
            return;
        }
        float dura = Math.max(1, Nbt.i(d, "dura"));
        if (gas()) {
            // se mece cada vez más y al final la cabeza se va de lado y hacia abajo
            float k = Math.min(1F, t / dura);
            float mece = (float) Math.sin(t * 0.13F) * 7F * k + (float) Math.sin(t * 0.31F) * 2F * k;
            float cae = Mth.clamp((t - dura * 0.75F) / (dura * 0.25F), 0F, 1F);
            e.setRoll(mece + 28F * cae * cae);
            e.setPitch(e.getPitch() + 22F * cae * cae);
        } else {
            // al despertar: un poco de lado, se va enderezando
            float k = Mth.clamp(1F - t / dura, 0F, 1F);
            e.setRoll(14F * k * (float) Math.sin(t * 0.05F + 1F));
        }
    }

    private static void dibujar(G g, int w, int h) {
        float t = t();
        if (t < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float dura = Math.max(1, Nbt.i(d, "dura"));
        if (gas()) {
            float k = Math.min(1F, t / dura);
            // el verde que sube y la orilla oscura
            g.fillGradient(0, 0, w, h, Ui.alpha(0xFF2E5A12, 0.18F + 0.32F * k), Ui.alpha(0xFF4C7A1C, 0.28F + 0.4F * k));
            int borde = (int) (Math.min(w, h) * 0.35F * k);
            for (int i = 0; i < 8; i++) {
                int a = (int) (k * 26);
                int o = borde * i / 8;
                g.fill(0, 0, w, o, a << 24);
                g.fill(0, h - o, w, h, a << 24);
            }
            // los ojos se cierran (párpados de arriba y de abajo), con un par de intentos por abrirlos
            float cierra = Mth.clamp((t - dura * 0.55F) / (dura * 0.45F), 0F, 1F);
            cierra = Math.min(1F, cierra + 0.25F * (float) Math.max(0, Math.sin(t * 0.25F)) * cierra);
            if (t > dura) {
                cierra = 1F;
            }
            parpados(g, w, h, cierra);
            // tos y el corazón
            long ahora = System.currentTimeMillis();
            int cada = (int) Math.max(380, 1100 - 700 * k);
            if (ahora - ultimoSonido > cada && t < dura + 20) {
                ultimoSonido = ahora;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 1F, 0.6F + 0.4F * k));
                if (mc.level != null && mc.level.random.nextFloat() < 0.35F && k < 0.85F) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_HURT_DROWN, 0.9F, 0.35F));
                }
            }
        } else {
            // despierta: negro, se abren poco, parpadea, se abren del todo; borroso al principio
            float abre;
            if (t < 30) {
                abre = 0;
            } else if (t < 55) {
                abre = (t - 30) / 25F * 0.35F;
            } else if (t < 66) {
                abre = 0.35F * (1 - (t - 55) / 11F);
            } else if (t < 110) {
                abre = (t - 66) / 44F;
            } else {
                abre = 1F;
            }
            float borroso = Mth.clamp(1F - (t - 66) / 80F, 0F, 1F);
            if (borroso > 0.01F) {
                g.fill(0, 0, w, h, Ui.alpha(0xFF9A9488, 0.55F * borroso));
            }
            parpados(g, w, h, 1F - abre);
            // el mensaje, abajo
            float a = Mth.clamp((t - 95) / 20F, 0F, 1F) * Mth.clamp((dura + 120 - t) / 30F, 0F, 1F);
            if (a > 0.02F) {
                Font f = Ui.font();
                String l1 = Component.translatable(MOD + ".gas.msg1").getString(), l2 = Component.translatable(MOD + ".gas.msg2").getString();
                int y = h - 78;
                int ancho = Math.max(f.width(l1) * 2, f.width(l2)) + 24;
                g.fillGradient(w / 2 - ancho / 2, y - 6, w / 2 + ancho / 2, y + 36, Ui.alpha(0xFF140C0E, 0.75F * a), Ui.alpha(0xFF080506, 0.6F * a));
                g.fill(w / 2 - ancho / 2, y + 35, w / 2 + ancho / 2, y + 36, Ui.alpha(0xFFB3121A, a));
                g.push();
                g.translate(w / 2F, y, 0);
                g.scale(2F, 2F, 1F);
                g.drawString(f, l1, -f.width(l1) / 2, 0, Ui.alpha(0xFFE8E2D8, a), true);
                g.pop();
                g.drawCenteredString(f, l2, w / 2, y + 22, Ui.alpha(0xFFB8A898, a));
            }
        }
    }

    /** Los párpados: 0 abiertos, 1 cerrados (negro). Con la orilla suave. */
    private static void parpados(G g, int w, int h, float cierra) {
        if (cierra <= 0.001F) {
            return;
        }
        int mitad = (int) (h / 2F * cierra);
        g.fill(0, 0, w, mitad, 0xFF000000);
        g.fill(0, h - mitad, w, h, 0xFF000000);
        for (int i = 0; i < 12; i++) {
            int a = 200 - i * 16;
            g.fill(0, mitad + i * 2, w, mitad + i * 2 + 2, a << 24);
            g.fill(0, h - mitad - i * 2 - 2, w, h - mitad - i * 2, a << 24);
        }
    }
}

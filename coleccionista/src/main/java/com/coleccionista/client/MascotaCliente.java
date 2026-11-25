package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.forma.client.FormaGeo;
import com.axeliscore.forma.client.FormasCliente;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Mascota;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

/**
 * (Cliente) La mascota: Cascabel se dibuja con su modelo (lo hace la forma del Core), el que es Cascabel ve sus teclas (rugir y olfatear)
 * y se pone la cámara detrás para verse; a los escondidos se les acelera el corazón y se les enrojece la orilla cuando la tienen cerca; y a
 * quien atrapa, la cinemática: se queda quieto, lo jala, se cierran los dientes y todo se pone negro.
 */
public final class MascotaCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag d = new CompoundTag(), comido = new CompoundTag();
    private static long ultimoLatido, rugidoListo, olfatoListo;
    private static boolean eraCazador;
    private static CameraType camaraAntes;

    private MascotaCliente() {
    }

    public static void init() {
        FormasCliente.registrar(Mascota.FORMA, new FormaGeo(Ids.rl(MOD, "geo/cascabel.geo.json"), Ids.rl(MOD, "textures/entity/cascabel.png"),
                Ids.rl(MOD, "animations/cascabel.animation.json")).escala(1.1F).rapidez(0.8F, 5.2F));
        ClientData.listen(Mascota.KEY, t -> {
            d = t;
            camara();
        });
        ClientData.listen(Mascota.KEY_COMIDO, t -> comido = t);
        com.axeliscore.fx.client.Hud.add(MascotaCliente::dibujar);
        MinecraftForge.EVENT_BUS.addListener(MascotaCliente::angulos);
    }

    public static String fase() {
        return Nbt.s(d, "fase");
    }

    private static boolean activa() {
        String f = fase();
        return !f.isEmpty() && !"NADA".equals(f);
    }

    private static UUID cazador() {
        return d.hasUUID("cazador") ? d.getUUID("cazador") : null;
    }

    /** ¿Soy Cascabel? */
    public static boolean soyCazador() {
        Minecraft mc = Minecraft.getInstance();
        UUID c = cazador();
        return mc.player != null && c != null && c.equals(mc.player.getUUID()) && ("CAZA".equals(fase()) || "FIN".equals(fase()));
    }

    /** Al volverse Cascabel la cámara se pone detrás (para que se vea); al volver a ser él, regresa como estaba. */
    private static void camara() {
        Minecraft mc = Minecraft.getInstance();
        boolean soy = soyCazador();
        if (soy && !eraCazador) {
            camaraAntes = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!soy && eraCazador && camaraAntes != null) {
            mc.options.setCameraType(camaraAntes);
            camaraAntes = null;
        }
        eraCazador = soy;
    }

    // ------------------------------------------------------------------ las teclas de Cascabel

    public static void rugir() {
        tecla("rugir");
    }

    public static void olfatear() {
        tecla("olfatear");
    }

    private static void tecla(String que) {
        Minecraft mc = Minecraft.getInstance();
        if (!soyCazador() || !"CAZA".equals(fase()) || mc.level == null || mc.screen != null) {
            return;
        }
        long now = mc.level.getGameTime();
        if ("rugir".equals(que)) {
            if (now < rugidoListo) {
                return;
            }
            rugidoListo = now + 120;
        } else {
            if (now < olfatoListo) {
                return;
            }
            olfatoListo = now + 300;
        }
        CompoundTag t = new CompoundTag();
        t.putString("que", que);
        ClientData.send(Mascota.ACCION, t);
    }

    // ------------------------------------------------------------------ a quien se comen

    /** Ticks desde que lo atrapó (-1: no me están comiendo). */
    private static float tComido() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !comido.contains("desde")) {
            return -1;
        }
        float t = mc.level.getGameTime() - comido.getLong("desde") + mc.getFrameTime();
        return t < 0 || t > 90 ? -1 : t;
    }

    /** Para la prueba automática. */
    public static boolean meEstanComiendo() {
        return tComido() >= 0;
    }

    private static void angulos(ViewportEvent.ComputeCameraAngles e) {
        float t = tComido();
        if (t < 0) {
            return;
        }
        // me sacude de un lado a otro (como un perro con un trapo) y luego todo se va hacia arriba
        if (t < 20) {
            float k = Mth.clamp(t / 8F, 0F, 1F);
            e.setRoll((float) Math.sin(t * 1.3F) * 14F * k);
            e.setYaw(e.getYaw() + (float) Math.sin(t * 1.1F) * 9F * k);
        } else {
            e.setRoll((float) Math.sin(t * 0.4F) * 5F);
            e.setPitch(e.getPitch() - Math.min(30F, (t - 20) * 2F));
        }
    }

    // ------------------------------------------------------------------ HUD y avisos

    private static void dibujar(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float tc = tComido();
        if (tc >= 0) {
            dientes(g, w, h, tc);
            return;
        }
        if (!activa()) {
            return;
        }
        if (soyCazador()) {
            if ("CAZA".equals(fase())) {
                hudCazador(g, w, h);
            }
            return;
        }
        if (!EventoHud.visible() || Presentacion.activa() || Presentacion.reglasVisibles()) {
            return;
        }
        CompoundTag yo = EventoCliente.yo();
        boolean pieza = yo != null && EventoCliente.vivo(yo);
        String f = fase();
        if (pieza && "ESCONDER".equals(f)) {
            EventoHud.lado(g, Component.translatable(MOD + ".man.hud.esconder").getString(), 0xFFE0B050, true);
            EventoHud.lado(g, Component.translatable(MOD + ".man.hud.consejo").getString(), 0xFFA89E94, false);
        } else if (pieza && "CAZA".equals(f)) {
            EventoHud.lado(g, Component.translatable(MOD + ".man.hud.caza").getString(), 0xFFE04040, true);
            cerca(g, w, h, mc);
        } else if ("CAZA".equals(f)) {
            EventoHud.lado(g, Component.translatable(MOD + ".man.hud.comidos", Nbt.i(d, "comidos")).getString(), 0xFFA89E94, false);
        }
    }

    /** Si Cascabel anda cerca: el corazón late más rápido y la orilla se pone roja. */
    private static void cerca(G g, int w, int h, Minecraft mc) {
        UUID c = cazador();
        Player b = c == null || mc.level == null ? null : mc.level.getPlayerByUUID(c);
        if (b == null) {
            return;
        }
        double dist = b.distanceTo(mc.player);
        if (dist > 16) {
            return;
        }
        float k = (float) (1 - dist / 16);
        float pulso = 0.6F + 0.4F * (float) Math.abs(Math.sin(System.currentTimeMillis() / (420.0 - 240 * k)));
        int borde = (int) (Math.min(w, h) * (0.12F + 0.2F * k));
        for (int i = 0; i < 10; i++) {
            int a = (int) (k * pulso * 22);
            int o = borde * (i + 1) / 10;
            int col = (a << 24) | 0x6A0008;
            g.fill(0, 0, w, o, col);
            g.fill(0, h - o, w, h, col);
            g.fill(0, 0, o, h, col);
            g.fill(w - o, 0, w, h, col);
        }
        long now = System.currentTimeMillis();
        int cada = (int) (1150 - 750 * k);
        if (now - ultimoLatido > cada) {
            ultimoLatido = now;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.5F + 0.6F * k, 0.8F + 0.4F * k));
        }
    }

    private static void hudCazador(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        Font f = Ui.font();
        String t1 = Component.translatable(MOD + ".man.hud.eres").getString();
        String t2 = Component.translatable(MOD + ".man.hud.golpea").getString();
        String r = rugidoListo > now ? Component.translatable(MOD + ".man.hud.espera", (rugidoListo - now + 19) / 20).getString()
                : Component.translatable(MOD + ".man.hud.listo").getString();
        String o = olfatoListo > now ? Component.translatable(MOD + ".man.hud.espera", (olfatoListo - now + 19) / 20).getString()
                : Component.translatable(MOD + ".man.hud.listo").getString();
        String t3 = Component.translatable(MOD + ".man.hud.rugir", tecla(0), r).getString();
        String t4 = Component.translatable(MOD + ".man.hud.olfatear", tecla(1), o).getString();
        String t5 = Component.translatable(MOD + ".man.hud.comidos", Nbt.i(d, "comidos")).getString();
        int ancho = Math.max(Math.max(f.width(t1), f.width(t2)), Math.max(Math.max(f.width(t3), f.width(t4)), f.width(t5))) + 16;
        int x = 6, y = h - 70 - 6;
        g.fillGradient(x, y, x + ancho, y + 66, 0xD0180806, 0xB00A0403);
        g.fill(x, y, x + 2, y + 66, 0xFFD9A93A);
        g.drawString(f, t1, x + 8, y + 5, 0xFFD9A93A, true);
        g.drawString(f, t2, x + 8, y + 17, 0xFFD8D0C8, true);
        g.drawString(f, t3, x + 8, y + 29, rugidoListo > now ? 0xFF8A8078 : 0xFFE8E2D8, true);
        g.drawString(f, t4, x + 8, y + 41, olfatoListo > now ? 0xFF8A8078 : 0xFFE8E2D8, true);
        g.drawString(f, t5, x + 8, y + 53, 0xFFA89E94, true);
    }

    /** La letra de la tecla (0 rugir, 1 olfatear) como la tenga puesta. */
    private static String tecla(int i) {
        String n = i == 0 ? "rugir" : "olfatear";
        for (com.axeliscore.ui.Teclas.Tecla k : com.axeliscore.ui.Teclas.TODAS) {
            if (k.modid.equals(MOD) && k.nombre.equals(n) && k.mapping instanceof net.minecraft.client.KeyMapping m) {
                return m.getTranslatedKeyMessage().getString();
            }
        }
        return i == 0 ? "G" : "H";
    }

    /** Lo atrapó: primero un golpe rojo, luego se le cierran los dientes de arriba y de abajo y queda en lo oscuro. */
    private static void dientes(G g, int w, int h, float t) {
        Font f = Ui.font();
        if (t < 8) {
            float k = 1 - t / 8F;
            g.fill(0, 0, w, h, Ui.alpha(0xFFB00010, 0.25F + 0.35F * k));
        }
        // los dientes cierran entre 10 y 22
        float cierra = Mth.clamp((t - 10) / 12F, 0F, 1F);
        cierra = cierra * cierra * (3 - 2 * cierra);
        if (t >= 8) {
            int boca = (int) (h * 0.5F * cierra);
            // el interior (rojo oscuro) entra por arriba y por abajo
            g.fill(0, 0, w, boca, 0xFF2A0406);
            g.fill(0, h - boca, w, h, 0xFF2A0406);
            int n = 9, dw = Math.max(8, w / n);
            int dh = (int) (h * 0.13F);
            for (int i = 0; i < n + 1; i++) {
                int x0 = i * dw - dw / 2 + (i % 2) * 3;
                // de arriba: puntas hacia abajo (escalones que se angostan)
                for (int s = 0; s < 6; s++) {
                    int a = dw * (6 - s) / 14;
                    g.fill(x0 + dw / 2 - a, boca + s * dh / 6, x0 + dw / 2 + a, boca + (s + 1) * dh / 6, 0xFFE8DFC8);
                }
                int x1 = x0 + dw / 2;
                for (int s = 0; s < 6; s++) {
                    int a = dw * (6 - s) / 14;
                    g.fill(x1 + dw / 2 - a, h - boca - (s + 1) * dh / 6, x1 + dw / 2 + a, h - boca - s * dh / 6, 0xFFE0D6BC);
                }
            }
        }
        if (t >= 22) {
            float k = Mth.clamp((t - 22) / 10F, 0F, 1F);
            g.fill(0, 0, w, h, Ui.alpha(0xFF050102, k));
            String s = Component.translatable(MOD + ".man.comido").getString();
            int a = (int) (Mth.clamp((t - 26) / 10F, 0F, 1F) * 255);
            if (a > 8) {
                g.drawCenteredString(f, s, w / 2, h / 2 - 4, (a << 24) | 0xC0202A);
            }
        }
    }
}

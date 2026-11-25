package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Traidor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * (Cliente) El traidor: la carta secreta del papel de cada uno (todos la ven al mismo tiempo: LEAL o TRAIDOR), el recordatorio en el
 * HUD, los intentos que le quedan a tu sala y la pantalla del traidor para elegir víctima.
 */
public final class TraidorCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag(), mia = new CompoundTag();
    private static String rolVisto = "";
    private static long cartaDesde;

    private TraidorCliente() {
    }

    public static void init() {
        ClientData.listen(Traidor.KEY, t -> estado = t);
        ClientData.listen(Traidor.KEY_MIA, t -> {
            mia = t;
            String rol = Nbt.s(t, "rol");
            if (!rol.isEmpty() && !rol.equals(rolVisto)) {
                rolVisto = rol;
                cartaDesde = System.currentTimeMillis();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.8F, 1F));
            }
            if (rol.isEmpty()) {
                rolVisto = "";
            }
        });
        com.axeliscore.fx.client.Hud.add(TraidorCliente::hud);
        com.axeliscore.ui.ScreenRegistry.register(Traidor.PANTALLA_TECLADO, TecladoScreen::new);
        com.axeliscore.ui.ScreenRegistry.register(Traidor.PANTALLA_VICTIMA, VictimaScreen::new);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    public static String rol() {
        return Nbt.s(mia, "rol");
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!activa() || mc.player == null) {
            return;
        }
        Font f = Ui.font();
        // la carta del papel (7 s): negro, y en medio LEAL o TRAIDOR
        float t = (System.currentTimeMillis() - cartaDesde) / 1000F;
        if (cartaDesde > 0 && t < 7F && !rolVisto.isEmpty()) {
            float a = suave(t / 0.6F) * (1 - suave((t - 6.2F) / 0.8F));
            boolean traidor = "traidor".equals(rolVisto);
            g.fill(0, 0, w, h, Ui.alpha(0xFF000000, 0.85F * a));
            EventoHud.texto(g, f, Component.translatable(MOD + ".tra.eres").getString(), w / 2, h / 2 - 46, 1.4F, Ui.alpha(0xFFA89E96, a));
            EventoHud.texto(g, f, Component.translatable(MOD + (traidor ? ".tra.rol_traidor" : ".tra.rol_leal")).getString(), w / 2, h / 2 - 26,
                    4F, Ui.alpha(traidor ? EventoHud.ROJO_CL : EventoHud.PORCELANA, a));
            Ui.text(g, Component.translatable(MOD + (traidor ? ".tra.explica_traidor" : ".tra.explica_leal")).getString(), w / 2 - 150, h / 2 + 22,
                    300, Ui.alpha(0xFFD8D0C8, a), 11);
            return;
        }
        if (!EventoHud.visible()) {
            return;
        }
        String linea = null;
        int color = 0xFFD8D0C8;
        if ("BUSCAR".equals(fase()) && !rol().isEmpty()) {
            linea = Component.translatable(MOD + ".tra.hud.buscar", Nbt.i(mia, "sala"), Nbt.i(mia, "intentos")).getString();
        } else if ("LISTA".equals(fase()) || "HABLANDO".equals(fase())) {
            linea = Component.translatable(MOD + ".tra.hud.lista").getString();
        }
        if (Nbt.b(mia, "elige")) {
            linea = Component.translatable(MOD + ".tra.hud.elige").getString();
            color = EventoHud.ROJO_CL;
        }
        if (linea != null) {
            EventoHud.lado(g, linea, color, true);
        }
        // recordatorio chiquito del papel (abajo a la izquierda, discreto)
        if ("traidor".equals(rol()) && "BUSCAR".equals(fase())) {
            g.drawString(f, Component.translatable(MOD + ".tra.hud.eres_traidor").getString(), 6, h - 12, 0x80E0404A, false);
        }
    }
}

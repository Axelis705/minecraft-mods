package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.LasPuertas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * (Cliente) Las puertas: la ronda, la pista del Coleccionista (en su color) mientras eligen, el redoble y qué puerta tenía la trampa.
 */
public final class PuertasCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag();

    private PuertasCliente() {
    }

    public static void init() {
        ClientData.listen(LasPuertas.KEY, t -> estado = t);
        com.axeliscore.fx.client.Hud.add(PuertasCliente::hud);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    public static int ronda() {
        return Nbt.i(estado, "ronda");
    }

    public static int rondas() {
        return Nbt.i(estado, "rondas");
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!activa() || mc.player == null || !EventoHud.visible()) {
            return;
        }
        Font f = Ui.font();
        String fase = fase();
        String linea = null;
        int color = 0xFFD8D0C8;
        switch (fase) {
            case "ELEGIR" -> {
                String pc = Component.translatable(MOD + ".pue.color." + Nbt.s(estado, "pista_color")).getString();
                linea = Component.translatable(MOD + ".pue." + Nbt.s(estado, "pista"), pc).getString();
                color = Nbt.i(estado, "pista_rgb") | 0xFF000000;
                String r = Component.translatable(MOD + ".pue.hud.elige", ronda(), rondas()).getString();
                g.drawCenteredString(f, r, w / 2, 58, 0xFFA89E96);
            }
            case "REDOBLE" -> linea = Component.translatable(MOD + ".pue.hud.redoble").getString();
            case "CAE", "ENTRE" -> {
                int k = Nbt.i(estado, "trampa");
                String c = Component.translatable(MOD + ".pue.color." + Nbt.s(estado, "color" + k)).getString();
                linea = Component.translatable(MOD + ".pue.hud.trampa", c).getString();
                color = EventoHud.ROJO_CL;
            }
            case "LISTA" -> linea = Component.translatable(MOD + ".pue.hud.lista", rondas()).getString();
            default -> {
            }
        }
        if (linea != null) {
            int lw = f.width(linea) + 16, y = EventoHud.lineaY(w, lw);
            g.fillGradient(w / 2 - lw / 2, y, w / 2 + lw / 2, y + 13, 0xB0140C0E, 0x90080506);
            g.drawCenteredString(f, linea, w / 2, y + 3, color);
        }
    }
}

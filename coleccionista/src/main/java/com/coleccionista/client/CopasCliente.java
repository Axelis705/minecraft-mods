package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.LasCopas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * (Cliente) La final, Las copas: de quién es el turno (y si es el tuyo, en grande), cuántas copas quedan, y el suspenso mientras alguien
 * bebe ("Pieza 03 bebió…" con latidos).
 */
public final class CopasCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag();

    private CopasCliente() {
    }

    public static void init() {
        ClientData.listen(LasCopas.KEY, t -> estado = t);
        com.axeliscore.fx.client.Hud.add(CopasCliente::hud);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    public static boolean miTurno() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && "TURNO".equals(fase()) && estado.hasUUID("turno") && estado.getUUID("turno").equals(mc.player.getUUID());
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
            case "TURNO" -> {
                linea = Component.translatable(MOD + ".cop.hud.turno", Nbt.s(estado, "turno_nombre"), Nbt.i(estado, "copas")).getString();
                if (miTurno()) {
                    float p = (float) (0.7 + 0.3 * Math.sin(System.currentTimeMillis() / 250.0));
                    String s = Component.translatable(MOD + ".cop.hud.tu_turno").getString();
                    EventoHud.texto(g, f, s, w / 2, h - 96, 1.6F, Ui.alpha(EventoHud.ORO, p));
                }
            }
            case "BEBE" -> {
                linea = Component.translatable(MOD + ".cop.hud.bebe", Nbt.s(estado, "bebe_nombre")).getString();
                color = EventoHud.ROJO_CL;
            }
            case "SIRVE" -> linea = Component.translatable(MOD + ".cop.hud.sirve").getString();
            case "LISTA", "HABLANDO" -> linea = Component.translatable(MOD + ".cop.hud.lista").getString();
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

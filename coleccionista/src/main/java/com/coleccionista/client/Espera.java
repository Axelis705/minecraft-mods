package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Pantallas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * (Cliente) Dos cosas en pantalla:
 * <ul>
 *     <li>La sala de espera: negro total con el reloj del Coleccionista haciendo tic y "Esperando jugadores… (12)".</li>
 *     <li>Los subtítulos del Coleccionista abajo al centro mientras habla (para quien no está mirando su pantalla y para la
 *     transmisión).</li>
 * </ul>
 */
public final class Espera {
    private static final String MOD = Coleccionista.MODID;
    private static int ultimoTic = -1;

    private Espera() {
    }

    public static void init() {
        com.axeliscore.fx.client.Hud.add(Espera::dibujar);
    }

    /** ¿Estoy en la sala de espera (soy pieza viva y aún no despiertan)? */
    public static boolean activa() {
        return EventoCliente.sala() && EventoCliente.vivo(EventoCliente.yo()) && !Cine.activo();
    }

    private static void dibujar(G g, int w, int h) {
        if (activa()) {
            sala(g, w, h);
        }
        if (!Cine.enPantalla()) {
            subtitulo(g, w, h);
            aviso(g, w, h);
        }
    }

    /** "Pulsa R para acercarte a la pantalla" mientras el Coleccionista está en las pantallas. */
    private static void aviso(G g, int w, int h) {
        CompoundTag t = ClientData.get(Pantallas.KEY);
        if (t == null || !"coleccionista".equals(Nbt.s(t, "modo")) || !EventoCliente.vivo(EventoCliente.yo()) || Cine.activo()
                || EventoHud.oculto || Atajos.pantallaCerca() == null) {
            return;
        }
        Font f = Ui.font();
        float a = (float) (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 420.0));
        String s = Component.translatable(MOD + ".cine.pulsa").getString();
        int x = w / 2 - (f.width(s) + 18) / 2, y = h - 92;
        g.fill(x - 4, y - 4, x + f.width(s) + 22, y + 12, Ui.alpha(0xC0000000, a));
        Ui.inset(g, x, y - 2, 13, 13, com.axeliscore.ui.Theme.PIEDRA);
        g.drawString(f, "R", x + 4, y + 1, Ui.alpha(0xFFFFFFFF, a), false);
        g.drawString(f, s, x + 18, y + 1, Ui.alpha(0xFFE8E2D8, a), true);
    }

    /** Subtítulos grandes dentro de la franja de cine de abajo (al ver la pantalla de cerca). */
    static void subtituloCine(G g, int w, int h, int barra) {
        CompoundTag t = ClientData.get(Pantallas.KEY);
        if (t == null || !Nbt.b(t, "hablando") || Nbt.s(t, "linea").isEmpty() || barra < 12) {
            return;
        }
        Font f = Ui.font();
        String s = Component.translatable(Nbt.s(t, "linea")).getString();
        float esc = 1.35F;
        List<net.minecraft.util.FormattedCharSequence> ls = f.split(Component.literal(s), (int) ((w - 60) / esc));
        float alto = ls.size() * 10 * esc;
        float y = h - barra / 2F - alto / 2F;
        for (var l : ls) {
            g.push();
            g.translate(w / 2F - f.width(l) * esc / 2F, y, 0);
            g.scale(esc, esc, 1);
            g.drawString(f, l, 0, 0, 0xFFF4ECE4, true);
            g.pop();
            y += 10 * esc;
        }
    }

    private static void sala(G g, int w, int h) {
        Font f = Ui.font();
        g.fill(0, 0, w, h, 0xFF000000);
        long ms = System.currentTimeMillis();
        int cx = w / 2, cy = h / 2 - 20;
        // el reloj: 12 marcas y una manecilla que avanza a saltos cada segundo
        int r = 22;
        for (int i = 0; i < 12; i++) {
            double ang = i * Math.PI / 6;
            int x = cx + (int) Math.round(Math.sin(ang) * r), y = cy - (int) Math.round(Math.cos(ang) * r);
            int s = i % 3 == 0 ? 2 : 1;
            g.fill(x - s, y - s, x + s, y + s, i == 0 ? 0xFFB3121A : 0xFF8A8078);
        }
        int seg = (int) ((ms / 1000) % 60);
        double ang = seg * Math.PI / 30;
        for (int k = 0; k < r - 5; k++) {
            int x = cx + (int) Math.round(Math.sin(ang) * k), y = cy - (int) Math.round(Math.cos(ang) * k);
            g.fill(x, y, x + 1, y + 1, 0xFFE8E2D8);
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFB3121A);
        if (seg != ultimoTic) {
            ultimoTic = seg;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.get(), 0.5F, 0.15F));
        }
        float puls = (float) (0.65 + 0.35 * Math.sin(ms / 900.0));
        EventoHud.texto(g, f, Component.translatable(MOD + ".espera.titulo").getString(), cx, cy - r - 30, 1.3F,
                Ui.alpha(0xFFE8E2D8, puls));
        String pts = ".".repeat((int) ((ms / 500) % 4));
        String esp = Component.translatable(MOD + ".espera.esperando").getString() + pts;
        g.drawString(f, esp, cx - f.width(Component.translatable(MOD + ".espera.esperando").getString() + "...") / 2, cy + r + 14,
                0xFFB8B0A8, false);
        String n = Component.translatable(MOD + ".espera.cuantos", EventoCliente.total()).getString();
        g.drawCenteredString(f, n, cx, cy + r + 28, 0xFF8A3A40);
        g.drawCenteredString(f, Component.translatable(MOD + ".espera.nota").getString(), cx, h - 24, 0xFF4A4440);
    }

    private static void subtitulo(G g, int w, int h) {
        CompoundTag t = ClientData.get(Pantallas.KEY);
        if (t == null || !Nbt.b(t, "hablando") || Nbt.s(t, "linea").isEmpty() || EventoHud.oculto) {
            return;
        }
        Font f = Ui.font();
        String s = Component.translatable(Nbt.s(t, "linea")).getString();
        List<net.minecraft.util.FormattedCharSequence> ls = f.split(Component.literal(s), Math.min(320, w - 40));
        int y = h - 70 - ls.size() * 11;
        int ancho = 0;
        for (var l : ls) {
            ancho = Math.max(ancho, f.width(l));
        }
        g.fill(w / 2 - ancho / 2 - 8, y - 5, w / 2 + ancho / 2 + 8, y + ls.size() * 11 + 3, 0xB0000000);
        g.fill(w / 2 - ancho / 2 - 8, y - 5, w / 2 - ancho / 2 - 6, y + ls.size() * 11 + 3, EventoHud.ROJO);
        for (var l : ls) {
            g.drawString(f, l, w / 2 - f.width(l) / 2, y, 0xFFF0E8E0, true);
            y += 11;
        }
    }
}

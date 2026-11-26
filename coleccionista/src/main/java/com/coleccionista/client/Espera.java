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
        // por encima del chat (Minecraft dibuja sus letras un poco "más adelante"): la sala de espera tapa el chat y los subtítulos
        // nunca quedan debajo de él
        g.push();
        g.translate(0, 0, 300);
        try {
            if (activa()) {
                sala(g, w, h);
            }
            if (!Cine.enPantalla() && !DesenlaceCliente.cine()) {
                subtitulo(g, w, h);
                aviso(g, w, h);
            }
        } finally {
            g.pop();
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
        if (barra < 12) {
            return;
        }
        // en la franja de abajo, grande (el texto se va escribiendo)
        Subtitulo.dibujar(g, w / 2, h - Math.max(6, barra / 4), w - 60, 1.5F, false);
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
        if (EventoHud.oculto) {
            return;
        }
        // arriba, debajo de la placa de la prueba y el reloj (y de las tarjetas): así no lo tapa el chat ni lo de abajo de la pantalla
        int ancho = Math.min(420, w - 40), cx = w / 2;
        int arriba = Math.max(EventoHud.abajoHud(), 34) + 6;
        // si está la franja de "eliminado" (o la del ganador), debajo de ella
        int franja = EscenaAviso.abajoFranja();
        if (franja > 0) {
            arriba = Math.max(arriba, franja + 6);
        }
        int abajo = arriba + Subtitulo.alto(ancho, 1.25F);
        // si la mini tele del Coleccionista está a la derecha a esa altura, el subtítulo se corre a la izquierda (no se le encima)
        if (MiniTele.bordeIzq < cx + ancho / 2 + 4 && arriba < MiniTele.bordeAbajo) {
            int libre = MiniTele.bordeIzq - 6 - 8;
            ancho = Math.min(ancho, libre);
            cx = 8 + libre / 2;
            abajo = arriba + Subtitulo.alto(ancho, 1.25F);
        }
        Subtitulo.dibujar(g, cx, abajo, ancho, 1.25F, true);
    }
}

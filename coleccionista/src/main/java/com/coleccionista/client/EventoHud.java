package com.coleccionista.client;

import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * (Cliente) El HUD del evento: arriba al centro la placa con la prueba y el reloj; arriba a la derecha cuántos quedan; arriba a la
 * izquierda tu tarjeta (tu número, tu nombre y tu equipo), o la de fantasma u organizador.
 */
public final class EventoHud {
    private static final String MOD = Coleccionista.MODID;
    public static final int ROJO = 0xFFB3121A, ROJO_CL = 0xFFE0404A, PORCELANA = 0xFFE8E2D8, ORO = 0xFFDEAC40, FONDO = 0xE0140C0E,
            FONDO2 = 0xC0080506, FANTASMA = 0xFFBFEFFF;
    /** El organizador puede esconder todo para grabar. */
    public static boolean oculto;
    private static int ultimoSegundo = -1;
    /** Dónde acabó la tarjeta de arriba a la izquierda en el último cuadro (para que los avisos del centro no se le encimen). */
    private static int tarjetaDer, tarjetaAbajo;

    private EventoHud() {
    }

    public static void init() {
        com.axeliscore.fx.client.Hud.add(EventoHud::render);
    }

    /** ¿Se dibuja el HUD del evento ahora? (no en escenas a pantalla completa, cinemáticas ni presentaciones) */
    public static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        return !(oculto || mc.options.hideGui || mc.player == null || EscenaAviso.pantallaCompleta() || Espera.activa() || Cine.activo()
                || Presentacion.activa() || (!EventoCliente.activo() && !EventoCliente.soyOrganizador()));
    }

    private static void render(G g, int w, int h) {
        tarjetaDer = 0;
        tarjetaAbajo = 0;
        if (!visible()) {
            return;
        }
        placa(g, w);
        quedan(g, w);
        tarjeta(g);
        tictac();
    }

    // ------------------------------------------------------------------ arriba al centro

    private static void placa(G g, int w) {
        Font f = Ui.font();
        String fase = EventoCliente.fase();
        boolean pausa = "PAUSA".equals(fase);
        String pr = EventoCliente.prueba();
        if (pr.isEmpty()) {
            pr = Component.translatable(MOD + ("FIN".equals(fase) ? ".hud.fin" : ".hud.espera")).getString();
        }
        int resta = EventoCliente.resta();
        String reloj = resta >= 0 ? String.format("%d:%02d", resta / 60, resta % 60) : null;
        boolean grande = reloj != null || pausa;
        int pw = Math.max(130, Math.max(f.width(pr) + 30, reloj != null ? f.width(reloj) * 2 + 40 : 0));
        int ph = grande ? 34 : 16;
        int x0 = w / 2 - pw / 2, x1 = w / 2 + pw / 2, y0 = 3, y1 = y0 + ph;
        g.fillGradient(x0, y0, x1, y1, FONDO, FONDO2);
        g.fill(x0, y0, x1, y0 + 1, Ui.alpha(PORCELANA, 0.35F));
        g.fill(x0, y1 - 1, x1, y1, ROJO);
        // adornos: rombitos en las puntas de la línea roja
        rombo(g, x0, y1 - 1, ROJO_CL);
        rombo(g, x1 - 1, y1 - 1, ROJO_CL);
        g.drawCenteredString(f, pr, w / 2, y0 + 4, PORCELANA);
        if (pausa) {
            float parpadeo = (float) (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 300.0));
            texto(g, f, "❚❚ " + Component.translatable(MOD + ".hud.pausa").getString(), w / 2, y0 + 17, 1.4F, Ui.alpha(0xFFD8D8D8,
                    parpadeo));
        } else if (reloj != null) {
            int color = resta <= 30 ? ROJO_CL : 0xFFFFFFFF;
            float s = 1.6F;
            if (resta <= 10) {
                double fr = (System.currentTimeMillis() % 1000) / 1000.0;
                s += (float) (0.25 * Math.max(0, 1 - fr * 3));
            }
            texto(g, f, reloj, w / 2, y0 + 16, s, color);
        }
    }

    private static void rombo(G g, int x, int y, int c) {
        g.fill(x - 1, y - 1, x + 2, y + 2, c);
        g.fill(x, y - 2, x + 1, y + 3, c);
        g.fill(x - 2, y, x + 3, y + 1, c);
    }

    /** Texto centrado en (cx, y) con escala. */
    static void texto(G g, Font f, String s, int cx, int y, float escala, int color) {
        g.push();
        g.translate(cx, y, 0);
        g.scale(escala, escala, 1);
        g.drawString(f, s, -f.width(s) / 2, 0, color, true);
        g.pop();
    }

    // ------------------------------------------------------------------ arriba a la derecha

    private static void quedan(G g, int w) {
        if (!EventoCliente.activo()) {
            return;
        }
        Font f = Ui.font();
        String n = EventoCliente.vivos() + "/" + EventoCliente.total();
        String et = Component.translatable(MOD + ".hud.en_juego").getString();
        int x1 = w - 6;
        int cw = Math.max(f.width(n) * 2 + 16, f.width(et)) + 12;
        g.fillGradient(x1 - cw, 3, x1, 33, FONDO, FONDO2);
        g.fill(x1 - cw, 32, x1, 33, ROJO);
        g.push();
        g.translate(x1 - 6, 6, 0);
        g.scale(2F, 2F, 1);
        g.drawString(f, n, -f.width(n), 0, 0xFFFFFFFF, true);
        g.pop();
        g.drawString(f, "☠", x1 - 6 - f.width(n) * 2 - 11, 10, ROJO_CL, true);
        g.drawString(f, et, x1 - 6 - f.width(et), 23, 0xFF9A9090, false);
    }

    // ------------------------------------------------------------------ arriba a la izquierda

    /**
     * Tu tarjeta, como la ficha de una pieza de museo: la cara en un marco de porcelana, "PIEZA 07" en rojo, tu nombre grande, una línea,
     * "♥ Una sola vida" (el corazón late) y, si tienes, tu pareja o equipo. De fantasma se pone azul y dice tu número en la colección.
     */
    private static void tarjeta(G g) {
        Font f = Ui.font();
        Minecraft mc = Minecraft.getInstance();
        CompoundTag yo = EventoCliente.yo();
        int x = 5, y = 4;
        if (yo == null) {
            if (EventoCliente.soyOrganizador()) {
                String t = Component.translatable(MOD + ".hud.organizador").getString();
                int cw = Math.max((int) (f.width(t) * 1.25F), f.width("/evento  ·  O")) + 18;
                fondo(g, x, y, cw, 30, ORO);
                izq(g, f, t, x + 9, y + 5, 1.25F, ORO, true);
                g.drawString(f, "/evento  ·  O", x + 9, y + 18, 0xFF9A9090, false);
            }
            return;
        }
        boolean vivo = EventoCliente.vivo(yo);
        int acento = vivo ? ROJO : 0xFF3A6E80;
        String num = Component.translatable(MOD + ".hud.pieza", String.format("%02d", Nbt.i(yo, "numero"))).getString();
        String nombre = Nbt.s(yo, "nombre");
        String l2 = vivo ? Component.translatable(MOD + ".hud.una_vida").getString()
                : Component.translatable(MOD + ".hud.coleccion", Nbt.i(yo, "orden")).getString();
        List<CompoundTag> comp = vivo ? EventoCliente.companeros() : List.of();
        String l3 = "";
        if (!comp.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (CompoundTag p : comp) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(Nbt.s(p, "nombre"));
            }
            String tipo = comp.size() == 1 ? ".hud.pareja" : ".hud.equipo";
            l3 = Component.translatable(MOD + tipo, yo.getInt("equipo") + 1).getString() + ": " + sb;
            if (f.width(l3) > 200) {
                l3 = f.plainSubstrByWidth(l3, 196) + "…";
            }
        }
        // medidas: la cara de 28 en un marco de 32, el texto a la derecha
        final int cara = 28, tx = x + 46;
        int anchoTexto = Math.max((int) (f.width(num) * 1.3F), (int) (f.width(nombre) * 1.2F));
        int ancho = Math.max(150, Math.max(tx - x + anchoTexto + 10, Math.max(f.width(l2) + 30, f.width(l3) + 20)));
        int alto = l3.isEmpty() ? 58 : 70;
        fondo(g, x, y, ancho, alto, acento);
        tarjetaDer = x + ancho;
        tarjetaAbajo = y + alto;
        // la cara en su marco
        int fx = x + 8, fy = y + 6;
        g.fill(fx - 2, fy - 2, fx + cara + 2, fy + cara + 2, vivo ? 0xFF2A1A1C : 0xFF1E3A44);
        g.fill(fx - 1, fy - 1, fx + cara + 1, fy + cara + 1, vivo ? PORCELANA : 0xFF7FB0C0);
        PlayerFaceRenderer.draw(g.raw, EventoCliente.piel(mc.player.getUUID()), fx, fy, cara);
        if (!vivo) {
            g.fill(fx, fy, fx + cara, fy + cara, 0x8840A0C0);
        }
        // sombra de adentro del marco (que se vea hundida)
        g.fill(fx, fy, fx + cara, fy + 1, 0x55000000);
        g.fill(fx, fy, fx + 1, fy + cara, 0x55000000);
        // número y nombre
        izq(g, f, num, tx, y + 7, 1.3F, vivo ? ROJO_CL : FANTASMA, true);
        izq(g, f, nombre, tx, y + 22, 1.2F, vivo ? 0xFFFFFFFF : 0xFFB0C8D0, true);
        // la línea y el estado
        int ly = y + 42;
        g.fill(x + 8, ly - 3, x + ancho - 8, ly - 2, Ui.alpha(PORCELANA, 0.16F));
        if (vivo) {
            corazon(g, f, x + 12, ly + 4);
            g.drawString(f, l2, x + 21, ly, 0xFFB8ACA6, false);
        } else {
            g.drawString(f, l2, x + 9, ly, FANTASMA, false);
        }
        if (!l3.isEmpty()) {
            g.drawString(f, l3, x + 9, ly + 12, 0xFF8FD8E8, false);
        }
    }

    /**
     * Altura para un aviso centrado de ancho {@code lw} debajo de la placa del centro: 41, o debajo de la tarjeta si en una pantalla
     * angosta se le encimaría.
     */
    public static int lineaY(int w, int lw) {
        return w / 2 - lw / 2 < tarjetaDer + 4 && tarjetaAbajo > 39 ? tarjetaAbajo + 4 : 41;
    }

    /** El fondo de las tarjetas: degradado oscuro, una barra de color a la izquierda, el filo de arriba claro y la línea de abajo. */
    private static void fondo(G g, int x, int y, int w, int h, int acento) {
        g.fill(x + 1, y + 1, x + w + 1, y + h + 1, 0x66000000); // sombra
        g.fillGradient(x, y, x + w, y + h, FONDO, FONDO2);
        g.fill(x, y, x + w, y + 1, Ui.alpha(PORCELANA, 0.3F));
        g.fill(x, y, x + 2, y + h, acento);
        g.fill(x, y + h - 1, x + w, y + h, acento);
        rombo(g, x + w - 1, y + h - 1, acento == ROJO ? ROJO_CL : acento);
    }

    /** El corazón que late (dos latidos y una pausa, como el sonido del despertar). */
    private static void corazon(G g, Font f, int cx, int cy) {
        double t = (System.currentTimeMillis() % 1400) / 1400.0;
        double pulso = Math.max(0, 1 - Math.abs(t - 0.08) * 14) + 0.7 * Math.max(0, 1 - Math.abs(t - 0.26) * 14);
        float esc = 1F + 0.28F * (float) pulso;
        g.push();
        g.translate(cx, cy, 0);
        g.scale(esc, esc, 1);
        g.drawString(f, "♥", -f.width("♥") / 2, -4, ROJO_CL, true);
        g.pop();
    }

    /** Texto alineado a la izquierda en (x, y) con escala. */
    private static void izq(G g, Font f, String s, int x, int y, float escala, int color, boolean sombra) {
        g.push();
        g.translate(x, y, 0);
        g.scale(escala, escala, 1);
        g.drawString(f, s, 0, 0, color, sombra);
        g.pop();
    }

    // ------------------------------------------------------------------ tictac de los últimos 10 segundos

    private static void tictac() {
        int r = EventoCliente.resta();
        if (r != ultimoSegundo) {
            ultimoSegundo = r;
            if (r >= 1 && r <= 10 && !"PAUSA".equals(EventoCliente.fase())) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.get(),
                        r <= 3 ? 0.7F : 1.0F, 0.8F));
            }
        }
    }
}

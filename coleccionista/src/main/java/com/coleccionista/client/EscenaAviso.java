package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Evento;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * (Cliente) Las escenas de "evento de televisión":
 * <ul>
 *     <li>Eliminación: una franja negra con líneas rojas cruza la pantalla con la cara, el número y el nombre ("ahora es parte de la
 *     colección").</li>
 *     <li>Al eliminado: pantalla completa (destello rojo, negro, su retrato en un marco de porcelana: "Has sido añadido a la colección").</li>
 *     <li>Ganador: franja dorada grande ("La última pieza").</li>
 * </ul>
 * Si llegan varias juntas, salen una tras otra.
 */
public final class EscenaAviso {
    private static final String MOD = Coleccionista.MODID;
    private static final Deque<Aviso> COLA = new ArrayDeque<>();
    private static Aviso actual;
    private static Aviso propia;

    private record Aviso(String tipo, UUID id, String nombre, int numero, int orden, int quedan, String motivo, float dur, long[] inicio) {
    }

    private EscenaAviso() {
    }

    public static void init() {
        ClientData.listen(Evento.KEY_AVISO, EscenaAviso::recibir);
        com.axeliscore.fx.client.Hud.add(EscenaAviso::render);
    }

    private static void recibir(CompoundTag t) {
        Minecraft mc = Minecraft.getInstance();
        String tipo = Nbt.s(t, "tipo");
        UUID id = t.getUUID("id");
        float dur = "ganador".equals(tipo) ? 8F : 4.6F;
        Aviso a = new Aviso(tipo, id, Nbt.s(t, "nombre"), Nbt.i(t, "numero"), Nbt.i(t, "orden"), Nbt.i(t, "quedan"), Nbt.s(t, "motivo"), dur,
                new long[] {0});
        boolean mio = mc.player != null && mc.player.getUUID().equals(id);
        if (mio && "eliminado".equals(tipo)) {
            propia = new Aviso(tipo, id, a.nombre, a.numero, a.orden, a.quedan, a.motivo, 7F, new long[] {System.currentTimeMillis()});
            return;
        }
        COLA.add(a);
    }

    /** ¿Se está viendo la escena de pantalla completa del propio eliminado? (el HUD se esconde mientras) */
    public static boolean pantallaCompleta() {
        return propia != null;
    }

    private static void render(G g, int w, int h) {
        long now = System.currentTimeMillis();
        if (propia != null) {
            float t = (now - propia.inicio[0]) / 1000F;
            if (t > propia.dur) {
                propia = null;
            } else {
                miEliminacion(g, w, h, propia, t);
                return;
            }
        }
        if (actual == null && !COLA.isEmpty()) {
            actual = COLA.poll();
            actual.inicio[0] = now;
        }
        if (actual == null) {
            return;
        }
        float t = (now - actual.inicio[0]) / 1000F;
        if (t > actual.dur) {
            actual = null;
            return;
        }
        franja(g, w, h, actual, t);
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    // ------------------------------------------------------------------ la franja que ven todos

    private static void franja(G g, int w, int h, Aviso a, float t) {
        Font f = Ui.font();
        boolean gana = "ganador".equals(a.tipo);
        float entra = suave(t / 0.35F), sale = t > a.dur - 0.4F ? suave((a.dur - t) / 0.4F) : 1F, al = entra * sale;
        int cy = (int) (h * (gana ? 0.42F : 0.30F));
        int alto = gana ? 70 : 46;
        int y0 = cy - alto / 2, y1 = cy + alto / 2;
        int linea = gana ? EventoHud.ORO : EventoHud.ROJO;
        int fondo = gana ? 0x3A2A10 : 0x0C0608;
        g.fillGradient(0, y0, w, cy, Ui.alpha(0xFF000000 | fondo, 0.55F * al), Ui.alpha(0xFF000000 | fondo, 0.88F * al));
        g.fillGradient(0, cy, w, y1, Ui.alpha(0xFF000000 | fondo, 0.88F * al), Ui.alpha(0xFF000000 | fondo, 0.55F * al));
        int lw = (int) (w * entra);
        g.fill(w / 2 - lw / 2, y0, w / 2 + lw / 2, y0 + 1, Ui.alpha(linea, al));
        g.fill(w / 2 - lw / 2, y1 - 1, w / 2 + lw / 2, y1, Ui.alpha(linea, al));
        if (al < 0.05F) {
            return;
        }
        int dx = (int) ((1 - entra) * 40);
        int cara = gana ? 44 : 30;
        String titulo = Component.translatable(MOD + (gana ? ".escena.ultima" : ".escena.eliminado")).getString();
        String sub = Component.translatable(MOD + (gana ? ".escena.gana" : ".escena.coleccion")).getString();
        float esc = gana ? 2.2F : 1.6F;
        int anchoTexto = Math.max((int) (f.width(a.nombre) * esc), f.width(sub));
        int bloque = cara + 10 + Math.max(anchoTexto, f.width(titulo));
        int x = w / 2 - bloque / 2 + dx;
        // retrato con marco
        int fy = cy - cara / 2;
        g.fill(x - 2, fy - 2, x + cara + 2, fy + cara + 2, Ui.alpha(gana ? EventoHud.ORO : EventoHud.PORCELANA, al));
        g.fill(x - 1, fy - 1, x + cara + 1, fy + cara + 1, Ui.alpha(linea, al));
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, al);
        PlayerFaceRenderer.draw(g.raw, EventoCliente.piel(a.id), x, fy, cara);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if ("eliminado".equals(a.tipo)) {
            // tache rojo sobre la cara
            for (int i = 0; i < cara; i++) {
                g.fill(x + i, fy + i, x + i + 2, fy + i + 2, Ui.alpha(EventoHud.ROJO, al * 0.85F));
                g.fill(x + cara - i - 2, fy + i, x + cara - i, fy + i + 2, Ui.alpha(EventoHud.ROJO, al * 0.85F));
            }
        }
        int tx = x + cara + 10;
        String num = "PIEZA " + String.format("%02d", a.numero);
        g.drawString(f, num + "  ·  " + titulo, tx, cy - alto / 2 + (gana ? 10 : 6), Ui.alpha(gana ? EventoHud.ORO : EventoHud.ROJO_CL, al),
                true);
        g.push();
        g.translate(tx, cy - (gana ? 6 : 4), 0);
        g.scale(esc, esc, 1);
        g.drawString(f, a.nombre, 0, 0, Ui.alpha(0xFFFFFFFF, al), true);
        g.pop();
        if (!sub.isEmpty()) {
            g.drawString(f, sub, tx, cy + (gana ? 16 : 10), Ui.alpha(0xFFB8A8A8, al), false);
        }
        if ("eliminado".equals(a.tipo)) {
            String q = Component.translatable(MOD + ".escena.quedan", a.quedan).getString();
            g.drawString(f, q, w - 16 - f.width(q) - dx, cy - 4, Ui.alpha(0xFF9A9090, al), true);
        }
    }

    // ------------------------------------------------------------------ la que ve el eliminado

    private static void miEliminacion(G g, int w, int h, Aviso a, float t) {
        Font f = Ui.font();
        // destello rojo y oscuridad
        float negro = suave(t / 1.2F) * (t > a.dur - 1F ? suave((a.dur - t) / 1F) : 1F);
        g.fill(0, 0, w, h, Ui.alpha(0xFF000000, 0.93F * negro));
        if (t < 0.6F) {
            g.fill(0, 0, w, h, Ui.alpha(0xFFB3121A, 0.65F * (1 - t / 0.6F)));
        }
        // franjas de cine
        int barra = (int) (h * 0.11F * negro);
        g.fill(0, 0, w, barra, 0xFF000000);
        g.fill(0, h - barra, w, h, 0xFF000000);
        float ap = suave((t - 0.9F) / 0.8F) * (t > a.dur - 1F ? suave((a.dur - t) / 1F) : 1F);
        if (ap <= 0.02F) {
            return;
        }
        // arriba del centro: abajo a la izquierda está el chat
        int cx = w / 2, cy = (int) (h * 0.30F);
        int lado = 64;
        int x = cx - lado / 2, y = cy - lado / 2;
        // marco de porcelana con filo rojo y placa
        g.fill(x - 7, y - 7, x + lado + 7, y + lado + 7, Ui.alpha(0xFF6A5A4A, ap));
        g.fill(x - 6, y - 6, x + lado + 6, y + lado + 6, Ui.alpha(EventoHud.PORCELANA, ap));
        g.fill(x - 3, y - 3, x + lado + 3, y + lado + 3, Ui.alpha(0xFFC8BEB0, ap));
        g.fill(x - 1, y - 1, x + lado + 1, y + lado + 1, Ui.alpha(EventoHud.ROJO, ap));
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.85F, 0.8F, 0.8F, ap);
        PlayerFaceRenderer.draw(g.raw, EventoCliente.piel(a.id), x, y, lado);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        String placa = "Nº " + a.orden;
        int pw = f.width(placa) + 10;
        g.fill(cx - pw / 2, y + lado + 4, cx + pw / 2, y + lado + 15, Ui.alpha(0xFF8A6A30, ap));
        g.fill(cx - pw / 2 + 1, y + lado + 5, cx + pw / 2 - 1, y + lado + 14, Ui.alpha(EventoHud.ORO, ap));
        g.drawCenteredString(f, placa, cx, y + lado + 6, Ui.alpha(0xFF3A2A10, ap));
        float tt = suave((t - 1.6F) / 0.8F) * ap;
        if (tt > 0.02F) {
            EventoHud.texto(g, f, Component.translatable(MOD + ".escena.mi_titulo").getString(), cx, y + lado + 26, 1.6F,
                    Ui.alpha(EventoHud.ROJO_CL, tt));
            g.drawCenteredString(f, "Pieza " + String.format("%02d", a.numero) + " · " + a.nombre, cx, y + lado + 44,
                    Ui.alpha(0xFFE0D8D8, tt));
            if (!a.motivo.isEmpty()) {
                g.drawCenteredString(f, a.motivo, cx, y + lado + 56, Ui.alpha(0xFF8A8080, tt));
            }
            g.drawCenteredString(f, Component.translatable(MOD + ".escena.mi_sub").getString(), cx, h - barra - 16,
                    Ui.alpha(EventoHud.FANTASMA, tt * 0.8F));
        }
    }
}

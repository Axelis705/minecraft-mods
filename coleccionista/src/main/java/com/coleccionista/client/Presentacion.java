package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.prueba.Escena;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * (Cliente) Las escenas de pantalla completa al cambiar de prueba:
 * <ul>
 *     <li>Fundido: todo a negro con una frase en medio (mientras el servidor los lleva a otro lugar).</li>
 *     <li>Presentación de la prueba, como en las series: franjas de cine, "PRUEBA I", el nombre en grande escribiéndose letra por letra,
 *     una línea roja que se abre y la frase de la prueba.</li>
 * </ul>
 */
public final class Presentacion {
    private static final float DURA = 7.5F;
    private static long fundidoDesde, presDesde;
    private static int fundidoTicks;
    private static String fundidoTexto = "", numero = "", titulo = "", lema = "", tituloClave = "";
    /** La tarjeta de reglas que sale después del título (lo necesario para jugar, sin decir cómo resolver la prueba). */
    private static final float REGLAS_DURA = 16F;
    private static long reglasDesde;
    /** El reloj que había cuando salió la tarjeta: si arranca otro (empezó la acción), la tarjeta se va para no tapar el juego. */
    private static long relojAlSalir;
    private static boolean alChat;
    private static java.util.List<String> reglas = java.util.List.of();
    private static int letrasSonadas;

    private Presentacion() {
    }

    public static void init() {
        ClientData.listen(Escena.KEY_FUNDIDO, t -> {
            fundidoDesde = System.currentTimeMillis();
            fundidoTicks = Math.max(10, Nbt.i(t, "ticks"));
            fundidoTexto = Nbt.s(t, "texto");
        });
        ClientData.listen(Escena.KEY_PRESENTACION, Presentacion::presentar);
        com.axeliscore.fx.client.Hud.add(Presentacion::dibujar);
    }

    private static void presentar(CompoundTag t) {
        presDesde = System.currentTimeMillis();
        numero = Nbt.s(t, "numero");
        tituloClave = Nbt.s(t, "titulo");
        titulo = Component.translatable(tituloClave).getString();
        if (reglasDesde > 0) {
            reglasAlChat();
        }
        reglasDesde = 0;
        lema = Component.translatable(Nbt.s(t, "lema")).getString();
        letrasSonadas = 0;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.7F, 1F));
    }

    /** ¿Se está viendo la tarjeta de reglas? (para la prueba automática) */
    public static boolean reglasVisibles() {
        return reglasDesde > 0 && !reglas.isEmpty();
    }

    /** ¿Hay una presentación o un fundido en pantalla? (el HUD del evento se esconde) */
    public static boolean activa() {
        long now = System.currentTimeMillis();
        return presDesde > 0 && now - presDesde < DURA * 1000 || fundidoDesde > 0 && now - fundidoDesde < fundidoTicks * 50L;
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** Junta las reglas de la prueba ("...titulo" → "...regla.1..5") y las deja también en el chat para releerlas. */
    private static void empezarReglas() {
        String pre = tituloClave.endsWith(".titulo") ? tituloClave.substring(0, tituloClave.length() - "titulo".length()) + "regla." : "";
        java.util.List<String> l = new java.util.ArrayList<>();
        for (int i = 1; i <= 5 && !pre.isEmpty(); i++) {
            if (net.minecraft.client.resources.language.I18n.exists(pre + i)) {
                l.add(Component.translatable(pre + i).getString());
            }
        }
        reglas = l;
        if (l.isEmpty()) {
            return;
        }
        reglasDesde = System.currentTimeMillis();
        relojAlSalir = EventoCliente.relojFin();
        alChat = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9F, 0.8F));
    }

    /** Cuando la tarjeta se va, las reglas quedan en el chat para releerlas (no antes: no se enciman con la tarjeta). */
    private static void reglasAlChat() {
        if (!alChat || reglas.isEmpty()) {
            return;
        }
        alChat = false;
        Minecraft mc = Minecraft.getInstance();
        mc.gui.getChat().addMessage(Component.literal("— " + Component.translatable("coleccionista.reglas.titulo", titulo).getString() + " —")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD));
        for (String r : reglas) {
            mc.gui.getChat().addMessage(Component.literal(" • " + r).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** La tarjeta de reglas: panel oscuro con borde rojo, el título y las reglas numeradas. */
    private static void tarjeta(G g, int w, int h, Font f, long now) {
        float t = (now - reglasDesde) / 1000F;
        if (t > REGLAS_DURA) {
            reglasDesde = 0;
            reglasAlChat();
            return;
        }
        if (t < REGLAS_DURA - 0.8F && EventoCliente.resta() >= 0 && EventoCliente.relojFin() != relojAlSalir) {
            // ya empezó la prueba (arrancó su reloj): la tarjeta se desvanece (las reglas siguen en el chat)
            reglasDesde = now - (long) ((REGLAS_DURA - 0.8F) * 1000);
            t = REGLAS_DURA - 0.8F;
        }
        float a = suave(t / 0.5F) * (1 - suave((t - (REGLAS_DURA - 0.8F)) / 0.8F));
        if (a < 0.02F) {
            return;
        }
        int cw = Math.min(w - 40, 300);
        java.util.List<net.minecraft.util.FormattedCharSequence> lineas = new java.util.ArrayList<>();
        java.util.List<Integer> numeros = new java.util.ArrayList<>();
        for (int i = 0; i < reglas.size(); i++) {
            java.util.List<net.minecraft.util.FormattedCharSequence> partes = f.split(Component.literal(reglas.get(i)), cw - 34);
            for (int j = 0; j < partes.size(); j++) {
                lineas.add(partes.get(j));
                numeros.add(j == 0 ? i + 1 : 0);
            }
        }
        int ch = 30 + lineas.size() * 11 + 8;
        // abajo de la placa de la prueba y de la línea de cada prueba (no se enciman)
        int x0 = w / 2 - cw / 2, y0 = Math.max((int) (h * 0.22F), 66);
        g.fillGradient(x0, y0, x0 + cw, y0 + ch, Ui.alpha(0xF0120A0C, a), Ui.alpha(0xE0080506, a));
        g.fill(x0, y0, x0 + cw, y0 + 1, Ui.alpha(EventoHud.ROJO, a));
        g.fill(x0, y0 + ch - 1, x0 + cw, y0 + ch, Ui.alpha(EventoHud.ROJO, a));
        String cab = Component.translatable("coleccionista.reglas.titulo", titulo).getString();
        EventoHud.texto(g, f, cab, w / 2, y0 + 8, 1.0F, Ui.alpha(EventoHud.ROJO_CL, a));
        int y = y0 + 26;
        for (int i = 0; i < lineas.size(); i++) {
            if (numeros.get(i) > 0) {
                g.drawString(f, numeros.get(i) + ".", x0 + 12, y, Ui.alpha(EventoHud.ROJO_CL, a), false);
            }
            g.drawString(f, lineas.get(i), x0 + 26, y, Ui.alpha(EventoHud.PORCELANA, a), false);
            y += 11;
        }
    }

    private static void dibujar(G g, int w, int h) {
        long now = System.currentTimeMillis();
        Font f = Ui.font();
        if (reglasDesde > 0) {
            tarjeta(g, w, h, f, now);
        }
        // ---- presentación
        if (presDesde > 0) {
            float t = (now - presDesde) / 1000F;
            if (t > DURA) {
                presDesde = 0;
                empezarReglas();
            } else {
                float salida = 1 - suave((t - (DURA - 0.9F)) / 0.9F);
                float entra = suave(t / 0.6F);
                g.fill(0, 0, w, h, Ui.alpha(0xFF000000, 0.55F * entra * salida));
                int barra = (int) (h * 0.13F * entra * salida);
                g.fill(0, 0, w, barra, 0xFF000000);
                g.fill(0, h - barra, w, h, 0xFF000000);
                int cy = h / 2 - 18;
                // "PRUEBA I" con letras separadas
                float a1 = suave((t - 0.3F) / 0.5F) * salida;
                if (a1 > 0.02F) {
                    String n = Component.translatable("coleccionista.pres.prueba", numero).getString();
                    String sep = String.join(" ", n.split(""));
                    EventoHud.texto(g, f, sep, w / 2, cy - 30, 1.2F, Ui.alpha(EventoHud.ROJO_CL, a1));
                }
                // el nombre, letra por letra, con un temblor chiquito
                float a2 = salida;
                int letras = (int) Mth.clamp((t - 0.8F) / 0.07F, 0, titulo.length());
                if (letras > letrasSonadas) {
                    letrasSonadas = letras;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 0.6F, 0.12F));
                }
                if (letras > 0) {
                    String s = titulo.substring(0, letras);
                    float esc = Math.min(4.2F, (w - 40) / (float) Math.max(1, f.width(titulo)));
                    float tiembla = t < 2.2F ? (float) Math.sin(t * 60) * 0.6F : 0;
                    g.push();
                    g.translate(w / 2F + tiembla, cy, 0);
                    g.scale(esc, esc, 1);
                    int tw = f.width(titulo);
                    // sombra roja y el texto en porcelana
                    g.drawString(f, s, -tw / 2 + 1, 1, Ui.alpha(0xFF5A0A0E, a2), false);
                    g.drawString(f, s, -tw / 2, 0, Ui.alpha(EventoHud.PORCELANA, a2), false);
                    g.pop();
                }
                // la línea roja que se abre desde el centro
                float l = suave((t - 1.6F) / 0.6F) * salida;
                if (l > 0) {
                    int lw = (int) (Math.min(w - 60, 320) / 2F * l);
                    int ly = cy + 42;
                    g.fill(w / 2 - lw, ly, w / 2 + lw, ly + 2, Ui.alpha(EventoHud.ROJO, l));
                    g.fill(w / 2 - 2, ly - 1, w / 2 + 2, ly + 3, Ui.alpha(EventoHud.ROJO_CL, l));
                }
                // la frase de la prueba
                float a3 = suave((t - 2.3F) / 0.8F) * salida;
                if (a3 > 0.02F) {
                    EventoHud.texto(g, f, "“" + lema + "”", w / 2, cy + 54, 1.15F, Ui.alpha(0xFFB8AEA6, a3));
                }
            }
        }
        // ---- fundido a negro
        if (fundidoDesde > 0) {
            float t = (now - fundidoDesde) / 50F;
            if (t > fundidoTicks) {
                fundidoDesde = 0;
                return;
            }
            float a = Math.min(suave(t / 8F), 1 - suave((t - (fundidoTicks - 16)) / 16F));
            g.fill(0, 0, w, h, Ui.alpha(0xFF000000, a));
            if (!fundidoTexto.isEmpty()) {
                float at = Math.min(suave((t - 10) / 12F), 1 - suave((t - (fundidoTicks - 24)) / 12F));
                if (at > 0.02F) {
                    String s = Component.translatable(fundidoTexto).getString();
                    EventoHud.texto(g, f, s, w / 2, h / 2 - 4, 1.2F, Ui.alpha(0xFFA89E96, at));
                }
            }
        }
    }
}

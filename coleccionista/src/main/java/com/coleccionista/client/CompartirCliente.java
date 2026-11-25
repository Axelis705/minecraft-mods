package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Compartir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

/**
 * (Cliente) Compartir o robar: lo que llega del servidor, el HUD (tu mesa y tu pareja, hablen, elijan, cuántos eligieron) y la
 * revelación de cada mesa para todos: las dos caras, las dos cartas boca abajo que se voltean una y otra, y el resultado.
 */
public final class CompartirCliente {
    private static final String MOD = Coleccionista.MODID;
    static CompoundTag estado = new CompoundTag();
    private static int revelando = -1;
    private static long revelaDesde;
    private static int volteadas;

    private CompartirCliente() {
    }

    public static void init() {
        ClientData.listen(Compartir.KEY, CompartirCliente::recibir);
        MinecraftForge.EVENT_BUS.addListener(CompartirCliente::tick);
        com.axeliscore.fx.client.Hud.add(CompartirCliente::hud);
        com.axeliscore.ui.ScreenRegistry.register(Compartir.PANTALLA, EleccionScreen::new);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    private static void recibir(CompoundTag t) {
        estado = t;
        int r = t.contains("revelando") ? t.getInt("revelando") : -1;
        if (r != revelando) {
            revelando = r;
            if (r >= 0) {
                revelaDesde = System.currentTimeMillis() - t.getLong("revela_t") * 50L;
                volteadas = 0;
            }
        }
    }

    /** Si me toca elegir y cerré la pantalla sin elegir, se vuelve a abrir (cada segundo). */
    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CompoundTag m = ClientData.get(Compartir.KEY_MIA);
        if (mc.player != null && mc.screen == null && Nbt.b(m, "elige") && Nbt.s(m, "eleccion").isEmpty() && mc.player.tickCount % 20 == 0) {
            mc.setScreen(new EleccionScreen());
        }
    }

    /** Mi mesa (null si no tengo). */
    private static CompoundTag miMesa() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        UUID yo = mc.player.getUUID();
        for (Tag x : estado.getList("mesas", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            if (c.getCompound("a").getUUID("id").equals(yo) || c.getCompound("b").getUUID("id").equals(yo)) {
                return c;
            }
        }
        return null;
    }

    private static String etiqueta(CompoundTag p) {
        return "Pieza " + String.format("%02d", Nbt.i(p, "numero")) + " · " + Nbt.s(p, "nombre");
    }

    // ------------------------------------------------------------------ HUD

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!activa() || mc.player == null) {
            return;
        }
        revelacion(g, w, h);
        if (!EventoHud.visible()) {
            return;
        }
        Font f = Ui.font();
        String fase = fase();
        String linea = null;
        CompoundTag m = miMesa();
        boolean perdonado = estado.hasUUID("perdonado") && estado.getUUID("perdonado").equals(mc.player.getUUID());
        if (perdonado && !"FIN".equals(fase)) {
            linea = Component.translatable(MOD + ".com.hud.perdonado").getString();
        } else if (m != null) {
            CompoundTag otro = m.getCompound("a").getUUID("id").equals(mc.player.getUUID()) ? m.getCompound("b") : m.getCompound("a");
            switch (fase) {
                case "LISTA", "HABLANDO_COLECCIONISTA" -> linea = Component.translatable(MOD + ".com.hud.mesa", Nbt.i(m, "n"), etiqueta(otro)).getString();
                case "HABLAR" -> linea = Component.translatable(MOD + ".com.hud.hablen", etiqueta(otro)).getString();
                case "ELEGIR" -> linea = Component.translatable(MOD + ".com.hud.eligieron", Nbt.i(estado, "eligieron"), Nbt.i(estado, "total"))
                        .getString();
                default -> {
                }
            }
        } else if ("ELEGIR".equals(fase)) {
            linea = Component.translatable(MOD + ".com.hud.eligieron", Nbt.i(estado, "eligieron"), Nbt.i(estado, "total")).getString();
        }
        if (linea != null) {
            EventoHud.lado(g, linea, 0xFFD8D0C8, true);
        }
    }

    // ------------------------------------------------------------------ la revelación de una mesa

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** ¿Se está viendo la revelación de una mesa? (las franjas de eliminación esperan a que termine) */
    public static boolean revelandoMesa() {
        if (revelando < 0 || !"REVELAR".equals(fase()) || estado == null) {
            return false;
        }
        return revelando < estado.getList("mesas", Tag.TAG_COMPOUND).size() && System.currentTimeMillis() - revelaDesde <= 7400L;
    }

    private static void revelacion(G g, int w, int h) {
        if (revelando < 0 || !"REVELAR".equals(fase())) {
            return;
        }
        var l = estado.getList("mesas", Tag.TAG_COMPOUND);
        if (revelando >= l.size()) {
            return;
        }
        CompoundTag m = l.getCompound(revelando);
        CompoundTag a = m.getCompound("a"), b = m.getCompound("b");
        float t = (System.currentTimeMillis() - revelaDesde) / 1000F;
        if (t > 7.4F) {
            return;
        }
        float entra = suave(t / 0.5F) * (1 - suave((t - 6.8F) / 0.6F));
        Font f = Ui.font();
        int pw = Math.min(w - 20, 330), ph = 150;
        int x0 = w / 2 - pw / 2, y0 = (int) (h * 0.2F) - (int) ((1 - entra) * 30);
        g.fillGradient(x0, y0, x0 + pw, y0 + ph, Ui.alpha(0xF0140C0E, entra), Ui.alpha(0xF0080506, entra));
        g.fill(x0, y0, x0 + pw, y0 + 1, Ui.alpha(EventoHud.ORO, entra));
        g.fill(x0, y0 + ph - 1, x0 + pw, y0 + ph, Ui.alpha(EventoHud.ROJO, entra));
        g.drawCenteredString(f, Component.translatable(MOD + ".com.hud.mesa_n", Nbt.i(m, "n")).getString(), w / 2, y0 + 6, Ui.alpha(EventoHud.ORO,
                entra));
        g.drawCenteredString(f, "VS", w / 2, y0 + 56, Ui.alpha(0xFF8A8078, entra));
        lado(g, f, a, x0 + pw / 4, y0, t, 1.6F, entra);
        lado(g, f, b, x0 + pw * 3 / 4, y0, t, 2.8F, entra);
        // el resultado
        float ar = suave((t - 3.7F) / 0.5F) * entra;
        if (ar > 0.02F && a.contains("robo") && b.contains("robo")) {
            boolean ra = a.getBoolean("robo"), rb = b.getBoolean("robo");
            String txt;
            int col;
            if (!ra && !rb) {
                txt = Component.translatable(MOD + ".com.res.comparten").getString();
                col = 0xFF9FE08A;
            } else if (ra && rb) {
                txt = Component.translatable(MOD + ".com.res.roban").getString();
                col = EventoHud.ROJO_CL;
            } else {
                CompoundTag ladron = ra ? a : b, victima = ra ? b : a;
                txt = Component.translatable(MOD + ".com.res.uno", etiqueta(ladron), etiqueta(victima)).getString();
                col = EventoHud.ROJO_CL;
            }
            Ui.text(g, txt, x0 + 10, y0 + ph - 24, pw - 20, Ui.alpha(col, ar), 10);
        }
        // sonido de cada carta al voltearse
        int deben = (t > 1.6F ? 1 : 0) + (t > 2.8F ? 1 : 0);
        while (volteadas < deben) {
            volteadas++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9F + volteadas * 0.1F, 1F));
        }
    }

    /** Un lado de la mesa: la cara, el nombre y su carta (boca abajo hasta {@code voltea}). */
    private static void lado(G g, Font f, CompoundTag p, int cx, int y0, float t, float voltea, float entra) {
        UUID id = p.getUUID("id");
        PlayerFaceRenderer.draw(g.raw, EventoCliente.piel(id), cx - 34, y0 + 22, 20);
        String n = String.format("%02d", Nbt.i(p, "numero"));
        g.drawString(f, "PIEZA " + n, cx - 10, y0 + 22, Ui.alpha(EventoHud.ROJO_CL, entra), true);
        g.drawString(f, Ui.fit(Nbt.s(p, "nombre"), 70), cx - 10, y0 + 33, Ui.alpha(EventoHud.PORCELANA, entra), true);
        // la carta: se aplasta hasta 0 y se abre del otro lado
        float k = Mth.clamp((t - voltea) / 0.5F, 0, 1);
        float escX = Math.abs(Mth.cos(k * Mth.PI));
        boolean cara = k >= 0.5F && p.contains("robo");
        ResourceLocation tex = !cara ? EleccionScreen.DORSO : p.getBoolean("robo") ? EleccionScreen.ROBAR : EleccionScreen.COMPARTIR;
        int cw = 40, chh = 60;
        g.raw.pose().pushPose();
        g.raw.pose().translate(cx, y0 + 50 + chh / 2F, 0);
        g.raw.pose().scale(Math.max(0.02F, escX), 1, 1);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.raw.setColor(1F, 1F, 1F, entra);
        g.raw.blit(tex, -cw / 2, -chh / 2, cw, chh, 0, 0, 64, 96, 64, 96);
        g.raw.setColor(1F, 1F, 1F, 1F);
        g.raw.pose().popPose();
        if (cara) {
            String s = Component.translatable(MOD + (p.getBoolean("robo") ? ".com.robar" : ".com.compartir")).getString();
            g.drawCenteredString(f, s, cx, y0 + 50 + chh + 3, Ui.alpha(p.getBoolean("robo") ? EventoHud.ROJO_CL : 0xFFE8D9B0, entra));
        }
    }
}

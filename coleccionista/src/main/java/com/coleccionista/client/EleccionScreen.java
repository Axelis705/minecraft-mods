package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Compartir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * (Cliente) Compartir o robar: la elección en secreto. Dos cartas grandes; al pasar el ratón la carta sube y brilla; un clic la elige
 * (ya no se cambia). Arriba, con quién estás y el tiempo. Se puede cerrar con Esc; si todavía no eligió, se vuelve a abrir sola.
 */
public class EleccionScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    static final ResourceLocation COMPARTIR = Ids.rl(MOD, "textures/gui/carta_compartir.png");
    static final ResourceLocation ROBAR = Ids.rl(MOD, "textures/gui/carta_robar.png");
    static final ResourceLocation DORSO = Ids.rl(MOD, "textures/gui/carta_dorso.png");
    private final long abierta = System.currentTimeMillis();
    private float subeA, subeB;

    public EleccionScreen() {
        super(Component.translatable(MOD + ".com.elige"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static CompoundTag mia() {
        return ClientData.get(Compartir.KEY_MIA);
    }

    private static String eleccion() {
        return Nbt.s(mia(), "eleccion");
    }

    private int cw() {
        return Math.min(110, (int) (height * 0.42F / 1.5F));
    }

    private int ch() {
        return (int) (cw() * 1.5F);
    }

    /** -1 ninguna, 0 compartir, 1 robar. */
    private int bajoRaton(double mx, double my) {
        int w = cw(), h = ch(), gap = Math.max(16, w / 3);
        int y0 = height / 2 - h / 2 + 10;
        int xa = width / 2 - gap / 2 - w, xb = width / 2 + gap / 2;
        if (my >= y0 - 10 && my <= y0 + h) {
            if (mx >= xa && mx <= xa + w) {
                return 0;
            }
            if (mx >= xb && mx <= xb + w) {
                return 1;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0 && eleccion().isEmpty() && System.currentTimeMillis() - abierta > 400) {
            int c = bajoRaton(mx, my);
            if (c >= 0) {
                CompoundTag t = new CompoundTag();
                t.putBoolean("robar", c == 1);
                ClientData.send(Compartir.ACCION, t);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1F));
                elegida = c;
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    /** Lo que eligió en esta pantalla (por si el servidor todavía no contesta). */
    private int elegida = -1;

    @Override
    public void tick() {
        super.tick();
        if (!Nbt.b(mia(), "elige")) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0xE0080406, 0xF0140608);
        Font f = Ui.font();
        String el = eleccion();
        int ya = el.isEmpty() ? elegida : "robar".equals(el) ? 1 : 0;
        // arriba: el título, con quién y el reloj
        EventoHud.texto(new com.axeliscore.plat.client.G(g), f, Component.translatable(MOD + ".com.elige").getString(), width / 2, 14, 2F,
                EventoHud.PORCELANA);
        String pareja = Component.translatable(MOD + ".com.tu_pareja", Nbt.s(mia(), "pareja")).getString();
        g.drawCenteredString(f, pareja, width / 2, 36, 0xFFB8AEA6);
        int r = EventoCliente.resta();
        if (r >= 0) {
            String rel = String.format("%d:%02d", r / 60, r % 60);
            EventoHud.texto(new com.axeliscore.plat.client.G(g), f, rel, width / 2, 48, 1.4F, r <= 5 ? EventoHud.ROJO_CL : 0xFFFFFFFF);
        }
        // las dos cartas
        int w = cw(), h = ch(), gap = Math.max(16, w / 3);
        int y0 = height / 2 - h / 2 + 10;
        int xa = width / 2 - gap / 2 - w, xb = width / 2 + gap / 2;
        int sobre = ya >= 0 ? -1 : bajoRaton(mx, my);
        subeA = Mth.lerp(0.25F, subeA, sobre == 0 || ya == 0 ? 1 : 0);
        subeB = Mth.lerp(0.25F, subeB, sobre == 1 || ya == 1 ? 1 : 0);
        carta(g, COMPARTIR, xa, y0 - (int) (subeA * 10), w, h, subeA, ya == 1 ? 0.35F : 1F, 0xFFDEAC40);
        carta(g, ROBAR, xb, y0 - (int) (subeB * 10), w, h, subeB, ya == 0 ? 0.35F : 1F, 0xFFE0404A);
        EventoHud.texto(new com.axeliscore.plat.client.G(g), f, Component.translatable(MOD + ".com.compartir").getString(), xa + w / 2,
                y0 + h + 8, 1.5F, Ui.alpha(0xFFE8D9B0, ya == 1 ? 0.35F : 1F));
        EventoHud.texto(new com.axeliscore.plat.client.G(g), f, Component.translatable(MOD + ".com.robar").getString(), xb + w / 2, y0 + h + 8,
                1.5F, Ui.alpha(0xFFE0404A, ya == 0 ? 0.35F : 1F));
        // abajo: las reglas o lo que eligió
        String abajo = ya < 0 ? Component.translatable(MOD + ".com.reglas").getString()
                : Component.translatable(MOD + (ya == 1 ? ".com.elegiste_robar" : ".com.elegiste_compartir")).getString();
        Ui.text(new com.axeliscore.plat.client.G(g), abajo, width / 2 - 170, height - 34, 340, 0xFFA89E96, 10);
        super.render(g, mx, my, pt);
    }

    private static void carta(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, float brillo, float alfa, int color) {
        if (brillo > 0.02F) {
            int b = (int) (4 * brillo);
            g.fill(x - b, y - b, x + w + b, y + h + b, Ui.alpha(color, 0.45F * brillo * alfa));
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor(1F, 1F, 1F, alfa);
        g.blit(tex, x, y, w, h, 0, 0, 64, 96, 64, 96);
        g.setColor(1F, 1F, 1F, 1F);
    }
}

package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Sorpresa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * (Cliente) Frente a la caja sorpresa: cuántas vueltas le das a la manivela (1, 2 o 3; con clic o las teclas 1 a 3). Si el de antes dio
 * solo 1, estás obligado a dar 3 (solo se puede elegir el 3).
 */
public class VueltasScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private long abierta;
    private boolean enviado;

    public VueltasScreen() {
        super(Component.translatable(MOD + ".sor.vueltas.titulo"));
    }

    @Override
    protected void init() {
        abierta = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!SorpresaCliente.elige()) {
            onClose();
        }
    }

    private int[] boton(int i) {
        int w = 70, h = 84, gap = 18;
        int total = 3 * w + 2 * gap;
        return new int[] {width / 2 - total / 2 + i * (w + gap), height / 2 - h / 2 + 10, w, h};
    }

    private boolean puede(int n) {
        return !SorpresaCliente.obligado() || n == 3;
    }

    private void elegir(int n) {
        if (enviado || !puede(n)) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("que", "vueltas");
        t.putInt("n", n);
        ClientData.send(Sorpresa.ACCION, t);
        enviado = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LEVER_CLICK, 1F, 0.6F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0) {
            for (int i = 0; i < 3; i++) {
                int[] r = boton(i);
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    elegir(i + 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && key <= org.lwjgl.glfw.GLFW.GLFW_KEY_3) {
            elegir(key - org.lwjgl.glfw.GLFW.GLFW_KEY_1 + 1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g0, int mx, int my, float pt) {
        G g = new G(g0);
        float a = Math.min(1F, (System.currentTimeMillis() - abierta) / 300F);
        g.fillGradient(0, 0, width, height, Ui.alpha(0xB0100406, a), Ui.alpha(0xD8200608, a));
        Font f = Ui.font();
        boolean ob = SorpresaCliente.obligado();
        EventoHud.texto(g, f, Component.translatable(MOD + ".sor.vueltas.titulo").getString(), width / 2, height / 2 - 82, 1.8F, EventoHud.ORO);
        Ui.text(g, Component.translatable(MOD + (ob ? ".sor.vueltas.obligado" : ".sor.vueltas.ayuda")).getString(), width / 2 - 160, height / 2 - 58,
                320, ob ? EventoHud.ROJO_CL : 0xFFD8D0C8, 10);
        for (int i = 0; i < 3; i++) {
            int n = i + 1;
            int[] r = boton(i);
            boolean ok = puede(n);
            boolean sobre = ok && !enviado && mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
            g.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] + r[3] + 2, !ok ? 0xFF2A2224 : sobre ? EventoHud.ORO : 0xFF6A4A3A);
            g.fillGradient(r[0], r[1], r[0] + r[2], r[1] + r[3], ok ? 0xFF4A1A2A : 0xFF201418, ok ? 0xFF2A0A14 : 0xFF140C0E);
            EventoHud.texto(g, f, String.valueOf(n), r[0] + r[2] / 2, r[1] + 18, 4F, ok ? EventoHud.PORCELANA : 0xFF4A3E3A);
            String txt = Component.translatable(MOD + (n == 1 ? ".sor.vueltas.una" : ".sor.vueltas.varias"), n).getString();
            g.drawCenteredString(f, txt, r[0] + r[2] / 2, r[1] + r[3] - 16, ok ? 0xFFD8D0C8 : 0xFF4A3E3A);
        }
        if (enviado) {
            g.drawCenteredString(f, Component.translatable(MOD + ".sor.vueltas.listo").getString(), width / 2, height / 2 + 70, EventoHud.PORCELANA);
        }
        super.render(g0, mx, my, pt);
    }
}

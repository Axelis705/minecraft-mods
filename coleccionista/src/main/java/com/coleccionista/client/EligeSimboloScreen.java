package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Caja;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * (Cliente) El arma te apunta: dices tu símbolo en voz alta y lo marcas aquí (con clic o con las teclas 1 a 4). Lo marcado lo ve el
 * organizador junto con el real; si no hay organizador, decide la prueba con esto.
 */
public class EligeSimboloScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private long abierta;
    private boolean enviado;

    public EligeSimboloScreen() {
        super(Component.translatable(MOD + ".caja.elige.titulo"));
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
        if (!CajaCliente.elige() || CajaCliente.eligio() >= 0 && enviado) {
            onClose();
        }
    }

    private int[] carta(int i) {
        int cw = 64, ch = 84, gap = 14;
        int total = 4 * cw + 3 * gap;
        return new int[] {width / 2 - total / 2 + i * (cw + gap), height / 2 - ch / 2 + 8, cw, ch};
    }

    private void elegir(int i) {
        if (enviado) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("que", "simbolo");
        t.putInt("s", i);
        ClientData.send(Caja.ACCION, t);
        enviado = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1F, 0.8F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0) {
            for (int i = 0; i < 4; i++) {
                int[] r = carta(i);
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    elegir(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && key <= org.lwjgl.glfw.GLFW.GLFW_KEY_4) {
            elegir(key - org.lwjgl.glfw.GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g0, int mx, int my, float pt) {
        G g = new G(g0);
        float a = Math.min(1F, (System.currentTimeMillis() - abierta) / 300F);
        g.fillGradient(0, 0, width, height, Ui.alpha(0xC0100406, a), Ui.alpha(0xE0200608, a));
        Font f = Ui.font();
        EventoHud.texto(g, f, Component.translatable(MOD + ".caja.elige.titulo").getString(), width / 2, height / 2 - 78, 1.8F, EventoHud.ROJO_CL);
        Ui.text(g, Component.translatable(MOD + ".caja.elige.ayuda").getString(), width / 2 - 150, height / 2 - 56, 300, 0xFFD8D0C8, 10);
        int marcado = CajaCliente.eligio();
        for (int i = 0; i < 4; i++) {
            int[] r = carta(i);
            boolean sobre = !enviado && mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
            boolean este = marcado == i;
            g.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] + r[3] + 2, este ? EventoHud.ROJO_CL : sobre ? EventoHud.ORO : 0xFF5A4A44);
            g.fillGradient(r[0], r[1], r[0] + r[2], r[1] + r[3], 0xFFE6DCC4, 0xFFC8BCA0);
            g.blit(CajaCliente.icono(i), r[0] + r[2] / 2 - 24, r[1] + 8, 48, 48, 0, 0, 64, 64, 64, 64);
            String n = CajaCliente.nombreSimbolo(i);
            g.drawCenteredString(f, n, r[0] + r[2] / 2, r[1] + 62, 0xFF3A2E2A);
            g.drawCenteredString(f, String.valueOf(i + 1), r[0] + r[2] / 2, r[1] + r[3] + 6, 0xFF8A7E74);
        }
        if (enviado) {
            g.drawCenteredString(f, Component.translatable(MOD + ".caja.elige.listo").getString(), width / 2, height / 2 + 72, EventoHud.PORCELANA);
        }
        super.render(g0, mx, my, pt);
    }
}

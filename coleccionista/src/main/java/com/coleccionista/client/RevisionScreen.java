package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
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
 * (Cliente, organizador) El arma apunta a una pieza: aquí ves su símbolo real y el que marcó, y decides: <b>Bien</b> (no dispara) o
 * <b>Bala</b>. Teclas: B y N. Se cierra sola al decidir; con Esc se cierra y la decisión queda en el panel del lado (o con /bala).
 */
public class RevisionScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private boolean decidido;

    public RevisionScreen() {
        super(Component.translatable(MOD + ".caja.org.pantalla"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        CompoundTag o = CajaCliente.org();
        if (!CajaCliente.decido() || !"ESPERA".equals(Nbt.s(o, "paso"))) {
            onClose();
        }
    }

    private int[] boton(int i) {
        int bw = 110, bh = 30;
        return new int[] {width / 2 + (i == 0 ? -bw - 8 : 8), height / 2 + 44, bw, bh};
    }

    private void decidir(boolean bala) {
        if (decidido) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("que", bala ? "bala" : "bien");
        ClientData.send(Caja.ACCION, t);
        decidido = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(bala ? SoundEvents.CROSSBOW_LOADING_END : SoundEvents.UI_BUTTON_CLICK
                .value(), 1F, 0.7F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0) {
            for (int i = 0; i < 2; i++) {
                int[] r = boton(i);
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    decidir(i == 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_B) {
            decidir(false);
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_N) {
            decidir(true);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g0, int mx, int my, float pt) {
        G g = new G(g0);
        CompoundTag o = CajaCliente.org();
        Font f = Ui.font();
        int pw = 280, ph = 170, x = width / 2 - pw / 2, y = height / 2 - ph / 2 - 10;
        g.fill(0, 0, width, height, 0x60000000);
        g.fillGradient(x, y, x + pw, y + ph, 0xF0140C0E, 0xF0080506);
        g.fill(x, y, x + pw, y + 2, EventoHud.ROJO);
        boolean traidor = Nbt.b(o, "traidor"), final_ = Nbt.b(o, "fase_traidores");
        g.drawCenteredString(f, Component.translatable(MOD + ".caja.org.titulo", Nbt.i(o, "i"), Nbt.i(o, "de")).getString(), width / 2, y + 8,
                0xFFA89E96);
        EventoHud.texto(g, f, Ui.fit(Nbt.s(o, "nombre"), 160), width / 2, y + 22, 1.6F, EventoHud.PORCELANA);
        int real = CajaCliente.indice(Nbt.s(o, "real")), marco = CajaCliente.indice(Nbt.s(o, "marco"));
        g.drawCenteredString(f, Component.translatable(MOD + ".caja.org.real").getString(), width / 2 - 60, y + 46, 0xFFA89E96);
        g.drawCenteredString(f, Component.translatable(MOD + ".caja.org.marco").getString(), width / 2 + 60, y + 46, 0xFFA89E96);
        if (real >= 0) {
            g.blit(CajaCliente.icono(real), width / 2 - 76, y + 58, 32, 32, 0, 0, 64, 64, 64, 64);
        }
        if (marco >= 0) {
            g.blit(CajaCliente.icono(marco), width / 2 + 44, y + 58, 32, 32, 0, 0, 64, 64, 64, 64);
        } else {
            g.drawCenteredString(f, "…", width / 2 + 60, y + 70, 0xFFA89E96);
        }
        String pie = traidor ? Component.translatable(MOD + (final_ ? ".caja.org.traidor_final" : ".caja.org.traidor")).getString()
                : Component.translatable(MOD + ".caja.org.decide").getString();
        Ui.text(g, pie, x + 12, y + 96, pw - 24, traidor ? EventoHud.ROJO_CL : 0xFFD8D0C8, 10);
        String[] txt = {Component.translatable(MOD + (final_ ? ".caja.org.perdonar" : ".caja.org.bien")).getString(),
                Component.translatable(MOD + ".caja.org.bala").getString()};
        for (int i = 0; i < 2; i++) {
            int[] r = boton(i);
            boolean sobre = !decidido && mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
            int borde = i == 1 ? EventoHud.ROJO_CL : 0xFF60A060;
            g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, sobre ? 0xFFFFFFFF : borde);
            g.fillGradient(r[0], r[1], r[0] + r[2], r[1] + r[3], i == 1 ? 0xFF4A0A10 : 0xFF143A1A, i == 1 ? 0xFF2A0408 : 0xFF0A200E);
            g.drawCenteredString(f, txt[i], r[0] + r[2] / 2, r[1] + 11, 0xFFFFFFFF);
        }
        g.drawCenteredString(f, Component.translatable(MOD + ".caja.org.teclas").getString(), width / 2, height / 2 + 82, 0xFF8A7E74);
        super.render(g0, mx, my, pt);
    }
}

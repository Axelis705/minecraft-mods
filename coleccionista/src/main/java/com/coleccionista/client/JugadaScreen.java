package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.ManoGigante;
import com.coleccionista.prueba.Te;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * (Cliente) Piedra, papel o tijera: tres cartas con la mano de porcelana (clic o teclas 1 a 3). Lo que eliges no lo ve nadie hasta que
 * las manos gigantes lo muestran. Se cierra sola al elegir (para ver la mesa) o si se acaba el tiempo.
 */
public class JugadaScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private static final ResourceLocation[] ICONO = {com.axeliscore.plat.Ids.rl(MOD, "textures/gui/te_piedra.png"),
            com.axeliscore.plat.Ids.rl(MOD, "textures/gui/te_papel.png"), com.axeliscore.plat.Ids.rl(MOD, "textures/gui/te_tijera.png")};
    private static final String[] NOMBRE = {".te.piedra", ".te.papel", ".te.tijera"};
    private long abierta, elegidaEn;
    private int elegida;

    public JugadaScreen() {
        super(Component.translatable(MOD + ".te.jugada.titulo"));
    }

    @Override
    protected void init() {
        if (abierta == 0) {
            abierta = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (elegida > 0 ? System.currentTimeMillis() - elegidaEn > 700 : !TeCliente.meToca("jugar")) {
            onClose();
        }
    }

    private int[] carta(int i) {
        int w = 92, h = 118, gap = 20;
        int total = 3 * w + 2 * gap;
        return new int[] {width / 2 - total / 2 + i * (w + gap), height / 2 - h / 2 + 12, w, h};
    }

    private void elegir(int g) {
        if (elegida > 0) {
            return;
        }
        elegida = g;
        elegidaEn = System.currentTimeMillis();
        CompoundTag t = new CompoundTag();
        t.putString("que", "jugada");
        t.putInt("g", g);
        ClientData.send(Te.ACCION, t);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1F, 0.8F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0) {
            for (int i = 0; i < 3; i++) {
                int[] r = carta(i);
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

    /** Para la prueba de cliente. */
    public void elegirPrueba(int g) {
        elegir(g);
    }

    @Override
    public void render(GuiGraphics g0, int mx, int my, float pt) {
        G g = new G(g0);
        float a = Math.min(1F, (System.currentTimeMillis() - abierta) / 300F);
        g.fillGradient(0, 0, width, height, Ui.alpha(0x90100406, a), Ui.alpha(0xC0200608, a));
        Font f = Ui.font();
        EventoHud.texto(g, f, Component.translatable(MOD + ".te.jugada.titulo").getString(), width / 2, height / 2 - 92, 1.8F, EventoHud.ORO);
        int seg = TeCliente.mio().getInt("segundos");
        long pasados = (System.currentTimeMillis() - abierta) / 1000;
        String ayuda = Component.translatable(MOD + ".te.jugada.ayuda", Math.max(0, seg - pasados)).getString();
        g.drawCenteredString(f, ayuda, width / 2, height / 2 - 66, 0xFFD8D0C8);
        for (int i = 0; i < 3; i++) {
            int[] r = carta(i);
            boolean sobre = elegida == 0 && mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
            boolean esta = elegida == i + 1;
            boolean apagada = elegida > 0 && !esta;
            g.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] + r[3] + 2, esta || sobre ? EventoHud.ORO : 0xFF6A4A3A);
            g.fillGradient(r[0], r[1], r[0] + r[2], r[1] + r[3], apagada ? 0xFF201418 : 0xFF4A1A2A, apagada ? 0xFF140C0E : 0xFF2A0A14);
            int tam = 64;
            g0.blit(ICONO[i], r[0] + r[2] / 2 - tam / 2, r[1] + 12, 0, 0, tam, tam, 64, 64);
            g.drawCenteredString(f, Component.translatable(MOD + NOMBRE[i]).getString(), r[0] + r[2] / 2, r[1] + r[3] - 28,
                    apagada ? 0xFF4A3E3A : EventoHud.PORCELANA);
            g.drawCenteredString(f, String.valueOf(i + 1), r[0] + r[2] / 2, r[1] + r[3] - 14, apagada ? 0xFF4A3E3A : 0xFFA89E96);
        }
        if (elegida > 0) {
            g.drawCenteredString(f, Component.translatable(MOD + ".te.jugada.listo").getString(), width / 2, height / 2 + 84, EventoHud.PORCELANA);
        }
        super.render(g0, mx, my, pt);
    }
}

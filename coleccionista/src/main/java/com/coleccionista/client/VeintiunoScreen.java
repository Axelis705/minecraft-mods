package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Veintiuno;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * (Cliente) Tu turno en el veintiuno: abajo, tus cartas (la tapada también: solo tú la ves) y las del otro, y cuatro botones: pedir carta,
 * plantarse, espiar y regresar (clic o teclas 1 a 4). Arriba no tapa nada: se sigue viendo la mesa, tu mano en la máquina y la cara del otro.
 * Se cierra sola cuando ya no es tu turno.
 */
public class VeintiunoScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private static final String[] QUE = {"pedir", "plantarse", "espiar", "regresar"};
    private long abierta;
    private boolean mando;

    public VeintiunoScreen() {
        super(Component.translatable(MOD + ".v21.turno.titulo"));
    }

    @Override
    protected void init() {
        if (abierta == 0) {
            abierta = System.currentTimeMillis();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.get(), 1F, 0.8F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!VeintiunoCliente.meToca()) {
            onClose();
        }
    }

    private int[] boton(int i) {
        int w = 96, h = 30, gap = 8;
        int total = 4 * w + 3 * gap;
        return new int[] {width / 2 - total / 2 + i * (w + gap), height - 74, w, h};
    }

    private boolean puede(int i) {
        CompoundTag e = VeintiunoCliente.estado();
        int l = VeintiunoCliente.miLado();
        if (l < 0 || mando) {
            return false;
        }
        return switch (i) {
            case 0 -> Nbt.i(e, "mazo") > 0;
            case 1 -> true;
            case 2 -> Nbt.b(e, "espiar" + l) && !Nbt.b(e, "espio" + l);
            case 3 -> Nbt.b(e, "regresar" + l) && Nbt.i(e, "cartas" + l) > 2;
            default -> false;
        };
    }

    private void elegir(int i) {
        if (!puede(i)) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("que", QUE[i]);
        ClientData.send(Veintiuno.ACCION, t);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1F, 0.9F));
        // pedir y plantarse terminan el turno; los comodines no
        if (i < 2) {
            mando = true;
        }
    }

    /** Para la prueba de cliente. */
    public void elegirPrueba(int i) {
        elegir(i);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0) {
            for (int i = 0; i < 4; i++) {
                int[] r = boton(i);
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
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
            elegir(key - GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g0, int mx, int my, float pt) {
        G g = new G(g0);
        Font f = Ui.font();
        CompoundTag e = VeintiunoCliente.estado();
        int l = VeintiunoCliente.miLado();
        float a = Math.min(1F, (System.currentTimeMillis() - abierta) / 250F);
        g.fillGradient(0, height - 150, width, height, 0x00000000, Ui.alpha(0xD0100406, a));
        // tus cartas (izquierda) y las del otro (derecha)
        int y = height - 136;
        VeintiunoCliente.dibujarMano(g0, l, 12, y, true);
        VeintiunoCliente.dibujarMano(g0, 1 - l, width - 12, y, false);
        // el título y el tiempo
        EventoHud.texto(g, f, Component.translatable(MOD + ".v21.turno.titulo").getString(), width / 2, height - 128, 1.6F, EventoHud.ORO);
        long seg = Math.max(0, Nbt.i(e, "segundosTurno") - (Minecraft.getInstance().level == null ? 0
                : (Minecraft.getInstance().level.getGameTime() - e.getLong("turnoDesde")) / 20));
        g.drawCenteredString(f, Component.translatable(MOD + ".v21.turno.ayuda", seg).getString(), width / 2, height - 104, 0xFFD8D0C8);
        for (int i = 0; i < 4; i++) {
            int[] r = boton(i);
            boolean ok = puede(i);
            boolean sobre = ok && mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
            g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, sobre ? EventoHud.ORO : ok ? 0xFF7A5A3A : 0xFF3A2A24);
            g.fillGradient(r[0], r[1], r[0] + r[2], r[1] + r[3], ok ? 0xFF4A1A2A : 0xFF201418, ok ? 0xFF2A0A14 : 0xFF140C0E);
            String s = Component.translatable(MOD + ".v21.boton." + QUE[i]).getString();
            g.drawCenteredString(f, s, r[0] + r[2] / 2, r[1] + 6, ok ? EventoHud.PORCELANA : 0xFF5A4E4A);
            String sub = (i + 1) + (i >= 2 ? " · " + Component.translatable(MOD + ".v21.boton.comodin").getString() : "");
            g.drawCenteredString(f, sub, r[0] + r[2] / 2, r[1] + 18, ok ? 0xFFA89E96 : 0xFF4A3E3A);
        }
        super.render(g0, mx, my, pt);
    }
}

package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Traidor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/** (Cliente) El traidor ganó: elige en secreto a quién de su equipo se lleva el Coleccionista. */
public class VictimaScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private boolean elegido;

    public VictimaScreen() {
        super(Component.translatable(MOD + ".tra.victima"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<CompoundTag> candidatos() {
        List<CompoundTag> l = new ArrayList<>();
        for (Tag y : ClientData.get(Traidor.KEY_MIA).getList("candidatos", Tag.TAG_COMPOUND)) {
            l.add((CompoundTag) y);
        }
        return l;
    }

    private int[] caja(int i, int n) {
        int cw = 76, ch = 96, gap = 12;
        int total = n * cw + (n - 1) * gap;
        return new int[] {width / 2 - total / 2 + i * (cw + gap), height / 2 - ch / 2 + 6, cw, ch};
    }

    @Override
    public void tick() {
        super.tick();
        if (!Nbt.b(ClientData.get(Traidor.KEY_MIA), "elige")) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0 && !elegido) {
            List<CompoundTag> cs = candidatos();
            for (int i = 0; i < cs.size(); i++) {
                int[] r = caja(i, cs.size());
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    CompoundTag t = new CompoundTag();
                    t.putUUID("id", cs.get(i).getUUID("id"));
                    ClientData.send(Traidor.ACCION_VICTIMA, t);
                    elegido = true;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BELL_BLOCK, 0.5F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public void render(GuiGraphics g0, int mx, int my, float pt) {
        G g = new G(g0);
        g0.fillGradient(0, 0, width, height, 0xE0140406, 0xF0200608);
        Font f = Ui.font();
        EventoHud.texto(g, f, Component.translatable(MOD + ".tra.victima").getString(), width / 2, 16, 2F, EventoHud.ROJO_CL);
        List<CompoundTag> cs = candidatos();
        for (int i = 0; i < cs.size(); i++) {
            CompoundTag m = cs.get(i);
            int[] b = caja(i, cs.size());
            boolean sobre = !elegido && mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3];
            g.fill(b[0] - 2, b[1] - 2, b[0] + b[2] + 2, b[1] + b[3] + 2, sobre ? EventoHud.ROJO_CL : 0xFF5A4A44);
            g.fillGradient(b[0], b[1], b[0] + b[2], b[1] + b[3], 0xFF241418, 0xFF100809);
            PlayerFaceRenderer.draw(g0, EventoCliente.piel(m.getUUID("id")), b[0] + b[2] / 2 - 20, b[1] + 10, 40);
            g.drawCenteredString(f, "PIEZA " + String.format("%02d", Nbt.i(m, "numero")), b[0] + b[2] / 2, b[1] + 58, EventoHud.ROJO_CL);
            g.drawCenteredString(f, Ui.fit(Nbt.s(m, "nombre"), b[2] - 6), b[0] + b[2] / 2, b[1] + 72, EventoHud.PORCELANA);
        }
        Ui.text(g, Component.translatable(MOD + ".tra.victima_ayuda").getString(), width / 2 - 160, height - 34, 320, 0xFFA89E96, 10);
        super.render(g0, mx, my, pt);
    }
}

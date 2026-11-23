package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Sacrificio;
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

/**
 * (Cliente) El sacrificio: votar en secreto quién se queda. Una tarjeta por cada pieza viva de mi cuarto (cara y nombre); un clic vota
 * (ya no se cambia). Se puede votar por uno mismo.
 */
public class VotoScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;

    public VotoScreen() {
        super(Component.translatable(MOD + ".sac.voto"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<CompoundTag> candidatos() {
        List<CompoundTag> l = new ArrayList<>();
        CompoundTag c = SacrificioCliente.miCuarto();
        if (c != null) {
            for (Tag y : c.getList("miembros", Tag.TAG_COMPOUND)) {
                CompoundTag m = (CompoundTag) y;
                if (Nbt.b(m, "vivo")) {
                    l.add(m);
                }
            }
        }
        return l;
    }

    private int[] caja(int i, int n) {
        int cw = 76, ch = 96, gap = 12;
        int total = n * cw + (n - 1) * gap;
        int x0 = width / 2 - total / 2 + i * (cw + gap), y0 = height / 2 - ch / 2 + 6;
        return new int[] {x0, y0, cw, ch};
    }

    @Override
    public void tick() {
        super.tick();
        if (!SacrificioCliente.meToca() && SacrificioCliente.miVoto == null) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0 && SacrificioCliente.miVoto == null) {
            List<CompoundTag> cs = candidatos();
            for (int i = 0; i < cs.size(); i++) {
                int[] r = caja(i, cs.size());
                if (mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3]) {
                    CompoundTag t = new CompoundTag();
                    t.putUUID("id", cs.get(i).getUUID("id"));
                    ClientData.send(Sacrificio.ACCION, t);
                    SacrificioCliente.miVoto = cs.get(i).getUUID("id");
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.8F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public void render(GuiGraphics g0, int mx, int my, float pt) {
        G g = new G(g0);
        g0.fillGradient(0, 0, width, height, 0xE0080406, 0xF0140608);
        Font f = Ui.font();
        EventoHud.texto(g, f, Component.translatable(MOD + ".sac.voto").getString(), width / 2, 16, 2F, EventoHud.PORCELANA);
        int r = EventoCliente.resta();
        if (r >= 0) {
            EventoHud.texto(g, f, String.format("%d:%02d", r / 60, r % 60), width / 2, 40, 1.4F, r <= 5 ? EventoHud.ROJO_CL : 0xFFFFFFFF);
        }
        List<CompoundTag> cs = candidatos();
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < cs.size(); i++) {
            CompoundTag m = cs.get(i);
            int[] b = caja(i, cs.size());
            boolean sobre = SacrificioCliente.miVoto == null && mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3];
            boolean elegido = m.getUUID("id").equals(SacrificioCliente.miVoto);
            float a = SacrificioCliente.miVoto == null || elegido ? 1F : 0.35F;
            int borde = elegido ? EventoHud.ROJO_CL : sobre ? EventoHud.ORO : 0xFF5A4A44;
            g.fill(b[0] - 2, b[1] - 2, b[0] + b[2] + 2, b[1] + b[3] + 2, Ui.alpha(borde, a));
            g.fillGradient(b[0], b[1], b[0] + b[2], b[1] + b[3], Ui.alpha(0xFF241418, a), Ui.alpha(0xFF100809, a));
            PlayerFaceRenderer.draw(g0, EventoCliente.piel(m.getUUID("id")), b[0] + b[2] / 2 - 20, b[1] + 10, 40);
            g.drawCenteredString(f, "PIEZA " + String.format("%02d", Nbt.i(m, "numero")), b[0] + b[2] / 2, b[1] + 58,
                    Ui.alpha(EventoHud.ROJO_CL, a));
            String n = Ui.fit(Nbt.s(m, "nombre"), b[2] - 6);
            if (mc.player != null && m.getUUID("id").equals(mc.player.getUUID())) {
                n = Component.translatable(MOD + ".sac.yo").getString();
            }
            g.drawCenteredString(f, n, b[0] + b[2] / 2, b[1] + 72, Ui.alpha(EventoHud.PORCELANA, a));
        }
        String abajo = SacrificioCliente.miVoto == null ? Component.translatable(MOD + ".sac.voto_ayuda").getString()
                : Component.translatable(MOD + ".sac.votaste").getString();
        Ui.text(g, abajo, width / 2 - 160, height - 34, 320, 0xFFA89E96, 10);
        super.render(g0, mx, my, pt);
    }
}

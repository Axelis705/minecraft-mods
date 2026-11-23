package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Celdas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * (Cliente) El candado de la celda: tres ruedas de números (flechas o escribir los números) y "Probar". El servidor revisa el código; si
 * es el bueno, se abre la puerta.
 */
public class CandadoScreen extends UiScreen {
    private static final String MOD = Coleccionista.MODID;
    private final int[] d = {0, 0, 0};
    private int cursor;

    public CandadoScreen() {
        super(Component.translatable(MOD + ".candado.titulo"), 190, 150, PanelScreen.TEMA);
    }

    private int celda() {
        CompoundTag t = ClientData.get(Celdas.KEY_CANDADO);
        return t == null ? 0 : Nbt.i(t, "celda");
    }

    @Override
    protected void build() {
        int x0 = px + pw / 2 - 3 * 34 / 2;
        for (int i = 0; i < 3; i++) {
            int k = i, x = x0 + i * 34;
            add(new Button(x + 4, py + 38, 26, 14, "▲", () -> girar(k, 1)));
            add(new Button(x + 4, py + 88, 26, 14, "▼", () -> girar(k, -1)));
        }
        add(new Button(px + pw / 2 - 45, py + ph - 30, 90, 18, Component.translatable(MOD + ".candado.probar").getString(), this::probar));
    }

    private void girar(int i, int s) {
        d[i] = (d[i] + s + 10) % 10;
        cursor = i;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1.6F, 0.4F));
    }

    private void probar() {
        CompoundTag t = new CompoundTag();
        t.putInt("celda", celda());
        t.putInt("codigo", d[0] * 100 + d[1] * 10 + d[2]);
        ClientData.send(Celdas.ACCION_CODIGO, t);
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= 48 && key <= 57 || key >= 320 && key <= 329) {
            d[cursor] = key >= 320 ? key - 320 : key - 48;
            cursor = (cursor + 1) % 3;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1.6F, 0.4F));
            return true;
        }
        if (key == 257 || key == 335) {
            probar();
            return true;
        }
        if (key == 259) {
            cursor = (cursor + 2) % 3;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        Font f = Ui.font();
        Ui.title(g, title.getString(), px + pw / 2, py + 12, theme);
        int x0 = px + pw / 2 - 3 * 34 / 2;
        for (int i = 0; i < 3; i++) {
            int x = x0 + i * 34;
            Ui.inset(g, x + 4, py + 55, 26, 30, theme);
            if (i == cursor) {
                g.fill(x + 4, py + 84, x + 30, py + 85, EventoHud.ROJO_CL);
            }
            EventoHud.texto(g, f, String.valueOf(d[i]), x + 17, py + 62, 2F, 0xFFE8E2D8);
        }
        g.drawCenteredString(f, Component.translatable(MOD + ".candado.ayuda").getString(), px + pw / 2, py + 108, theme.textDim);
    }
}

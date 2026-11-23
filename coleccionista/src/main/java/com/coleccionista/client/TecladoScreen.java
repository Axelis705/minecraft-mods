package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Traidor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * (Cliente) El teclado de la bóveda (El traidor): tantas ruedas de números como tiene el código del equipo, "Probar" y cuántos intentos
 * quedan. Se escribe con las flechas o con el teclado (Enter prueba, Retroceso regresa).
 */
public class TecladoScreen extends UiScreen {
    private static final String MOD = Coleccionista.MODID;
    private final int[] d;
    private int cursor;

    public TecladoScreen() {
        super(Component.translatable(MOD + ".tra.teclado"), 260, 160, PanelScreen.TEMA);
        d = new int[Math.max(3, Math.min(6, Nbt.i(ClientData.get(Traidor.KEY_MIA), "digitos")))];
    }

    private int x0() {
        return px + pw / 2 - d.length * 34 / 2;
    }

    @Override
    protected void build() {
        for (int i = 0; i < d.length; i++) {
            int k = i, x = x0() + i * 34;
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
        StringBuilder c = new StringBuilder();
        for (int x : d) {
            c.append(x);
        }
        CompoundTag t = new CompoundTag();
        t.putString("codigo", c.toString());
        ClientData.send(Traidor.ACCION_CODIGO, t);
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= 48 && key <= 57 || key >= 320 && key <= 329) {
            d[cursor] = key >= 320 ? key - 320 : key - 48;
            cursor = (cursor + 1) % d.length;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 1.6F, 0.4F));
            return true;
        }
        if (key == 257 || key == 335) {
            probar();
            return true;
        }
        if (key == 259) {
            cursor = (cursor + d.length - 1) % d.length;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        Font f = Ui.font();
        Ui.title(g, title.getString(), px + pw / 2, py + 12, theme);
        for (int i = 0; i < d.length; i++) {
            int x = x0() + i * 34;
            Ui.inset(g, x + 4, py + 55, 26, 30, theme);
            if (i == cursor) {
                g.fill(x + 4, py + 84, x + 30, py + 85, EventoHud.ROJO_CL);
            }
            EventoHud.texto(g, f, String.valueOf(d[i]), x + 17, py + 62, 2F, 0xFFE8E2D8);
            g.drawCenteredString(f, String.valueOf(i + 1), x + 17, py + 106, theme.textDim);
        }
        int quedan = Nbt.i(ClientData.get(Traidor.KEY_MIA), "intentos");
        g.drawCenteredString(f, Component.translatable(MOD + ".tra.intentos", quedan).getString(), px + pw / 2, py + 118,
                quedan <= 1 ? EventoHud.ROJO_CL : theme.textDim);
    }
}

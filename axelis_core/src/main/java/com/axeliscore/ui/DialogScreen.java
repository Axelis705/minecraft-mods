package com.axeliscore.ui;

import com.axeliscore.net.CoreNet;
import com.axeliscore.net.Txt;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.G;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventana de diálogo: retrato (la cara de la piel de quien habla, con su sombrero), nombre, texto que se escribe solo (clic para
 * verlo completo) y respuestas cuando termina. La abre el servidor con {@link Dialogs#show}.
 */
public class DialogScreen extends UiScreen {
    private final CoreNet.DialogMsg msg;
    private final Component speaker, text;
    private final List<Component> options = new ArrayList<>();
    private final ResourceLocation skin;
    private boolean replied, skip;
    private int lastShown;

    public DialogScreen(CoreNet.DialogMsg m) {
        super(Txt.unpack(m.speaker()), 300, 128, m.theme() == Dialogs.THEME_NOCHE ? Theme.NOCHE : m.theme() == Dialogs.THEME_MAR ? Theme.MAR
                : Theme.PERGAMINO);
        this.msg = m;
        this.speaker = Txt.unpack(m.speaker());
        this.text = Txt.unpack(m.text());
        for (String o : m.options()) {
            options.add(Txt.unpack(o));
        }
        if (options.isEmpty()) {
            options.add(Component.translatable("axelis_core.dialog.seguir"));
        }
        this.skin = m.skin().isEmpty() ? null : Ids.parse(m.skin());
    }

    public static void open(CoreNet.DialogMsg m) {
        Minecraft.getInstance().setScreen(new DialogScreen(m));
    }

    private int textX() {
        return px + (skin != null ? 64 : 12);
    }

    private int shown() {
        int total = text.getString().length();
        return skip ? total : Math.min(total, (int) Math.max(0, (openTime() - 0.25F) * 45));
    }

    private boolean done() {
        return shown() >= text.getString().length();
    }

    @Override
    protected void build() {
        int n = options.size();
        int bw = Math.min(120, (pw - 24 - (n - 1) * 6) / n);
        int total = n * bw + (n - 1) * 6;
        for (int i = 0; i < n; i++) {
            final int k = i;
            Button b = add(new Button(px + (pw - total) / 2 + i * (bw + 6), py + ph - 26, bw, 18, options.get(i).getString(), () -> choose(k)));
            b.visible = false;
        }
    }

    private void choose(int k) {
        if (!replied) {
            replied = true;
            CoreNet.NET.toServer(new CoreNet.ReplyMsg(msg.id(), k));
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (!replied) {
            replied = true;
            CoreNet.NET.toServer(new CoreNet.ReplyMsg(msg.id(), -1));
        }
        super.onClose();
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        if (skin != null) {
            Ui.inset(g, px + 10, py + 22, 46, 46, theme);
            g.blit(skin, px + 13, py + 25, 40, 40, 8, 8, 8, 8, 64, 64);
            g.blit(skin, px + 13, py + 25, 40, 40, 40, 8, 8, 8, 64, 64);
            // la boca se mueve mientras escribe
            if (!done() && ((int) (openTime() * 8)) % 2 == 0) {
                g.blit(skin, px + 13, py + 25, 40, 40, 24, 0, 8, 8, 64, 64);
            }
        }
        String s = text.getString();
        int n = shown();
        if (n > lastShown && n % 3 == 0 && n < s.length()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 2.0F, 0.08F));
        }
        lastShown = n;
        Ui.text(g, s.substring(0, n), textX(), py + 24, px + pw - textX() - 12, theme.text, 10);
        if (!done() && ((int) (openTime() * 3)) % 2 == 0) {
            g.drawString(Ui.font(), "▼", px + pw - 18, py + ph - 20, theme.accent, false);
        }
        for (Widget w : widgets) {
            w.visible = done();
        }
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        if (!done()) {
            skip = true;
            return true;
        }
        return super.onClick(x, y, b);
    }
}

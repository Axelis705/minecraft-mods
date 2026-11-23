package com.axeliscore.npc.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.npc.Mision;
import com.axeliscore.npc.Misiones;
import com.axeliscore.npc.NpcAcciones;
import com.axeliscore.npc.NpcDef;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.ListView;
import com.axeliscore.ui.Tabs;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * (Cliente) El diario de misiones (tecla N): las que están en curso (con su progreso; se elige cuál se ve en pantalla o se abandona) y
 * las ya hechas.
 */
public class DiarioScreen extends UiScreen {
    private int tab;
    private String elegida = "";
    private CompoundTag visto;

    public DiarioScreen() {
        super(Component.translatable("axelis_core.diario"), 330, 210, Theme.PERGAMINO);
        elegida = NpcCliente.sigue();
    }

    /** Para las pruebas. */
    public void mostrar(int t, String mision) {
        tab = t;
        elegida = mision;
        init();
    }

    @Override
    protected void build() {
        Tabs t = add(new Tabs(px + 10, py + 22, pw - 20, 16, i -> {
            tab = i;
            elegida = "";
            rehacer();
        }, Component.translatable("axelis_core.diario.activas").getString(), Component.translatable("axelis_core.diario.hechas").getString()));
        t.active = tab;
        rehacer();
    }

    private List<Mision> lista() {
        List<Mision> l = new ArrayList<>();
        for (Mision m : Misiones.todas()) {
            Misiones.Estado e = NpcCliente.estado(m);
            boolean activa = e == Misiones.Estado.ACTIVA || e == Misiones.Estado.LISTA;
            if (tab == 0 ? activa : NpcCliente.hechas(m.id) > 0 && !activa) {
                l.add(m);
            }
        }
        return l;
    }

    private void rehacer() {
        widgets.removeIf(w -> !(w instanceof Tabs));
        visto = NpcCliente.yo();
        int x = px + 12, y = py + 44, iw = pw - 24, lw = 130;
        List<Mision> l = lista();
        if (l.isEmpty()) {
            return;
        }
        if (l.stream().noneMatch(m -> m.id.equals(elegida))) {
            elegida = l.get(0).id;
        }
        String sigue = NpcCliente.sigue();
        ListView<Mision> lv = add(new ListView<>(x, y, lw, ph - 44 - 10, 18, (g, m, rx, ry, rw, rh, sel, hov, th) -> {
            Misiones.Estado e = NpcCliente.estado(m);
            boolean s = m.id.equals(sigue);
            String mk = tab == 1 ? "✔" : s ? "◆" : NpcScreen.marcaDe(e);
            g.drawString(Ui.font(), mk, rx + 1, ry + 5, s ? th.accent : NpcScreen.colorDe(e, th), false);
            g.drawString(Ui.font(), Ui.fit(m.titulo().getString(), rw - 14), rx + 12, ry + 5, sel ? th.title : th.text, false);
        }, m -> {
            elegida = m.id;
            rehacer();
        }));
        lv.items.addAll(l);
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id.equals(elegida)) {
                lv.selected = i;
            }
        }
        if (tab == 0) {
            int dx = x + lw + 8, dw = iw - lw - 8, by = py + ph - 30, bw = (dw - 6) / 2;
            boolean laSigue = elegida.equals(sigue);
            CompoundTag d = new CompoundTag();
            d.putString("mision", laSigue ? "" : elegida);
            add(new Button(dx, by, bw, 20, Component.translatable(laSigue ? "axelis_core.diario.no_seguir" : "axelis_core.diario.seguir")
                    .getString(), () -> ClientData.send(NpcAcciones.SEGUIR, d)));
            CompoundTag a = new CompoundTag();
            a.putString("mision", elegida);
            add(new Button(dx + bw + 6, by, bw, 20, Component.translatable("axelis_core.mision.abandonar").getString(),
                    () -> ClientData.send(NpcAcciones.ABANDONAR, a)));
        }
    }

    @Override
    protected void content(G g, int mx, int my, float pt) {
        if (NpcCliente.yo() != visto) {
            rehacer();
        }
        int x = px + 12, y = py + 44, iw = pw - 24, lw = 130;
        Mision m = Misiones.porId(elegida);
        if (m == null || lista().stream().noneMatch(mm -> mm.id.equals(elegida))) {
            Ui.text(g, Component.translatable(tab == 0 ? "axelis_core.diario.vacio" : "axelis_core.diario.vacio_hechas").getString(), x + 4,
                    y + 6, iw - 8, theme.textDim, 10);
            return;
        }
        int dx = x + lw + 8, dw = iw - lw - 8;
        NpcDef d = NpcDef.porId(m.npc());
        int yy = y;
        if (d != null) {
            g.blit(NpcCliente.piel(d), dx, y, 12, 12, 8, 8, 8, 8, 64, 64);
            g.blit(NpcCliente.piel(d), dx, y, 12, 12, 40, 8, 8, 8, 64, 64);
            g.drawString(Ui.font(), d.nombre().getString(), dx + 16, y + 2, theme.textDim, false);
            yy += 16;
        }
        if (tab == 1) {
            String veces = Component.translatable("axelis_core.diario.veces", NpcCliente.hechas(m.id)).getString();
            g.drawString(Ui.font(), veces, dx + dw - Ui.font().width(veces), y + 2, theme.good, false);
        }
        NpcScreen.detalle(g, m, dx, yy, dw, ph - (yy - py) - (tab == 0 ? 36 : 10), theme);
    }

    @Override
    protected InfoCard extraCard(int mx, int my) {
        Mision m = Misiones.porId(elegida);
        if (m == null) {
            return null;
        }
        int x = px + 12, y = py + 44, iw = pw - 24, lw = 130;
        int yy = y + (NpcDef.porId(m.npc()) != null ? 16 : 0);
        return NpcScreen.premioBajo(m, x + lw + 8, yy, iw - lw - 8, ph - (yy - py) - (tab == 0 ? 36 : 10), mx, my, theme);
    }
}

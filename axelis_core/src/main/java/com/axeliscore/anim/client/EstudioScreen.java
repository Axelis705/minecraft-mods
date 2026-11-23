package com.axeliscore.anim.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.mc.EstudioServidor;
import com.axeliscore.anim.Geo;
import com.axeliscore.combat.client.Proyeccion;
import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Button;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.ListView;
import com.axeliscore.ui.Slider;
import com.axeliscore.ui.Tabs;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Ui;
import com.axeliscore.ui.UiScreen;
import com.axeliscore.ui.Widget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * (Cliente) El editor de animaciones (/axeliscore estudio): la criatura se ve en el mundo, en medio, con su esqueleto encima.
 * <ul>
 *     <li>Izquierda: los huesos (clic para elegir uno; se marca en la criatura).</li>
 *     <li>Derecha: las animaciones (elegir, nueva, en bucle, duración) y el giro / posición / escala del hueso en este momento.</li>
 *     <li>Abajo: la línea de tiempo con los cuadros clave del hueso (arrastrar para moverse), reproducir, cuadro por cuadro, velocidad,
 *     poner y quitar cuadros, curva, ver con o sin el código del personaje, guardar.</li>
 *     <li>En medio: arrastrar con el botón izquierdo gira la criatura; con el derecho gira el hueso elegido (pone un cuadro clave).</li>
 * </ul>
 * Teclas: espacio (reproducir), flechas (cuadro por cuadro), Supr (quitar cuadro), S (guardar).
 */
public class EstudioScreen extends UiScreen {
    private static final float CUADRO = 1 / 24F;
    private static final String[] CANALES = {"rotation", "position", "scale"};
    private int pestana;
    private final Slider[] props = new Slider[3];
    private boolean girandoModelo, girandoHueso;
    private long ultimoPoner;
    private float[] arrastre;
    private final GeoRenderer.Frame[] frame = new GeoRenderer.Frame[1];
    private Geo frameGeo;

    public EstudioScreen() {
        super(Component.literal("Estudio"), 10, 10, Theme.PIEDRA);
        panelPrincipal = false;
        CompoundTag d = ClientData.get(EstudioServidor.DATOS);
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(Nbt.i(d, "ent"));
        if (e == null || !EstudioEditor.empezar(e)) {
            if (mc.player != null) {
                Plat.msg(mc.player, Component.literal("[Estudio] esa criatura no usa el motor de animación del Core").withStyle(ChatFormatting.RED),
                        false);
            }
        }
    }

    @Override
    protected void drawBackground(G g, int mx, int my, float pt) {
        // sin fondo oscuro: se ve el mundo
    }

    // ------------------------------------------------------------------ armar

    int lx() {
        return 4;
    }

    int lw() {
        return 98;
    }

    int rw() {
        return 124;
    }

    int rx() {
        return width - rw() - 4;
    }

    int arriba() {
        return 4;
    }

    int bh() {
        return 60;
    }

    int by() {
        return height - bh() - 4;
    }

    @Override
    protected void build() {
        if (!EstudioEditor.activo()) {
            return;
        }
        Geo geo = EstudioEditor.geo();
        int listaAlto = by() - arriba() - 20;
        // huesos
        ListView<Integer> huesos = add(new ListView<>(lx() + 3, arriba() + 16, lw() - 6, listaAlto, 11, (g, b, x, y, w, h, sel, hov, th) -> {
            Geo gg = EstudioEditor.geo();
            if (gg == null || b >= gg.bones.length) {
                return;
            }
            int prof = profundidad(gg, b);
            String n = gg.bones[b].name;
            boolean tiene = !EstudioEditor.claves(n, "rotation").isEmpty() || !EstudioEditor.claves(n, "position").isEmpty()
                    || !EstudioEditor.claves(n, "scale").isEmpty();
            int ix = x + Math.min(30, prof * 5);
            if (tiene) {
                g.fill(ix, y + 4, ix + 3, y + 7, 0xFFFFC04D);
            }
            g.drawString(Ui.font(), Ui.fit(n, w - (ix - x) - 6), ix + 5, y + 2, sel ? th.title : th.text, false);
        }, b -> {
            Geo gg = EstudioEditor.geo();
            EstudioEditor.hueso = gg == null ? "" : gg.bones[b].name;
            rehacerProps();
        }));
        if (geo != null) {
            for (int i = 0; i < geo.bones.length; i++) {
                huesos.items.add(i);
                if (geo.bones[i].name.equals(EstudioEditor.hueso)) {
                    huesos.selected = i;
                }
            }
        }
        // animaciones
        int x = rx() + 3, w = rw() - 6, y = arriba() + 16;
        int clipsAlto = Math.max(33, Math.min(66, (by() - arriba()) - 150));
        List<String> cs = EstudioEditor.clips();
        ListView<String> clips = add(new ListView<>(x, y, w, clipsAlto, 11, (g, c, xx, yy, ww, hh, sel, hov, th) -> {
            int p = c.lastIndexOf('.');
            g.drawString(Ui.font(), Ui.fit(p >= 0 ? c.substring(p + 1) : c, ww - 4), xx, yy + 2, sel ? th.title : th.text, false);
        }, c -> {
            EstudioEditor.clip = c;
            EstudioEditor.tiempo = 0;
            init();
        }));
        clips.items.addAll(cs);
        clips.selected = cs.indexOf(EstudioEditor.clip);
        y += clipsAlto + 3;
        int bw = (w - 3) / 2;
        add(new Button(x, y, bw, 13, "Nueva", () -> {
            EstudioEditor.nueva();
            init();
        }));
        add(new Button(x + bw + 3, y, bw, 13, EstudioEditor.enBucle() ? "En bucle" : "Una vez", () -> {
            EstudioEditor.bucle(!EstudioEditor.enBucle());
            init();
        }));
        y += 16;
        add(new Slider(x, y, w, "Duración", 0.1F, 10F, 0.05F, EstudioEditor.largo(), v -> String.format(Locale.ROOT, "%.2f s", v),
                EstudioEditor::largo));
        y += 22;
        Tabs t = add(new Tabs(x, y, w, 12, i -> {
            pestana = i;
            rehacerProps();
        }, "Giro", "Mover", "Escala"));
        t.active = pestana;
        rehacerProps();
        // abajo: botones
        int bx = lx() + 3, byy = by() + bh() - 18, sb = 16;
        add(new Button(bx, byy, sb, 14, "|<", () -> irA(0)));
        add(new Button(bx + sb + 2, byy, sb, 14, "<", () -> irA(EstudioEditor.tiempo - CUADRO)));
        add(new Button(bx + 2 * (sb + 2), byy, sb + 4, 14, EstudioEditor.reproducir ? "||" : "▶", () -> {
            EstudioEditor.reproducir = !EstudioEditor.reproducir;
            init();
        }));
        add(new Button(bx + 3 * (sb + 2) + 4, byy, sb, 14, ">", () -> irA(EstudioEditor.tiempo + CUADRO)));
        add(new Button(bx + 4 * (sb + 2) + 4, byy, sb, 14, ">|", () -> irA(EstudioEditor.largo())));
        int cx = bx + 5 * (sb + 2) + 8;
        add(new Slider(cx, byy - 7, 58, "Vel.", 0.1F, 2F, 0.05F, EstudioEditor.velocidad, v -> String.format(Locale.ROOT, "%.2fx", v),
                v -> EstudioEditor.velocidad = v));
        cx += 64;
        add(new Button(cx, byy, 36, 14, "+Clave", () -> {
            EstudioEditor.poner(CANALES[pestana], EstudioEditor.valor(EstudioEditor.hueso, CANALES[pestana]));
            init();
        })).help(Component.literal("Poner cuadro clave"), Component.literal("Guarda el valor del hueso en este momento."));
        cx += 38;
        add(new Button(cx, byy, 36, 14, "Quitar", () -> {
            EstudioEditor.quitar();
            init();
        }));
        cx += 38;
        String cur = EstudioEditor.curva();
        add(new Button(cx, byy, 62, 14, cur.isEmpty() ? "Curva" : corto(cur), () -> {
            String c = EstudioEditor.curva();
            int i = java.util.Arrays.asList(EstudioEditor.CURVAS).indexOf(c);
            EstudioEditor.curva(EstudioEditor.CURVAS[(i + 1) % EstudioEditor.CURVAS.length]);
            init();
        })).help(Component.literal("Curva"), Component.literal("Cómo llega a este cuadro clave (clic para cambiar)."));
        cx += 64;
        add(new Button(cx, byy, 40, 14, EstudioEditor.codigo ? "Código ✔" : "Código", () -> {
            EstudioEditor.codigo = !EstudioEditor.codigo;
            init();
        })).help(Component.literal("Con el código"), Component.literal("Muestra también lo que hace el personaje por código (mirar, resortes)."));
        cx += 42;
        add(new Button(cx, byy, 44, 14, EstudioEditor.cambios ? "Guardar*" : "Guardar", () -> {
            guardar();
            init();
        }));
        // línea de tiempo
        add(new Linea(lx() + 3, by() + 14, width - 14, 22));
    }

    private static String corto(String c) {
        return c.replace("easeInOut", "InOut ").replace("easeIn", "In ").replace("easeOut", "Out ").replace("catmullrom", "suave")
                .replace("linear", "lineal").replace("step", "salto");
    }

    private void irA(float t) {
        EstudioEditor.reproducir = false;
        float l = EstudioEditor.largo();
        EstudioEditor.tiempo = Math.max(0, Math.min(l, Math.round(t / CUADRO) * CUADRO));
        init();
    }

    private void rehacerProps() {
        widgets.removeIf(w -> w instanceof Slider s && s.label.startsWith("·"));
        String canal = CANALES[pestana];
        float[] v = EstudioEditor.valor(EstudioEditor.hueso, canal);
        float min = canal.equals("rotation") ? -180 : canal.equals("position") ? -16 : 0;
        float max = canal.equals("rotation") ? 180 : canal.equals("position") ? 16 : 3;
        float paso = canal.equals("rotation") ? 0.5F : canal.equals("position") ? 0.1F : 0.01F;
        int x = rx() + 3, w = rw() - 6;
        int y = by() - 64;
        String[] ejes = {"· X", "· Y", "· Z"};
        for (int i = 0; i < 3; i++) {
            int fi = i;
            Slider s = add(new Slider(x, y + i * 20, w, ejes[i], min, max, paso, v[i], f -> String.format(Locale.ROOT, "%.2f", f), f -> {
                float[] nv = EstudioEditor.valor(EstudioEditor.hueso, CANALES[pestana]);
                nv[fi] = f;
                EstudioEditor.poner(CANALES[pestana], nv);
            }));
            s.enabled = !EstudioEditor.hueso.isEmpty();
            props[i] = s;
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void content(G g, int mx, int my, float pt) {
        if (!EstudioEditor.activo()) {
            onClose();
            return;
        }
        Theme t = theme;
        Ui.panel(g, lx(), arriba(), lw(), by() - arriba() - 4, t, 1F);
        Ui.panel(g, rx(), arriba(), rw(), by() - arriba() - 4, t, 1F);
        Ui.panel(g, lx(), by(), width - 8, bh(), t, 1F);
        g.drawString(Ui.font(), "Huesos", lx() + 5, arriba() + 5, t.title, true);
        g.drawString(Ui.font(), "Animaciones", rx() + 5, arriba() + 5, t.title, true);
        String h = EstudioEditor.hueso.isEmpty() ? "(elige un hueso)" : EstudioEditor.hueso;
        g.drawString(Ui.font(), Ui.fit(h, rw() - 10), rx() + 5, by() - 76, EstudioEditor.hueso.isEmpty() ? t.textDim : t.accent, true);
        // valores en vivo mientras se reproduce
        if (EstudioEditor.reproducir && !EstudioEditor.hueso.isEmpty() && !girandoHueso) {
            float[] v = EstudioEditor.valor(EstudioEditor.hueso, CANALES[pestana]);
            for (int i = 0; i < 3; i++) {
                if (props[i] != null) {
                    props[i].value = Mth.clamp(v[i], props[i].min, props[i].max);
                }
            }
        }
        // tiempo y aviso
        String tt = String.format(Locale.ROOT, "%.2f / %.2f s  (cuadro %d)", EstudioEditor.tiempo, EstudioEditor.largo(),
                Math.round(EstudioEditor.tiempo / CUADRO));
        g.drawString(Ui.font(), tt, lx() + 6, by() + 4, t.text, false);
        String ayuda = "Izq: girar  ·  Der: mover hueso  ·  Espacio, flechas, Supr, S";
        g.drawString(Ui.font(), Ui.fit(ayuda, width - 20 - Ui.font().width(tt) - 30), width - 10 - Math.min(Ui.font().width(ayuda),
                width - 20 - Ui.font().width(tt) - 30), by() + 4, t.textDim, false);
        if (!EstudioEditor.aviso.isEmpty()) {
            g.drawString(Ui.font(), "Guardado: " + EstudioEditor.aviso, lx() + lw() + 8, arriba() + 4, 0xFF7CFC9A, true);
        }
        esqueleto(g);
    }

    /** Huesos encima de la criatura: líneas de padre a hijo y el elegido resaltado. */
    private void esqueleto(G g) {
        Entity e = EstudioEditor.objetivo();
        AnimHost h = EstudioEditor.host();
        if (e == null || h == null || !h.ready()) {
            return;
        }
        Geo geo = h.geo();
        if (frame[0] == null || frameGeo != geo) {
            frame[0] = new GeoRenderer.Frame(geo);
            frameGeo = geo;
        }
        float pt = Proyeccion.partial();
        net.minecraft.world.phys.Vec3 cam = Draw.camPos();
        PoseStack ps = new PoseStack();
        ps.translate((float) (Mth.lerp(pt, e.xo, e.getX()) - cam.x), (float) (Mth.lerp(pt, e.yo, e.getY()) - cam.y),
                (float) (Mth.lerp(pt, e.zo, e.getZ()) - cam.z));
        float yaw = EstudioEditor.giro(0);
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180F - yaw)));
        ps.scale(-1F, -1F, 1F);
        float sc = EstudioEditor.renderer == null ? 1 : EstudioEditor.renderer.escalaPara(e);
        ps.scale(sc, sc, sc);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.compute(geo, h.pose(), ps, frame[0]);
        int n = geo.bones.length;
        float[][] pant = new float[n][];
        for (int b = 0; b < n; b++) {
            Vector3f p = frame[0].world[b].transformPosition(new Vector3f());
            pant[b] = Proyeccion.aPantalla(p.x + cam.x, p.y + cam.y, p.z + cam.z, width, height);
        }
        int sel = geo.bone(EstudioEditor.hueso);
        for (int b = 0; b < n; b++) {
            int par = geo.bones[b].parent;
            if (par < 0 || pant[b] == null || pant[par] == null) {
                continue;
            }
            boolean rama = sel >= 0 && geo.isUnder(b, sel);
            linea(g, pant[par][0], pant[par][1], pant[b][0], pant[b][1], rama ? 0xC0FFC04D : 0x60FFFFFF);
        }
        float tm = Ui.tiempo();
        for (int b = 0; b < n; b++) {
            if (pant[b] == null) {
                continue;
            }
            int x = Math.round(pant[b][0]), y = Math.round(pant[b][1]);
            if (b == sel) {
                int r = 3 + (int) (Math.sin(tm * 6) + 1);
                g.fill(x - r - 1, y - r - 1, x + r + 2, y + r + 2, 0xC0000000);
                g.fill(x - r, y - r, x + r + 1, y + r + 1, 0xFFFFC04D);
                g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF000000);
            } else {
                g.fill(x - 1, y - 1, x + 2, y + 2, 0x90000000);
                g.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
            }
        }
    }

    private static void linea(G g, float x0, float y0, float x1, float y1, int c) {
        float dx = x1 - x0, dy = y1 - y0;
        int pasos = (int) Math.max(Math.abs(dx), Math.abs(dy));
        if (pasos > 600) {
            return;
        }
        for (int i = 0; i <= pasos; i += 2) {
            float f = pasos == 0 ? 0 : i / (float) pasos;
            int x = Math.round(x0 + dx * f), y = Math.round(y0 + dy * f);
            g.fill(x, y, x + 1, y + 1, c);
        }
    }

    // ------------------------------------------------------------------ mouse y teclado

    private boolean enMedio(double x, double y) {
        return x > lx() + lw() + 4 && x < rx() - 4 && y > arriba() && y < by() - 4;
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        if (enMedio(x, y)) {
            if (b == 0) {
                girandoModelo = true;
                arrastre = new float[] {(float) x, (float) y};
                return true;
            }
            if (b == 1 && !EstudioEditor.hueso.isEmpty()) {
                girandoHueso = true;
                EstudioEditor.reproducir = false;
                arrastre = new float[] {(float) x, (float) y};
                return true;
            }
        }
        return super.onClick(x, y, b);
    }

    @Override
    protected boolean onDrag(double x, double y, int b, double dx, double dy) {
        if (girandoModelo && arrastre != null) {
            EstudioEditor.giro = EstudioEditor.giro(0) - (float) (x - arrastre[0]) * 1.5F;
            arrastre[0] = (float) x;
            arrastre[1] = (float) y;
            return true;
        }
        if (girandoHueso && arrastre != null) {
            float ddx = (float) (x - arrastre[0]), ddy = (float) (y - arrastre[1]);
            arrastre[0] = (float) x;
            arrastre[1] = (float) y;
            float[] v = EstudioEditor.valor(EstudioEditor.hueso, "rotation");
            if (CoreScreen.shift()) {
                v[2] += ddx * 1.2F;
            } else {
                v[1] += ddx * 1.2F;
                v[0] += ddy * 1.2F;
            }
            long now = System.currentTimeMillis();
            if (now - ultimoPoner > 40) {
                ultimoPoner = now;
                EstudioEditor.poner("rotation", v);
            }
            return true;
        }
        return super.onDrag(x, y, b, dx, dy);
    }

    @Override
    protected boolean onRelease(double x, double y, int b) {
        if (girandoHueso) {
            girandoHueso = false;
            pestana = 0;
            init();
            return true;
        }
        if (girandoModelo) {
            girandoModelo = false;
            return true;
        }
        return super.onRelease(x, y, b);
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        switch (key) {
            case 32 -> {
                EstudioEditor.reproducir = !EstudioEditor.reproducir;
                init();
                return true;
            }
            case 262 -> {
                irA(EstudioEditor.tiempo + CUADRO);
                return true;
            }
            case 263 -> {
                irA(EstudioEditor.tiempo - CUADRO);
                return true;
            }
            case 261 -> {
                EstudioEditor.quitar();
                init();
                return true;
            }
            case 83 -> {
                guardar();
                init();
                return true;
            }
            default -> {
                return super.onKey(key, scan, mods);
            }
        }
    }

    private void guardar() {
        var f = EstudioEditor.guardar();
        Minecraft mc = Minecraft.getInstance();
        if (f != null && mc.player != null) {
            Plat.msg(mc.player, Component.literal("[Estudio] ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("guardado en axelis_estudio/" + f.getFileName()).withStyle(ChatFormatting.WHITE)), false);
        }
    }

    @Override
    public void onClose() {
        Entity e = EstudioEditor.objetivo();
        if (EstudioEditor.cambios) {
            guardar();
        }
        if (e != null) {
            CompoundTag d = new CompoundTag();
            d.putInt("ent", e.getId());
            ClientData.send(EstudioServidor.FIN, d);
        }
        EstudioEditor.terminar();
        super.onClose();
    }

    private static int profundidad(Geo geo, int b) {
        int d = 0;
        while (geo.bones[b].parent >= 0) {
            b = geo.bones[b].parent;
            d++;
        }
        return d;
    }

    /** La línea de tiempo: marcas de tiempo, cuadros clave del hueso elegido y la aguja (se arrastra). */
    final class Linea extends Widget {
        private boolean arrastrando;

        Linea(int x, int y, int w, int h) {
            super(x, y, w, h);
        }

        float tiempoEn(double mx) {
            return (float) Math.max(0, Math.min(1, (mx - x - 2) / (w - 4))) * EstudioEditor.largo();
        }

        int xDe(float t) {
            return x + 2 + Math.round(t / EstudioEditor.largo() * (w - 4));
        }

        @Override
        protected void draw(G g, int mx, int my, Theme t) {
            Ui.inset(g, x, y, w, h, t);
            float l = EstudioEditor.largo();
            // marcas: cada cuadro (si caben), cada 0.25 s y cada segundo con número
            for (int i = 0; i * CUADRO <= l + 1e-4F; i++) {
                int px = xDe(i * CUADRO);
                boolean seg = i % 24 == 0, cuarto = i % 6 == 0;
                if (!cuarto && (w - 4) / (l / CUADRO) < 3) {
                    continue;
                }
                g.fill(px, y + 1, px + 1, y + (seg ? 6 : cuarto ? 4 : 2), Ui.alpha(0xFFFFFFFF, seg ? 0.5F : 0.22F));
                if (seg) {
                    g.drawString(Ui.font(), String.valueOf(i / 24), px + 2, y + 1, Ui.alpha(0xFFFFFFFF, 0.45F), false);
                }
            }
            // cuadros clave del hueso: giro (dorado), posición (celeste), escala (verde)
            int[] colores = {0xFFFFC04D, 0xFF5FD0FF, 0xFF7CFC9A};
            for (int c = 0; c < 3; c++) {
                int ky = y + 8 + c * 4;
                for (float k : EstudioEditor.claves(EstudioEditor.hueso, CANALES[c])) {
                    int px = xDe(k);
                    g.fill(px - 2, ky, px + 3, ky + 3, 0xFF000000);
                    g.fill(px - 1, ky, px + 2, ky + 3, colores[c]);
                }
            }
            // aguja
            int ax = xDe(EstudioEditor.tiempo);
            g.fill(ax, y - 2, ax + 1, y + h, 0xFFFF4D4D);
            g.fill(ax - 2, y - 3, ax + 3, y - 1, 0xFFFF4D4D);
        }

        @Override
        public boolean click(double mx, double my, int button) {
            if (button != 0 || !inside(mx, my)) {
                return false;
            }
            arrastrando = true;
            EstudioEditor.reproducir = false;
            mover(mx);
            return true;
        }

        void mover(double mx) {
            float tt = tiempoEn(mx);
            // se pega a un cuadro clave si está a 3 píxeles
            for (String c : CANALES) {
                for (float k : EstudioEditor.claves(EstudioEditor.hueso, c)) {
                    if (Math.abs(xDe(k) - mx) <= 3) {
                        tt = k;
                    }
                }
            }
            EstudioEditor.tiempo = tt;
        }

        @Override
        public boolean drag(double mx, double my, int button) {
            if (!arrastrando) {
                return false;
            }
            mover(mx);
            return true;
        }

        @Override
        public boolean release(double mx, double my, int button) {
            if (arrastrando) {
                arrastrando = false;
                init();
                return true;
            }
            return false;
        }

        @Override
        public InfoCard card(double mx, double my) {
            return new InfoCard(Component.literal("Línea de tiempo")).text(Component.literal(
                    "Arrastra para moverte. Dorado: giro, celeste: posición, verde: escala.")).width(170);
        }
    }
}

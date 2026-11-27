package com.navidad.client;

import com.axeliscore.data.client.ClientData;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.arbol.NivelArbol;
import com.navidad.arbol.PuntosArbol;
import com.navidad.libro.Libro;
import com.navidad.libro.Tareas;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * La pantalla del Libro de Navidad: un libro abierto con cintas de colores a la derecha (un capítulo por cinta y la de "Mi árbol").
 * En cada capítulo: a la izquierda el título, un texto, cuánto llevas y el premio grande; a la derecha sus seis tareas con su avance y
 * sus premios (y el botón "Reclamar" cuando están listas; al reclamar cae un sello de cera, saltan chispas y los premios vuelan a tu
 * inventario). En "Mi árbol": tu árbol de verdad girando en 3D, su nivel, sus puntos y qué regalos da cada nivel. Afuera cae nieve.
 */
public class LibroPantalla extends Screen {
    public static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/gui/libro.png");
    private static final int TW = 1024, TH = 512, BW = 300, BH = 190, ARBOL = 4;
    private static final int TINTA = 0xFF4A2E1A, TINTA_SUAVE = 0xFF8A6A4A;
    private static int tabGuardado;
    /** Solo para la prueba con fotos: la pestaña que abre. */
    public static int tabPrueba = -1;

    private int tab, tabAntes, x0, y0;
    private long abierto = -1, cambio = -1, ultimo;
    private final Random rnd = new Random();
    private final List<float[]> copos = new ArrayList<>();
    private final List<float[]> chispas = new ArrayList<>();
    private final List<Volador> voladores = new ArrayList<>();
    private final Map<String, Long> sellos = new HashMap<>();
    private CompoundTag visto;

    private record Volador(ItemStack st, float x, float y, long desde) {
    }

    public LibroPantalla() {
        super(Component.translatable("item.navidad.libro_navidad"));
        tab = tabPrueba >= 0 ? tabPrueba : tabGuardado;
    }

    public static void abrir() {
        Minecraft.getInstance().setScreen(new LibroPantalla());
        ClientData.send(Navidad.MODID + ":libro_abrir");
    }

    @Override
    protected void init() {
        x0 = (width - BW) / 2 - 10;
        y0 = (height - BH) / 2;
        if (copos.isEmpty()) {
            for (int i = 0; i < 70; i++) {
                copos.add(new float[]{rnd.nextFloat() * width, rnd.nextFloat() * height, 0.3F + rnd.nextFloat() * 0.9F, rnd.nextFloat() * 6.28F,
                        rnd.nextInt(3) == 0 ? 2 : 1});
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private CompoundTag datos() {
        return ClientData.get(Libro.KEY);
    }

    private static float easeOutBack(float k) {
        float c1 = 1.70158F, c3 = c1 + 1;
        float x = k - 1;
        return 1 + c3 * x * x * x + c1 * x * x;
    }

    // ------------------------------------------------------------------ dibujar

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long ahora = System.currentTimeMillis();
        if (abierto < 0) {
            abierto = ahora;
            ultimo = ahora;
        }
        float dt = Math.min(0.1F, (ahora - ultimo) / 1000F);
        ultimo = ahora;
        CompoundTag d = datos();
        notarCambios(d, ahora);
        renderBackground(g);
        nieve(g, dt, false);
        float k = Mth.clamp((ahora - abierto) / 280F, 0, 1);
        float esc = 0.86F + 0.14F * easeOutBack(k);
        PoseStack ps = g.pose();
        ps.pushPose();
        float cx = x0 + BW / 2F, cy = y0 + BH / 2F;
        ps.translate(cx, cy, 0);
        ps.scale(esc, esc, 1);
        ps.translate(-cx, -cy, 0);
        RenderSystem.enableBlend();
        g.setColor(1, 1, 1, Math.min(1, k * 1.6F));
        // las cintas de atrás (las que no están elegidas) van detrás del libro
        cintas(g, mx, my, false);
        g.blit(TEX, x0, y0, BW, BH, 0, 0, 600, 380, TW, TH);
        cintas(g, mx, my, true);
        g.setColor(1, 1, 1, 1);
        if (tab == ARBOL) {
            miArbol(g, d, mx, my, ahora);
        } else {
            capitulo(g, d, tab, mx, my, ahora);
        }
        pasarHoja(g, ahora);
        ps.popPose();
        efectos(g, dt, ahora);
        nieve(g, dt, true);
        tooltips(g, d, mx, my);
    }

    /** Copos que caen: los grandes delante del libro, los chiquitos detrás. */
    private void nieve(GuiGraphics g, float dt, boolean delante) {
        for (float[] c : copos) {
            boolean grande = c[4] > 1;
            if (grande != delante) {
                continue;
            }
            if (!delante) {
                c[1] += c[2] * 22 * dt;
                c[3] += dt;
                c[0] += Mth.sin(c[3]) * 8 * dt;
                if (c[1] > height + 4) {
                    c[1] = -4;
                    c[0] = rnd.nextFloat() * width;
                }
            } else {
                c[1] += c[2] * 30 * dt;
                c[3] += dt * 1.3F;
                c[0] += Mth.sin(c[3]) * 10 * dt;
                if (c[1] > height + 4) {
                    c[1] = -4;
                    c[0] = rnd.nextFloat() * width;
                }
            }
            int a = delante ? 0xC8 : 0x90;
            int x = (int) c[0], y = (int) c[1], s = (int) c[4];
            g.fill(x, y, x + s, y + s, a << 24 | 0xFFFFFF);
        }
    }

    private int tabY(int i) {
        return y0 + 14 + i * 32;
    }

    /** Las cintas de la derecha: cada una con el dibujo de su capítulo. */
    private void cintas(GuiGraphics g, int mx, int my, boolean elegida) {
        for (int i = 0; i <= ARBOL; i++) {
            boolean sel = i == tab;
            if (sel != elegida) {
                continue;
            }
            boolean encima = !sel && mx >= x0 + BW - 6 && mx < x0 + BW + 16 && my >= tabY(i) && my < tabY(i) + 28;
            int x = x0 + BW - 6 + (sel ? 4 : encima ? 2 : 0);
            g.blit(TEX, x, tabY(i), 20, 28, sel ? 652 : 608, i * 60, 40, 56, TW, TH);
            ItemStack ic = icono(i);
            if (!ic.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + 4, tabY(i) + 7, 0);
                g.pose().scale(0.75F, 0.75F, 1);
                g.renderItem(ic, 0, 0);
                g.pose().popPose();
            }
        }
    }

    private ItemStack icono(int i) {
        if (i == ARBOL) {
            return Libro.pila("navidad:adorno_esfera_roja", 1);
        }
        return Libro.pila(Tareas.CAPITULOS.get(i).icono(), 1);
    }

    private int texto(GuiGraphics g, Component c, int x, int y, int ancho, int color) {
        for (FormattedCharSequence l : font.split(c, ancho)) {
            g.drawString(font, l, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void titulo(GuiGraphics g, Component c, int x, int y, int color, float esc) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(esc, esc, 1);
        g.drawString(font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    private static int oscuro(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 255) * k), gg = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
        return 0xFF000000 | r << 16 | gg << 8 | b;
    }

    private void barra(GuiGraphics g, int x, int y, int w, int h, float f, int color) {
        g.fill(x, y, x + w, y + h, 0xFFD8C8A4);
        g.fill(x, y, x + Math.round(w * Mth.clamp(f, 0, 1)), y + h, 0xFF000000 | color);
        g.fill(x, y, x + Math.round(w * Mth.clamp(f, 0, 1)), y + 1, 0x60FFFFFF);
    }

    private boolean boton(GuiGraphics g, int x, int y, int w, int h, Component c, int mx, int my, boolean activo, long ahora) {
        boolean encima = activo && mx >= x && mx < x + w && my >= y && my < y + h;
        int v = !activo ? 144 : encima ? 112 : 80;
        if (activo && !encima) {
            // late suavecito para que se note que hay algo que reclamar
            float p = 0.5F + 0.5F * Mth.sin(ahora / 180F);
            g.setColor(1, 1, 1, 1);
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, ((int) (60 + 80 * p)) << 24 | 0xFFE070);
        }
        g.blit(TEX, x, y, w, h, 720, v, 100, 28, TW, TH);
        int tw = font.width(c);
        g.drawString(font, c, x + (w - tw) / 2, y + (h - 8) / 2, 0xFFFFFFFF, true);
        return encima;
    }

    // ------------------------------------------------------------------ un capítulo

    private void capitulo(GuiGraphics g, CompoundTag d, int cap, int mx, int my, long ahora) {
        Tareas.Capitulo c = Tareas.CAPITULOS.get(cap);
        List<Tareas.Tarea> ts = Tareas.delCapitulo(cap);
        CompoundTag prog = d.getCompound("p"), rec = d.getCompound("r");
        int lx = x0 + 16, ly = y0 + 14, ancho = 124;
        g.drawString(font, Component.translatable("navidad.libro.capitulo", cap + 1), lx, ly, TINTA_SUAVE, false);
        titulo(g, Component.translatable("navidad.libro.cap." + cap), lx, ly + 11, oscuro(c.color(), 0.8F), 1.35F);
        int y = texto(g, Component.translatable("navidad.libro.cap." + cap + ".texto"), lx, ly + 30, ancho, TINTA);
        int hechas = 0;
        for (Tareas.Tarea t : ts) {
            if (rec.getBoolean(t.id())) {
                hechas++;
            }
        }
        y = Math.max(y + 6, y0 + 104);
        g.drawString(font, Component.translatable("navidad.libro.avance", hechas, ts.size()), lx, y, TINTA, false);
        barra(g, lx, y + 11, ancho, 4, hechas / (float) ts.size(), c.color());
        y += 22;
        g.drawString(font, Component.translatable("navidad.libro.premio_capitulo"), lx, y, TINTA_SUAVE, false);
        y += 11;
        int ix = lx;
        for (Tareas.Premio p : c.premios()) {
            ItemStack st = Libro.pila(p.id(), p.n());
            g.fill(ix - 1, y - 1, ix + 17, y + 17, 0x30704A20);
            g.renderItem(st, ix, y);
            g.renderItemDecorations(font, st, ix, y);
            ix += 20;
        }
        y += 22;
        boolean capHecho = d.getCompound("cr").getBoolean(String.valueOf(cap));
        if (capHecho) {
            g.blit(TEX, lx, y - 3, 16, 16, 720, 0, 48, 48, TW, TH);
            g.drawString(font, Component.translatable("navidad.libro.capitulo_listo").withStyle(ChatFormatting.ITALIC), lx + 20, y + 1, 0xFF2E7A3A,
                    false);
        } else {
            boolean listo = hechas == ts.size();
            boton(g, lx, y - 2, 90, 14, Component.translatable("navidad.libro.reclamar"), mx, my, listo, ahora);
        }
        // las tareas
        int rx = x0 + 158, ry = y0 + 12;
        for (int i = 0; i < ts.size(); i++) {
            tarea(g, ts.get(i), prog, rec, rx, ry + i * 28, mx, my, ahora);
        }
    }

    private void tarea(GuiGraphics g, Tareas.Tarea t, CompoundTag prog, CompoundTag rec, int x, int y, int mx, int my, long ahora) {
        int w = 128;
        boolean encima = mx >= x - 2 && mx < x + w + 2 && my >= y - 1 && my < y + 26;
        if (encima) {
            g.fill(x - 2, y - 1, x + w + 2, y + 26, 0x22704A20);
        }
        int n = Math.min(prog.getInt(t.id()), t.meta());
        boolean lista = n >= t.meta(), reclamada = rec.getBoolean(t.id());
        g.renderItem(Libro.pila(t.icono(), 1), x, y + 3);
        Component nom = Component.translatable("navidad.libro.tarea." + t.id());
        List<FormattedCharSequence> ls = font.split(nom, reclamada ? 104 : lista ? 58 : 74);
        int color = reclamada ? 0xFF7A8A6A : TINTA;
        g.drawString(font, ls.get(0), x + 19, y + 1, color, false);
        if (ls.size() > 1) {
            g.drawString(font, ls.get(1), x + 19, y + 10, color, false);
        }
        int by = y + (ls.size() > 1 ? 20 : 12);
        if (!reclamada) {
            barra(g, x + 19, by, 44, 3, n / (float) t.meta(), lista ? 0x3BB860 : 0xC81E2A);
            g.pose().pushPose();
            g.pose().translate(x + 66, by - 2, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, n + "/" + t.meta(), 0, 0, TINTA_SUAVE, false);
            g.pose().popPose();
        }
        int px = x + w - 2;
        if (reclamada) {
            sello(g, t.id(), px - 20, y + 1, ahora);
        } else if (lista) {
            boton(g, px - 46, y + 5, 46, 14, Component.translatable("navidad.libro.reclamar_corto"), mx, my, true, ahora);
        } else {
            // los premios, chiquitos
            int ix = px - 14;
            for (int i = t.premios().size() - 1; i >= 0; i--) {
                Tareas.Premio p = t.premios().get(i);
                ItemStack st = Libro.pila(p.id(), p.n());
                g.pose().pushPose();
                g.pose().translate(ix, y + 6, 0);
                g.pose().scale(0.8F, 0.8F, 1);
                g.renderItem(st, 0, 0);
                g.renderItemDecorations(font, st, 0, 0);
                g.pose().popPose();
                ix -= 15;
            }
        }
    }

    /** El sello de cera de una tarea reclamada: cae grande y se asienta (la primera vez que se ve). */
    private void sello(GuiGraphics g, String id, int x, int y, long ahora) {
        Long desde = sellos.get(id);
        float esc = 1, giro = -12;
        if (desde != null) {
            float k = Mth.clamp((ahora - desde) / 220F, 0, 1);
            esc = 2.2F - 1.2F * (k * k);
            giro = -12 + (1 - k) * 30;
        }
        g.pose().pushPose();
        g.pose().translate(x + 10, y + 10, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(giro));
        g.pose().scale(esc, esc, 1);
        g.blit(TEX, -10, -10, 20, 20, 720, 0, 48, 48, TW, TH);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ mi árbol

    private void miArbol(GuiGraphics g, CompoundTag d, int mx, int my, long ahora) {
        int lx = x0 + 16, ly = y0 + 14, ancho = 124;
        titulo(g, Component.translatable("navidad.libro.mi_arbol"), lx, ly, oscuro(0x2E8A48, 0.75F), 1.35F);
        CompoundTag a = d.getCompound("arbol");
        if (!a.getBoolean("tiene")) {
            texto(g, Component.translatable("navidad.libro.sin_arbol"), lx, ly + 22, ancho, TINTA);
            ItemStack pino = Libro.pila("navidad:pino_navidad", 1);
            g.pose().pushPose();
            g.pose().translate(lx + 40, ly + 90, 0);
            g.pose().scale(3, 3, 1);
            g.renderItem(pino, 0, 0);
            g.pose().popPose();
        } else {
            int nivel = Math.max(1, a.getInt("nivel"));
            // estrellas
            for (int i = 0; i < NivelArbol.DESDE.length; i++) {
                g.blit(TEX, lx + i * 11, ly + 17, 10, 10, i < nivel ? 720 : 740, 176, 16, 16, TW, TH);
            }
            g.drawString(font, Component.translatable("navidad.arbol.nivel." + nivel).withStyle(ChatFormatting.BOLD), lx, ly + 30,
                    0xFFB8860B, false);
            // el árbol de verdad, girando
            arbol3d(g, a, lx + 32, ly + 134, ahora);
            int y = ly + 44;
            int rx = lx + 66;
            g.drawString(font, Component.translatable("navidad.libro.puntos_total", a.getInt("total")), rx, y, TINTA, false);
            y += 12;
            y = texto(g, Component.translatable("navidad.libro.puntos_adornos", a.getInt("adornos")), rx, y, 60, TINTA_SUAVE);
            y = texto(g, Component.translatable("navidad.libro.puntos_variedad", a.getInt("variedad")), rx, y, 60, TINTA_SUAVE);
            y = texto(g, Component.translatable("navidad.libro.puntos_casa", a.getInt("casa"), NivelArbol.MAX_CASA), rx, y, 60, TINTA_SUAVE);
            int sig = nivel >= NivelArbol.DESDE.length ? -1 : NivelArbol.DESDE[nivel];
            int by = y0 + 160;
            if (sig > 0) {
                int antes = NivelArbol.DESDE[nivel - 1];
                g.drawString(font, Component.translatable("navidad.libro.siguiente", sig - a.getInt("total"),
                        Component.translatable("navidad.arbol.nivel." + (nivel + 1))), lx, by, TINTA, false);
                barra(g, lx, by + 11, ancho, 4, (a.getInt("total") - antes) / (float) (sig - antes), 0xFFC040);
            } else {
                g.drawString(font, Component.translatable("navidad.libro.maximo").withStyle(ChatFormatting.ITALIC), lx, by, 0xFFB8860B, false);
            }
        }
        // la derecha: qué regalos da cada nivel y consejos
        int rx = x0 + 158, ry = y0 + 14;
        ry = texto(g, Component.translatable("navidad.libro.regalos_nivel"), rx, ry, 128, oscuro(0xC81E2A, 0.8F)) + 3;
        int actual = a.getBoolean("tiene") ? Math.max(1, a.getInt("nivel")) : 0;
        for (int n = 1; n <= NivelArbol.DESDE.length; n++) {
            if (n == actual) {
                g.fill(rx - 2, ry - 2, rx + 130, ry + 16, 0x40FFC040);
            }
            g.drawString(font, Component.literal(NivelArbol.DESDE[n - 1] + "+ ").append(Component.translatable("navidad.arbol.nivel." + n)),
                    rx, ry, n <= actual ? TINTA : TINTA_SUAVE, false);
            g.pose().pushPose();
            g.pose().translate(rx + 4, ry + 9, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, Component.translatable("navidad.libro.da." + n), 0, 0, TINTA_SUAVE, false);
            g.pose().popPose();
            ry += 18;
        }
        g.pose().pushPose();
        g.pose().translate(rx, ry + 1, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        texto(g, Component.translatable("navidad.libro.consejo"), 0, 0, 170, 0xFF2E6A3A);
        g.pose().popPose();
    }

    /** El árbol del jugador (con sus adornos de verdad) girando despacio, como en el inventario. */
    private void arbol3d(GuiGraphics g, CompoundTag a, int x, int y, long ahora) {
        String[] lugares = new String[PuntosArbol.LUGARES.length];
        java.util.Arrays.fill(lugares, "");
        CompoundTag dib = a.getCompound("dibujo");
        ListTag l = dib.getList("Adornos", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(l.size(), lugares.length); i++) {
            lugares[i] = l.getString(i);
        }
        var mc = Minecraft.getInstance();
        float t = mc.level == null ? ahora / 50F : mc.level.getGameTime() + mc.getFrameTime();
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 120);
        ps.scale(36, -36, 36);
        ps.mulPose(Axis.XP.rotationDegrees(-12));
        ps.mulPose(Axis.YP.rotationDegrees(ahora / 40F % 360));
        ps.translate(-0.5F, 0, -0.5F);
        Lighting.setupForEntityInInventory();
        PinoRenderer.dibujar(ps, g.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, lugares, dib.getInt("Luces"), dib.getInt("Guirnaldas"),
                dib.getString("Punta"), t, null, mc.level, 3, true);
        g.flush();
        Lighting.setupFor3DItems();
        ps.popPose();
    }

    // ------------------------------------------------------------------ hoja que pasa, chispas y premios que vuelan

    private void pasarHoja(GuiGraphics g, long ahora) {
        if (cambio < 0) {
            return;
        }
        float k = (ahora - cambio) / 320F;
        if (k >= 1) {
            cambio = -1;
            return;
        }
        int lomo = x0 + 150, top = y0 + 8, alto = BH - 16;
        boolean adelante = tab > tabAntes;
        float f = adelante ? k : 1 - k;
        // de la derecha al lomo (se dobla) y del lomo a la izquierda (se abre)
        int ancho = (int) (140 * Math.abs(Mth.cos(f * Mth.PI)));
        int sombra = (int) (90 * Mth.sin(f * Mth.PI));
        if (f < 0.5F) {
            g.blit(TEX, lomo + 2, top, ancho, alto, 304, 16, 278, 348, TW, TH);
            g.fill(lomo + 2, top, lomo + 2 + ancho, top + alto, sombra << 24);
        } else {
            g.blit(TEX, lomo - 2 - ancho, top, ancho, alto, 18, 16, 278, 348, TW, TH);
            g.fill(lomo - 2 - ancho, top, lomo - 2, top + alto, sombra << 24);
        }
    }

    private void efectos(GuiGraphics g, float dt, long ahora) {
        for (int i = chispas.size() - 1; i >= 0; i--) {
            float[] c = chispas.get(i);
            c[0] += c[2] * dt;
            c[1] += c[3] * dt;
            c[3] += 260 * dt;
            c[4] -= dt;
            if (c[4] <= 0) {
                chispas.remove(i);
                continue;
            }
            int a = (int) (255 * Math.min(1, c[4] * 2));
            int col = c[5] > 0.5F ? 0xFFE070 : 0xFFFFFF;
            g.fill((int) c[0], (int) c[1], (int) c[0] + 2, (int) c[1] + 2, a << 24 | col);
        }
        for (int i = voladores.size() - 1; i >= 0; i--) {
            Volador v = voladores.get(i);
            float k = (ahora - v.desde()) / 700F;
            if (k >= 1) {
                voladores.remove(i);
                continue;
            }
            float tx = width / 2F, ty = height - 20;
            float e = k * k * (3 - 2 * k);
            float x = Mth.lerp(e, v.x(), tx), y = Mth.lerp(e, v.y(), ty) - Mth.sin(k * Mth.PI) * 60;
            float s = 1.2F - 0.6F * k;
            g.pose().pushPose();
            g.pose().translate(x, y, 300);
            g.pose().scale(s, s, 1);
            g.renderItem(v.st(), -8, -8);
            g.pose().popPose();
        }
    }

    private void chisporroteo(float x, float y, int n) {
        for (int i = 0; i < n; i++) {
            float a = rnd.nextFloat() * 6.28F, v = 40 + rnd.nextFloat() * 110;
            chispas.add(new float[]{x, y, Mth.cos(a) * v, Mth.sin(a) * v - 80, 0.6F + rnd.nextFloat() * 0.6F, rnd.nextFloat()});
        }
    }

    /** Lo que cambió desde la última vez (tareas recién reclamadas: su sello cae). */
    private void notarCambios(CompoundTag d, long ahora) {
        if (visto != null) {
            CompoundTag r = d.getCompound("r"), r0 = visto.getCompound("r");
            for (String k : r.getAllKeys()) {
                if (r.getBoolean(k) && !r0.getBoolean(k)) {
                    sellos.put(k, ahora);
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PUT, 0.9F));
                }
            }
        }
        visto = d.copy();
    }

    // ------------------------------------------------------------------ ratón

    @Override
    public boolean mouseClicked(double mxd, double myd, int b) {
        int mx = (int) mxd, my = (int) myd;
        long ahora = System.currentTimeMillis();
        for (int i = 0; i <= ARBOL; i++) {
            if (mx >= x0 + BW - 6 && mx < x0 + BW + 18 && my >= tabY(i) && my < tabY(i) + 28 && i != tab) {
                tabAntes = tab;
                tab = i;
                tabGuardado = i;
                cambio = ahora;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1F));
                return true;
            }
        }
        if (tab != ARBOL) {
            CompoundTag d = datos();
            CompoundTag prog = d.getCompound("p"), rec = d.getCompound("r");
            List<Tareas.Tarea> ts = Tareas.delCapitulo(tab);
            int rx = x0 + 158, ry = y0 + 12;
            for (int i = 0; i < ts.size(); i++) {
                Tareas.Tarea t = ts.get(i);
                int y = ry + i * 28, bx = rx + 128 - 2 - 46, by = y + 5;
                if (mx >= bx && mx < bx + 46 && my >= by && my < by + 14 && prog.getInt(t.id()) >= t.meta() && !rec.getBoolean(t.id())) {
                    CompoundTag q = new CompoundTag();
                    q.putString("tarea", t.id());
                    ClientData.send(Navidad.MODID + ":libro_reclamar", q);
                    chisporroteo(bx + 23, by + 7, 26);
                    for (int k = 0; k < t.premios().size(); k++) {
                        Tareas.Premio p = t.premios().get(k);
                        voladores.add(new Volador(Libro.pila(p.id(), p.n()), bx + 10 + k * 14, by, ahora + k * 90L));
                    }
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    return true;
                }
            }
            // el premio del capítulo
            int hechas = 0;
            for (Tareas.Tarea t : ts) {
                if (rec.getBoolean(t.id())) {
                    hechas++;
                }
            }
            int[] cb = botonCapitulo();
            if (hechas == ts.size() && !d.getCompound("cr").getBoolean(String.valueOf(tab)) && mx >= cb[0] && mx < cb[0] + 90 && my >= cb[1]
                    && my < cb[1] + 14) {
                CompoundTag q = new CompoundTag();
                q.putInt("cap", tab);
                ClientData.send(Navidad.MODID + ":libro_capitulo", q);
                chisporroteo(cb[0] + 45, cb[1] + 7, 60);
                List<Tareas.Premio> ps = Tareas.CAPITULOS.get(tab).premios();
                for (int k = 0; k < ps.size(); k++) {
                    voladores.add(new Volador(Libro.pila(ps.get(k).id(), ps.get(k).n()), cb[0] + 10 + k * 18, cb[1], ahora + k * 120L));
                }
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mxd, myd, b);
    }

    /** Dónde queda el botón del premio del capítulo (depende del largo del texto, igual que al dibujarlo). */
    private int[] botonCapitulo() {
        int lx = x0 + 16, ly = y0 + 14;
        int lineas = font.split(Component.translatable("navidad.libro.cap." + tab + ".texto"), 124).size();
        int y = ly + 30 + lineas * 10;
        y = Math.max(y + 6, y0 + 104);
        y += 22 + 11 + 22;
        return new int[]{lx, y - 2};
    }

    // ------------------------------------------------------------------ ayudas al pasar el ratón

    private void tooltips(GuiGraphics g, CompoundTag d, int mx, int my) {
        for (int i = 0; i <= ARBOL; i++) {
            if (mx >= x0 + BW - 6 && mx < x0 + BW + 18 && my >= tabY(i) && my < tabY(i) + 28) {
                g.renderTooltip(font, i == ARBOL ? Component.translatable("navidad.libro.mi_arbol")
                        : Component.translatable("navidad.libro.cap." + i), mx, my);
                return;
            }
        }
        if (tab == ARBOL) {
            return;
        }
        CompoundTag prog = d.getCompound("p");
        List<Tareas.Tarea> ts = Tareas.delCapitulo(tab);
        int rx = x0 + 158, ry = y0 + 12;
        for (int i = 0; i < ts.size(); i++) {
            Tareas.Tarea t = ts.get(i);
            int y = ry + i * 28;
            if (mx >= rx - 2 && mx < rx + 130 && my >= y - 1 && my < y + 26) {
                List<Component> l = new ArrayList<>();
                l.add(Component.translatable("navidad.libro.tarea." + t.id()).withStyle(ChatFormatting.GOLD));
                l.add(Component.translatable("navidad.libro.como." + t.id()).withStyle(ChatFormatting.GRAY));
                l.add(Component.translatable("navidad.libro.llevas", Math.min(prog.getInt(t.id()), t.meta()), t.meta())
                        .withStyle(ChatFormatting.WHITE));
                l.add(Component.translatable("navidad.libro.premio").withStyle(ChatFormatting.YELLOW));
                for (Tareas.Premio p : t.premios()) {
                    ItemStack st = Libro.pila(p.id(), p.n());
                    l.add(Component.literal("  " + p.n() + "× ").append(st.getHoverName()).withStyle(ChatFormatting.AQUA));
                }
                g.renderComponentTooltip(font, l, mx, my);
                return;
            }
        }
        // los premios del capítulo
        int lx = x0 + 16;
        int[] cb = botonCapitulo();
        int iy = cb[1] + 2 - 22;
        List<Tareas.Premio> ps = Tareas.CAPITULOS.get(tab).premios();
        for (int k = 0; k < ps.size(); k++) {
            int ix = lx + k * 20;
            if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
                g.renderTooltip(font, Libro.pila(ps.get(k).id(), ps.get(k).n()), mx, my);
                return;
            }
        }
    }
}

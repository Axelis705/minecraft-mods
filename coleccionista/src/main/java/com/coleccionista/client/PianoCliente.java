package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.mapa.SalaPiano;
import com.coleccionista.prueba.Piano;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * (Cliente) El piano gigante. Lo que brilla en los pianos (mientras toca la bailarina, la tecla de cada nota, en todos los pianos a la vez,
 * con una columna de luz de su color; mientras tocan, el destello de la tecla que pisó cada quien: de su color si estuvo bien, rojo si no;
 * el piano entero en rojo si te equivocaste y dorado si ya terminaste) y lo que dice el HUD (escucha, toca, la nota en la que vas y el
 * tiempo que te queda, cómo van los demás y qué pasó al final de la ronda).
 */
public final class PianoCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag();
    /** Cuándo (tiempo del mundo) empieza a tocar la bailarina, desde que empieza la muestra. */
    private static final int ANTES = 30;

    private PianoCliente() {
    }

    public static void init() {
        ClientData.listen(Piano.KEY, t -> estado = t);
        com.axeliscore.fx.client.Hud.add(PianoCliente::hud);
        MinecraftForge.EVENT_BUS.addListener(PianoCliente::dibujarMundo);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    public static CompoundTag estado() {
        return estado;
    }

    private static List<CompoundTag> pianos() {
        List<CompoundTag> l = new ArrayList<>();
        for (Tag t : estado.getList("pianos", Tag.TAG_COMPOUND)) {
            l.add((CompoundTag) t);
        }
        return l;
    }

    /** Lo mío (mi piano), o null si no toco. */
    public static CompoundTag mio() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        UUID yo = mc.player.getUUID();
        for (CompoundTag c : pianos()) {
            if (c.hasUUID("id") && yo.equals(c.getUUID("id"))) {
                return c;
            }
        }
        return null;
    }

    private static long ahora() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    /** La nota que está tocando la bailarina ahora (-1 ninguna) y qué tan encendida está (0..1). */
    private static int notaMuestra() {
        if (!"MUESTRA".equals(fase())) {
            return -1;
        }
        int[] m = estado.getIntArray("melodia");
        int tn = Math.max(1, Nbt.i(estado, "tNota"));
        long dt = ahora() - estado.getLong("desde") - ANTES;
        if (dt < 0) {
            return -1;
        }
        int i = (int) (dt / tn);
        return i < m.length ? i : -1;
    }

    private static float brilloMuestra() {
        int tn = Math.max(1, Nbt.i(estado, "tNota"));
        long dt = ahora() - estado.getLong("desde") - ANTES;
        float c = (dt % tn) / (float) tn;
        return c < 0.7F ? 1F - c * 0.4F : Math.max(0F, (1F - c) / 0.3F * 0.72F);
    }

    // ------------------------------------------------------------------ las teclas que brillan

    private static void dibujarMundo(RenderLevelStageEvent ev) {
        if (ev.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !activa()) {
            return;
        }
        String f = fase();
        if (!"MUESTRA".equals(f) && !"TOCA".equals(f) && !"REVISA".equals(f) && !"CAE".equals(f)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = ev.getCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = ev.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderType rt = RenderType.entityTranslucent(com.axeliscore.plat.client.Draw.BLANCO);
        java.util.List<float[]> cols = new java.util.ArrayList<>();
        VertexConsumer vc = buf.getBuffer(rt);
        long gt = ahora();
        int i = notaMuestra();
        int[] mel = estado.getIntArray("melodia");
        for (CompoundTag c : pianos()) {
            int[] p = c.getIntArray("pos");
            if (p.length < 3 || Nbt.b(c, "caido")) {
                continue;
            }
            float y = p[1] + 1.02F;
            // un brillo tenue en todas las teclas (para que se vean bien con la luz baja)
            for (int k = 0; k < SalaPiano.TECLA.length; k++) {
                tecla(vc, ps, p, k, y, SalaPiano.BRILLO[k], 0.16F);
            }
            boolean juega = Nbt.b(c, "juega");
            if (i >= 0 && i < mel.length && juega) {
                float a = brilloMuestra();
                tecla(vc, ps, p, mel[i], y + 0.005F, SalaPiano.BRILLO[mel[i]], 0.95F * a);
                cols.add(new float[] {p[0], p[1], p[2], mel[i], y, SalaPiano.BRILLO[mel[i]], 0.7F * a});
            }
            if (("TOCA".equals(f) || "REVISA".equals(f)) && juega) {
                int d = Nbt.i(c, "destello");
                long en = c.getLong("destelloEn");
                if (d >= 0 && gt - en < 14) {
                    float a = 1F - (gt - en) / 14F;
                    boolean bien = Nbt.b(c, "destelloBien");
                    tecla(vc, ps, p, d, y + 0.006F, bien ? SalaPiano.BRILLO[d] : 0xFF2020, 0.85F * a);
                    cols.add(new float[] {p[0], p[1], p[2], d, y, bien ? SalaPiano.BRILLO[d] : 0xFF2020, 0.6F * a});
                }
                if (Nbt.b(c, "mal")) {
                    float a = (float) (0.28 + 0.14 * Math.sin(gt * 0.5));
                    for (int k = 0; k < SalaPiano.TECLA.length; k++) {
                        tecla(vc, ps, p, k, y + 0.007F, 0xFF1A1A, a);
                    }
                } else if (Nbt.b(c, "listo")) {
                    cuadro(vc, ps, p[0] + 0.08F, y + 0.007F, p[2] + 0.08F, 1.84F, 0xFFD04A, 0.5F);
                }
            }
        }
        buf.endBatch(rt);
        // las columnas de luz: con mezcla aditiva (brillan de verdad en lo oscuro)
        if (!cols.isEmpty()) {
            VertexConsumer lv = buf.getBuffer(RenderType.lightning());
            for (float[] c : cols) {
                columna(lv, ps, new int[] {(int) c[0], (int) c[1], (int) c[2]}, (int) c[3], c[4], (int) c[5], c[6]);
            }
            buf.endBatch(RenderType.lightning());
        }
        ps.popPose();
    }

    private static void tecla(VertexConsumer vc, PoseStack ps, int[] p, int k, float y, int rgb, float a) {
        int[] t = SalaPiano.TECLA[k];
        cuadro(vc, ps, p[0] + t[0] + 0.06F, y, p[2] + t[1] + 0.06F, 1.88F, rgb, a);
    }

    private static void cuadro(VertexConsumer vc, PoseStack ps, float x, float y, float z, float lado, int rgb, float a) {
        if (a <= 0.01F) {
            return;
        }
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, al = (int) (Math.min(1F, a) * 255);
        int luz = 0xF000F0;
        vc.vertex(m, x, y, z).color(r, g, b, al).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x, y, z + lado).color(r, g, b, al).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + lado, y, z + lado).color(r, g, b, al).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0)
                .endVertex();
        vc.vertex(m, x + lado, y, z).color(r, g, b, al).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
    }

    /**
     * Una columna de luz sobre la tecla (cuatro caras que se desvanecen hacia arriba, y un brillo encima de la tecla), para el tipo de
     * dibujo aditivo (posición y color).
     */
    private static void columna(VertexConsumer vc, PoseStack ps, int[] p, int k, float y, int rgb, float a) {
        if (a <= 0.01F) {
            return;
        }
        int[] t = SalaPiano.TECLA[k];
        float x0 = p[0] + t[0] + 0.1F, z0 = p[2] + t[1] + 0.1F, x1 = x0 + 1.8F, z1 = z0 + 1.8F, alto = 3.2F;
        Matrix4f m = ps.last().pose();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, al = (int) (Math.min(1F, a) * 255);
        float[][] caras = {{x0, z0, x1, z0}, {x1, z0, x1, z1}, {x1, z1, x0, z1}, {x0, z1, x0, z0}};
        for (float[] c : caras) {
            // las dos caras (por dentro y por fuera)
            vc.vertex(m, c[0], y, c[1]).color(r, g, b, al).endVertex();
            vc.vertex(m, c[2], y, c[3]).color(r, g, b, al).endVertex();
            vc.vertex(m, c[2], y + alto, c[3]).color(r, g, b, 0).endVertex();
            vc.vertex(m, c[0], y + alto, c[1]).color(r, g, b, 0).endVertex();
            vc.vertex(m, c[0], y + alto, c[1]).color(r, g, b, 0).endVertex();
            vc.vertex(m, c[2], y + alto, c[3]).color(r, g, b, 0).endVertex();
            vc.vertex(m, c[2], y, c[3]).color(r, g, b, al).endVertex();
            vc.vertex(m, c[0], y, c[1]).color(r, g, b, al).endVertex();
        }
        // el brillo sobre la tecla (las dos caras)
        float yy = y + 0.01F;
        vc.vertex(m, x0, yy, z0).color(r, g, b, al).endVertex();
        vc.vertex(m, x0, yy, z1).color(r, g, b, al).endVertex();
        vc.vertex(m, x1, yy, z1).color(r, g, b, al).endVertex();
        vc.vertex(m, x1, yy, z0).color(r, g, b, al).endVertex();
        vc.vertex(m, x1, yy, z0).color(r, g, b, al).endVertex();
        vc.vertex(m, x1, yy, z1).color(r, g, b, al).endVertex();
        vc.vertex(m, x0, yy, z1).color(r, g, b, al).endVertex();
        vc.vertex(m, x0, yy, z0).color(r, g, b, al).endVertex();
    }

    // ------------------------------------------------------------------ el HUD

    private static String t(String k, Object... a) {
        return Component.translatable(MOD + ".piano.hud." + k, a).getString();
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa() || !EventoHud.visible()) {
            return;
        }
        String f = fase();
        if (!"MUESTRA".equals(f) && !"TOCA".equals(f) && !"REVISA".equals(f) && !"CAE".equals(f)) {
            return;
        }
        Font font = Ui.font();
        int largo = Nbt.i(estado, "largo"), ronda = Nbt.i(estado, "ronda");
        boolean desempate = Nbt.b(estado, "desempate");
        CompoundTag yo = mio();
        boolean juego = yo != null && Nbt.b(yo, "juega");
        // arriba a la izquierda: la ronda
        EventoHud.lado(g, t(desempate ? "ronda_desempate" : "ronda", ronda, largo), EventoHud.ORO, true);
        if (yo == null) {
            EventoHud.lado(g, t("no_tocas"), 0xFFA89E96, false);
        } else if (Nbt.b(yo, "salvado")) {
            EventoHud.lado(g, t("salvado"), EventoHud.PORCELANA, false);
        } else if (juego && ("MUESTRA".equals(f) || "TOCA".equals(f))) {
            EventoHud.lado(g, t("centro"), 0xFFA89E96, false);
        }
        // cómo van los demás
        for (CompoundTag c : pianos()) {
            if (yo != null && c.hasUUID("id") && yo.hasUUID("id") && c.getUUID("id").equals(yo.getUUID("id"))) {
                continue;
            }
            String n = Nbt.s(c, "nombre");
            String s;
            int col;
            if (Nbt.b(c, "caido")) {
                s = t("otro_cayo", n);
                col = 0xFF6A6260;
            } else if (Nbt.b(c, "salvado")) {
                s = t("otro_salvado", n);
                col = 0xFFA89E96;
            } else if (!Nbt.b(c, "juega")) {
                continue;
            } else if ("TOCA".equals(f) || "REVISA".equals(f)) {
                if (Nbt.b(c, "mal")) {
                    s = t("otro_mal", n);
                    col = EventoHud.ROJO_CL;
                } else if (Nbt.b(c, "listo")) {
                    s = t("otro_listo", n);
                    col = EventoHud.ORO;
                } else {
                    s = t("otro_va", n, Nbt.i(c, "nota"), largo);
                    col = 0xFFD8D0C8;
                }
            } else {
                s = t("otro_escucha", n);
                col = 0xFFA89E96;
            }
            EventoHud.lado(g, s, col, false);
        }
        long gt = ahora();
        switch (f) {
            case "MUESTRA" -> {
                EventoHud.texto(g, font, t("escucha"), w / 2, h / 2 - 72, 2.2F, 0xFFF4ECE4);
                puntos(g, w / 2, h / 2 - 46, largo, notaMuestra() + 1, -1, false);
            }
            case "TOCA" -> {
                if (yo != null && juego) {
                    int nota = Nbt.i(yo, "nota");
                    if (Nbt.b(yo, "mal")) {
                        EventoHud.texto(g, font, t("mal"), w / 2, h / 2 - 72, 2.4F, EventoHud.ROJO_CL);
                        puntos(g, w / 2, h / 2 - 46, largo, nota, nota, false);
                    } else if (Nbt.b(yo, "listo")) {
                        EventoHud.texto(g, font, t("listo"), w / 2, h / 2 - 72, 2.2F, EventoHud.ORO);
                        puntos(g, w / 2, h / 2 - 46, largo, largo, -1, true);
                    } else {
                        EventoHud.texto(g, font, t("toca", nota + 1, largo), w / 2, h / 2 - 72, 2.2F, 0xFFF4ECE4);
                        puntos(g, w / 2, h / 2 - 46, largo, nota, -1, false);
                        // el tiempo para esta nota
                        float total = Math.max(1, Nbt.i(estado, "segundosNota")) * 20F;
                        float queda = Math.max(0F, 1F - (gt - yo.getLong("desdeNota")) / total);
                        int bw = 120, bx = w / 2 - bw / 2, by = h / 2 - 30;
                        g.fill(bx - 1, by - 1, bx + bw + 1, by + 4, 0x90000000);
                        g.fill(bx, by, bx + (int) (bw * queda), by + 3, queda < 0.3F ? EventoHud.ROJO_CL : 0xFFE8E2D8);
                    }
                } else {
                    EventoHud.texto(g, font, t("tocan"), w / 2, h / 2 - 72, 1.8F, 0xFFD8D0C8);
                }
            }
            case "REVISA" -> {
                String r = Nbt.s(estado, "resultado");
                List<String> mal = new ArrayList<>();
                for (CompoundTag c : pianos()) {
                    if (Nbt.b(c, "mal") && !Nbt.b(c, "caido") && (Nbt.b(c, "juega") || "desempate".equals(r) || "cae".equals(r))) {
                        mal.add(Nbt.s(c, "nombre"));
                    }
                }
                String s = switch (r) {
                    case "nadie" -> t("r_nadie");
                    case "todos" -> t("r_todos");
                    case "cae" -> t("r_cae", String.join(", ", mal));
                    case "desempate" -> t("r_desempate", String.join(", ", mal));
                    default -> "";
                };
                int col = "nadie".equals(r) ? EventoHud.ORO : "todos".equals(r) ? 0xFFD8D0C8 : EventoHud.ROJO_CL;
                EventoHud.texto(g, font, s, w / 2, h / 2 - 72, 1.8F, col);
                if ("desempate".equals(r)) {
                    EventoHud.texto(g, font, t("r_desempate2"), w / 2, h / 2 - 50, 1.2F, 0xFFD8D0C8);
                }
            }
            case "CAE" -> {
                List<String> caen = new ArrayList<>();
                for (CompoundTag c : pianos()) {
                    if (Nbt.b(c, "caido") && Nbt.b(c, "mal")) {
                        caen.add(Nbt.s(c, "nombre"));
                    }
                }
                long dt = gt - estado.getLong("desde");
                float a = Math.min(1F, dt / 10F);
                if (!caen.isEmpty()) {
                    EventoHud.texto(g, font, t("cae", String.join(", ", caen)), w / 2, h / 2 - 72, 2.2F, Ui.alpha(EventoHud.ROJO_CL, a));
                }
            }
            default -> {
            }
        }
    }

    /** Una bolita por nota: llenas las que ya van, la actual brillando, la fallada en rojo. */
    private static void puntos(G g, int cx, int y, int n, int hechas, int fallo, boolean todas) {
        if (n <= 0) {
            return;
        }
        int paso = 10, x0 = cx - (n * paso) / 2;
        for (int i = 0; i < n; i++) {
            int x = x0 + i * paso;
            int col = i == fallo ? EventoHud.ROJO_CL : i < hechas || todas ? EventoHud.ORO : 0x70FFFFFF;
            g.fill(x + 1, y, x + 7, y + 6, 0xA0000000);
            g.fill(x + 2, y + 1, x + 6, y + 5, col);
        }
    }
}

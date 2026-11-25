package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.coleccionista.Coleccionista;
import com.coleccionista.bloque.PantallaBE;
import com.coleccionista.bloque.PantallaBlock;
import com.coleccionista.evento.Pantallas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * (Cliente) Dibuja la pantalla: 3x2 bloques (la grande 11x7) delante del panel, siempre brillante. Según lo que mande el evento:
 * apagada (cristal oscuro con un reflejo), estática, el Coleccionista (con los ojos que brillan, voz en barras rojas y subtítulos) o un
 * texto grande. Al cambiar de modo pasa un chispazo de estática.
 */
public class PantallaRenderer implements BlockEntityRenderer<PantallaBE> {
    private static final String MOD = Coleccionista.MODID;
    private static final ResourceLocation BLANCO = Draw.BLANCO;
    private static final ResourceLocation RETRATO = Ids.rl(MOD, "textures/pantalla/coleccionista.png");
    private static final ResourceLocation ESTATICA = Ids.rl(MOD, "textures/pantalla/estatica.png");
    private static final int LUZ = 0xF000F0;
    /** El último modo y cuándo cambió, aparte para las chicas [0] y las grandes [1] (pueden estar en modos distintos). */
    private static final String[] modoAnterior = {"", ""};
    /** La línea que dice ahora el Coleccionista y desde cuándo (para que las figuras de la caja aparezcan una por una). */
    private static String lineaVista = "";
    private static long lineaDesde;
    private static final long[] cambioK = new long[2];
    private static long cambio;

    public PantallaRenderer(BlockEntityRendererProvider.Context c) {
    }

    @Override
    public boolean shouldRenderOffScreen(PantallaBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static CompoundTag estado() {
        CompoundTag t = ClientData.get(Pantallas.KEY);
        return t == null ? new CompoundTag() : t;
    }

    @Override
    public void render(PantallaBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!(st.getBlock() instanceof PantallaBlock)) {
            return;
        }
        Direction f = st.getValue(PantallaBlock.FACING);
        boolean grande = st.getValue(PantallaBlock.GRANDE);
        Atajos.vista(be.getBlockPos());
        boolean mediana = grande && be.tam() == 1;
        float w = mediana ? 7F : grande ? 11F : 3F, h = mediana ? 4F : grande ? 7F : 2F;
        CompoundTag t = estado();
        String modo = Nbt.s(t, "modo");
        if (modo.isEmpty() || !grande && t.getBoolean("soloGrande")) {
            modo = "apagada";
        }
        long ms = System.currentTimeMillis();
        int kk = grande ? 1 : 0;
        if (!modo.equals(modoAnterior[kk])) {
            modoAnterior[kk] = modo;
            cambioK[kk] = ms;
        }
        cambio = cambioK[kk];
        buf0 = buf;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        giro = -f.toYRot();
        float z = -0.5F + 2.05F / 16F;
        Draw d = new Draw(buf);
        // marco
        quad(d.translucent(BLANCO), ps, -w / 2 - 0.08F, -h / 2 - 0.08F, w / 2 + 0.08F, h / 2 + 0.08F, z, 0, 0, 1, 1, 0xFF0C0C10, 0xFF1C1C22);
        z += 0.004F;
        float desde = (ms - cambio) / 1000F;
        switch (modo) {
            case "estatica" -> estatica(d, ps, w, h, z, 0.9F);
            case "coleccionista" -> coleccionista(d, ps, w, h, z, t, ms);
            case "texto" -> texto(d, ps, w, h, z, Nbt.s(t, "texto"));
            default -> {
                // apagada: cristal oscuro con un reflejo diagonal suave
                quad(d.translucent(BLANCO), ps, -w / 2, -h / 2, w / 2, h / 2, z, 0, 0, 1, 1, 0xFF05060A, 0xFF101420);
                quad(d.translucent(BLANCO), ps, -w / 2, h / 2 - h * 0.18F, w / 2, h / 2, z + 0.002F, 0, 0, 1, 1, 0x00FFFFFF, 0x10FFFFFF);
            }
        }
        if (desde < 0.35F && !"apagada".equals(modo)) {
            estatica(d, ps, w, h, z + ColeccionistaTV.FRENTE + 0.01F, 1F - desde / 0.35F);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ modos

    private static void estatica(Draw d, PoseStack ps, float w, float h, float z, float alfa) {
        float u = (float) Math.random(), v = (float) Math.random();
        int a = (int) (alfa * 255) << 24;
        quad(d.translucent(ESTATICA), ps, -w / 2, -h / 2, w / 2, h / 2, z, u, v, u + w * 0.35F, v + h * 0.35F, a | 0xC8C8C8, a | 0xFFFFFF);
    }

    private static void coleccionista(Draw d, PoseStack ps, float w, float h, float z, CompoundTag t, long ms) {
        boolean habla = Nbt.b(t, "hablando");
        String clave = Nbt.s(t, "linea");
        // fondo de terciopelo rojo oscuro (más claro arriba)
        quad(d.translucent(BLANCO), ps, -w / 2, -h / 2, w / 2, h / 2, z, 0, 0, 1, 1, 0xFF180406, 0xFF3A0A10);
        if (!clave.equals(lineaVista)) {
            lineaVista = clave;
            lineaDesde = ms;
        }
        // él, en vivo y en 3D (si el modelo no cargara, el retrato de antes)
        if (ColeccionistaTV.dibujar(ps, buf0, w, h, z, habla, clave, ms, giro)) {
            encima(d, ps, w, h, z + ColeccionistaTV.FRENTE, habla, clave, ms);
            figuras(d, ps, w, h, z + ColeccionistaTV.FRENTE + 0.01F, clave, ms);
            return;
        }
        figuras(d, ps, w, h, z + 0.01F, clave, ms);
        // el retrato: cuadrado, un poco a la izquierda en la grande; se mece apenas
        float lado = h * 0.92F;
        float s = (float) Math.sin(ms / 700.0) * 0.02F * h, giro = habla ? (float) Math.sin(ms / 230.0) * 1.6F : 0F;
        ps.pushPose();
        ps.translate(0, -h / 2 + lado / 2 + s, z + 0.002F);
        ps.mulPose(Axis.ZP.rotationDegrees(giro));
        quad(d.translucent(RETRATO), ps, -lado / 2, -lado / 2, lado / 2, lado / 2, 0, 0, 0, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF);
        // los ojos brillan (más cuando habla)
        float brillo = habla ? 0.55F + 0.45F * (float) Math.abs(Math.sin(ms / 140.0)) : 0.25F + 0.1F * (float) Math.sin(ms / 900.0);
        int a = (int) (brillo * 200) << 24;
        for (float ox : new float[] {24.5F, 39.5F}) {
            float cx = -lado / 2 + lado * ox / 64F, cy = lado / 2 - lado * 26.8F / 64F, r = lado * 2.4F / 64F;
            quad(d.translucent(Draw.ORBE), ps, cx - r, cy - r, cx + r, cy + r, 0.003F, 0, 0, 1, 1, a | 0xFF2020, a | 0xFF4040);
        }
        ps.popPose();
        // barras de la voz
        if (habla) {
            int n = 9;
            float bw = w * 0.022F, x0 = w / 2 - n * bw * 1.6F - w * 0.04F, y0 = -h / 2 + h * 0.06F;
            for (int i = 0; i < n; i++) {
                float k = (float) Math.abs(Math.sin(ms / (90.0 + i * 17) + i));
                float bh = h * (0.05F + 0.22F * k);
                float x = x0 + i * bw * 1.6F;
                quad(d.translucent(BLANCO), ps, x, y0, x + bw, y0 + bh, z + 0.004F, 0, 0, 1, 1, 0xFFB3121A, 0xFFFF5A5A);
            }
        }
        // líneas de TV vieja
        for (int i = 0; i < (int) (h * 24); i++) {
            float y = -h / 2 + (i + 0.5F) * h / (h * 24F);
            quad(d.translucent(BLANCO), ps, -w / 2, y, w / 2, y + h / (h * 24F) * 0.2F, z + 0.006F, 0, 0, 1, 1, 0x12000000, 0x12000000);
        }
        // subtítulo dentro de la pantalla
        if (habla && !clave.isEmpty() && !Cine.enPantalla()) {
            texto(d, ps, w, h, z + 0.008F, Component.translatable(clave).getString(), -h / 2 + h * 0.18F, w * 0.9F, 0.85F);
        }
    }

    private static final ResourceLocation[] TARJETAS = new ResourceLocation[com.coleccionista.prueba.Caja.SIMBOLOS.length];

    static {
        for (int i = 0; i < TARJETAS.length; i++) {
            TARJETAS[i] = Ids.rl(MOD, "textures/prenda/caja_etiqueta_" + com.coleccionista.prueba.Caja.SIMBOLOS[i] + ".png");
        }
    }

    /**
     * Mientras el Coleccionista explica la caja de música (las figuras de la espalda, que el de atrás te la dice y que los traidores
     * mienten), arriba de la pantalla aparecen, una por una, las cuatro tarjetas que alguien puede llevar en la espalda (con su nombre).
     */
    private static void figuras(Draw d, PoseStack ps, float w, float h, float z, String clave, long ms) {
        if (!clave.startsWith(MOD + ".voz.caja.") || !(clave.endsWith(".espalda") || clave.endsWith(".no_se_ve") || clave.endsWith(".diganse")
                || clave.endsWith(".traidores"))) {
            return;
        }
        boolean primera = clave.endsWith(".espalda");
        float lado = h * 0.2F, sep = lado * 0.28F;
        float total = TARJETAS.length * lado + (TARJETAS.length - 1) * sep;
        float cy = h / 2 - lado / 2 - h * 0.07F;
        for (int i = 0; i < TARJETAS.length; i++) {
            // en la primera línea salen una por una (cada medio segundo), con un saltito
            float t = primera ? (ms - lineaDesde) / 1000F - 0.3F - i * 0.5F : 9F;
            if (t <= 0) {
                continue;
            }
            float e = Math.min(1F, t / 0.25F);
            float esc = e < 1F ? e * 1.15F : 1F + 0.15F * (float) Math.max(0, Math.cos(Math.min(1F, (t - 0.25F) / 0.3F) * Math.PI / 2)) - 0.15F;
            esc = Math.max(0.05F, Math.min(1.15F, esc));
            float cx = -total / 2 + lado / 2 + i * (lado + sep);
            float m = lado / 2 * esc;
            float bam = (float) Math.sin(ms / 600.0 + i) * h * 0.006F;
            quad(d.translucent(BLANCO), ps, cx - m - lado * 0.04F, cy - m - lado * 0.04F + bam, cx + m + lado * 0.04F, cy + m + lado * 0.04F + bam, z,
                    0, 0, 1, 1, 0xC0000000, 0xC0000000);
            quad(d.translucent(TARJETAS[i]), ps, cx - m, cy - m + bam, cx + m, cy + m + bam, z + 0.002F, 0, 0, 1, 1, 0xFFFFFFFF, 0xFFFFFFFF);
            if (e >= 1F) {
                texto(d, ps, w, h, z + 0.004F, CajaCliente.nombreSimbolo(i), cy - lado / 2 - h * 0.035F, lado * 1.3F, 0.32F);
            }
        }
    }

    /** Lo que va delante del Coleccionista en 3D: barras de la voz, rayas de tele vieja, franja de abajo y el subtítulo. */
    private static void encima(Draw d, PoseStack ps, float w, float h, float z, boolean habla, String clave, long ms) {
        if (habla) {
            // las barras de la voz: chiquitas, en la esquina de abajo a la derecha (no tapan al Coleccionista)
            int n = 7;
            float bw = w * 0.012F, x0 = w / 2 - n * bw * 1.7F - w * 0.025F, y0 = -h / 2 + h * 0.045F;
            for (int i = 0; i < n; i++) {
                float k = (float) Math.abs(Math.sin(ms / (90.0 + i * 17) + i));
                float bh = h * (0.02F + 0.08F * k);
                float x = x0 + i * bw * 1.7F;
                quad(d.translucent(BLANCO), ps, x, y0, x + bw, y0 + bh, z + 0.002F, 0, 0, 1, 1, 0x90B3121A, 0xA0FF5A5A);
            }
        }
        for (int i = 0; i < (int) (h * 24); i++) {
            float y = -h / 2 + (i + 0.5F) * h / (h * 24F);
            quad(d.translucent(BLANCO), ps, -w / 2, y, w / 2, y + h / (h * 24F) * 0.2F, z + 0.003F, 0, 0, 1, 1, 0x12000000, 0x12000000);
        }
        // la orilla de abajo (como el borde de una transmisión): tapa donde se corta el sillón
        quad(d.translucent(BLANCO), ps, -w / 2, -h / 2, w / 2, -h / 2 + h * 0.025F, z + 0.004F, 0, 0, 1, 1, 0xFF0A0406, 0xFF14070A);
        quad(d.translucent(BLANCO), ps, -w / 2, -h / 2 + h * 0.025F, w / 2, -h / 2 + h * 0.031F, z + 0.004F, 0, 0, 1, 1, 0xFF8E1018, 0xFF8E1018);
        if (habla && !clave.isEmpty() && !Cine.enPantalla()) {
            // franja oscura detrás del subtítulo (si no, se pierde contra la camisa blanca)
            quad(d.translucent(BLANCO), ps, -w / 2, -h / 2 + h * 0.07F, w / 2, -h / 2 + h * 0.29F, z + 0.005F, 0, 0, 1, 1, 0xC0080204,
                    0x50080204);
            texto(d, ps, w, h, z + 0.007F, Component.translatable(clave).getString(), -h / 2 + h * 0.18F, w * 0.9F, 0.85F);
        }
    }

    private static void texto(Draw d, PoseStack ps, float w, float h, float z, String s) {
        quad(d.translucent(BLANCO), ps, -w / 2, -h / 2, w / 2, h / 2, z, 0, 0, 1, 1, 0xFF0A0406, 0xFF1E070B);
        texto(d, ps, w, h, z + 0.004F, s, 0, w * 0.85F, 1.6F);
    }

    /** Texto centrado en x, con su centro vertical en {@code cy}, que cabe en {@code ancho} (bloques). */
    private static void texto(Draw d, PoseStack ps, float w, float h, float z, String s, float cy, float ancho, float tamano) {
        Font f = Minecraft.getInstance().font;
        float escala = tamano * (h / 2F) / 64F;
        int px = (int) (ancho / escala);
        List<FormattedCharSequence> ls = f.split(Component.literal(s), Math.max(40, px));
        ps.pushPose();
        ps.translate(0, cy + ls.size() * 10 * escala / 2F, z);
        ps.scale(escala, -escala, escala);
        int y = 0;
        for (FormattedCharSequence l : ls) {
            float lw = f.width(l);
            f.drawInBatch(l, -lw / 2F, y, 0xFFF4ECE4, false, ps.last().pose(), buf0, Font.DisplayMode.NORMAL, 0, LUZ);
            y += 10;
        }
        ps.popPose();
    }

    /** El búfer de este cuadro (para el texto y el Coleccionista en 3D). */
    private static MultiBufferSource buf0;
    /** Los grados que se giró la pila para esta pantalla. */
    private static float giro;

    /** Un rectángulo plano mirando hacia +z, con degradado de abajo ({@code c0}) a arriba ({@code c1}). */
    private static void quad(VB vb, PoseStack ps, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int c0,
            int c1) {
        PoseStack.Pose p = ps.last();
        int a0 = c0 >>> 24, r0 = (c0 >> 16) & 255, g0 = (c0 >> 8) & 255, b0 = c0 & 255;
        int a1 = c1 >>> 24, r1 = (c1 >> 16) & 255, g1 = (c1 >> 8) & 255, b1 = c1 & 255;
        vb.v(p, x0, y0, z, r0, g0, b0, a0, u0, v1, OverlayTexture.NO_OVERLAY, LUZ, 0, 0, 1);
        vb.v(p, x1, y0, z, r0, g0, b0, a0, u1, v1, OverlayTexture.NO_OVERLAY, LUZ, 0, 0, 1);
        vb.v(p, x1, y1, z, r1, g1, b1, a1, u1, v0, OverlayTexture.NO_OVERLAY, LUZ, 0, 0, 1);
        vb.v(p, x0, y1, z, r1, g1, b1, a1, u0, v0, OverlayTexture.NO_OVERLAY, LUZ, 0, 0, 1);
    }
}

package com.casino.client;

import com.casino.plat.client.CasinoBER;
import com.casino.plat.client.Draw;
import com.casino.game.GalleryPlan;
import com.casino.plat.client.VB;
import com.casino.west.BullBE;
import com.casino.west.BullRideEntity;
import com.casino.west.GalleryBE;
import com.casino.west.PianoBE;
import com.casino.west.SaloonDoorBE;
import com.casino.west.WestBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** Dibujos del saloon: las hojas de las puertas batientes (y la puertita VIP) y las teclas y el rollo de la pianola. */
public final class WestRenderers {
    private WestRenderers() {
    }

    // ------------------------------------------------------------------ puertas batientes

    public static class Door extends CasinoBER<SaloonDoorBE> {
        public Door(BlockEntityRendererProvider.Context ctx) {
        }

        /** Altura del borde de arriba a lo largo de la hoja (u = 0 bisagra, 1 centro): curva que baja hacia el centro. */
        private static double top(double u) {
            return 1.46 - 0.16 * Math.sin(u * Math.PI / 2);
        }

        @Override
        protected void draw(SaloonDoorBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            BlockState st = be.getBlockState();
            if (!st.hasProperty(WestBlocks.HINGE)) {
                return;
            }
            boolean vip = st.getBlock() instanceof WestBlocks.VipGate;
            Direction f = be.facing();
            boolean left = st.getValue(WestBlocks.HINGE) == DoorHingeSide.LEFT;
            double sgn = left ? 1 : -1;
            ps.pushPose();
            HeistRenderers.face(ps, f);
            ps.translate(left ? -0.5 : 0.5, 0, 0);
            ps.mulPose(Axis.YP.rotationDegrees((float) (sgn * be.angle(partial))));
            Box3 b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            int frame = vip ? 0x4A2414 : 0x6E4222, slat = vip ? 0x6A1018 : 0x9A6636, rail = vip ? 0x5A2C18 : 0x7E4D28;
            double t = 0.028, y0 = 0.36, mid = 0.88;
            // largueros: el de la bisagra (alto) y el del centro
            box(b, sgn, 0.01, y0, 0.085, top(0.04), t, frame);
            box(b, sgn, 0.895, y0, 0.975, top(0.96), t, frame);
            // travesaños de abajo y del medio
            box(b, sgn, 0.085, y0, 0.895, y0 + 0.09, t, rail);
            box(b, sgn, 0.085, mid, 0.895, mid + 0.07, t, rail);
            // borde de arriba en curva (tramos cortos)
            int n = 10;
            for (int i = 0; i < n; i++) {
                double u0 = 0.085 + 0.81 * i / n, u1 = 0.085 + 0.81 * (i + 1) / n;
                double h = top((u0 + u1) / 2);
                box(b, sgn, u0, h - 0.07, u1, h, t, rail);
            }
            if (vip) {
                // panel de terciopelo abajo y arriba, con filo dorado y placa "VIP"
                box(b, sgn, 0.085, y0 + 0.09, 0.895, mid, t * 0.6, slat);
                for (int i = 0; i < n; i++) {
                    double u0 = 0.085 + 0.81 * i / n, u1 = 0.085 + 0.81 * (i + 1) / n;
                    box(b, sgn, u0, mid + 0.07, u1, top((u0 + u1) / 2) - 0.07, t * 0.6, slat);
                }
                box(b, sgn, 0.3, 0.98, 0.7, 1.12, t + 0.012, 0xD4AF37);
                box(b, sgn, 0.34, 1.01, 0.66, 1.09, t + 0.016, 0x2A1A08);
            } else {
                // barrotes torneados abajo y celosía arriba
                for (double u = 0.15; u < 0.86; u += 0.09) {
                    box(b, sgn, u, y0 + 0.09, u + 0.035, mid, t * 0.7, slat);
                }
                for (double y = mid + 0.12; y < 1.3; y += 0.07) {
                    double u1 = 0.895;
                    double lim = 0.085;
                    // las tablillas llegan hasta donde baja la curva de arriba
                    double uEnd = u1;
                    for (double u = lim; u <= u1; u += 0.02) {
                        if (top(u) - 0.07 < y + 0.035) {
                            uEnd = u;
                            break;
                        }
                    }
                    if (uEnd > lim + 0.05) {
                        box(b, sgn, lim, y, uEnd, y + 0.035, t * 0.5, slat);
                    }
                }
            }
            // bisagras de fierro
            box(b, sgn, -0.01, y0 + 0.12, 0.04, y0 + 0.2, t + 0.01, 0x2A2A2A);
            box(b, sgn, -0.01, top(0.04) - 0.2, 0.04, top(0.04) - 0.12, t + 0.01, 0x2A2A2A);
            ps.popPose();
        }

        /** Caja en coordenadas de la hoja: u va de la bisagra (0) hacia el centro (1); sgn voltea para la hoja de la derecha. */
        private static void box(Box3 b, double sgn, double u0, double y0, double u1, double y1, double half, int rgb) {
            b.box(sgn * u0, y0, -half, sgn * u1, y1, half, rgb);
        }
    }

    // ------------------------------------------------------------------ pianola

    public static class Piano extends CasinoBER<PianoBE> {
        public Piano(BlockEntityRendererProvider.Context ctx) {
        }

        /** Número pseudoaleatorio estable para la tecla k en el compás n. */
        private static int hash(long n, int k) {
            long h = n * 0x9E3779B97F4A7C15L + k * 0xC2B2AE3D27D4EB4FL;
            h ^= h >>> 29;
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 32;
            return (int) (h & 0x7FFFFFFF);
        }

        @Override
        protected void draw(PianoBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            if (be.getLevel() == null) {
                return;
            }
            float time = be.getLevel().getGameTime() + partial;
            boolean on = be.on();
            ps.pushPose();
            HeistRenderers.face(ps, be.facing());
            Box3 b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            // teclado: 15 blancas en 14 px (de x 1 a 15), sobre la repisa (y 8.5 px), de z 2.6 a 6 px
            double px = 1 / 16.0;
            double kx0 = -7 * px, kw = 14 * px / 15, ky = 8.5 * px, kz0 = 2.6 * px - 0.5, kz1 = 6 * px - 0.5;
            long beat = (long) (time / 3.0);          // corcheas a ~200 por minuto (ragtime)
            for (int k = 0; k < 15; k++) {
                boolean down = on && hash(beat, k) % 5 == 0;
                double x0 = kx0 + k * kw + 0.004, x1 = kx0 + (k + 1) * kw - 0.004;
                double y1 = ky + 0.03 - (down ? 0.018 : 0);
                b.box(x0, ky, kz0, x1, y1, kz1, down ? 0xD6D0C0 : 0xF4EFE2);
            }
            int[] black = {0, 1, 3, 4, 5, 7, 8, 10, 11, 12};
            for (int k : black) {
                boolean down = on && hash(beat + 7, k + 30) % 4 == 0;
                double cx = kx0 + (k + 1) * kw;
                b.box(cx - kw * 0.3, ky + 0.02, kz0 + 0.06, cx + kw * 0.3, ky + 0.06 - (down ? 0.014 : 0), kz1, 0x15110E);
            }
            // rollo de papel en la ventana (x 4..12, y 11..14.5, z 6.9 px): agujeritos que bajan
            double rz = 6.95 * px - 0.5;
            b.box(-4 * px, 11 * px, rz, 4 * px, 14.5 * px, rz + 0.01, 0xEDE4CC);
            float scroll = on ? time * 0.012F : 0;
            for (int row = 0; row < 9; row++) {
                double y = 11 * px + ((row * 0.42 - scroll) % 3.5 + 3.5) % 3.5 * px;
                long n = (long) Math.floor((row * 0.42 - scroll) / 3.5) * 9 + row;
                for (int c = 0; c < 10; c++) {
                    if (hash(n, c) % 3 == 0) {
                        double x = -3.6 * px + c * 0.75 * px;
                        b.box(x, y, rz - 0.004, x + 0.5 * px, y + 0.22 * px, rz, 0x3A2A1A);
                    }
                }
            }
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ textos en el mundo

    /** Escribe un texto centrado en x (en el plano xy del dibujo, mirando hacia +z), de altura {@code size} bloques. */
    static void label(PoseStack ps, Draw d, Component c, float x, float y, float z, float size, int color, int light) {
        net.minecraft.client.gui.Font font = Draw.font();
        float s = size / 9F;
        ps.pushPose();
        ps.translate(x - font.width(c) * s / 2F, y + size, z);
        ps.scale(s, -s, s);
        d.text(ps, c, 0, 0, color, 0, light);
        ps.popPose();
    }

    private static final int FULL = 0xF000F0;

    // ------------------------------------------------------------------ toro mecánico

    public static class Bull extends CasinoBER<BullBE> {
        public Bull(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected void draw(BullBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            if (be.getLevel() == null) {
                return;
            }
            float time = be.getLevel().getGameTime() + partial;
            BullRideEntity r = be.ride();
            float spin, roll, pitch;
            if (r != null) {
                spin = r.spin(partial);
                roll = r.roll(partial);
                pitch = r.pitchDeg(partial);
            } else {
                spin = 0;
                roll = 0;
                pitch = (float) Math.sin(time * 0.06) * 1.5F;  // respira
            }
            Direction f = be.facing();
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));   // x = izquierda del toro, z = hacia la cabeza
            Box3 b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            // base de fierro y pistón (no se mueven)
            b.box(-0.5, 0, -0.5, 0.5, 0.08, 0.5, 0x2E2E34);
            b.box(-0.44, 0.08, -0.44, 0.44, 0.12, 0.44, 0x48484F);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + Math.PI / 4;
                double bx = Math.cos(a) * 0.36, bz = Math.sin(a) * 0.36;
                b.box(bx - 0.03, 0.12, bz - 0.03, bx + 0.03, 0.15, bz + 0.03, 0xC8A040);
            }
            b.box(-0.14, 0.12, -0.14, 0.14, 0.40, 0.14, 0x5A5A62);
            double stroke = 0.04 * Math.sin(Math.toRadians(pitch) * 3);
            b.box(-0.08, 0.40, -0.08, 0.08, BullRideEntity.PIVOT + stroke, 0.08, 0xB8B8C0);
            ps.pushPose();
            ps.translate(0, BullRideEntity.PIVOT, 0);
            ps.mulPose(Axis.YP.rotationDegrees(-spin));
            ps.mulPose(Axis.XP.rotationDegrees(-pitch));
            ps.mulPose(Axis.ZP.rotationDegrees(-roll));
            b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            drawBull(b);
            ps.popPose();
            ps.popPose();
            // letrero del récord junto a la entrada del ruedo
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
            ps.translate(-2.1, 0, 3.3);
            Box3 s = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            s.box(-0.62, 0, -0.04, -0.54, 1.05, 0.04, 0x5A3A1E);
            s.box(0.54, 0, -0.04, 0.62, 1.05, 0.04, 0x5A3A1E);
            s.box(-0.72, 1.0, -0.05, 0.72, 1.72, 0.02, 0x7A5028);
            s.box(-0.66, 1.05, 0.02, 0.66, 1.67, 0.03, 0xE8D8B0);
            label(ps, d, Component.translatable("casino.bull.board_title"), 0, 1.50F, 0.04F, 0.13F, 0xFF7A2010, light);
            if (be.bestTicks() > 0) {
                label(ps, d, Component.literal(be.bestName()), 0, 1.32F, 0.04F, 0.11F, 0xFF2A1A0A, light);
                label(ps, d, Component.literal(String.format(java.util.Locale.ROOT, "%.1f s", be.bestTicks() / 20.0)), 0, 1.12F, 0.04F, 0.14F,
                        0xFF1A4A1A, light);
            } else {
                label(ps, d, Component.translatable("casino.bull.board_empty"), 0, 1.22F, 0.04F, 0.10F, 0xFF2A1A0A, light);
            }
            ps.popPose();
        }

        /** El toro en su sistema: x = izquierda, y = arriba (0 = pivote), z = hacia la cabeza. */
        private static void drawBull(Box3 b) {
            int hide = 0x4A2816, dark = 0x2E180C, belly = 0x6A3C22, horn = 0xE8DEC4, leather = 0x8E1C16, gold = 0xD4AF37;
            // cuerpo (lomo más alto en la cruz)
            b.box(-0.30, 0.10, -0.60, 0.30, 0.56, 0.52, hide);
            b.box(-0.32, 0.30, 0.10, 0.32, 0.64, 0.52, hide);
            b.box(-0.27, 0.06, -0.50, 0.27, 0.14, 0.40, belly);
            // manchas
            b.box(0.301, 0.30, -0.30, 0.302, 0.48, -0.05, dark);
            b.box(-0.302, 0.22, 0.05, -0.301, 0.40, 0.30, dark);
            // patas dobladas (es un toro de feria)
            for (int sx = -1; sx <= 1; sx += 2) {
                b.box(sx * 0.12, -0.10, 0.28, sx * 0.27, 0.14, 0.44, hide);
                b.box(sx * 0.12, -0.14, 0.30, sx * 0.27, -0.08, 0.50, dark);
                b.box(sx * 0.12, -0.10, -0.52, sx * 0.27, 0.14, -0.36, hide);
                b.box(sx * 0.12, -0.14, -0.56, sx * 0.27, -0.08, -0.40, dark);
            }
            // silla de montar con cuerno dorado
            b.box(-0.34, 0.56, -0.24, 0.34, 0.66, 0.16, leather);
            b.box(-0.35, 0.40, -0.18, -0.33, 0.62, 0.10, leather);
            b.box(0.33, 0.40, -0.18, 0.35, 0.62, 0.10, leather);
            b.box(-0.06, 0.66, 0.10, 0.06, 0.78, 0.18, gold);
            b.box(-0.09, 0.76, 0.08, 0.09, 0.80, 0.20, gold);
            b.box(-0.30, 0.58, -0.30, 0.30, 0.70, -0.22, leather);
            // estribos
            for (int sx = -1; sx <= 1; sx += 2) {
                b.box(sx * 0.35, 0.10, -0.06, sx * 0.37, 0.42, -0.02, 0x2A1A10);
                b.box(sx * 0.33, 0.06, -0.10, sx * 0.40, 0.10, 0.02, 0x9A9AA0);
            }
            // cuello y cabeza (un poco agachada, embistiendo)
            b.box(-0.22, 0.20, 0.48, 0.22, 0.58, 0.70, hide);
            b.box(-0.20, 0.12, 0.66, 0.20, 0.50, 0.98, hide);
            b.box(-0.16, 0.10, 0.96, 0.16, 0.30, 1.06, 0xB08A6A);
            b.box(-0.10, 0.18, 1.06, -0.04, 0.24, 1.065, 0x1A0A06);
            b.box(0.04, 0.18, 1.06, 0.10, 0.24, 1.065, 0x1A0A06);
            b.box(-0.07, 0.07, 1.04, 0.07, 0.11, 1.08, gold);         // argolla
            b.box(-0.201, 0.36, 0.84, -0.20, 0.42, 0.90, 0xF0F0E8);   // ojos
            b.box(0.20, 0.36, 0.84, 0.201, 0.42, 0.90, 0xF0F0E8);
            b.box(-0.2015, 0.37, 0.86, -0.2005, 0.41, 0.89, 0x100808);
            b.box(0.2005, 0.37, 0.86, 0.2015, 0.41, 0.89, 0x100808);
            b.box(-0.20, 0.50, 0.66, 0.20, 0.56, 0.80, dark);          // copete
            // orejas
            b.box(0.20, 0.40, 0.70, 0.30, 0.46, 0.76, hide);
            b.box(-0.30, 0.40, 0.70, -0.20, 0.46, 0.76, hide);
            // cuernos: salen de lado y suben
            for (int sx = -1; sx <= 1; sx += 2) {
                b.box(sx * 0.18, 0.48, 0.74, sx * 0.40, 0.56, 0.82, horn);
                b.box(sx * 0.36, 0.54, 0.75, sx * 0.44, 0.70, 0.81, horn);
                b.box(sx * 0.39, 0.70, 0.76, sx * 0.44, 0.80, 0.80, 0xF8F4E8);
            }
            // cola con borla
            b.box(-0.03, 0.18, -0.66, 0.03, 0.52, -0.60, hide);
            b.box(-0.06, 0.10, -0.68, 0.06, 0.22, -0.60, 0x1A0E06);
            // remaches de latón a los lados
            for (int i = 0; i < 4; i++) {
                double z = -0.45 + i * 0.28;
                b.box(0.30, 0.16, z, 0.315, 0.19, z + 0.03, gold);
                b.box(-0.315, 0.16, z, -0.30, 0.19, z + 0.03, gold);
            }
        }
    }

    // ------------------------------------------------------------------ galería de tiro

    public static class Gallery extends CasinoBER<GalleryBE> {
        private static final net.minecraft.resources.ResourceLocation[] TEX = new net.minecraft.resources.ResourceLocation[5];
        private static final net.minecraft.resources.ResourceLocation BACK = com.casino.plat.Ids.rl(com.casino.Casino.MODID,
                "textures/entity/west/target_back.png");

        static {
            String[] n = {"duck", "bottle", "bandit", "lady", "star"};
            for (int i = 0; i < 5; i++) {
                TEX[i] = com.casino.plat.Ids.rl(com.casino.Casino.MODID, "textures/entity/west/target_" + n[i] + ".png");
            }
        }

        public Gallery(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected void draw(GalleryBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            if (be.getLevel() == null) {
                return;
            }
            Direction f = be.facing();
            float time = be.getLevel().getGameTime() + partial;
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));   // ahora x = u (derecha del tirador), y = v, z = w (hacia el tirador)
            // escenario: agua de los patitos, repisa de las botellas, riel de los bandidos y del cielo
            Box3 b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            double U = GalleryPlan.U + 0.6;
            b.box(-U, 0.0, GalleryPlan.W_FRONT - 0.42, U, 0.18, GalleryPlan.W_FRONT + 0.42, 0x2A6A9A);
            for (int i = 0; i < 18; i++) {
                double u0 = -U + (i + (time * 0.02) % 1.0) * (2 * U / 18.0);
                if (u0 + 0.22 < U) {
                    b.box(u0, 0.18, GalleryPlan.W_FRONT - 0.30, u0 + 0.22, 0.21, GalleryPlan.W_FRONT + 0.30, 0x6AB0D8);
                }
            }
            double mv = GalleryPlan.V_MID - GalleryPlan.HALF_H[GalleryPlan.BOTTLE];
            b.box(-U, mv - 0.07, GalleryPlan.W_MID - 0.14, U, mv, GalleryPlan.W_MID + 0.14, 0x6E4426);
            double bv = GalleryPlan.V_BACK - GalleryPlan.HALF_H[GalleryPlan.BANDIT];
            b.box(-U, bv - 0.06, GalleryPlan.W_BACK - 0.08, U, bv, GalleryPlan.W_BACK + 0.08, 0x2A2A2A);
            b.box(-U, GalleryPlan.V_STAR + 0.32, GalleryPlan.W_STAR - 0.04, U, GalleryPlan.V_STAR + 0.35, GalleryPlan.W_STAR + 0.02, 0x3A3A3A);
            // marcador colgado arriba del mostrador
            double bw = 1.6, by0 = 2.55, by1 = 3.35, bz = -0.25;
            b.box(-bw - 0.06, by0 - 0.06, bz - 0.05, bw + 0.06, by1 + 0.06, bz, 0x5A3418);
            b.box(-bw, by0, bz, bw, by1, bz + 0.01, 0x1E120A);
            b.box(-bw + 0.2, by1 + 0.06, bz - 0.03, -bw + 0.24, 4.0, bz - 0.01, 0x3A3A3A);
            b.box(bw - 0.24, by1 + 0.06, bz - 0.03, bw - 0.2, 4.0, bz - 0.01, 0x3A3A3A);
            // blancos
            GalleryPlan plan = be.playing() ? be.plan() : null;
            double t = be.time(partial);
            if (plan != null && t >= 0) {
                for (int type = 0; type < 5; type++) {
                    VB vb = null;
                    for (GalleryPlan.Target g : plan.targets) {
                        if (g.type != type || t < g.start - 1 || t > g.end + 1) {
                            continue;
                        }
                        int hit = be.hitAt(g.id);
                        if (hit >= 0 && (g.type == GalleryPlan.BOTTLE || t - hit > 8)) {
                            continue;
                        }
                        if (vb == null) {
                            vb = d.cutout(TEX[type]);
                        }
                        double tilt = hit >= 0 ? Math.min(1, (t - hit) / 6.0) * 90 : (1 - g.raise(t)) * 90;
                        target(vb, ps.last(), g, t, tilt, time, light);
                    }
                }
            }
            ps.pushPose();
            // textos del marcador
            float tz = (float) bz + 0.012F;
            label(ps, d, Component.translatable("casino.gallery.board_title"), 0, (float) by1 - 0.22F, tz, 0.16F, 0xFFFFC040, FULL);
            int res = be.resultScore();
            if (be.playing()) {
                int left = (int) Math.max(0, Math.ceil((GalleryPlan.DURATION - Math.max(0, t)) / 20.0));
                label(ps, d, Component.literal(be.shooterName()), 0, (float) by1 - 0.42F, tz, 0.11F, 0xFFFFFFFF, FULL);
                label(ps, d, Component.translatable("casino.gallery.board_play", be.score(), be.shots(), left), 0, (float) by0 + 0.08F, tz, 0.13F,
                        0xFF8CF08C, FULL);
            } else if (res >= 0) {
                label(ps, d, Component.translatable("casino.gallery.board_result", res), 0, (float) by1 - 0.46F, tz, 0.15F, 0xFFFFFFFF, FULL);
                label(ps, d, be.resultPay() > 0 ? Component.translatable("casino.gallery.board_paid", be.resultPay())
                        : Component.translatable("casino.gallery.board_nopay"), 0, (float) by0 + 0.08F, tz, 0.11F,
                        be.resultPay() > 0 ? 0xFF8CF08C : 0xFFC0A080, FULL);
            } else if (be.bestScore() > 0) {
                label(ps, d, Component.translatable("casino.gallery.board_record"), 0, (float) by1 - 0.42F, tz, 0.11F, 0xFFC0A080, FULL);
                label(ps, d, Component.literal(be.bestName() + "  " + be.bestScore()), 0, (float) by0 + 0.08F, tz, 0.13F, 0xFFFFFFFF, FULL);
            } else {
                label(ps, d, Component.translatable("casino.gallery.board_idle"), 0, (float) by0 + 0.18F, tz, 0.11F, 0xFFE8D8B0, FULL);
            }
            ps.popPose();
            ps.popPose();
        }

        /** Un blanco: cartón de dos caras; los que se levantan giran sobre su orilla de abajo (tilt 0 = de pie, 90 = acostado). */
        private static void target(VB vb, PoseStack.Pose p, GalleryPlan.Target g, double t, double tilt, float time, int light) {
            double u = g.u(t), v = g.v(t), w = g.w();
            double hw = GalleryPlan.HALF_W[g.type] * 1.12, hh = GalleryPlan.HALF_H[g.type] * 1.08;
            double a = Math.toRadians(tilt);
            double vb0 = v - hh;
            // esquinas: abajo-izq, abajo-der, arriba-der, arriba-izq
            double topV = vb0 + 2 * hh * Math.cos(a), topW = w - 2 * hh * Math.sin(a);
            double[][] c;
            if (g.type == GalleryPlan.STAR) {
                double phi = time * 0.25 + g.id;
                double cu = Math.cos(phi) * hw, cw = Math.sin(phi) * hw;
                c = new double[][]{{u - cu, v - hh, w - cw}, {u + cu, v - hh, w + cw}, {u + cu, v + hh, w + cw}, {u - cu, v + hh, w - cw}};
            } else {
                c = new double[][]{{u - hw, vb0, w}, {u + hw, vb0, w}, {u + hw, topV, topW}, {u - hw, topV, topW}};
            }
            float nx = 0, ny = (float) Math.sin(a), nz = (float) Math.cos(a);
            int lt = Math.max(light, 0xB000B0);
            // frente
            vb.v(p, (float) c[0][0], (float) c[0][1], (float) c[0][2], 255, 255, 255, 255, 0, 1, OverlayTexture.NO_OVERLAY, lt, nx, ny, nz);
            vb.v(p, (float) c[1][0], (float) c[1][1], (float) c[1][2], 255, 255, 255, 255, 1, 1, OverlayTexture.NO_OVERLAY, lt, nx, ny, nz);
            vb.v(p, (float) c[2][0], (float) c[2][1], (float) c[2][2], 255, 255, 255, 255, 1, 0, OverlayTexture.NO_OVERLAY, lt, nx, ny, nz);
            vb.v(p, (float) c[3][0], (float) c[3][1], (float) c[3][2], 255, 255, 255, 255, 0, 0, OverlayTexture.NO_OVERLAY, lt, nx, ny, nz);
            // reverso (más oscuro)
            vb.v(p, (float) c[3][0], (float) c[3][1], (float) c[3][2], 120, 100, 80, 255, 0, 0, OverlayTexture.NO_OVERLAY, lt, -nx, -ny, -nz);
            vb.v(p, (float) c[2][0], (float) c[2][1], (float) c[2][2], 120, 100, 80, 255, 1, 0, OverlayTexture.NO_OVERLAY, lt, -nx, -ny, -nz);
            vb.v(p, (float) c[1][0], (float) c[1][1], (float) c[1][2], 120, 100, 80, 255, 1, 1, OverlayTexture.NO_OVERLAY, lt, -nx, -ny, -nz);
            vb.v(p, (float) c[0][0], (float) c[0][1], (float) c[0][2], 120, 100, 80, 255, 0, 1, OverlayTexture.NO_OVERLAY, lt, -nx, -ny, -nz);
        }
    }

    // ------------------------------------------------------------------ duelo al mediodía

    public static class Duel extends CasinoBER<com.casino.west.DuelBE> {
        public Duel(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected void draw(com.casino.west.DuelBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            if (be.getLevel() == null) {
                return;
            }
            net.minecraft.core.BlockPos o = be.getBlockPos();
            Box3 b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            // las dos X de gis en la calle (la tuya y la del pistolero)
            for (net.minecraft.core.BlockPos m : new net.minecraft.core.BlockPos[]{be.playerMark(), be.npcMark()}) {
                double x = m.getX() - o.getX() + 0.5, y = m.getY() - o.getY() + 0.012, z = m.getZ() - o.getZ() + 0.5;
                for (int k = -5; k <= 5; k++) {
                    double s = k * 0.07;
                    b.box(x + s - 0.045, y, z + s - 0.045, x + s + 0.045, y + 0.01, z + s + 0.045, 0xEAE4D4);
                    b.box(x + s - 0.045, y, z - s - 0.045, x + s + 0.045, y + 0.01, z - s + 0.045, 0xEAE4D4);
                }
            }
            // letrero en el poste: el más rápido del pueblo
            Direction f = be.facing();
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            // el letrero mira hacia la calle (la izquierda de FACING)
            ps.mulPose(Axis.YP.rotationDegrees(-f.getCounterClockWise().toYRot()));
            ps.translate(0, 0, 0.19);
            label(ps, d, Component.translatable("casino.duel.board_title"), 0, 1.08F, 0.0F, 0.065F, 0xFF7A2010, light);
            if (be.bestTicks() > 0) {
                label(ps, d, Component.literal(be.bestName()), 0, 0.98F, 0.0F, 0.055F, 0xFF2A1A0A, light);
                label(ps, d, Component.literal(String.format(java.util.Locale.ROOT, "%.2f s", be.bestTicks() / 20.0)), 0, 0.86F, 0.0F, 0.065F,
                        0xFF1A4A1A, light);
            } else {
                label(ps, d, Component.translatable("casino.duel.board_empty"), 0, 0.92F, 0.0F, 0.05F, 0xFF2A1A0A, light);
            }
            ps.popPose();
        }
    }
}

package com.casino.client;

import com.casino.plat.client.CasinoBER;
import com.casino.plat.client.Draw;
import com.casino.west.PianoBE;
import com.casino.west.SaloonDoorBE;
import com.casino.west.WestBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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
}

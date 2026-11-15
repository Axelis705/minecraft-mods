package com.casino.client;

import com.casino.heist.CameraBE;
import com.casino.heist.HeistManager;
import com.casino.heist.LaserBE;
import com.casino.heist.VaultDoorBE;
import com.casino.plat.client.CasinoBER;
import com.casino.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Dibujos del robo: la cámara que gira, el rayo láser y la puerta redonda de la bóveda. */
public final class HeistRenderers {
    private static final int FULL = 0xF000F0;

    private HeistRenderers() {
    }

    /** Gira para que el "frente" (hacia FACING) quede en -z local, con el centro del bloque en el origen. */
    static void face(PoseStack ps, Direction f) {
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(180F - f.toYRot()));
    }

    // ------------------------------------------------------------------ cámara

    public static class Camera extends CasinoBER<CameraBE> {
        public Camera(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected void draw(CameraBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            if (be.getLevel() == null) {
                return;
            }
            float time = be.getLevel().getGameTime() + partial;
            Direction f = be.facing();
            ps.pushPose();
            face(ps, f);
            // soporte en la pared (atrás está +z local: la pared)
            Box3 b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            b.box(-0.12, 0.62, 0.38, 0.12, 0.92, 0.5, 0x2A2C30);
            b.box(-0.03, 0.74, 0.12, 0.03, 0.8, 0.4, 0x3A3C42);
            // cabeza: gira de lado a lado y mira un poco hacia abajo
            float yaw = be.off ? 0 : be.yaw(time) - f.toYRot();
            ps.translate(0, 0.77, 0.12);
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            ps.mulPose(Axis.XP.rotationDegrees(be.off ? 35F : 18F));
            b = new Box3(d.cutout(Box3.WHITE), ps.last(), light);
            b.box(-0.11, -0.09, -0.3, 0.11, 0.09, 0.06, 0xE4E6EA);
            b.box(-0.12, 0.06, -0.33, 0.12, 0.1, 0.06, 0xC8CACE);
            b.box(-0.07, -0.06, -0.34, 0.07, 0.06, -0.3, 0x111216);
            boolean red = be.seeing && (time % 6) < 3;
            Box3 gl = new Box3(d.glow(Box3.WHITE), ps.last(), FULL);
            gl.box(-0.035, -0.035, -0.345, 0.035, 0.035, -0.335, be.off ? 0x202020 : be.seeing ? 0xFF2020 : 0x3050FF);
            gl.box(0.06, 0.04, -0.31, 0.09, 0.07, -0.29, be.off ? 0x101010 : red || (!be.seeing && (time % 40) < 20) ? 0xFF2020 : 0x401010);
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ láser

    public static class Laser extends CasinoBER<LaserBE> {
        public Laser(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected void draw(LaserBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            if (be.getLevel() == null) {
                return;
            }
            long now = be.getLevel().getGameTime();
            Direction f = be.facing();
            boolean on = LaserBE.on(now, be.pattern());
            int len = LaserBE.length(be.getLevel(), be.getBlockPos(), f);
            ps.pushPose();
            face(ps, f);
            float y = (float) LaserBE.HEIGHT;
            Box3 gl = new Box3(d.glow(Box3.WHITE), ps.last(), FULL);
            gl.box(-0.05, y - 0.05, -0.36, 0.05, y + 0.05, -0.3, on ? 0xFF3030 : 0x401010);
            if (on && len > 0) {
                float flick = 0.85F + 0.15F * (float) Math.sin((now + partial) * 1.7);
                int c = ((int) (255 * flick) << 16) | ((int) (40 * flick) << 8) | (int) (40 * flick);
                gl.box(-0.012, y - 0.012, -0.36 - len - 0.14, 0.012, y + 0.012, -0.36, c);
                gl.box(-0.03, y - 0.03, -0.36 - len - 0.14, 0.03, y + 0.03, -0.36, 0x400808);
            }
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ puerta de la bóveda

    public static class VaultDoor extends CasinoBER<VaultDoorBE> {
        private static final double R = 2.45;

        public VaultDoor(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected void draw(VaultDoorBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
            if (be.getLevel() == null) {
                return;
            }
            float time = be.getLevel().getGameTime() + partial;
            float[] a = be.anim(time);
            float open = a[0], wheel = a[1];
            Direction f = be.facing();
            int lit = Math.max(light, 0x00A000A0);
            ps.pushPose();
            face(ps, f);
            ps.translate(0, 0.5, 0);
            // marco fijo en la pared: anillo grueso
            Box3 b = new Box3(d.cutout(Box3.WHITE), ps.last(), lit);
            ring(b, R + 0.05, R + 0.45, -0.52, -0.42, 0x2C2E34);
            ring(b, R + 0.05, R + 0.2, -0.56, -0.52, 0xB8902E);
            // la puerta gira en una bisagra a la izquierda (vista de frente: -x local)
            ps.pushPose();
            double hx = -R - 0.15;
            ps.translate(hx, 0, -0.5);
            ps.mulPose(Axis.YP.rotationDegrees(open * 105F));
            ps.translate(-hx, 0, 0.5);
            b = new Box3(d.cutout(Box3.WHITE), ps.last(), lit);
            // disco por rebanadas
            for (int i = -10; i <= 10; i++) {
                double y0 = i * R / 10.5, y1 = (i + 1) * R / 10.5;
                double ym = (y0 + y1) / 2;
                double hw = Math.sqrt(Math.max(0, R * R - ym * ym));
                if (hw <= 0.01) {
                    continue;
                }
                b.box(-hw, y0 - R / 21, -0.5, hw, y1 - R / 21, -0.1, 0x8A8F98);
                double iw = Math.sqrt(Math.max(0, (R - 0.3) * (R - 0.3) - ym * ym));
                if (iw > 0.01) {
                    b.box(-iw, y0 - R / 21, -0.56, iw, y1 - R / 21, -0.5, 0xA9AEB6);
                }
            }
            // pernos alrededor (se meten al girar la rueda)
            float pull = Math.min(1F, Math.max(0F, wheel / 9F));
            for (int k = 0; k < 12; k++) {
                double ang = k * Math.PI * 2 / 12;
                double rr = R - 0.22 + 0.25 * (1 - pull);
                double px = Math.cos(ang) * rr, py = Math.sin(ang) * rr;
                b.box(px - 0.09, py - 0.09, -0.62, px + 0.09, py + 0.09, -0.5, 0xC9A227);
            }
            // centro: cubo y rueda con 3 rayos que gira
            b.box(-0.32, -0.32, -0.7, 0.32, 0.32, -0.56, 0x55595F);
            ps.pushPose();
            ps.translate(0, 0, -0.72);
            ps.mulPose(Axis.ZP.rotation(wheel));
            Box3 w = new Box3(d.cutout(Box3.WHITE), ps.last(), lit);
            for (int k = 0; k < 3; k++) {
                ps.pushPose();
                ps.mulPose(Axis.ZP.rotationDegrees(k * 60F));
                w = new Box3(d.cutout(Box3.WHITE), ps.last(), lit);
                w.box(-1.1, -0.05, -0.06, 1.1, 0.05, 0.06, 0xD8B040);
                w.box(-1.18, -0.09, -0.1, -1.0, 0.09, 0.1, 0x1C1C20);
                w.box(1.0, -0.09, -0.1, 1.18, 0.09, 0.1, 0x1C1C20);
                ps.popPose();
            }
            ps.popPose();
            // luz de estado
            Box3 gl = new Box3(d.glow(Box3.WHITE), ps.last(), FULL);
            boolean green = be.openStart >= 0;
            gl.box(0.9, 1.5, -0.58, 1.1, 1.7, -0.54, green ? 0x30FF60 : 0xFF2020);
            ps.popPose();
            ps.popPose();
        }

        /** Anillo de cajas entre dos radios, en la profundidad z0..z1. */
        private static void ring(Box3 b, double r0, double r1, double z0, double z1, int c) {
            int n = 24;
            for (int i = 0; i < n; i++) {
                double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
                double am = (a0 + a1) / 2, rm = (r0 + r1) / 2;
                double half = (r1 - r0) / 2, along = rm * Math.PI / n + 0.02;
                double cx = Math.cos(am) * rm, cy = Math.sin(am) * rm;
                double ex = Math.abs(Math.cos(am)) * half + Math.abs(Math.sin(am)) * along;
                double ey = Math.abs(Math.sin(am)) * half + Math.abs(Math.cos(am)) * along;
                b.box(cx - ex, cy - ey, z0, cx + ex, cy + ey, z1, c);
            }
        }
    }

    @SuppressWarnings("unused")
    private static HeistManager.Phase unused() {
        return HeistManager.Phase.STEALTH;
    }
}

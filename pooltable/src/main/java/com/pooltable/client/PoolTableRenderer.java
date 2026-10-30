package com.pooltable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pooltable.PoolTable;
import com.pooltable.block.PoolTableBE;
import com.pooltable.game.PoolPhysics;
import com.pooltable.item.PoolCueItem;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Dibuja las bolas rodando (suavizadas entre ticks), la línea de puntería del taco y el cartel de la partida sobre la mesa. */
public class PoolTableRenderer implements BlockEntityRenderer<PoolTableBE> {
    private static final ResourceLocation[] TEX = new ResourceLocation[PoolPhysics.N];

    static {
        for (int i = 0; i < PoolPhysics.N; i++) {
            TEX[i] = new ResourceLocation(PoolTable.MODID, "textures/entity/ball_" + i + ".png");
        }
    }


    public PoolTableRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(PoolTableBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        if (be.getLevel() == null || !be.clientInit) {
            return;
        }
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
        float a = be.clientMoving ? Mth.clamp((System.nanoTime() - be.recvNanos) / 50.0e6F, 0.0F, 1.0F) : 1.0F;
        Vec3 du = be.dirU();
        Vec3 dv = be.dirV();
        Vec3 c = be.center();
        double baseX = c.x - be.getBlockPos().getX();
        double baseZ = c.z - be.getBlockPos().getZ();
        double ballY = PoolTableBE.FELT_Y + PoolPhysics.R;
        double landY = PoolTableBE.SHELF_Y + PoolPhysics.R;
        long now = System.nanoTime();

        for (int i = 0; i < PoolPhysics.N; i++) {
            float u;
            float v;
            double y = ballY;
            if (!be.ph.out[i]) {
                u = Mth.lerp(a, be.px[i], be.cx[i]);
                v = Mth.lerp(a, be.pz[i], be.cz[i]);
            } else {
                PoolTableBE.Drop d = dropOf(be, i);
                if (d != null) {
                    double[] pos = dropPos(be, d, now, ballY, landY);
                    if (pos == null) {
                        be.drops.remove(d);
                        d = null;
                    } else {
                        u = (float) pos[0];
                        v = (float) pos[1];
                        y = pos[2];
                        drawBall(be, i, u, v, y, ps, buf, light, du, dv, baseX, baseZ);
                        continue;
                    }
                }
                int slot = be.tray.indexOf(i);
                if (slot < 0) {
                    continue;
                }
                u = (float) PoolTableBE.TRAY_U;
                v = (float) (PoolTableBE.TRAY_V0 + slot * PoolTableBE.TRAY_DV);
                y = landY;
            }
            drawBall(be, i, u, v, y, ps, buf, light, du, dv, baseX, baseZ);
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p != null && !be.clientMoving && !be.ph.out[0]
                && (p.getMainHandItem().getItem() instanceof PoolCueItem || p.getOffhandItem().getItem() instanceof PoolCueItem)
                && be.distanceTo(p.position()) <= PoolCueItem.REACH + 0.4 && myTurn(be, p)
                && ClientAim.nearest(be, p)) {
            drawAim(be, p, partial, ps, buf);
        }
    }

    private static PoolTableBE.Drop dropOf(PoolTableBE be, int ball) {
        for (PoolTableBE.Drop d : be.drops) {
            if (d.ball == ball) {
                return d;
            }
        }
        return null;
    }

    /** Tiempo (s) que tarda en rodar por la bandeja hasta su sitio. */
    private static double rollTime(PoolTableBE.Drop d) {
        if (d.slot < 0) {
            return 0.25;
        }
        double tu = PoolTableBE.TRAY_U;
        double tv = PoolTableBE.TRAY_V0 + d.slot * PoolTableBE.TRAY_DV;
        return Mth.clamp(2.0 * Math.hypot(tu - d.pu, tv - d.pv) / 3.2, 0.45, 3.0);
    }

    /**
     * Dónde está una bola que cae: primero rueda hasta el centro del hoyo, luego cae recta por él hasta la bandeja y por último rueda por ella
     * (frenando) hasta su sitio. Devuelve {u, v, y} o null cuando ya terminó.
     */
    private static double[] dropPos(PoolTableBE be, PoolTableBE.Drop d, long now, double ballY, double landY) {
        double t = (now - d.start) / 1.0e9;
        final double t1 = 0.14;
        final double g = 8.0;
        double t2 = Math.sqrt(2.0 * (ballY - landY) / g);
        double t3 = rollTime(d);
        if (t < t1) {
            double w = t / t1;
            double f = w * w * (3.0 - 2.0 * w);
            return new double[] {d.su + (d.pu - d.su) * f, d.sv + (d.pv - d.sv) * f, ballY - 0.02 * f};
        }
        if (t < t1 + t2) {
            double q = t - t1;
            return new double[] {d.pu, d.pv, Math.max(landY, ballY - 0.02 - 0.5 * g * q * q)};
        }
        if (!d.landed) {
            d.landed = true;
            clack(be, d.pu, d.pv, 0.35F, 1.2F);
        }
        if (t < t1 + t2 + t3) {
            if (d.slot < 0) {
                return new double[] {d.pu, d.pv, landY};
            }
            double w = (t - t1 - t2) / t3;
            double s = 1.0 - (1.0 - w) * (1.0 - w);
            double tu = PoolTableBE.TRAY_U;
            double tv = PoolTableBE.TRAY_V0 + d.slot * PoolTableBE.TRAY_DV;
            return new double[] {d.pu + (tu - d.pu) * s, d.pv + (tv - d.pv) * s, landY};
        }
        if (!d.arrived) {
            d.arrived = true;
            if (d.slot >= 0) {
                clack(be, PoolTableBE.TRAY_U, PoolTableBE.TRAY_V0 + d.slot * PoolTableBE.TRAY_DV, 0.25F, 1.5F);
            }
        }
        return null;
    }

    private static void clack(PoolTableBE be, double u, double v, float vol, float pitch) {
        if (be.getLevel() == null) {
            return;
        }
        Vec3 w = be.world(u, v);
        be.getLevel().playLocalSound(w.x, be.getBlockPos().getY() + PoolTableBE.SHELF_Y, w.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, vol, pitch, false);
    }

    /** Dibuja una bola en (u, v) del tablero a la altura y (centro), girándola según lo que rodó desde el último dibujo. */
    private void drawBall(PoolTableBE be, int i, float u, float v, double y, PoseStack ps, MultiBufferSource buf, int light, Vec3 du, Vec3 dv,
                          double baseX, double baseZ) {
        Quaternionf q = be.orient[i];
        if (q == null) {
            q = new Quaternionf().rotationY((float) (Math.random() * Math.PI * 2)).rotateX(-Mth.HALF_PI);
            be.orient[i] = q;
            be.rx[i] = u;
            be.rz[i] = v;
        }
        // al rodar, la bola gira alrededor del eje horizontal perpendicular a su movimiento
        float dx = u - be.rx[i];
        float dz = v - be.rz[i];
        be.rx[i] = u;
        be.rz[i] = v;
        float dist = Mth.sqrt(dx * dx + dz * dz);
        if (dist > 1.0e-6F && dist < 0.5F) {
            float wdx = (float) (du.x * dx + dv.x * dz);
            float wdz = (float) (du.z * dx + dv.z * dz);
            Vector3f axis = new Vector3f(wdz, 0.0F, -wdx).normalize();
            q.premul(new Quaternionf().rotateAxis(dist / (float) PoolPhysics.R, axis));
            q.normalize();
        }
        double lu = u - PoolPhysics.L / 2;
        double lv = v - PoolPhysics.W / 2;
        ps.pushPose();
        ps.translate(baseX + du.x * lu + dv.x * lv, y, baseZ + du.z * lu + dv.z * lv);
        ps.mulPose(q);
        ps.scale((float) PoolPhysics.R, (float) PoolPhysics.R, (float) PoolPhysics.R);
        BallMesh.draw(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX[i])), light);
        ps.popPose();
    }

    private static boolean myTurn(PoolTableBE be, LocalPlayer p) {
        UUID cur = be.currentPlayer();
        return cur == null || cur.equals(p.getUUID());
    }

    private void drawAim(PoolTableBE be, LocalPlayer p, float partial, PoseStack ps, MultiBufferSource buf) {
        Vec3 look = p.getViewVector(partial);
        double[] d = be.toTableDir(look.x, look.z);
        if (Math.hypot(d[0], d[1]) < 1.0e-4) {
            return;
        }
        PoolPhysics.Trace t = be.ph.trace(d[0], d[1]);
        VertexConsumer lc = buf.getBuffer(RenderType.lines());
        Vec3 du = be.dirU();
        Vec3 dv = be.dirV();
        Vec3 ctr = be.center();
        double bx = ctr.x - be.getBlockPos().getX();
        double bz = ctr.z - be.getBlockPos().getZ();
        float y = (float) (PoolTableBE.FELT_Y + PoolPhysics.R);
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        double sx = be.ph.x[0];
        double sz = be.ph.z[0];
        line(lc, m, n, wx(du, dv, bx, sx, sz), y, wz(du, dv, bz, sx, sz), wx(du, dv, bx, t.hitX, t.hitZ), y, wz(du, dv, bz, t.hitX, t.hitZ), 255, 255, 255, 220);
        // la bola fantasma en el punto de contacto
        int segs = 20;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs;
            double a1 = Math.PI * 2 * (i + 1) / segs;
            double u0 = t.hitX + Math.cos(a0) * PoolPhysics.R;
            double v0 = t.hitZ + Math.sin(a0) * PoolPhysics.R;
            double u1 = t.hitX + Math.cos(a1) * PoolPhysics.R;
            double v1 = t.hitZ + Math.sin(a1) * PoolPhysics.R;
            line(lc, m, n, wx(du, dv, bx, u0, v0), y, wz(du, dv, bz, u0, v0), wx(du, dv, bx, u1, v1), y, wz(du, dv, bz, u1, v1), 255, 255, 255, 200);
        }
        if (t.ball >= 0) {
            // hacia dónde saldrá la bola tocada (amarillo) y por dónde seguirá la blanca (celeste)
            double ex = t.hitX + t.outX * 0.55;
            double ez = t.hitZ + t.outZ * 0.55;
            double bx0 = be.ph.x[t.ball];
            double bz0 = be.ph.z[t.ball];
            line(lc, m, n, wx(du, dv, bx, bx0, bz0), y, wz(du, dv, bz, bx0, bz0), wx(du, dv, bx, bx0 + t.outX * 0.55, bz0 + t.outZ * 0.55), y,
                    wz(du, dv, bz, bx0 + t.outX * 0.55, bz0 + t.outZ * 0.55), 255, 220, 60, 230);
            if (t.cueX != 0 || t.cueZ != 0) {
                line(lc, m, n, wx(du, dv, bx, t.hitX, t.hitZ), y, wz(du, dv, bz, t.hitX, t.hitZ), wx(du, dv, bx, t.hitX + t.cueX * 0.4, t.hitZ + t.cueZ * 0.4), y,
                        wz(du, dv, bz, t.hitX + t.cueX * 0.4, t.hitZ + t.cueZ * 0.4), 120, 200, 255, 230);
            }
            if (ex == Double.MAX_VALUE) {
                return;
            }
        } else {
            line(lc, m, n, wx(du, dv, bx, t.hitX, t.hitZ), y, wz(du, dv, bz, t.hitX, t.hitZ), wx(du, dv, bx, t.hitX + t.outX * 0.5, t.hitZ + t.outZ * 0.5), y,
                    wz(du, dv, bz, t.hitX + t.outX * 0.5, t.hitZ + t.outZ * 0.5), 120, 200, 255, 200);
        }
    }

    private static float wx(Vec3 du, Vec3 dv, double base, double u, double v) {
        return (float) (base + du.x * (u - PoolPhysics.L / 2) + dv.x * (v - PoolPhysics.W / 2));
    }

    private static float wz(Vec3 du, Vec3 dv, double base, double u, double v) {
        return (float) (base + du.z * (u - PoolPhysics.L / 2) + dv.z * (v - PoolPhysics.W / 2));
    }

    private static void line(VertexConsumer vc, Matrix4f m, Matrix3f n, float x0, float y0, float z0, float x1, float y1, float z1, int r, int g, int b, int al) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6F) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        vc.vertex(m, x0, y0, z0).color(r, g, b, al).normal(n, dx, dy, dz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, al).normal(n, dx, dy, dz).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(PoolTableBE be) {
        return true;
    }
}

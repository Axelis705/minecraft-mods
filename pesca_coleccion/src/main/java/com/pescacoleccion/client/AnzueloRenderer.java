package com.pescacoleccion.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.client.GeoEntityRenderer;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pesca.Anzuelo;
import com.pescacoleccion.pesca.CanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/**
 * (Cliente) El corcho (modelo animado) y el hilo desde la punta de la caña: cuelga flojo mientras se espera y se tensa recto durante la
 * pelea. El hilo se dibuja como una cinta delgada que siempre mira a la cámara (igual en todas las versiones).
 */
public class AnzueloRenderer extends GeoEntityRenderer<Anzuelo> {
    private static final ResourceLocation TEX = Ids.rl(PescaColeccion.MODID, "textures/entity/anzuelo.png");
    private static final ResourceLocation HILO = Ids.rl(PescaColeccion.MODID, "textures/entity/hilo.png");

    public AnzueloRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Ids.rl(PescaColeccion.MODID, "geo/anzuelo.geo.json"), 0F, Ids.rl(PescaColeccion.MODID, "animations/anzuelo.animation.json"));
    }

    @Override
    protected ResourceLocation texture(Anzuelo e) {
        return TEX;
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
        h.animator().layer("action");
    }

    @Override
    protected void fill(Anzuelo e, float pt, Snap s) {
        super.fill(e, pt, s);
        Player o = e.owner();
        if (o == null) {
            s.host.data.remove("hilo");
            return;
        }
        double bx = Mth.lerp(pt, e.xo, e.getX()), by = Mth.lerp(pt, e.yo, e.getY()), bz = Mth.lerp(pt, e.zo, e.getZ());
        double[] tip = rodTip(o, pt);
        float tension = e.state() == Anzuelo.PELEA ? 1F : e.state() == Anzuelo.PICA ? 0.7F : e.state() == Anzuelo.VUELA ? 0.5F : 0.1F;
        s.host.data.put("hilo", new double[] {tip[0] - bx, tip[1] - by, tip[2] - bz, tension, bx, by, bz});
    }

    /** Dónde está la punta de la caña (en el mundo), en primera persona y vista desde afuera. */
    private static double[] rodTip(Player p, float pt) {
        Minecraft mc = Minecraft.getInstance();
        int hand = p.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (!(p.getMainHandItem().getItem() instanceof CanaItem)) {
            hand = -hand;
        }
        double px = Mth.lerp(pt, p.xo, p.getX()), py = Mth.lerp(pt, p.yo, p.getY()), pz = Mth.lerp(pt, p.zo, p.getZ());
        if (p == mc.player && mc.options.getCameraType().isFirstPerson()) {
            double yaw = Math.toRadians(p.getViewYRot(pt)), pitch = Math.toRadians(p.getViewXRot(pt));
            double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
            double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
            // arriba de la vista (perpendicular a mirar y a la derecha)
            double ux = -Math.sin(yaw) * -Math.sin(pitch) * -1, uy = Math.cos(pitch), uz = Math.cos(yaw) * Math.sin(pitch);
            double ex = px, ey = py + p.getEyeHeight(), ez = pz;
            return new double[] {ex + fx * 0.9 + rx * 0.42 * hand + ux * 0.18, ey + fy * 0.9 + uy * 0.18 - 0.05, ez + fz * 0.9 + rz * 0.42 * hand + uz * 0.18};
        }
        double body = Math.toRadians(Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot));
        double sin = Math.sin(body), cos = Math.cos(body);
        double side = hand * 0.35;
        return new double[] {px - cos * side - sin * 0.8, py + p.getEyeHeight() - 0.45 + (p.isCrouching() ? -0.19 : 0), pz - sin * side + cos * 0.8};
    }

    @Override
    protected void drawSnap(Snap s, PoseStack ps, Draw d) {
        super.drawSnap(s, ps, d);
        Object o = s.host == null ? null : s.host.data.get("hilo");
        if (!(o instanceof double[] h)) {
            return;
        }
        drawLine(ps, d, h, s.light);
    }

    /** La cinta del hilo: del corcho (arriba) a la punta, con caída si está flojo. */
    private static void drawLine(PoseStack ps, Draw d, double[] h, int light) {
        double dx = h[0], dy = h[1] - 0.22, dz = h[2];
        double tension = h[3];
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double sag = (1 - tension) * Math.min(1.4, dist * 0.18);
        var cam = Draw.camPos();
        VB vb = d.cutout(HILO);
        PoseStack.Pose pose = ps.last();
        int n = 18;
        double half = 0.012;
        double[] prev = null;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double x = dx * t, y = 0.22 + dy * t + sag * (t * t - t) * 2, z = dz * t;
            double[] cur = {x, y, z};
            if (prev != null) {
                // lado de la cinta: perpendicular al tramo y a la dirección hacia la cámara
                double sx = cur[0] - prev[0], sy = cur[1] - prev[1], sz = cur[2] - prev[2];
                double cx = cam.x - (h[4] + prev[0]), cy = cam.y - (h[5] + prev[1]), cz = cam.z - (h[6] + prev[2]);
                double qx = sy * cz - sz * cy, qy = sz * cx - sx * cz, qz = sx * cy - sy * cx;
                double ql = Math.sqrt(qx * qx + qy * qy + qz * qz);
                if (ql < 1e-6) {
                    prev = cur;
                    continue;
                }
                qx = qx / ql * half;
                qy = qy / ql * half;
                qz = qz / ql * half;
                float nx = (float) (cx / Math.max(1e-6, Math.sqrt(cx * cx + cy * cy + cz * cz))), ny = 0.6F, nz = 0F;
                quad(vb, pose, prev, cur, qx, qy, qz, light, nx, ny, nz);
            }
            prev = cur;
        }
    }

    private static void quad(VB vb, PoseStack.Pose pose, double[] a, double[] b, double qx, double qy, double qz, int light, float nx, float ny,
            float nz) {
        int r = 230, g = 230, bl = 235, al = 255;
        int ov = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        // por las dos caras (se ve igual desde cualquier lado)
        vb.v(pose, (float) (a[0] - qx), (float) (a[1] - qy), (float) (a[2] - qz), r, g, bl, al, 0, 0, ov, light, nx, ny, nz);
        vb.v(pose, (float) (a[0] + qx), (float) (a[1] + qy), (float) (a[2] + qz), r, g, bl, al, 1, 0, ov, light, nx, ny, nz);
        vb.v(pose, (float) (b[0] + qx), (float) (b[1] + qy), (float) (b[2] + qz), r, g, bl, al, 1, 1, ov, light, nx, ny, nz);
        vb.v(pose, (float) (b[0] - qx), (float) (b[1] - qy), (float) (b[2] - qz), r, g, bl, al, 0, 1, ov, light, nx, ny, nz);
        vb.v(pose, (float) (b[0] - qx), (float) (b[1] - qy), (float) (b[2] - qz), r, g, bl, al, 0, 1, ov, light, nx, ny, nz);
        vb.v(pose, (float) (b[0] + qx), (float) (b[1] + qy), (float) (b[2] + qz), r, g, bl, al, 1, 1, ov, light, nx, ny, nz);
        vb.v(pose, (float) (a[0] + qx), (float) (a[1] + qy), (float) (a[2] + qz), r, g, bl, al, 1, 0, ov, light, nx, ny, nz);
        vb.v(pose, (float) (a[0] - qx), (float) (a[1] - qy), (float) (a[2] - qz), r, g, bl, al, 0, 0, ov, light, nx, ny, nz);
    }
}

package com.coleccionista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * (Cliente) Las cadenas de Encadenados dibujadas en el mundo con la textura de la cadena de Minecraft: entre cada pareja (cuelga cuando
 * están cerca, se estira cuando se alejan y se queda en el piso si cuelga de más) y del grillete de la pared a la cintura de quien sigue
 * encadenado. También marca las placas de las compuertas (roja sin pisar, verde pisada).
 */
public final class CadenaRender {
    private static final ResourceLocation CADENA = new ResourceLocation("minecraft", "textures/block/chain.png");
    private static final ResourceLocation BLANCO = com.axeliscore.plat.client.Draw.BLANCO;

    private CadenaRender() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(CadenaRender::dibujar);
    }

    private static Vec3 cintura(Entity e, float pt) {
        return e.getPosition(pt).add(0, e.getBbHeight() * 0.48, 0);
    }

    private static void dibujar(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !EncadenadosCliente.enJuego()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderType rt = RenderType.entityCutoutNoCull(CADENA);
        VertexConsumer vc = buf.getBuffer(rt);
        for (EncadenadosCliente.Cadena c : EncadenadosCliente.cadenas) {
            Entity a = EncadenadosCliente.ents.get(c.a()), b = EncadenadosCliente.ents.get(c.b());
            if (a == null || b == null || a.distanceToSqr(cam) > 96 * 96) {
                continue;
            }
            double piso = Math.min(a.getPosition(pt).y, b.getPosition(pt).y) + 0.06;
            cadena(vc, ps, cintura(a, pt), cintura(b, pt), EncadenadosCliente.largo, piso);
        }
        for (EncadenadosCliente.Grillete g : EncadenadosCliente.grilletes.values()) {
            Entity a = EncadenadosCliente.ents.get(g.id());
            if (a == null || g.argolla() == null || a.distanceToSqr(cam) > 96 * 96) {
                continue;
            }
            Vec3 pie = a.getPosition(pt).add(0, 0.25, 0);
            cadena(vc, ps, g.argolla(), pie, EncadenadosCliente.radio + 1.6, a.getPosition(pt).y + 0.06);
        }
        buf.endBatch(rt);
        // las placas de las compuertas
        if ("JUEGO".equals(EncadenadosCliente.fase)) {
            RenderType rp = RenderType.entityTranslucent(BLANCO);
            VertexConsumer vp = buf.getBuffer(rp);
            float brillo = (float) (0.55 + 0.25 * Math.sin(System.currentTimeMillis() / 300.0));
            for (EncadenadosCliente.Placa p : EncadenadosCliente.placas) {
                if (cam.distanceToSqr(p.x() + 0.5, p.y(), p.z() + 0.5) > 48 * 48) {
                    continue;
                }
                int col = p.abierta() ? 0x60E070 : p.on() ? 0xB0E060 : 0xE04040;
                placa(vp, ps, p.x(), p.y() + 0.08F, p.z(), col, (p.on() || p.abierta() ? 0.45F : 0.3F) * brillo);
            }
            buf.endBatch(rp);
        }
        ps.popPose();
    }

    /**
     * Una cadena de {@code a} a {@code b}: cuelga más mientras más floja está (largo menos la distancia) y no baja del piso. Cada tramo son
     * dos tiras cruzadas con la textura de la cadena (como el bloque de cadena de Minecraft).
     */
    static void cadena(VertexConsumer vc, PoseStack ps, Vec3 a, Vec3 b, double largo, double piso) {
        double d = a.distanceTo(b);
        if (d < 0.05) {
            return;
        }
        double floja = Math.max(0, largo - d);
        double cuelga = 0.12 + floja * 0.42;
        int n = Math.max(4, (int) Math.ceil((d + floja) / 0.3));
        Vec3 prev = null;
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            Vec3 p = a.lerp(b, t).add(0, -4 * cuelga * t * (1 - t), 0);
            if (p.y < piso) {
                p = new Vec3(p.x, piso, p.z);
            }
            if (prev != null) {
                int luz = LevelRenderer.getLightColor(mc.level, BlockPos.containing(prev.lerp(p, 0.5)));
                tramo(vc, ps, prev, p, luz);
            }
            prev = p;
        }
    }

    private static void tramo(VertexConsumer vc, PoseStack ps, Vec3 p0, Vec3 p1, int luz) {
        Vec3 dir = p1.subtract(p0);
        double len = dir.length();
        if (len < 1e-4) {
            return;
        }
        dir = dir.scale(1 / len);
        Vec3 arriba = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 l1 = dir.cross(arriba).normalize().scale(0.095);
        Vec3 l2 = dir.cross(l1).normalize().scale(0.095);
        float v1 = (float) Math.min(1, len);
        tira(vc, ps, p0, p1, l1, 0F, 3 / 16F, v1, luz);
        tira(vc, ps, p0, p1, l2, 3 / 16F, 6 / 16F, v1, luz);
    }

    private static void tira(VertexConsumer vc, PoseStack ps, Vec3 p0, Vec3 p1, Vec3 lado, float u0, float u1, float v1, int luz) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        Vec3 nn = lado.normalize();
        v(vc, m, n, p0.subtract(lado), u0, 0, luz, nn);
        v(vc, m, n, p0.add(lado), u1, 0, luz, nn);
        v(vc, m, n, p1.add(lado), u1, v1, luz, nn);
        v(vc, m, n, p1.subtract(lado), u0, v1, luz, nn);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 p, float u, float v, int luz, Vec3 nn) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz)
                .normal(n, (float) nn.x, (float) nn.y, (float) nn.z).endVertex();
    }

    private static void placa(VertexConsumer vc, PoseStack ps, float x, float y, float z, int rgb, float a) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, al = (int) (a * 255);
        float i = 0.06F;
        int luz = 0xF000F0;
        vc.vertex(m, x + i, y, z + i).color(r, g, b, al).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + i, y, z + 1 - i).color(r, g, b, al).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + 1 - i, y, z + 1 - i).color(r, g, b, al).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0)
                .endVertex();
        vc.vertex(m, x + 1 - i, y, z + i).color(r, g, b, al).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
    }
}

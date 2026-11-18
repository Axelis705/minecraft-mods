package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.AnclaMagma;
import com.mundosdeaxelis.entity.CorazonMagma;
import com.mundosdeaxelis.entity.LarvaMagma;
import com.mundosdeaxelis.vfx.BossMesh;
import com.mundosdeaxelis.vfx.Cam;
import com.mundosdeaxelis.vfx.Sink;
import com.mundosdeaxelis.vfx.V3;
import com.mundosdeaxelis.vfx.VfxTex;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderers de la etapa 4 (fuego): el Corazón de Magma con sus cadenas, las anclas y las larvas. No usan modelos de cubos:
 * dibujan las mallas suaves de {@link BossMesh} (el mismo motor que las mascotas 3D) directamente en los búferes de entidad.
 */
public final class MagmaRenderers {
    static final ResourceLocation CREATURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/vfx/creature3d.png");
    static final ResourceLocation SOFT = new ResourceLocation(MundosDeAxelis.MODID, "textures/vfx/soft.png");

    private MagmaRenderers() {
    }

    /** Escribe los cuadros de la malla en los búferes de entidad (lo aditivo como «ojos»: suma luz). */
    static Sink sink(PoseStack ps, MultiBufferSource buf, int light) {
        Matrix4f pose = ps.last().pose();
        Matrix3f nrm = ps.last().normal();
        return (tex, blend, p, uv, col) -> {
            ResourceLocation rl = tex == VfxTex.SOFT ? SOFT : CREATURE;
            boolean add = blend == Sink.ADD;
            // pedir el búfer justo antes de usarlo (los búferes de entidad se comparten)
            VertexConsumer vc = buf.getBuffer(add ? RenderType.eyes(rl) : RenderType.entityTranslucent(rl));
            for (int k = 0; k < 4; k++) {
                float a = col[k * 4 + 3];
                float r = col[k * 4], g = col[k * 4 + 1], b = col[k * 4 + 2];
                if (add) {
                    r *= a;
                    g *= a;
                    b *= a;
                    a = 1F;
                }
                vc.vertex(pose, (float) p[k * 3], (float) p[k * 3 + 1], (float) p[k * 3 + 2])
                        .color(Mth.clamp(r, 0F, 1F), Mth.clamp(g, 0F, 1F), Mth.clamp(b, 0F, 1F), Mth.clamp(a, 0F, 1F))
                        .uv(uv[k * 2], uv[k * 2 + 1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nrm, 0F, 1F, 0F).endVertex();
            }
        };
    }

    /** Cámara del juego en coordenadas relativas a la entidad (para la luz de borde de la malla). */
    static Cam cam(Camera camera, Entity e, float pt) {
        Vec3 cp = camera.getPosition();
        double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector(), look = camera.getLookVector();
        return new Cam(new V3(cp.x - ex, cp.y - ey, cp.z - ez), new V3(-left.x(), -left.y(), -left.z()), new V3(up.x(), up.y(), up.z()),
                new V3(look.x(), look.y(), look.z()));
    }

    static Vec3 lerpPos(Entity e, float pt) {
        return new Vec3(Mth.lerp(pt, e.xo, e.getX()), Mth.lerp(pt, e.yo, e.getY()), Mth.lerp(pt, e.zo, e.getZ()));
    }

    // ------------------------------------------------------------------ corazón

    public static final class Heart extends EntityRenderer<CorazonMagma> {
        private static final double SCALE = 2.3;

        public Heart(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(CorazonMagma e, Frustum frustum, double x, double y, double z) {
            // las cadenas salen muy lejos de su caja: se dibuja siempre que esté cerca
            return e.distanceToSqr(x, y, z) < 120 * 120;
        }

        @Override
        public void render(CorazonMagma e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + pt;
            double death = e.deathProgress(pt);
            double beat = BossMesh.beat(t, e.beatPeriod());
            Cam cam = cam(this.entityRenderDispatcher.camera, e, pt);
            Vec3 me = lerpPos(e, pt);
            V3 center = new V3(0, e.getBbHeight() * 0.5, 0);
            if (death > 0) {
                // convulsiona antes de reventar
                center = center.add(Math.sin(t * 2.3) * 0.08 * death, Math.sin(t * 3.1) * 0.08 * death, Math.cos(t * 2.7) * 0.08 * death);
            }
            List<V3> hooks = new ArrayList<>();
            if (e.state() == CorazonMagma.S_CHAINED || e.state() == CorazonMagma.S_DESCEND || e.state() == CorazonMagma.S_RISE) {
                for (AnclaMagma a : e.level().getEntitiesOfClass(AnclaMagma.class, e.getBoundingBox().inflate(40.0))) {
                    if (a.isAlive()) {
                        Vec3 h = lerpPos(a, pt).add(0, 0.95, 0).subtract(me);
                        hooks.add(new V3(h.x, h.y, h.z));
                    }
                }
            }
            V3[] dirs = new V3[hooks.size()];
            for (int i = 0; i < hooks.size(); i++) {
                dirs[i] = hooks.get(i).sub(center);
            }
            Sink s = sink(ps, buf, LightTexture.FULL_BRIGHT);
            double scale = SCALE * (1 - 0.15 * death);
            BossMesh.heart(s, cam, center, scale, t, Math.max(beat, death * 0.6), e.plates(), Math.max(e.crack(), death),
                    (e.phase() - 1) * 0.5 + death, dirs);
            for (int i = 0; i < hooks.size(); i++) {
                V3 d = dirs[i];
                V3 from = center.add(BossMesh.heartSurface(d).mul(scale)).add(d.norm().mul(0.12));
                BossMesh.chain(s, cam, from, hooks.get(i), t, 1.0, 0.6 + 0.2 * Math.sin(t * 0.05 + i));
            }
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(CorazonMagma e) {
            return CREATURE;
        }
    }

    // ------------------------------------------------------------------ ancla

    public static final class Anchor extends EntityRenderer<AnclaMagma> {
        public Anchor(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.6F;
        }

        @Override
        public void render(AnclaMagma e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + pt;
            double hurt = e.hurtTime > 0 ? (e.hurtTime - pt) / 10.0 : 0.0;
            double life = e.getMaxHealth() > 0 ? e.getHealth() / e.getMaxHealth() : 1.0;
            BossMesh.anchor(sink(ps, buf, LightTexture.FULL_BRIGHT), cam(this.entityRenderDispatcher.camera, e, pt), new V3(0, 0, 0), t,
                    Math.max(0, hurt), life);
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(AnclaMagma e) {
            return CREATURE;
        }
    }

    // ------------------------------------------------------------------ larva

    public static final class Larva extends EntityRenderer<LarvaMagma> {
        public Larva(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.35F;
        }

        @Override
        public void render(LarvaMagma e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + pt;
            double hurt = e.hurtTime > 0 ? (e.hurtTime - pt) / 10.0 : 0.0;
            float body = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
            double walk = e.walkAnimation.speed(pt);
            double sc = e.isDeadOrDying() ? Math.max(0.05, 1 - (e.deathTime + pt) / 20.0) : 1.0;
            BossMesh.larva(sink(ps, buf, LightTexture.FULL_BRIGHT), cam(this.entityRenderDispatcher.camera, e, pt), new V3(0, 0, 0), sc, t,
                    Math.min(1, walk * 1.5), -Math.toRadians(body), Math.max(0, hurt));
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(LarvaMagma e) {
            return CREATURE;
        }
    }
}

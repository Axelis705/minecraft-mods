package com.coleccionista.client;

import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.coleccionista.Coleccionista;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * (Cliente) El gas de Encadenados: un frente verde amarillento que avanza por las Cloacas de oeste a este. Se ve como una pared de
 * niebla que se mueve (con capas detrás), suelta nubecitas y silba; si te alcanza, la niebla te envuelve (no ves a más de unos bloques)
 * y todo se tiñe de verde.
 */
public final class Gas {
    private static final ResourceLocation TEX = Ids.rl(Coleccionista.MODID, "textures/misc/gas.png");
    private static boolean on;
    private static double x, vel, fin;
    private static int x0, z0, z1, y0, y1;
    static long recibido;
    private static long tickRecibido, ticks;
    /** Qué tan metido estoy en el gas (0..1, suave). */
    private static float dentro;

    private Gas() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Gas::niebla);
        MinecraftForge.EVENT_BUS.addListener(Gas::colorNiebla);
        MinecraftForge.EVENT_BUS.addListener(Gas::dibujar);
        MinecraftForge.EVENT_BUS.addListener(Gas::tick);
    }

    static void recibir(CompoundTag g) {
        on = Nbt.b(g, "on");
        x = g.getDouble("x");
        vel = g.getDouble("vel");
        fin = g.getDouble("fin");
        x0 = Nbt.i(g, "x0");
        z0 = Nbt.i(g, "z0");
        z1 = Nbt.i(g, "z1");
        y0 = Nbt.i(g, "y0");
        y1 = Nbt.i(g, "y1");
        recibido = System.currentTimeMillis();
        tickRecibido = ticks;
    }

    /** Dónde va el frente ahora (se adelanta solo entre mensajes, salvo en pausa). */
    public static double frente(float pt) {
        if (!on) {
            return x;
        }
        boolean pausa = "PAUSA".equals(EventoCliente.fase());
        double dt = pausa ? 0 : ticks - tickRecibido + pt;
        return Math.min(fin, x + vel * dt);
    }

    public static boolean activo() {
        return on && EncadenadosCliente.activa();
    }

    private static boolean enZona(Vec3 p) {
        return p.x >= x0 && p.z >= z0 && p.z <= z1 + 1 && p.y >= y0 - 2 && p.y <= y1 + 2;
    }

    /** ¿Este punto está en el gas? */
    public static boolean adentro(Vec3 p, float pt) {
        return activo() && enZona(p) && p.x < frente(pt) && p.x < fin;
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        boolean aqui = adentro(cam, 0);
        dentro = Mth.clamp(dentro + (aqui ? 0.06F : -0.04F), 0, 1);
        if (!activo()) {
            return;
        }
        double fx = frente(0);
        RandomSource r = mc.level.random;
        // nubecitas en el frente (cerca de mí) y alrededor si estoy adentro
        if (Math.abs(cam.x - fx) < 28 && enZona(cam)) {
            for (int i = 0; i < 7; i++) {
                double px = fx - r.nextDouble() * 4.5, py = y0 + 1 + r.nextDouble() * 5, pz = cam.z + (r.nextDouble() - 0.5) * 26;
                mc.level.addParticle(ParticleTypes.ENTITY_EFFECT, px, py, pz, 0.55, 0.75, 0.2);
            }
            if (r.nextInt(3) == 0) {
                mc.level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, fx - r.nextDouble() * 2, y0 + 1.2, cam.z + (r.nextDouble() - 0.5) * 10, 0.01,
                        0.01, 0);
            }
            if (ticks % 50 == 0) {
                mc.level.playLocalSound(fx, cam.y, cam.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 0.35F, 0.45F, false);
            }
        }
        if (aqui) {
            for (int i = 0; i < 3; i++) {
                mc.level.addParticle(ParticleTypes.ENTITY_EFFECT, cam.x + (r.nextDouble() - 0.5) * 6, cam.y + (r.nextDouble() - 0.5) * 3,
                        cam.z + (r.nextDouble() - 0.5) * 6, 0.55, 0.75, 0.2);
            }
        }
    }

    private static void niebla(ViewportEvent.RenderFog e) {
        if (dentro <= 0.01F) {
            return;
        }
        float lejos = Mth.lerp(dentro, e.getFarPlaneDistance(), 6F);
        e.setNearPlaneDistance(Mth.lerp(dentro, e.getNearPlaneDistance(), -2F));
        e.setFarPlaneDistance(lejos);
        e.setCanceled(true);
    }

    private static void colorNiebla(ViewportEvent.ComputeFogColor e) {
        if (dentro <= 0.01F) {
            return;
        }
        e.setRed(Mth.lerp(dentro, e.getRed(), 0.30F));
        e.setGreen(Mth.lerp(dentro, e.getGreen(), 0.38F));
        e.setBlue(Mth.lerp(dentro, e.getBlue(), 0.10F));
    }

    /** La pared de gas (y dos capas detrás), dibujada en el mundo: las paredes de roca la tapan donde no hay túnel. */
    private static void dibujar(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !activo()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Camera c = e.getCamera();
        Vec3 cam = c.getPosition();
        float pt = e.getPartialTick();
        double fx = frente(pt);
        if (Math.abs(cam.x - fx) > 80 || fx >= fin) {
            return;
        }
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderType rt = RenderType.entityTranslucent(TEX);
        VertexConsumer vc = buf.getBuffer(rt);
        float tiempo = (ticks + pt) / 20F;
        double[] capas = {0, -1.6, -3.6};
        float[] alfas = {0.55F, 0.4F, 0.3F};
        for (int k = 0; k < capas.length; k++) {
            float px = (float) (fx + capas[k] + 0.15 * Math.sin(tiempo * 0.9 + k));
            for (int z = z0; z < z1 + 1; z += 4) {
                for (int y = y0 - 1; y < y1; y += 4) {
                    int i = z * 7 + y * 13 + k * 31;
                    float du = 0.25F + 0.25F * (float) Math.sin(tiempo * 0.35 + i), dv = 0.25F + 0.25F * (float) Math.cos(tiempo * 0.27 + i * 0.7);
                    cuadro(vc, ps, px, y, z, Math.min(4, z1 + 1 - z), Math.min(4, y1 - y), du, dv, alfas[k]);
                }
            }
        }
        ps.popPose();
        buf.endBatch(rt);
    }

    private static void cuadro(VertexConsumer vc, PoseStack ps, float x, float y, float z, float ancho, float alto, float u, float v, float a) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int al = (int) (a * 255);
        int luz = 0xF000F0;
        float u1 = u + 0.5F * ancho / 4F, v1 = v + 0.5F * alto / 4F;
        vc.vertex(m, x, y, z).color(170, 200, 90, al).uv(u, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 1, 0, 0).endVertex();
        vc.vertex(m, x, y, z + ancho).color(170, 200, 90, al).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 1, 0, 0).endVertex();
        vc.vertex(m, x, y + alto, z + ancho).color(170, 200, 90, al).uv(u1, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 1, 0, 0)
                .endVertex();
        vc.vertex(m, x, y + alto, z).color(170, 200, 90, al).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 1, 0, 0).endVertex();
    }
}

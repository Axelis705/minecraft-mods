package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.OreRadarPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Marcas brillantes sobre los minerales detectados, visibles a través de las paredes (solo para quien usó el Pico Prospector). */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OreRadar {
    private OreRadar() {
    }

    private static long[] pos = new long[0];
    private static int[] colors = new int[0];
    private static long startTick;
    private static int life;

    public static void show(OreRadarPacket p) {
        pos = p.pos;
        colors = p.colors;
        life = p.ticks;
        Minecraft mc = Minecraft.getInstance();
        startTick = mc.level == null ? 0 : mc.level.getGameTime();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (pos.length == 0 || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float t = (mc.level.getGameTime() - startTick) + event.getPartialTick();
        if (t >= life) {
            pos = new long[0];
            return;
        }
        float fade = Mth.clamp((life - t) / 12.0F, 0, 1) * Mth.clamp(t / 4.0F, 0, 1);
        Camera cam = event.getCamera();
        Vec3 cp = cam.getPosition();
        Vector3f left = cam.getLeftVector();
        Vector3f up = cam.getUpVector();
        Matrix4f m = event.getPoseStack().last().pose();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, CrateUi.HALO);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int i = 0; i < pos.length; i++) {
            BlockPos bp = BlockPos.of(pos[i]);
            double dx = bp.getX() + 0.5 - cp.x, dy = bp.getY() + 0.5 - cp.y, dz = bp.getZ() + 0.5 - cp.z;
            float pulse = 0.75F + 0.25F * Mth.sin(t * 0.35F + i);
            float s = 0.55F * pulse;
            int c = colors[i];
            float r = CrateUi.r(c), g = CrateUi.g(c), b = CrateUi.b(c);
            float a = fade * 0.9F;
            quad(bb, m, (float) dx, (float) dy, (float) dz, left, up, s, r, g, b, a);
            quad(bb, m, (float) dx, (float) dy, (float) dz, left, up, s * 0.35F, 1, 1, 1, a);
        }
        Tesselator.getInstance().end();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
    }

    private static void quad(BufferBuilder bb, Matrix4f m, float x, float y, float z, Vector3f l, Vector3f u, float s,
                             float r, float g, float b, float a) {
        float lx = l.x * s, ly = l.y * s, lz = l.z * s, ux = u.x * s, uy = u.y * s, uz = u.z * s;
        bb.vertex(m, x + lx - ux, y + ly - uy, z + lz - uz).color(r, g, b, a).uv(0, 1).endVertex();
        bb.vertex(m, x - lx - ux, y - ly - uy, z - lz - uz).color(r, g, b, a).uv(1, 1).endVertex();
        bb.vertex(m, x - lx + ux, y - ly + uy, z - lz + uz).color(r, g, b, a).uv(1, 0).endVertex();
        bb.vertex(m, x + lx + ux, y + ly + uy, z + lz + uz).color(r, g, b, a).uv(0, 0).endVertex();
    }
}

package com.mundosdeaxelis.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.VfxPacket;
import com.mundosdeaxelis.vfx.Cam;
import com.mundosdeaxelis.vfx.Sink;
import com.mundosdeaxelis.vfx.V3;
import com.mundosdeaxelis.vfx.VfxData;
import com.mundosdeaxelis.vfx.VfxGeom;
import com.mundosdeaxelis.vfx.VfxTex;
import com.mundosdeaxelis.vfx.VfxType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Efectos visuales dentro del mundo: los manda el servidor y aquí se animan y se dibujan. */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VfxClient {
    private VfxClient() {
    }

    /** Sombreador propio (lo pone ClientModEvents). Si es null se usa el de Minecraft. */
    public static ShaderInstance shader;

    private static final int MAX_FX = 320;
    private static final int MAX_QUADS = 36000;
    private static final double MAX_DIST = 140;

    private static final Map<Integer, VfxData> FX = new HashMap<>();
    private static ClientLevel lastLevel;
    private static long tick;

    private static final ResourceLocation[] TEX = new ResourceLocation[VfxTex.COUNT];

    static {
        for (int i = 0; i < VfxTex.COUNT; i++) {
            TEX[i] = new ResourceLocation(MundosDeAxelis.MODID, "textures/vfx/" + VfxTex.FILE[i] + ".png");
        }
    }

    // ------------------------------------------------------------------ mensajes del servidor

    public static void handle(VfxPacket m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        switch (m.action) {
            case VfxType.ACT_ADD -> {
                if (FX.size() >= MAX_FX && !FX.containsKey(m.id)) {
                    return;
                }
                VfxData old = FX.get(m.id);
                if (old != null && old.type == VfxType.SPRITE && m.type == VfxType.SPRITE) {
                    // reenvío de refresco (las mascotas fijas se renuevan cada 20 s): se alarga su vida SIN reiniciarlo,
                    // si no el dibujo volvía a "nacer" (se encogía a nada y rebotaba de nuevo) y parecía que desaparecía
                    old.rgb = m.rgb;
                    old.flags = m.flags;
                    old.size = m.size;
                    old.a = m.a;
                    old.b = m.b;
                    if (m.life > 0) {
                        old.life = (int) (tick - old.start) + m.life;
                    }
                    return;
                }
                VfxData d = new VfxData();
                d.id = m.id;
                d.type = m.type;
                d.rgb = m.rgb;
                d.flags = m.flags;
                d.life = m.life;
                d.follow = m.follow;
                d.size = m.size;
                d.a = m.a;
                d.b = m.b;
                d.dx = m.dx;
                d.dy = m.dy;
                d.dz = m.dz;
                d.start = tick;
                if (d.follow > 0) {
                    d.ox = m.x;
                    d.oy = m.y;
                    d.oz = m.z;
                    Entity e = mc.level.getEntity(d.follow);
                    if (e != null) {
                        d.x = e.getX() + d.ox;
                        d.y = e.getY() + d.oy;
                        d.z = e.getZ() + d.oz;
                    } else {
                        d.x = m.x;
                        d.y = m.y;
                        d.z = m.z;
                    }
                } else {
                    d.x = m.x;
                    d.y = m.y;
                    d.z = m.z;
                }
                d.px = d.x;
                d.py = d.y;
                d.pz = d.z;
                FX.put(d.id, d);
            }
            case VfxType.ACT_UPDATE -> {
                VfxData d = FX.get(m.id);
                if (d == null) {
                    return;
                }
                d.rgb = m.rgb;
                d.flags = m.flags;
                d.size = m.size;
                d.b = m.b;
                if (d.type == VfxType.DOME) {
                    d.a = Math.max(d.a, m.a);
                } else {
                    d.a = m.a;
                }
                d.dx = m.dx;
                d.dy = m.dy;
                d.dz = m.dz;
                if (m.life > 0) {
                    d.life = (int) (tick - d.start) + m.life;
                }
                if (d.follow > 0) {
                    d.ox = m.x;
                    d.oy = m.y;
                    d.oz = m.z;
                } else if (d.type == VfxType.SPRITE) {
                    // los dibujos que andan por ahí (dragoncito, hadas) persiguen la posición recibida cada tick
                    d.tx = m.x;
                    d.ty = m.y;
                    d.tz = m.z;
                    d.hasT = true;
                } else {
                    d.px = d.x;
                    d.py = d.y;
                    d.pz = d.z;
                    d.x = m.x;
                    d.y = m.y;
                    d.z = m.z;
                    d.updated = true;
                }
            }
            case VfxType.ACT_REMOVE -> FX.remove(m.id);
            default -> {
            }
        }
    }

    public static void clear() {
        FX.clear();
    }

    // ------------------------------------------------------------------ animación (cada tick)

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            FX.clear();
            lastLevel = mc.level;
        }
        if (mc.level == null || mc.isPaused() || FX.isEmpty()) {
            return;
        }
        tick++;
        Iterator<VfxData> it = FX.values().iterator();
        while (it.hasNext()) {
            VfxData d = it.next();
            long age = tick - d.start;
            if (d.life > 0 && age > d.life) {
                it.remove();
                continue;
            }
            if (d.follow > 0) {
                Entity e = mc.level.getEntity(d.follow);
                if (e == null || !e.isAlive()) {
                    if (age > 6) {
                        it.remove();
                    }
                    continue;
                }
                d.px = d.x;
                d.py = d.y;
                d.pz = d.z;
                d.x = e.getX() + d.ox;
                d.y = e.getY() + d.oy;
                d.z = e.getZ() + d.oz;
            } else if (d.type == VfxType.SPRITE && d.hasT) {
                // Suavizado: el servidor manda la posición a 20 por segundo y los paquetes llegan a destiempo respecto al tick
                // del cliente; si se interpolaba directamente, el dibujo daba tirones. Ahora siempre avanza una fracción fija
                // hacia el último destino y el dibujo interpola entre ticks.
                d.px = d.x;
                d.py = d.y;
                d.pz = d.z;
                double ex = d.tx - d.x, ey = d.ty - d.y, ez = d.tz - d.z;
                if (ex * ex + ey * ey + ez * ez > 64) {
                    d.x = d.tx;
                    d.y = d.ty;
                    d.z = d.tz;
                    d.px = d.x;
                    d.py = d.y;
                    d.pz = d.z;
                } else {
                    d.x += ex * 0.42;
                    d.y += ey * 0.42;
                    d.z += ez * 0.42;
                }
            } else if (d.updated) {
                // recibió una posición nueva: se desliza hasta ella durante este tick
                d.updated = false;
            } else {
                // sin novedades: se queda quieto (si no, vibraría al volver a empezar la interpolación)
                d.px = d.x;
                d.py = d.y;
                d.pz = d.z;
            }
            if ((d.flags & VfxType.F_TRAIL) != 0) {
                d.pushTrail(d.x, d.y, d.z);
            }
            if (d.type == VfxType.SPRITE && ((int) d.a == VfxTex.DRAGON3D || (int) d.a == VfxTex.CREATURE3D)) {
                // el dragoncito 3D gira poco a poco hacia donde mira (nunca de golpe)
                double tgt = Math.abs(d.dx) + Math.abs(d.dz) > 1.0E-4 ? Math.atan2(d.dx, d.dz) : d.yaw;
                if (!d.yawInit) {
                    d.yaw = tgt;
                    d.pyaw = tgt;
                    d.yawInit = true;
                } else {
                    d.pyaw = d.yaw;
                    double diff = tgt - d.yaw;
                    while (diff > Math.PI) {
                        diff -= Math.PI * 2;
                    }
                    while (diff < -Math.PI) {
                        diff += Math.PI * 2;
                    }
                    d.yaw += diff * 0.22;
                }
            }
            if (d.type == VfxType.DOME && d.a > 0) {
                d.a = Math.max(0, d.a - 0.12F);
            }
        }
    }

    // ------------------------------------------------------------------ dibujo

    /** Cuadriláteros acumulados por textura y modo de mezcla. */
    private static final class Batch {
        float[] v = new float[9 * 4 * 64];
        int n;

        void quad(double[] p, float[] uv, float[] col, double cx, double cy, double cz) {
            int need = (n + 4) * 9;
            if (need > v.length) {
                v = java.util.Arrays.copyOf(v, Math.max(need, v.length * 2));
            }
            for (int k = 0; k < 4; k++) {
                int o = (n + k) * 9;
                v[o] = (float) (p[k * 3] - cx);
                v[o + 1] = (float) (p[k * 3 + 1] - cy);
                v[o + 2] = (float) (p[k * 3 + 2] - cz);
                v[o + 3] = col[k * 4];
                v[o + 4] = col[k * 4 + 1];
                v[o + 5] = col[k * 4 + 2];
                v[o + 6] = col[k * 4 + 3];
                v[o + 7] = uv[k * 2];
                v[o + 8] = uv[k * 2 + 1];
            }
            n += 4;
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || FX.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 cp = camera.getPosition();
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector(), look = camera.getLookVector();
        Cam cam = new Cam(new V3(cp.x, cp.y, cp.z), new V3(-left.x(), -left.y(), -left.z()), new V3(up.x(), up.y(), up.z()),
                new V3(look.x(), look.y(), look.z()));
        float partial = event.getPartialTick();

        Batch[] batches = new Batch[VfxTex.COUNT * 3];
        final int[] quads = {0};
        final double cx = cp.x, cy = cp.y, cz = cp.z;
        Sink sink = (tex, blend, p, uv, col) -> {
            if (quads[0] >= MAX_QUADS || tex < 0 || tex >= VfxTex.COUNT) {
                return;
            }
            quads[0]++;
            int key = tex * 3 + (blend == Sink.ADD ? 0 : (blend == Sink.SOLID ? 2 : 1));
            Batch b = batches[key];
            if (b == null) {
                b = new Batch();
                batches[key] = b;
            }
            b.quad(p, uv, col, cx, cy, cz);
        };

        List<VfxData> list = new ArrayList<>(FX.values());
        for (VfxData d : list) {
            double dist = Math.sqrt((d.x - cp.x) * (d.x - cp.x) + (d.y - cp.y) * (d.y - cp.y) + (d.z - cp.z) * (d.z - cp.z));
            if (dist > MAX_DIST) {
                continue;
            }
            double age = (tick - d.start) + (mc.isPaused() ? 0 : partial);
            if (age < 0) {
                age = 0;
            }
            // la geometría trabaja en coordenadas del mundo
            VfxGeom.emit(d, age, partial, cam, sink);
        }
        if (quads[0] == 0) {
            return;
        }

        // Se dibuja en AFTER_WEATHER: en ese momento la rotación de la cámara YA está en RenderSystem.getModelViewMatrix(),
        // así que los vértices (relativos a la cámara) se mandan sin rotar. (AFTER_LEVEL no sirve: su PoseStack no trae
        // la rotación y salía todo mal.)
        // Con los gráficos "Fabulosos" este momento se dibuja en una capa aparte (weatherTarget) que luego se mezcla con
        // el mundo ordenando por profundidad y descartando los píxeles con alfa 0: por eso (1) la luz aditiva escribe un
        // alfa mínimo y (2) al final hay una pasada que solo escribe profundidad, para que los efectos queden delante
        // de las nubes y sobre el terreno en vez de empatados con él.
        boolean custom = shader != null;
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        // empuja un poco los cuadros hacia la cámara para que los dibujos pegados al suelo nunca queden tapados por el terreno
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-2.0F, -4.0F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        if (custom) {
            RenderSystem.setShader(() -> shader);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        }
        Tesselator ts = Tesselator.getInstance();
        BufferBuilder bb = ts.getBuilder();
        // primero lo opaco (con profundidad, el dragoncito 3D), luego lo translúcido y por último la luz aditiva
        for (int pass = 0; pass < (custom ? 4 : 3); pass++) {
            boolean solid = pass == 0;
            boolean add = pass == 2;
            boolean depthOnly = pass == 3;
            RenderSystem.depthMask(solid || depthOnly);
            if (depthOnly) {
                RenderSystem.colorMask(false, false, false, false);
                shader.safeGetUniform("AddMode").set(2F);
            } else if (custom) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                shader.safeGetUniform("AddMode").set(add ? 1F : 0F);
            } else if (add) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            for (int tex = 0; tex < VfxTex.COUNT; tex++) {
                // la pasada de profundidad repite lo translúcido y lo aditivo (lo opaco ya escribió la suya)
                for (int part = 0; part < (depthOnly ? 2 : 1); part++) {
                    int key = depthOnly ? tex * 3 + (part == 0 ? 1 : 0) : tex * 3 + (add ? 0 : (solid ? 2 : 1));
                    Batch b = batches[key];
                    if (b == null || b.n == 0) {
                        continue;
                    }
                    RenderSystem.setShaderTexture(0, TEX[tex]);
                    bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                    float[] v = b.v;
                    for (int i = 0; i < b.n; i++) {
                        int o = i * 9;
                        bb.vertex(v[o], v[o + 1], v[o + 2]).color(v[o + 3], v[o + 4], v[o + 5], v[o + 6]).uv(v[o + 7], v[o + 8]).endVertex();
                    }
                    BufferUploader.drawWithShader(bb.end());
                }
            }
            if (depthOnly) {
                RenderSystem.colorMask(true, true, true, true);
            }
        }
        RenderSystem.disablePolygonOffset();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}

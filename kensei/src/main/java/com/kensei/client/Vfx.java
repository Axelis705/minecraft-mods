package com.kensei.client;

import com.kensei.Kensei;
import com.kensei.KenseiConfig;
import com.kensei.net.FxPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Los efectos visuales del combate, dibujados en el mundo con luz propia (aditivos, brillan aunque sea de noche): círculos mágicos en
 * el suelo, ondas expansivas, tajos de luz que se barren, estrellas que salen al golpear y chispas que giran alrededor del jugador.
 * Cada efecto es un cuadrado con una textura (anillo, círculo, estrella, brillo o media luna) que se estira, gira y se desvanece.
 */
@Mod.EventBusSubscriber(modid = Kensei.MODID, value = Dist.CLIENT)
public final class Vfx {
    private static final ResourceLocation RING = tex("ring");
    private static final ResourceLocation CIRCLE = tex("circle");
    private static final ResourceLocation STAR = tex("star");
    private static final ResourceLocation GLOW = tex("glow");
    private static final ResourceLocation CRESCENT = tex("crescent");

    private static final int FULL_BRIGHT = 0xF000F0;

    // colores (RRGGBB)
    public static final int CYAN = 0x5FD4FF;
    public static final int WHITE = 0xFFFFFF;
    public static final int GOLD = 0xFFC83C;
    public static final int CRIMSON = 0xFF3A3A;
    public static final int ORANGE = 0xFF8A2E;

    private static final int RING_FLAT = 0;
    private static final int RING_BILL = 1;
    private static final int CIRCLE_K = 2;
    private static final int SLASH = 3;
    private static final int STAR_K = 4;
    private static final int FLASH = 5;
    private static final int STREAK = 6;
    private static final int ORBIT = 7;
    private static final int GHOST = 8;

    private static final Random RNG = new Random();

    private static final class Fx {
        int kind;
        int entityId = -1;
        double x;
        double y;
        double z;
        float x2;
        float y2;
        float z2;
        float yaw;
        float p1;
        float p2;
        float p3;
        int color = WHITE;
        float start;
        float life;
        float delay;
        float alpha = 1.0F;
        /** Quién lo lanzó (para poder cancelarlo si esa entidad esquiva a media preparación). */
        int owner = -1;
        boolean captured;
    }

    private static int currentOwner = -1;

    private static final List<Fx> LIST = new ArrayList<>();

    private Vfx() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(Kensei.MODID, "textures/vfx/" + name + ".png");
    }

    public static void clear() {
        LIST.clear();
        DamageNumbers.clear();
    }

    private static boolean enabled() {
        return KenseiConfig.VFX_ENABLED.get() && Minecraft.getInstance().level != null;
    }

    private static float now() {
        ClientLevel lv = Minecraft.getInstance().level;
        return lv == null ? 0 : lv.getGameTime();
    }

    private static Fx add(int kind, double x, double y, double z, int color, float life) {
        Fx f = new Fx();
        f.kind = kind;
        f.x = x;
        f.y = y;
        f.z = z;
        f.color = color;
        f.life = life;
        f.start = now();
        f.owner = currentOwner;
        if (LIST.size() > 400) {
            LIST.remove(0);
        }
        LIST.add(f);
        return f;
    }

    // ------------------------------------------------------------------ cosas que se pueden lanzar

    private static void groundRing(double x, double y, double z, float radius, int color, float life, float delay, float alpha) {
        Fx f = add(RING_FLAT, x, y + 0.04, z, color, life);
        f.p1 = radius;
        f.delay = delay;
        f.alpha = alpha;
    }

    private static void billRing(double x, double y, double z, float radius, int color, float life, float alpha) {
        Fx f = add(RING_BILL, x, y, z, color, life);
        f.p1 = radius;
        f.alpha = alpha;
    }

    private static void circle(Entity e, float radius, int color, float life, float spin) {
        Fx f = add(CIRCLE_K, e.getX(), e.getY() + 0.05, e.getZ(), color, life);
        f.entityId = e.getId();
        f.p1 = radius;
        f.p2 = spin;
    }

    /**
     * Un tajo de luz que se barre delante del jugador. (sr, su) = hacia dónde va el tajo (derecha / arriba del jugador), length = mitad del
     * largo, lean = cuánto se inclina el arco hacia la cámara, reach = a qué distancia delante, height = altura sobre los pies.
     */
    private static void slash(Entity e, float sr, float su, float length, float lean, float reach, float height, int color, float life, float delay) {
        Fx f = add(SLASH, e.getX(), e.getY() + height, e.getZ(), color, life);
        f.entityId = e.getId();
        f.yaw = e.getYRot();
        f.x2 = sr;
        f.y2 = su;
        f.z2 = reach;
        f.p1 = length;
        f.p2 = lean;
        f.p3 = height;
        f.delay = delay;
    }

    private static void star(double x, double y, double z, float dx, float dy, float dz, float size, int color, float life, float delay) {
        Fx f = add(STAR_K, x, y, z, color, life);
        f.x2 = dx;
        f.y2 = dy;
        f.z2 = dz;
        f.p1 = size;
        f.p2 = (RNG.nextFloat() - 0.5F) * 6.0F;
        f.delay = delay;
    }

    private static void flash(double x, double y, double z, float size, int color, float life) {
        Fx f = add(FLASH, x, y, z, color, life);
        f.p1 = size;
    }

    private static void streak(double x, double y, double z, double x2, double y2, double z2, float width, int color, float life, float delay) {
        Fx f = add(STREAK, x, y, z, color, life);
        f.x2 = (float) x2;
        f.y2 = (float) y2;
        f.z2 = (float) z2;
        f.p1 = width;
        f.delay = delay;
    }

    private static void orbit(Entity e, float radius, int count, int color, float life, float delay) {
        orbit(e, radius, count, color, life, delay, 1.9F);
    }

    /** height > 0: las chispas suben en espiral hasta esa altura; height < 0: giran en una corona fija a esa altura (como las estrellitas de un aturdido). */
    private static void orbit(Entity e, float radius, int count, int color, float life, float delay, float height) {
        Fx f = add(ORBIT, e.getX(), e.getY(), e.getZ(), color, life);
        f.entityId = e.getId();
        f.p1 = radius;
        f.p2 = count;
        f.p3 = height;
        f.delay = delay;
    }

    /** Una silueta brillante del jugador que se queda donde estaba cuando le toca (delay) y se desvanece. pose: 0 = esquive (brazos atrás), 1 = ataque (brazo adelante). */
    private static void ghost(Entity e, int pose, int color, float life, float delay) {
        if (!KenseiConfig.GHOSTS.get() || !(e instanceof AbstractClientPlayer)) {
            return;
        }
        Fx f = add(GHOST, e.getX(), e.getY(), e.getZ(), color, life);
        f.entityId = e.getId();
        f.p3 = pose;
        f.delay = delay;
    }

    /** Quita lo que esa entidad tenía a medias (el círculo y las chispas de la carga, los tajos que aún no salieron): por ejemplo al esquivar. */
    private static void cancelPending(int entityId) {
        float t0 = now();
        LIST.removeIf(f -> f.owner == entityId && (f.kind == CIRCLE_K || f.kind == ORBIT || t0 - f.start < f.delay));
    }

    private static void burst(double x, double y, double z, int count, float speed, float size, int color, float life) {
        for (int i = 0; i < count; i++) {
            double a = RNG.nextDouble() * Math.PI * 2;
            double el = (RNG.nextDouble() - 0.35) * 1.3;
            float sp = speed * (0.55F + RNG.nextFloat() * 0.75F);
            float dx = (float) (Math.cos(a) * Math.cos(el)) * sp;
            float dy = (float) Math.sin(el) * sp;
            float dz = (float) (Math.sin(a) * Math.cos(el)) * sp;
            star(x, y, z, dx, dy, dz, size * (0.6F + RNG.nextFloat() * 0.8F), RNG.nextInt(4) == 0 ? WHITE : color, life + RNG.nextInt(4), 0);
        }
    }

    // ------------------------------------------------------------------ quién dispara qué

    /** Empezó un movimiento de una entidad (llega de ClientAnims.set). */
    public static void onAnim(int entityId, int move, float arg) {
        if (!enabled()) {
            return;
        }
        ClientLevel lv = Minecraft.getInstance().level;
        Entity e = lv.getEntity(entityId);
        if (e == null) {
            return;
        }
        boolean self = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == entityId;
        if (move == ClientAnims.DODGE) {
            cancelPending(entityId);
        }
        currentOwner = entityId;
        try {
            spawnMove(lv, e, self, move, arg);
        } finally {
            currentOwner = -1;
        }
    }

    private static void spawnMove(ClientLevel lv, Entity e, boolean self, int move, float arg) {
        switch (move) {
            case 1 -> slash(e, -1.0F, 0.12F, 2.2F, 0.85F, 1.35F, 1.1F, CYAN, 9, 0);
            case 2 -> slash(e, 1.0F, 0.12F, 2.2F, 0.85F, 1.35F, 1.1F, CYAN, 9, 0);
            case 3 -> slash(e, -0.72F, -0.7F, 2.4F, 0.9F, 1.4F, 1.25F, CYAN, 9, 0);
            case 4 -> {
                slash(e, 0.3F, 1.0F, 2.7F, 0.95F, 1.5F, 1.0F, GOLD, 11, 0);
                groundRing(e.getX(), e.getY(), e.getZ(), 2.6F, GOLD, 12, 0, 0.9F);
                ghost(e, 1, GOLD, 9, 1);
                for (int i = 0; i < 6; i++) {
                    float a = RNG.nextFloat() * 6.2832F;
                    star(e.getX(), e.getY() + 0.2, e.getZ(), Mth.cos(a) * 1.4F, 0.4F + RNG.nextFloat() * 0.8F, Mth.sin(a) * 1.4F, 0.2F, GOLD, 12, 0);
                }
            }
            case ClientAnims.THRUST -> {
                Vec3 f = forward(e.getYRot());
                double y = e.getY() + e.getBbHeight() * 0.62;
                streak(e.getX() + f.x * 0.4, y, e.getZ() + f.z * 0.4, e.getX() + f.x * 3.6, y, e.getZ() + f.z * 3.6, 0.34F, WHITE, 8, 0);
                streak(e.getX() + f.x * 0.4, y, e.getZ() + f.z * 0.4, e.getX() + f.x * 3.2, y + 0.03, e.getZ() + f.z * 3.2, 0.7F, CYAN, 9, 0);
                groundRing(e.getX(), e.getY(), e.getZ(), 2.0F, CYAN, 9, 0, 0.8F);
                ghost(e, 1, CYAN, 10, 0);
                ghost(e, 1, CYAN, 10, 2);
                ghost(e, 1, WHITE, 9, 4);
                if (self) {
                    ScreenFx.speedLines(CYAN, 360);
                }
            }
            case ClientAnims.HEAVY -> {
                int w = KenseiConfig.HEAVY_WINDUP.get();
                circle(e, 2.5F, CRIMSON, w + 14, 9.0F);
                orbit(e, 1.25F, 7, ORANGE, w + 2, 0);
                groundRing(e.getX(), e.getY(), e.getZ(), 1.6F, ORANGE, w, 0, 0.6F);
                // el tajo grande cae al terminar la preparación
                slash(e, 0.0F, -1.0F, 3.3F, 1.0F, 1.7F, 1.55F, CRIMSON, 12, w);
                slash(e, 0.0F, -1.0F, 2.9F, 0.9F, 1.85F, 1.55F, WHITE, 11, w + 1);
                ghost(e, 1, CRIMSON, 10, w + 1);
                ghost(e, 1, ORANGE, 9, w + 3);
                groundRing(e.getX(), e.getY(), e.getZ(), 5.2F, CRIMSON, 14, w, 1.0F);
                groundRing(e.getX(), e.getY(), e.getZ(), 3.2F, WHITE, 10, w + 1, 0.8F);
                if (self) {
                    ScreenFx.speedLines(CRIMSON, 520);
                }
            }
            case ClientAnims.DODGE -> {
                double yaw = Math.toRadians(arg);
                double dx = -Math.sin(yaw);
                double dz = Math.cos(yaw);
                groundRing(e.getX(), e.getY(), e.getZ(), 2.3F, CYAN, 10, 0, 0.9F);
                for (int g = 0; g < 4; g++) {
                    ghost(e, 0, g == 3 ? WHITE : CYAN, 11, g * 2);
                }
                for (int i = 0; i < 3; i++) {
                    double sy = e.getY() + 0.35 + i * 0.5;
                    double side = (i - 1) * 0.28;
                    streak(e.getX() - dz * side, sy, e.getZ() + dx * side, e.getX() - dx * 4.2 - dz * side, sy, e.getZ() - dz * 4.2 + dx * side, 0.3F, i == 1 ? WHITE : CYAN, 9, 0);
                }
                for (int i = 0; i < 5; i++) {
                    star(e.getX() + (RNG.nextFloat() - 0.5) * 0.8, e.getY() + 0.2 + RNG.nextFloat() * 1.4, e.getZ() + (RNG.nextFloat() - 0.5) * 0.8,
                            (float) (-dx * 1.6), 0.2F, (float) (-dz * 1.6), 0.18F, CYAN, 10, 0);
                }
                if (self) {
                    ScreenFx.speedLines(WHITE, 300);
                }
            }
            default -> {
            }
        }
    }

    /** Algo pasó en el servidor (golpe, parry, esquive perfecto, bloqueo). */
    public static void onServerFx(FxPacket m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        boolean fxOn = KenseiConfig.VFX_ENABLED.get();
        int myId = mc.player == null ? -2 : mc.player.getId();
        boolean mine = m.attackerId == myId || m.victimId == myId;
        switch (m.type) {
            case FxPacket.HIT -> {
                int step = m.extra & 63;
                boolean kill = (m.extra & FxPacket.KILL_FLAG) != 0;
                int col = step == 5 ? CRIMSON : step == 4 ? GOLD : CYAN;
                float big = step == 5 ? 1.7F : step == 4 ? 1.25F : step == 7 ? 1.0F : 0.72F;
                if (fxOn) {
                    flash(m.x, m.y, m.z, 1.1F * big, col, 6);
                    billRing(m.x, m.y, m.z, 1.5F * big, WHITE, 8, 0.9F);
                    billRing(m.x, m.y, m.z, 2.3F * big, col, 11, 0.7F);
                    burst(m.x, m.y, m.z, Math.round(5 * big) + 2, 3.2F * big, 0.22F * big, col, 8);
                    if (step == 5 || step == 4) {
                        groundRing(m.x, m.y - 0.8, m.z, 3.4F * big, col, 13, 0, 0.9F);
                    }
                    if (kill) {
                        // remate: destello grande, onda doble y lluvia de estrellas
                        flash(m.x, m.y, m.z, 2.4F, WHITE, 9);
                        billRing(m.x, m.y, m.z, 4.2F, col, 14, 1.0F);
                        billRing(m.x, m.y, m.z, 2.8F, WHITE, 11, 1.0F);
                        groundRing(m.x, m.y - 0.8, m.z, 4.0F, col, 14, 0, 0.9F);
                        burst(m.x, m.y, m.z, 18, 4.4F, 0.3F, col, 11);
                    }
                }
                if (mine && KenseiConfig.DAMAGE_NUMBERS.get() && m.value > 0) {
                    boolean taken = m.victimId == myId;
                    DamageNumbers.add(m.x, m.y, m.z, m.value, taken ? 0xFF5050 : step == 5 ? 0xFF4A4A : step == 4 ? 0xFFD040 : 0xFFFFFF,
                            taken ? 1.0F : (step == 5 ? 2.0F : step == 4 ? 1.6F : 1.15F));
                }
                if (m.attackerId == myId) {
                    if (KenseiConfig.DAMAGE_NUMBERS.get()) {
                        DamageNumbers.comboHit(m.value);
                    }
                    if (KenseiConfig.SCREEN_FX.get()) {
                        ScreenFx.hitMarker(kill ? CRIMSON : step == 5 ? CRIMSON : step == 4 ? GOLD : WHITE, kill ? 1.5F : step >= 4 ? 1.2F : 1.0F);
                        if (kill) {
                            ScreenFx.flash(0xFFFFFF, 50, 160);
                        }
                    }
                    if (KenseiConfig.DAMAGE_NUMBERS.get()) {
                        if (step == 5) {
                            DamageNumbers.banner("kensei.fx.critical", 0xFF5A4A);
                        } else if (step == 4) {
                            DamageNumbers.banner("kensei.fx.finisher", 0xFFD040);
                        }
                    }
                    if (KenseiConfig.SCREEN_FX.get() && (step == 4 || step == 5)) {
                        ScreenFx.flash(step == 5 ? 0xFFB0A0 : 0xFFF0C0, step == 5 ? 90 : 55, 220);
                    }
                }
            }
            case FxPacket.PARRY -> {
                if (fxOn) {
                    flash(m.x, m.y, m.z, 1.5F, WHITE, 8);
                    billRing(m.x, m.y, m.z, 2.6F, GOLD, 12, 0.95F);
                    billRing(m.x, m.y, m.z, 1.5F, WHITE, 9, 0.9F);
                    burst(m.x, m.y, m.z, 14, 4.2F, 0.24F, GOLD, 10);
                    groundRing(m.x, m.y - 1.2, m.z, 3.2F, GOLD, 12, 0, 0.8F);
                    // el atacante queda aturdido: estrellitas girando sobre su cabeza mientras dura el aturdimiento
                    Entity stunned = m.attackerId >= 0 ? mc.level.getEntity(m.attackerId) : null;
                    int stun = KenseiConfig.STUN_TICKS.get();
                    if (stunned != null && stunned.isAlive() && stun > 0) {
                        orbit(stunned, Math.max(0.45F, stunned.getBbWidth() * 0.7F), 4, GOLD, stun, 0, -(stunned.getBbHeight() + 0.25F));
                    }
                }
                if (m.victimId == myId && KenseiConfig.DAMAGE_NUMBERS.get()) {
                    DamageNumbers.banner("kensei.fx.perfect_parry", 0xFFE070);
                }
                if (m.victimId == myId && KenseiConfig.SCREEN_FX.get()) {
                    ScreenFx.speedLines(GOLD, 260);
                }
            }
            case FxPacket.PERFECT_DODGE -> {
                Entity v = mc.level.getEntity(m.victimId);
                if (fxOn && v != null) {
                    circle(v, 1.9F, CYAN, 16, -14.0F);
                    orbit(v, 1.0F, 6, CYAN, 16, 0);
                    billRing(m.x, m.y, m.z, 2.4F, CYAN, 11, 0.9F);
                    burst(m.x, m.y, m.z, 10, 3.0F, 0.22F, CYAN, 10);
                }
                if (m.victimId == myId && KenseiConfig.DAMAGE_NUMBERS.get()) {
                    DamageNumbers.banner("kensei.fx.perfect_dodge", 0x7FE4FF);
                }
            }
            case FxPacket.BLOCK -> {
                if (fxOn) {
                    flash(m.x, m.y, m.z, 0.8F, 0xBFE8FF, 5);
                    billRing(m.x, m.y, m.z, 1.3F, 0xBFE8FF, 8, 0.8F);
                    burst(m.x, m.y, m.z, 5, 2.6F, 0.17F, 0xBFE8FF, 7);
                }
                if (m.victimId == myId && KenseiConfig.DAMAGE_NUMBERS.get() && m.value > 0) {
                    DamageNumbers.add(m.x, m.y, m.z, m.value, 0x9FD6FF, 1.0F);
                }
            }
            case FxPacket.GUARD_BREAK -> {
                if (fxOn) {
                    flash(m.x, m.y, m.z, 1.6F, CRIMSON, 8);
                    billRing(m.x, m.y, m.z, 2.8F, CRIMSON, 11, 1.0F);
                    burst(m.x, m.y, m.z, 12, 3.8F, 0.24F, CRIMSON, 9);
                }
                if (mine && KenseiConfig.DAMAGE_NUMBERS.get()) {
                    DamageNumbers.banner("kensei.fx.guard_break", 0xFF6A5A);
                }
            }
            case FxPacket.DAMAGE_TAKEN -> {
                if (mine && KenseiConfig.DAMAGE_NUMBERS.get() && m.value > 0) {
                    DamageNumbers.add(m.x, m.y, m.z, m.value, 0xFF5050, 1.0F);
                }
                if (m.victimId == myId && KenseiConfig.SCREEN_FX.get()) {
                    ScreenFx.flash(0xFF3030, 40, 200);
                }
            }
            case FxPacket.KO -> {
                if (fxOn) {
                    flash(m.x, m.y, m.z, 3.4F, WHITE, 12);
                    billRing(m.x, m.y, m.z, 6.0F, CRIMSON, 18, 1.0F);
                    billRing(m.x, m.y, m.z, 3.6F, WHITE, 12, 1.0F);
                    burst(m.x, m.y, m.z, 30, 6.0F, 0.32F, CRIMSON, 16);
                    burst(m.x, m.y, m.z, 16, 4.0F, 0.22F, WHITE, 12);
                    groundRing(m.x, m.y - 1.2, m.z, 7.0F, CRIMSON, 18, 0, 1.0F);
                }
                if (KenseiConfig.SCREEN_FX.get()) {
                    ScreenFx.flash(0xFFFFFF, 70, 220);
                    ScreenFx.speedLines(CRIMSON, 420);
                }
            }
            default -> {
            }
        }
    }

    private static Vec3 forward(float yawDeg) {
        double yaw = Math.toRadians(yawDeg);
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    // ------------------------------------------------------------------ dibujar

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LIST.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            LIST.clear();
            return;
        }
        float pt = e.getPartialTick();
        float t0 = level.getGameTime() + pt;
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = e.getPoseStack();
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        Vector3f left = new Vector3f(cam.getLeftVector());
        Vector3f up = new Vector3f(cam.getUpVector());
        Vector3f look = new Vector3f(cam.getLookVector());
        Ctx c = new Ctx(m, cp, left.negate(), up, look, pt, level);

        ghostSkins.clear();
        Iterator<Fx> it = LIST.iterator();
        while (it.hasNext()) {
            Fx f = it.next();
            float age = t0 - f.start - f.delay;
            if (age > f.life) {
                it.remove();
                continue;
            }
            if (age < 0) {
                continue;
            }
            draw(f, age, buf, c);
        }
        buf.endBatch(RenderType.eyes(RING));
        buf.endBatch(RenderType.eyes(CIRCLE));
        buf.endBatch(RenderType.eyes(STAR));
        buf.endBatch(RenderType.eyes(GLOW));
        buf.endBatch(RenderType.eyes(CRESCENT));
        for (ResourceLocation skin : ghostSkins) {
            buf.endBatch(RenderType.eyes(skin));
        }
    }

    /** Intensidad general de la luz aditiva: con 1.0 los efectos que se superponen se queman a blanco. */
    private static final float GLOW_INTENSITY = 0.85F;
    private static final Set<ResourceLocation> ghostSkins = new HashSet<>();
    private static boolean ghostFailed = false;

    /** Lo que hace falta para dibujar un cuadro: la matriz, la cámara y sus ejes. */
    private record Ctx(Matrix4f m, Vec3 cam, Vector3f right, Vector3f up, Vector3f look, float pt, ClientLevel level) {
    }

    private static float easeOut(float t) {
        float u = 1.0F - Mth.clamp(t, 0, 1);
        return 1.0F - u * u * u;
    }

    private static float easeInOut(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int col(int rgb, int shift) {
        return (rgb >> shift) & 0xFF;
    }

    /** Un cuadrado con centro y dos ejes (medio ancho y medio alto), dibujado por las dos caras. s0/s1 = tramo visible a lo largo del eje u (-1 a 1). */
    private static void quad(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float ux, float uy, float uz, float vx, float vy, float vz,
                             float s0, float s1, int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (a <= 1 || s1 <= s0) {
            return;
        }
        // El modo aditivo de RenderType.eyes es ONE+ONE: ignora el alfa y suma el color. Por eso el desvanecido se hace
        // multiplicando el color por el alfa (y las texturas llevan el alfa ya metido en el color: negro = transparente).
        float fade = a / 255.0F * GLOW_INTENSITY;
        int r = Math.round(col(rgb, 16) * fade);
        int g = Math.round(col(rgb, 8) * fade);
        int b = Math.round(col(rgb, 0) * fade);
        a = 255;
        float u0 = (s0 + 1.0F) * 0.5F;
        float u1 = (s1 + 1.0F) * 0.5F;
        float[] sx = {s0, s1, s1, s0};
        float[] st = {-1, -1, 1, 1};
        float[] tu = {u0, u1, u1, u0};
        float[] tv = {1, 1, 0, 0};
        for (int face = 0; face < 2; face++) {
            for (int i = 0; i < 4; i++) {
                int k = face == 0 ? i : 3 - i;
                float px = cx + ux * sx[k] + vx * st[k];
                float py = cy + uy * sx[k] + vy * st[k];
                float pz = cz + uz * sx[k] + vz * st[k];
                vc.vertex(m, px, py, pz).color(r, g, b, a).uv(tu[k], tv[k]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT).normal(0, 1, 0).endVertex();
            }
        }
    }

    /** Un cuadrado que siempre mira a la cámara (con giro). */
    private static void bill(VertexConsumer vc, Ctx c, double wx, double wy, double wz, float size, float spin, int rgb, float alpha) {
        float cx = (float) (wx - c.cam.x);
        float cy = (float) (wy - c.cam.y);
        float cz = (float) (wz - c.cam.z);
        float cs = Mth.cos(spin) * size;
        float sn = Mth.sin(spin) * size;
        float ux = c.right.x * cs + c.up.x * sn;
        float uy = c.right.y * cs + c.up.y * sn;
        float uz = c.right.z * cs + c.up.z * sn;
        float vx = -c.right.x * sn + c.up.x * cs;
        float vy = -c.right.y * sn + c.up.y * cs;
        float vz = -c.right.z * sn + c.up.z * cs;
        quad(vc, c.m, cx, cy, cz, ux, uy, uz, vx, vy, vz, -1, 1, rgb, alpha);
    }

    /** Un cuadrado acostado en el suelo con giro. */
    private static void flat(VertexConsumer vc, Ctx c, double wx, double wy, double wz, float radius, float spin, int rgb, float alpha) {
        float cx = (float) (wx - c.cam.x);
        float cy = (float) (wy - c.cam.y);
        float cz = (float) (wz - c.cam.z);
        float cs = Mth.cos(spin) * radius;
        float sn = Mth.sin(spin) * radius;
        quad(vc, c.m, cx, cy, cz, cs, 0, sn, -sn, 0, cs, -1, 1, rgb, alpha);
    }

    private static double lerpPos(Entity e, int axis, float pt) {
        return switch (axis) {
            case 0 -> Mth.lerp(pt, e.xo, e.getX());
            case 1 -> Mth.lerp(pt, e.yo, e.getY());
            default -> Mth.lerp(pt, e.zo, e.getZ());
        };
    }

    private static void draw(Fx f, float age, MultiBufferSource.BufferSource buf, Ctx c) {
        float t = age / f.life;
        double x = f.x;
        double y = f.y;
        double z = f.z;
        Entity follow = f.entityId >= 0 ? c.level.getEntity(f.entityId) : null;
        if (follow != null) {
            double fy = f.kind == CIRCLE_K ? 0.05 : f.kind == SLASH ? f.p3 : 0.0;
            x = lerpPos(follow, 0, c.pt);
            y = lerpPos(follow, 1, c.pt) + fy;
            z = lerpPos(follow, 2, c.pt);
        }
        switch (f.kind) {
            case RING_FLAT -> {
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(RING));
                float r = f.p1 * (0.18F + 0.82F * easeOut(t));
                float a = f.alpha * (float) Math.pow(1.0F - t, 1.3);
                flat(vc, c, x, y, z, r, 0, f.color, a);
                flat(vc, c, x, y + 0.01, z, r * 0.9F, 0, WHITE, a * 0.55F);
            }
            case RING_BILL -> {
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(RING));
                float r = f.p1 * (0.15F + 0.85F * easeOut(t));
                float a = f.alpha * (float) Math.pow(1.0F - t, 1.2);
                bill(vc, c, x, y, z, r, 0, f.color, a);
            }
            case CIRCLE_K -> {
                float in = easeOut(Math.min(1.0F, age / 5.0F));
                float out = 1.0F - easeInOut((t - 0.7F) / 0.3F);
                float a = in * out;
                float r = f.p1 * (0.45F + 0.55F * in);
                float spin = (float) Math.toRadians(age * f.p2);
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(CIRCLE));
                flat(vc, c, x, y, z, r, spin, f.color, a * 0.95F);
                flat(vc, c, x, y + 0.01, z, r * 0.72F, -spin * 1.4F, WHITE, a * 0.55F);
                VertexConsumer gl = buf.getBuffer(RenderType.eyes(GLOW));
                flat(gl, c, x, y + 0.02, z, r * 0.9F, 0, f.color, a * 0.28F);
            }
            case SLASH -> drawSlash(f, age, t, x, y, z, buf, c);
            case GHOST -> drawGhost(f, age, t, buf, c);
            case STAR_K -> {
                float e = easeOut(t);
                float size = f.p1 * (1.0F - 0.75F * t);
                float a = 1.0F - t * t;
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(STAR));
                bill(vc, c, x + f.x2 * e, y + f.y2 * e - 0.25F * t * t, z + f.z2 * e, size, f.p2 * age * 0.3F, f.color, a);
            }
            case FLASH -> {
                float a = (float) Math.pow(1.0F - t, 1.6);
                float s = f.p1 * (0.55F + 0.7F * easeOut(t));
                bill(buf.getBuffer(RenderType.eyes(GLOW)), c, x, y, z, s * 1.4F, 0, f.color, a * 0.85F);
                bill(buf.getBuffer(RenderType.eyes(STAR)), c, x, y, z, s * 1.5F, age * 0.35F, WHITE, a);
                bill(buf.getBuffer(RenderType.eyes(STAR)), c, x, y, z, s * 1.0F, -age * 0.5F + 0.78F, f.color, a * 0.8F);
            }
            case STREAK -> {
                float k = easeInOut(t * 1.1F);
                float ax = (float) (f.x + (f.x2 - f.x) * k * 0.9F - c.cam.x);
                float ay = (float) (f.y + (f.y2 - f.y) * k * 0.9F - c.cam.y);
                float az = (float) (f.z + (f.z2 - f.z) * k * 0.9F - c.cam.z);
                float bx = (float) (f.x2 - c.cam.x);
                float by = (float) (f.y2 - c.cam.y);
                float bz = (float) (f.z2 - c.cam.z);
                float mx = (ax + bx) * 0.5F;
                float my = (ay + by) * 0.5F;
                float mz = (az + bz) * 0.5F;
                float dx = (bx - ax) * 0.5F;
                float dy = (by - ay) * 0.5F;
                float dz = (bz - az) * 0.5F;
                // perpendicular a la línea y a la mirada, para que se vea siempre de frente
                float px = dy * c.look.z - dz * c.look.y;
                float py = dz * c.look.x - dx * c.look.z;
                float pz = dx * c.look.y - dy * c.look.x;
                float pl = (float) Math.sqrt(px * px + py * py + pz * pz);
                if (pl < 1.0E-4F) {
                    return;
                }
                float w = f.p1 * (1.0F - 0.6F * t) / pl;
                float a = (float) Math.pow(1.0F - t, 1.2);
                quad(buf.getBuffer(RenderType.eyes(GLOW)), c.m, mx, my, mz, dx, dy, dz, px * w, py * w, pz * w, -1, 1, f.color, a);
            }
            case ORBIT -> {
                int n = (int) f.p2;
                float in = Math.min(1.0F, age / 3.0F);
                float out = 1.0F - easeInOut((t - 0.75F) / 0.25F);
                VertexConsumer vc = buf.getBuffer(RenderType.eyes(STAR));
                for (int i = 0; i < n; i++) {
                    float ph = i / (float) n;
                    double ang = age * 0.55 + ph * Math.PI * 2;
                    float h = ((ph + age * 0.05F) % 1.0F);
                    boolean halo = f.p3 < 0;
                    float rad = halo ? f.p1 : f.p1 * (0.75F + 0.25F * Mth.sin(h * 6.2832F));
                    double sx = x + Math.cos(ang) * rad;
                    double sz = z + Math.sin(ang) * rad;
                    double sy = halo ? y - f.p3 + 0.12 * Mth.sin(age * 0.4F + i * 2.0F) : y + 0.15 + h * f.p3;
                    bill(vc, c, sx, sy, sz, 0.2F + 0.08F * Mth.sin(age + i), age * 0.4F + i, f.color, in * out);
                }
            }
            default -> {
            }
        }
    }

    /** La silueta del jugador en el aire: se saca el modelo del propio jugador, se le da la postura del movimiento y se pinta con luz aditiva. */
    private static void drawGhost(Fx f, float age, float t, MultiBufferSource.BufferSource buf, Ctx c) {
        if (ghostFailed) {
            return;
        }
        Entity ent = c.level.getEntity(f.entityId);
        if (!(ent instanceof AbstractClientPlayer pl)) {
            return;
        }
        if (!f.captured) {
            f.captured = true;
            f.x = lerpPos(ent, 0, c.pt);
            f.y = lerpPos(ent, 1, c.pt);
            f.z = lerpPos(ent, 2, c.pt);
            f.yaw = Mth.rotLerp(c.pt, pl.yBodyRotO, pl.yBodyRot);
            f.p1 = pl.walkAnimation.position(c.pt);
            f.p2 = Math.min(1.0F, pl.walkAnimation.speed(c.pt));
        }
        float dx = (float) (f.x - c.cam.x);
        float dy = (float) (f.y - c.cam.y);
        float dz = (float) (f.z - c.cam.z);
        if (dx * dx + dy * dy + dz * dz < 2.2F) {
            return; // pegada a la cámara taparía la pantalla
        }
        float a = 0.4F * (float) Math.pow(1.0F - t, 1.4);
        if (a <= 0.02F) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            EntityRenderer<?> er = mc.getEntityRenderDispatcher().getRenderer(pl);
            if (!(er instanceof PlayerRenderer pr)) {
                return;
            }
            PlayerModel<AbstractClientPlayer> model = pr.getModel();
            model.young = false;
            model.riding = false;
            model.crouching = false;
            model.swimAmount = 0.0F;
            model.attackTime = 0.0F;
            model.setAllVisible(true);
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            model.setupAnim(pl, f.p1, f.p2, pl.tickCount + c.pt, 0.0F, 0.0F);
            if (f.p3 < 0.5F) { // esquive: brazos echados atrás y piernas separadas
                model.rightArm.xRot = 0.55F;
                model.leftArm.xRot = 0.7F;
                model.rightLeg.xRot = -0.75F;
                model.leftLeg.xRot = 0.6F;
            } else { // ataque: el brazo de la katana al frente y zancada
                model.rightArm.xRot = -1.45F;
                model.rightArm.yRot = -0.25F;
                model.leftArm.xRot = -0.6F;
                model.leftArm.yRot = 0.5F;
                model.rightLeg.xRot = -0.8F;
                model.leftLeg.xRot = 0.65F;
            }
            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
            model.rightPants.copyFrom(model.rightLeg);
            model.leftPants.copyFrom(model.leftLeg);
            ResourceLocation skin = pl.getSkinTextureLocation();
            ghostSkins.add(skin);
            VertexConsumer vc = buf.getBuffer(RenderType.eyes(skin));
            PoseStack ps = new PoseStack();
            ps.last().pose().set(c.m);
            ps.translate(dx, dy, dz);
            ps.mulPose(Axis.YP.rotationDegrees(180.0F - f.yaw));
            ps.scale(-1.0F, -1.0F, 1.0F);
            ps.scale(0.9375F, 0.9375F, 0.9375F);
            ps.translate(0.0F, -1.501F, 0.0F);
            float r = col(f.color, 16) / 255.0F;
            float g = col(f.color, 8) / 255.0F;
            float b = col(f.color, 0) / 255.0F;
            model.renderToBuffer(ps, vc, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r * a, g * a, b * a, 1.0F); // aditivo: se desvanece con el color
        } catch (Throwable ex) {
            ghostFailed = true;
            Kensei.LOGGER.warn("Kensei: no se pudieron dibujar las imágenes residuales, se desactivan.", ex);
        }
    }

    private static void drawSlash(Fx f, float age, float t, double x, double y, double z, MultiBufferSource.BufferSource buf, Ctx c) {
        double yawRad = Math.toRadians(f.yaw);
        float fx = (float) -Math.sin(yawRad);
        float fz = (float) Math.cos(yawRad);
        float rx = (float) -Math.cos(yawRad);
        float rz = (float) -Math.sin(yawRad);
        // dirección del tajo (derecha, arriba) y la perpendicular que se inclina hacia la cámara
        float sr = f.x2;
        float su = f.y2;
        float sl = (float) Math.sqrt(sr * sr + su * su);
        sr /= sl;
        su /= sl;
        float pr = -su;
        float pu = sr;
        if (pu * 1.0F + pr * 0.3F < 0) {
            pr = -pr;
            pu = -pu;
        }
        float lean = f.p2;
        float cl = Mth.cos(lean);
        float sn = Mth.sin(lean);
        float L = f.p1;
        // eje largo (u) y eje de abombado (v)
        float ux = (rx * sr) * L;
        float uy = su * L;
        float uz = (rz * sr) * L;
        float H = L * 0.6F;
        float vx = (fx * cl + rx * pr * sn) * H;
        float vy = pu * sn * H;
        float vz = (fz * cl + rz * pr * sn) * H;
        // centro: delante del jugador
        float cx = (float) (x + fx * f.z2 - c.cam.x);
        float cy = (float) (y - c.cam.y);
        float cz = (float) (z + fz * f.z2 - c.cam.z);
        // barrido: la cabeza avanza rápido y la cola la sigue
        float head = -1.0F + 2.0F * easeOut(Math.min(1.0F, age / (f.life * 0.42F)));
        float tail = -1.0F + 2.0F * easeInOut((age - f.life * 0.22F) / (f.life * 0.78F));
        tail = Math.max(-1.0F, Math.max(tail, head - 1.5F));
        float a = 1.0F - easeInOut((t - 0.35F) / 0.65F);
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(CRESCENT));
        quad(vc, c.m, cx, cy, cz, ux * 1.12F, uy * 1.12F, uz * 1.12F, vx * 1.18F, vy * 1.18F, vz * 1.18F, tail, head, f.color, a * 0.85F);
        quad(vc, c.m, cx, cy, cz, ux, uy, uz, vx, vy, vz, tail, head, WHITE, a);
        // destello en la punta que avanza
        if (head < 0.98F && a > 0.2F) {
            float hx = cx + ux * head + vx * 0.35F;
            float hy = cy + uy * head + vy * 0.35F;
            float hz = cz + uz * head + vz * 0.35F;
            bill(buf.getBuffer(RenderType.eyes(STAR)), c, hx + c.cam.x, hy + c.cam.y, hz + c.cam.z, 0.42F, age * 0.6F, WHITE, a);
        }
    }
}

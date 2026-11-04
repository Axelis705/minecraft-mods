package com.mundosdeaxelis.client;

import com.mundosdeaxelis.cinematic.Keyframe;
import com.mundosdeaxelis.cinematic.Subtitle;
import com.mundosdeaxelis.network.StartCinematicPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Lado cliente de las cinemáticas: mueve la cámara por la trayectoria, bloquea controles
 * y lleva el tiempo para las barras negras y los subtítulos.
 * La cámara es una entidad "Marker" que solo existe en el cliente (no se añade al mundo).
 */
public final class CinematicClient {
    private static boolean active = false;
    private static int duration;
    private static int fadeOut;
    private static boolean bars;
    private static List<Keyframe> frames = new ArrayList<>();
    private static List<Subtitle> subtitles = new ArrayList<>();
    private static int tick;
    private static Marker camera;
    private static float savedYaw;
    private static float savedPitch;
    private static float lastYaw;
    private static boolean yawInitialised;

    private CinematicClient() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void start(StartCinematicPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || packet.frames.size() < 2) {
            return;
        }
        if (active) {
            restoreCamera(mc);
        }
        duration = packet.duration;
        fadeOut = packet.fadeOut;
        bars = packet.bars;
        frames = new ArrayList<>(packet.frames);
        subtitles = new ArrayList<>(packet.subtitles);
        tick = 0;
        yawInitialised = false;
        savedYaw = player.getYRot();
        savedPitch = player.getXRot();

        camera = new Marker(EntityType.MARKER, mc.level);
        Keyframe first = frames.get(0);
        camera.setPos(first.pos().x, first.pos().y, first.pos().z);
        mc.setCameraEntity(camera);
        active = true;

        // Arranca desde negro y se aclara.
        FadeOverlay.start(0x000000, 0, 0, 25);
        applyCamera(0.0F);
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (!active) {
            return;
        }
        restoreCamera(mc);
    }

    private static void restoreCamera(Minecraft mc) {
        active = false;
        camera = null;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            LocalPlayer p = mc.player;
            p.setYRot(savedYaw);
            p.yRotO = savedYaw;
            p.setXRot(savedPitch);
            p.xRotO = savedPitch;
            p.yHeadRot = savedYaw;
            p.yHeadRotO = savedYaw;
        } else {
            mc.setCameraEntity(null);
        }
    }

    /** Se llama una vez por tick del cliente. */
    public static void clientTick() {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || camera == null || camera.level() != mc.level) {
            stop();
            return;
        }
        tick++;
        if (tick == duration - fadeOut) {
            // Fundido a negro: justo cuando termina, se devuelve la cámara al jugador.
            FadeOverlay.start(0x000000, fadeOut, 6, 25);
        }
        if (tick >= duration) {
            stop();
            return;
        }
        // El jugador no puede girar durante la cinemática.
        player.setYRot(savedYaw);
        player.yRotO = savedYaw;
        player.setXRot(savedPitch);
        player.xRotO = savedPitch;
        player.setSprinting(false);
        // El movimiento es libre. Si por alguna razón el juego no deja pasar las teclas
        // mientras la cámara es otra, se mueve al jugador directamente (mismo ritmo que caminar).
        if (vanillaSeen > 0) {
            vanillaSeen--;
        } else {
            manualMove(mc, player);
        }
    }

    private static int vanillaSeen = 0;

    /** Lo llama el evento de movimiento cuando el juego sí está procesando las teclas. */
    public static void vanillaMoveSeen() {
        vanillaSeen = 6;
    }

    private static void manualMove(Minecraft mc, LocalPlayer player) {
        var o = mc.options;
        float f = (o.keyUp.isDown() ? 1.0F : 0.0F) - (o.keyDown.isDown() ? 1.0F : 0.0F);
        float s = (o.keyLeft.isDown() ? 1.0F : 0.0F) - (o.keyRight.isDown() ? 1.0F : 0.0F);
        Vec3 d = player.getDeltaMovement();
        if (f != 0.0F || s != 0.0F) {
            double len = Math.sqrt(f * f + s * s);
            f /= len;
            s /= len;
            double yaw = Math.toRadians(player.getYRot());
            double speed = player.onGround() ? 0.2158D : 0.12D;
            double vx = (-f * Math.sin(yaw) + s * Math.cos(yaw)) * speed;
            double vz = (f * Math.cos(yaw) + s * Math.sin(yaw)) * speed;
            d = new Vec3(vx, d.y, vz);
            player.setDeltaMovement(d);
        }
        if (o.keyJump.isDown() && player.onGround()) {
            player.setDeltaMovement(d.x, 0.42D, d.z);
        }
    }

    /** Se llama justo antes de dibujar cada fotograma: así la cámara se mueve fluida aunque el juego vaya a 144 fps. */
    public static void renderTick(float partialTick) {
        if (!active || camera == null) {
            return;
        }
        applyCamera(partialTick);
    }

    private static void applyCamera(float partialTick) {
        double t = tick + partialTick;
        Vec3 pos = sample(t, true);
        Vec3 target = sample(t, false);

        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * 57.29577951308232D) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * 57.29577951308232D));
        pitch = Mth.clamp(pitch, -89.0F, 89.0F);

        // Evita saltos de 360 grados en el giro.
        if (!yawInitialised) {
            lastYaw = yaw;
            yawInitialised = true;
        } else {
            yaw = lastYaw + Mth.wrapDegrees(yaw - lastYaw);
            lastYaw = yaw;
        }

        camera.setPos(pos.x, pos.y, pos.z);
        camera.xo = pos.x;
        camera.yo = pos.y;
        camera.zo = pos.z;
        camera.xOld = pos.x;
        camera.yOld = pos.y;
        camera.zOld = pos.z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
    }

    /** Posición de la cámara (o del punto al que mira) en el instante t, con curva Catmull-Rom. */
    private static Vec3 sample(double t, boolean position) {
        int n = frames.size();
        if (t <= frames.get(0).tick()) {
            return pick(frames.get(0), position);
        }
        if (t >= frames.get(n - 1).tick()) {
            return pick(frames.get(n - 1), position);
        }
        int i = 0;
        while (i < n - 2 && t >= frames.get(i + 1).tick()) {
            i++;
        }
        Keyframe a = frames.get(i);
        Keyframe b = frames.get(i + 1);
        double span = Math.max(1, b.tick() - a.tick());
        double u = (t - a.tick()) / span;

        Vec3 p0 = pick(frames.get(Math.max(i - 1, 0)), position);
        Vec3 p1 = pick(a, position);
        Vec3 p2 = pick(b, position);
        Vec3 p3 = pick(frames.get(Math.min(i + 2, n - 1)), position);
        return new Vec3(
                catmull(p0.x, p1.x, p2.x, p3.x, u),
                catmull(p0.y, p1.y, p2.y, p3.y, u),
                catmull(p0.z, p1.z, p2.z, p3.z, u));
    }

    private static Vec3 pick(Keyframe k, boolean position) {
        return position ? k.pos() : k.target();
    }

    private static double catmull(double p0, double p1, double p2, double p3, double u) {
        double u2 = u * u;
        double u3 = u2 * u;
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * u
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * u2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * u3);
    }

    // ------------------------------------------------------------------ datos para las capas de interfaz

    /** 0.0 a 1.0: cuánto han "bajado" las barras negras. */
    public static float barAmount(float partialTick) {
        if (!active || !bars) {
            return 0.0F;
        }
        float x = Mth.clamp((tick + partialTick) / 20.0F, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    public static List<Subtitle> subtitles() {
        return subtitles;
    }

    public static float time(float partialTick) {
        return tick + partialTick;
    }
}

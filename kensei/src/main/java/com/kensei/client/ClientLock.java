package com.kensei.client;

import com.kensei.combat.KatanaUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fijar objetivo: con la tecla Z el jugador se queda mirando al enemigo (el más cercano al centro de la vista), así se puede rodearlo
 * esquivando de lado sin perderlo. Solo mueve la mirada del jugador local; el servidor ve la rotación normal. Se suelta solo si el
 * enemigo muere, se aleja o se guarda la katana.
 */
public final class ClientLock {
    private static LivingEntity target = null;
    private static int lostSight = 0;

    /** Mirada suavizada que se actualiza en cada cuadro de pantalla (no solo 20 veces por segundo): así el giro es limpio. */
    private static float smoothYaw, smoothPitch;
    private static boolean engaged = false;
    private static long lastNanos = 0;
    private static long acquiredMs = 0;

    /** Matrices del último cuadro dibujado, para saber en qué punto de la pantalla está el enemigo. */
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJ = new Matrix4f();
    private static Vec3 camPos = Vec3.ZERO;
    static boolean haveMatrices = false;
    /** Gesto lateral (como en Dark Souls): lo que se movió el ratón de lado mientras hay objetivo, y cuándo se puede volver a cambiar. */
    private static float flick = 0.0F;
    private static long flickReadyMs = 0;
    private static final float FLICK_DEGREES = 11.0F;
    private static double markerY = Double.NaN;
    private static long markerNanos = 0;

    private static final ResourceLocation RING = new ResourceLocation("kensei", "textures/gui/lock_ring.png");
    private static final ResourceLocation CHEVRON = new ResourceLocation("kensei", "textures/gui/lock_chevron.png");

    private static final double MAX_DIST = 20.0;
    private static final double MAX_ANGLE = 70.0;

    private ClientLock() {
    }

    public static LivingEntity target() {
        return target;
    }

    public static void clear() {
        target = null;
        lostSight = 0;
        engaged = false;
        lastNanos = 0;
        flick = 0.0F;
    }

    /** Empieza a seguir a un enemigo: la mirada suavizada parte de donde está mirando el jugador (sin tirones) y la marca "entra" con un pequeño efecto. */
    private static void acquire(LocalPlayer p, LivingEntity t) {
        target = t;
        lostSight = 0;
        if (t != null) {
            smoothYaw = p.getYRot();
            smoothPitch = p.getXRot();
            engaged = true;
            lastNanos = 0;
            acquiredMs = System.currentTimeMillis();
            markerY = Double.NaN;
        } else {
            engaged = false;
        }
    }

    /** Fija a este enemigo (lo usa el duelo para fijar al rival al empezar cada ronda). */
    public static void lockOn(LocalPlayer p, LivingEntity t) {
        acquire(p, t);
    }

    public static void toggle(LocalPlayer p) {
        if (target != null) {
            clear();
            return;
        }
        acquire(p, pick(p));
    }

    /** Enemigos a los que se puede fijar: vivos, a la vista y dentro del cono delante del jugador (maxAngle) y de la distancia dada. */
    private static List<LivingEntity> candidates(LocalPlayer p, double maxAngle, double maxDist) {
        Vec3 look = p.getLookAngle();
        List<LivingEntity> list = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(maxDist),
                e -> e != p && e.isAlive() && !e.isSpectator() && !e.isInvisible() && !(e instanceof ArmorStand));
        List<LivingEntity> out = new java.util.ArrayList<>();
        for (LivingEntity e : list) {
            if (e instanceof TamableAnimal ta && ta.isOwnedBy(p)) {
                continue;
            }
            if (ClientDuel.relation(e) == 1) {
                continue; // los aliados del duelo no se pueden fijar
            }
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(p.getEyePosition());
            double dist = to.length();
            if (dist > maxDist || dist < 0.01) {
                continue;
            }
            double angle = Math.toDegrees(Math.acos(Mth.clamp(to.normalize().dot(look), -1.0, 1.0)));
            if (angle > maxAngle || !p.hasLineOfSight(e)) {
                continue;
            }
            out.add(e);
        }
        return out;
    }

    /**
     * Qué tan buen candidato es un enemigo (menos es mejor): el más centrado en la vista y cercano, prefiriendo los monstruos, evitando
     * los bichitos muy chicos (por ejemplo slimes pequeños) y los animales o aldeanos pacíficos.
     */
    private static double score(LocalPlayer p, LivingEntity e) {
        Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(p.getEyePosition());
        double angle = Math.toDegrees(Math.acos(Mth.clamp(to.normalize().dot(p.getLookAngle()), -1.0, 1.0)));
        double sc = angle * 1.6 + to.length() * 1.2;
        if (e instanceof net.minecraft.world.entity.monster.Enemy) {
            sc -= 14.0;
        } else if (e instanceof net.minecraft.world.entity.animal.Animal || e instanceof net.minecraft.world.entity.ambient.AmbientCreature
                || e instanceof net.minecraft.world.entity.npc.AbstractVillager || e instanceof net.minecraft.world.entity.animal.WaterAnimal) {
            sc += 16.0;
        }
        if (e.getBbHeight() < 0.7F && e.getBbWidth() < 0.7F) {
            sc += 14.0;
        }
        return sc;
    }

    /** El enemigo vivo más centrado en la vista (y cercano) dentro de un cono delante del jugador. */
    private static LivingEntity pick(LocalPlayer p) {
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : candidates(p, MAX_ANGLE, MAX_DIST)) {
            double sc = score(p, e);
            if (sc < bestScore) {
                bestScore = sc;
                best = e;
            }
        }
        return best;
    }

    /** Pasa al siguiente enemigo, de izquierda a derecha en la pantalla (da la vuelta al llegar al último). Sin objetivo, fija uno. */
    public static void cycle(LocalPlayer p) {
        if (target == null) {
            acquire(p, pick(p));
            return;
        }
        List<LivingEntity> list = candidates(p, 180.0, MAX_DIST);
        if (list.size() < 2) {
            return;
        }
        double yaw = p.getYRot();
        list.sort(java.util.Comparator.comparingDouble(e -> Mth.wrapDegrees(
                (float) Math.toDegrees(Math.atan2(-(e.getX() - p.getX()), e.getZ() - p.getZ())) - (float) yaw) * -1.0));
        int i = list.indexOf(target);
        LivingEntity next = list.get((i + 1) % list.size());
        target = next;
        lostSight = 0;
        acquiredMs = System.currentTimeMillis();
    }


    private static double yawTo(LocalPlayer p, LivingEntity e) {
        return Math.toDegrees(Math.atan2(-(e.getX() - p.getX()), e.getZ() - p.getZ()));
    }

    /**
     * Cambia el objetivo al enemigo más cercano hacia la derecha o la izquierda del actual (medido desde el jugador), como mover el
     * stick derecho en Dark Souls. Devuelve false si no hay ninguno de ese lado.
     */
    public static boolean switchSide(LocalPlayer p, boolean right) {
        if (target == null) {
            return false;
        }
        double ty = yawTo(p, target);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : candidates(p, 115.0, ClientDuel.active() ? 40.0 : MAX_DIST)) {
            if (e == target) {
                continue;
            }
            double diff = Mth.wrapDegrees((float) (yawTo(p, e) - ty)); // más a la derecha = yaw mayor
            if (right ? diff < 4.0 : diff > -4.0) {
                continue;
            }
            double sc = Math.abs(diff) + p.distanceTo(e) * 0.6;
            if (sc < bestScore) {
                bestScore = sc;
                best = e;
            }
        }
        if (best == null) {
            return false;
        }
        target = best;
        lostSight = 0;
        acquiredMs = System.currentTimeMillis();
        markerY = Double.NaN;
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), right ? 1.7F : 1.5F, 0.5F));
        return true;
    }

    /** Cada tick del cliente (antes de que el juego mande la posición): gira suavemente la mirada hacia el objetivo. */
    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || target == null) {
            return;
        }
        if (!KatanaUtil.holdsKatana(p)) {
            clear();
            return;
        }
        if (!target.isAlive() || target.isRemoved() || target.level() != p.level() || p.distanceTo(target) > (ClientDuel.active() ? 60.0 : MAX_DIST + 6.0)) {
            // si murió, se pasa solo al enemigo más cercano a la vista para no perder el ritmo del combate
            boolean died = !target.isAlive() || target.isRemoved();
            clear();
            if (died) {
                LivingEntity next = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : candidates(p, 180.0, ClientDuel.active() ? 40.0 : 10.0)) {
                    double d = e.distanceToSqr(p);
                    if (d < best) {
                        best = d;
                        next = e;
                    }
                }
                acquire(p, next);
            }
            return;
        }
        if (!p.hasLineOfSight(target)) {
            if (++lostSight > 50) {
                clear();
                return;
            }
        } else {
            lostSight = 0;
        }
    }

    private static float ease(float u) {
        u = Mth.clamp(u, 0.0F, 1.0F);
        return u * u * (3.0F - 2.0F * u);
    }

    /**
     * En cada cuadro de pantalla (se llama antes de dibujar): gira la mirada del jugador hacia el enemigo con un movimiento suave que no
     * depende de los 20 ticks por segundo. El ratón no gira la mirada mientras hay un objetivo fijado (se suelta con el clic central).
     */
    public static void frame(Minecraft mc, float partial) {
        LocalPlayer p = mc.player;
        if (p == null || target == null || !engaged || mc.screen != null || mc.isPaused() || !target.isAlive()) {
            lastNanos = 0;
            return;
        }
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016F : Math.min(0.1F, (now - lastNanos) / 1.0E9F);
        lastNanos = now;
        // lo que el ratón giró la mirada desde el cuadro anterior (el juego lo suma a la rotación del jugador justo antes de dibujar)
        float raw = Mth.wrapDegrees(p.getYRot() - smoothYaw);
        if (Math.abs(raw) < 70.0F) {
            flick = flick * (float) Math.exp(-dt * 5.0F) + raw;
        } else {
            flick = 0.0F;
        }
        if (Math.abs(flick) > FLICK_DEGREES) {
            long nowMs = System.currentTimeMillis();
            if (nowMs >= flickReadyMs && switchSide(p, flick > 0)) {
                flickReadyMs = nowMs + 380;
            }
            flick = 0.0F;
        }
        Vec3 tp = target.getPosition(partial);
        Vec3 eye = p.getEyePosition(partial);
        double dx = tp.x - eye.x;
        double dz = tp.z - eye.z;
        double dy = (tp.y + target.getBbHeight() * 0.5) - eye.y;
        double flat = Math.sqrt(dx * dx + dz * dz);
        // la altura de la mirada casi no sigue al enemigo (así no se mueve la cámara cuando salta o rebota): un ángulo base un poco hacia abajo
        // y solo una parte de la diferencia de altura
        float elev = (float) -Math.toDegrees(Math.atan2(dy, Math.max(flat, 0.5)));
        float wantPitch = 7.0F + 0.35F * Mth.clamp(elev, -30.0F, 30.0F);
        float ky = 1.0F - (float) Math.exp(-dt * 9.0F);
        float kp = 1.0F - (float) Math.exp(-dt * 4.0F);
        if (flat > 1.2) { // casi pegado al jugador: se mantiene la dirección para no dar vueltas
            float wantYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float step = Mth.wrapDegrees(wantYaw - smoothYaw) * ky;
            float maxStep = 600.0F * dt;
            smoothYaw += Mth.clamp(step, -maxStep, maxStep);
        }
        smoothPitch += (wantPitch - smoothPitch) * kp;
        p.setYRot(smoothYaw);
        p.yRotO = smoothYaw;
        p.setXRot(smoothPitch);
        p.xRotO = smoothPitch;
    }

    /** La cámara usa la mirada suavizada (el ratón ya se sumó a la del jugador en este cuadro; así no se nota ningún tirón). */
    public static void overrideCamera(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (target == null || !engaged || mc.screen != null || mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) {
            return;
        }
        e.setYaw(smoothYaw);
        e.setPitch(smoothPitch);
    }

    /** Guarda las matrices de la cámara de este cuadro para poder dibujar la marca sobre el enemigo. */
    public static void captureView(com.mojang.blaze3d.vertex.PoseStack ps, Matrix4f projection, Vec3 cameraPos) {
        VIEW.set(ps.last().pose());
        PROJ.set(projection);
        camPos = cameraPos;
        haveMatrices = true;
    }

    /** Convierte un punto del mundo en un punto de la pantalla (coordenadas del interfaz), o null si queda detrás de la cámara. */
    static float[] project(Vec3 world, int w, int h) {
        Vec3 rel = world.subtract(camPos);
        Vector4f v = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1.0F);
        VIEW.transform(v);
        PROJ.transform(v);
        if (v.w <= 0.05F) {
            return null;
        }
        return new float[] {(v.x / v.w * 0.5F + 0.5F) * w, (1.0F - (v.y / v.w * 0.5F + 0.5F)) * h};
    }

    /**
     * La marca del enemigo fijado, dibujada en pantalla (siempre nítida y fluida): una flecha roja con borde blanco que flota sobre su
     * cabeza y un aro blanco que gira a la altura del pecho. Entra con un pequeño efecto al fijar.
     */
    public static void renderMarker(GuiGraphics g, int w, int h, float partial) {
        LivingEntity t = target;
        Minecraft mc = Minecraft.getInstance();
        if (t == null || !haveMatrices || mc.player == null || !t.isAlive()) {
            return;
        }
        Vec3 base = t.getPosition(partial);
        double wantY = base.y + t.getBbHeight() * 0.5;
        long nn = System.nanoTime();
        float mdt = markerNanos == 0 ? 0.016F : Math.min(0.1F, (nn - markerNanos) / 1.0E9F);
        markerNanos = nn;
        if (Double.isNaN(markerY) || Math.abs(markerY - wantY) > 3.0) {
            markerY = wantY;
        } else {
            markerY += (wantY - markerY) * (1.0 - Math.exp(-mdt * 10.0)); // la marca no salta con los rebotes del enemigo
        }
        Vec3 centre = new Vec3(base.x, markerY, base.z);
        Vec3 head = new Vec3(base.x, markerY + t.getBbHeight() * 0.5 + 0.3, base.z);
        float[] c = project(centre, w, h);
        float[] hp = project(head, w, h);
        if (c == null || hp == null) {
            return;
        }
        double dist = camPos.distanceTo(centre);
        long ms = System.currentTimeMillis();
        float age = ease((ms - acquiredMs) / 260.0F);
        float intro = 1.0F + (1.0F - age) * 0.9F;
        float alpha = 0.35F + 0.65F * age;
        float spin = (ms % 7000L) / 7000.0F * 360.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        // aro
        int ring = Math.round((float) Mth.clamp(130.0 / Math.max(1.0, dist), 20.0, 40.0) * intro);
        g.pose().pushPose();
        g.pose().translate(c[0], c[1], 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(spin));
        g.blit(RING, -ring / 2, -ring / 2, ring, ring, 0.0F, 0.0F, 64, 64, 64, 64);
        g.pose().popPose();
        // flecha sobre la cabeza
        int cs = Math.round((float) Mth.clamp(150.0 / Math.max(1.0, dist), 16.0, 30.0));
        float bob = 2.5F * (float) Math.sin(ms / 200.0);
        g.pose().pushPose();
        g.pose().translate(hp[0], hp[1] - cs * 0.55F + bob - (intro - 1.0F) * 14.0F, 0.0F);
        g.blit(CHEVRON, -cs / 2, -cs / 2, cs, cs, 0.0F, 0.0F, 32, 32, 32, 32);
        g.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}

package com.kensei.client;

import com.kensei.Kensei;
import com.kensei.KenseiConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

/** Efectos de pantalla: sacudida de cámara al golpear/recibir y la inclinación del cuerpo (esquive, crítico) en tercera persona. */
@Mod.EventBusSubscriber(modid = Kensei.MODID, value = Dist.CLIENT)
public final class ClientEffects {
    private static int shakeTicks = 0;
    private static int shakeTotal = 1;
    private static float shakeStrength = 0;

    /** La rotación aplicada al jugador que se está dibujando (para deshacerla al terminar) o null. */
    private static Quaternionf applied = null;

    private static final float PIVOT_Y = 0.9F;

    private ClientEffects() {
    }

    public static void shake(float strength, int ticks) {
        if (!KenseiConfig.CAMERA_SHAKE.get()) {
            return;
        }
        ClientCamera.punch(strength);
        if (strength >= shakeStrength * (shakeTicks / (float) shakeTotal)) {
            shakeStrength = strength;
            shakeTicks = ticks;
            shakeTotal = Math.max(1, ticks);
        }
    }

    public static void tick() {
        if (shakeTicks > 0) {
            shakeTicks--;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (shakeTicks <= 0) {
            return;
        }
        float life = Mth.clamp((shakeTicks - (float) e.getPartialTick()) / shakeTotal, 0.0F, 1.0F);
        float amp = shakeStrength * life;
        Minecraft mc = Minecraft.getInstance();
        float t = (mc.level == null ? 0 : mc.level.getGameTime()) + (float) e.getPartialTick();
        e.setRoll(e.getRoll() + Mth.sin(t * 2.3F) * amp * 1.4F);
        e.setPitch(e.getPitch() + Mth.sin(t * 3.7F + 1.0F) * amp * 0.9F);
        e.setYaw(e.getYaw() + Mth.cos(t * 2.9F) * amp * 0.7F);
    }

    // ------------------------------------------------------------------ inclinación del cuerpo

    /** Lo que se inclina el jugador ahora mismo (grados; positivo = hacia delante del movimiento) y hacia dónde (vector horizontal). */
    private static final java.util.Map<Player, float[]> SPRINT = new java.util.WeakHashMap<>();

    /** Cuánto se inclina al correr con katana (0 a 1), con entrada y salida suaves. */
    private static float sprintBlend(Player p) {
        float[] st = SPRINT.computeIfAbsent(p, k -> new float[] {0.0F, 0.0F});
        long now = System.nanoTime();
        float dt = st[1] == 0.0F ? 0.016F : Math.min(0.1F, (float) ((now - (long) st[1]) / 1.0E9));
        st[1] = now;
        boolean on = p.isSprinting() && com.kensei.combat.KatanaUtil.holdsKatana(p) && !p.isUsingItem();
        st[0] = Mth.clamp(st[0] + (on ? 1.0F : -1.0F) * dt * 6.0F, 0.0F, 1.0F);
        return st[0];
    }

    private static float[] lean(Player p, float partial) {
        ClientAnims.Anim a = ClientAnims.get(p, 40);
        if (a == null) {
            float sb = sprintBlend(p);
            if (sb < 0.03F) {
                return null;
            }
            return new float[] {13.0F * sb, Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot)};
        }
        float t = ClientAnims.age(a, partial);
        float angle;
        float yawDeg;
        if (a.move == ClientAnims.DODGE) {
            angle = 38.0F * Mth.sin(Mth.clamp(t / 9.0F, 0.0F, 1.0F) * (float) Math.PI);
            yawDeg = a.arg;
        } else if (a.move == ClientAnims.HEAVY) {
            float w = KenseiConfig.HEAVY_WINDUP.get();
            if (t < w) {
                float u = t / w;
                angle = -14.0F * u * u;
            } else if (t < w + 5.0F) {
                float u = (t - w) / 5.0F;
                angle = Mth.lerp(u, -14.0F, 26.0F);
            } else {
                float u = Mth.clamp((t - w - 5.0F) / 12.0F, 0.0F, 1.0F);
                angle = 26.0F * (1.0F - u);
            }
            yawDeg = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        } else if (a.move == ClientAnims.THRUST) {
            angle = 24.0F * Mth.sin(Mth.clamp(t / 9.0F, 0.0F, 1.0F) * (float) Math.PI);
            yawDeg = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        } else if (a.move >= 1 && a.move <= 4) {
            angle = 9.0F * Mth.sin(Mth.clamp(t / 7.0F, 0.0F, 1.0F) * (float) Math.PI);
            yawDeg = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        } else {
            return null;
        }
        if (Math.abs(angle) < 0.5F) {
            return null;
        }
        return new float[] {angle, yawDeg};
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderPlayerEvent.Pre e) {
        applied = null;
        if (!KenseiConfig.BODY_LEAN.get()) {
            return;
        }
        float[] l = lean(e.getEntity(), e.getPartialTick());
        if (l == null) {
            return;
        }
        double yaw = Math.toRadians(l[1]);
        float dx = (float) -Math.sin(yaw);
        float dz = (float) Math.cos(yaw);
        // eje horizontal perpendicular a la dirección: girar sobre él hace que la cabeza vaya hacia la dirección
        Quaternionf q = new Quaternionf().rotationAxis((float) Math.toRadians(l[0]), dz, 0.0F, -dx);
        PoseStack ps = e.getPoseStack();
        ps.translate(0.0F, PIVOT_Y, 0.0F);
        ps.mulPose(q);
        ps.translate(0.0F, -PIVOT_Y, 0.0F);
        applied = q;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPost(RenderPlayerEvent.Post e) {
        if (applied == null) {
            return;
        }
        Quaternionf inv = applied.conjugate(new Quaternionf());
        applied = null;
        PoseStack ps = e.getPoseStack();
        ps.translate(0.0F, PIVOT_Y, 0.0F);
        ps.mulPose(inv);
        ps.translate(0.0F, -PIVOT_Y, 0.0F);
    }
}

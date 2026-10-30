package com.kensei.client;

import com.kensei.Kensei;
import com.kensei.KenseiConfig;
import com.kensei.combat.KatanaUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * La cámara de combate (la vista de espaldas de F5, solo con la katana en la mano): más cerca que la normal, corrida sobre el hombro
 * derecho para ver al enemigo en el centro, un poco más alta, y dinámica: se aleja y abre el campo de visión al esquivar, al correr y al
 * hacer el crítico. Todo se mueve suave. No toca la primera persona.
 *
 * El juego no deja mover la cámara desde un evento, así que se usa su método interno de poner la posición. Como los nombres internos
 * cambian entre el modo desarrollo y el juego normal, el método se busca por su forma (tres decimales, sin resultado) y se comprueba
 * en una cámara de prueba. Si no se encuentra, la cámara queda como la del juego y no pasa nada más.
 */
@Mod.EventBusSubscriber(modid = Kensei.MODID, value = Dist.CLIENT)
public final class ClientCamera {
    /** Distancia normal del juego en tercera persona. */
    private static final float VANILLA_DISTANCE = 4.0F;

    private static float side, sidePrev;
    private static float closer, closerPrev;
    private static float up, upPrev;
    private static float fov, fovPrev;
    /** Golpe de zoom al conectar un golpe: el campo de visión se cierra un instante y vuelve. */
    private static float kick, kickPrev;

    private static Method setPos;
    private static boolean resolved;

    private ClientCamera() {
    }

    /** Un golpe conectó con esta fuerza: se cierra un poco el campo de visión (se recupera solo). */
    public static void punch(float strength) {
        kick = Math.min(kick, -Math.min(7.0F, 2.5F + strength * 2.5F));
    }

    /** Busca el método interno que pone la posición de la cámara (x, y, z) y lo comprueba en una cámara de prueba. */
    private static Method resolve() {
        if (resolved) {
            return setPos;
        }
        resolved = true;
        try {
            for (Method m : Camera.class.getDeclaredMethods()) {
                if (m.getParameterCount() != 3 || m.getReturnType() != void.class || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Class<?>[] pt = m.getParameterTypes();
                if (pt[0] != double.class || pt[1] != double.class || pt[2] != double.class) {
                    continue;
                }
                m.setAccessible(true);
                Camera probe = new Camera();
                m.invoke(probe, 1.0, 2.0, 3.0);
                Vec3 v = probe.getPosition();
                if (Math.abs(v.x - 1.0) < 1.0E-6 && Math.abs(v.y - 2.0) < 1.0E-6 && Math.abs(v.z - 3.0) < 1.0E-6) {
                    setPos = m;
                    Kensei.LOGGER.info("Kensei: camara de combate lista.");
                    break;
                }
            }
            if (setPos == null) {
                Kensei.LOGGER.warn("Kensei: no se encontro como mover la camara; se usa la camara normal.");
            }
        } catch (Throwable t) {
            setPos = null;
            Kensei.LOGGER.warn("Kensei: la camara de combate no esta disponible; se usa la camara normal.", t);
        }
        return setPos;
    }

    /** Cada tick del cliente: calcula a dónde quiere estar la cámara y se acerca suave. */
    public static void tick(Minecraft mc) {
        sidePrev = side;
        closerPrev = closer;
        upPrev = up;
        fovPrev = fov;
        kickPrev = kick;
        kick *= 0.55F;
        if (Math.abs(kick) < 0.05F) {
            kick = 0.0F;
        }
        LocalPlayer p = mc.player;
        boolean active = p != null && KenseiConfig.CAMERA_ENABLED.get() && KatanaUtil.holdsKatana(p)
                && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
        float tSide = 0.0F;
        float tCloser = 0.0F;
        float tUp = 0.0F;
        float tFov = 0.0F;
        if (active) {
            tSide = KenseiConfig.CAMERA_SHOULDER.get().floatValue();
            tCloser = VANILLA_DISTANCE - KenseiConfig.CAMERA_DISTANCE.get().floatValue();
            tUp = KenseiConfig.CAMERA_HEIGHT.get().floatValue();
            if (ClientLock.target() != null) {
                tSide += 0.2F; // con un enemigo fijado el jugador se corre un poco más a la izquierda
            }
            if (KenseiConfig.CAMERA_DYNAMIC.get()) {
                if (p.isSprinting()) {
                    tCloser -= 0.3F;
                    tFov += 4.0F;
                }
                ClientAnims.Anim a = ClientAnims.get(p, 40);
                if (a != null) {
                    float t = ClientAnims.age(a, 0.0F);
                    if (a.move == ClientAnims.DODGE && t < 16.0F) {
                        float k = Mth.sin(Mth.clamp(t / 16.0F, 0.0F, 1.0F) * (float) Math.PI);
                        tCloser -= 0.9F * k;
                        tFov += 9.0F * k;
                    } else if (a.move == ClientAnims.HEAVY) {
                        float w = KenseiConfig.HEAVY_WINDUP.get();
                        if (t < w + 14.0F) {
                            float k = t < w ? t / w : 1.0F - (t - w) / 14.0F;
                            tCloser -= 0.5F * Mth.clamp(k, 0.0F, 1.0F);
                            tFov += (t >= w && t < w + 6.0F ? 8.0F : 2.0F) * Mth.clamp(k, 0.0F, 1.0F);
                        }
                    } else if (a.move == ClientAnims.THRUST && t < 10.0F) {
                        tFov += 5.0F * Mth.sin(Mth.clamp(t / 10.0F, 0.0F, 1.0F) * (float) Math.PI);
                    }
                }
            }
        }
        side += (tSide - side) * 0.2F;
        closer += (tCloser - closer) * 0.2F;
        up += (tUp - up) * 0.2F;
        fov += (tFov - fov) * 0.3F;
        if (!active && Math.abs(side) < 0.005F && Math.abs(closer) < 0.005F && Math.abs(up) < 0.005F && Math.abs(fov) < 0.05F) {
            side = closer = up = fov = 0.0F;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        float pt = (float) e.getPartialTick();
        float sd = Mth.lerp(pt, sidePrev, side);
        float cl = Mth.lerp(pt, closerPrev, closer);
        float u = Mth.lerp(pt, upPrev, up);
        if (Math.abs(sd) < 0.01F && Math.abs(cl) < 0.01F && Math.abs(u) < 0.01F) {
            return;
        }
        Camera cam = e.getCamera();
        Entity ent = cam.getEntity();
        if (!cam.isDetached() || ent == null || ent.level() == null) {
            return;
        }
        Method m = resolve();
        if (m == null) {
            return;
        }
        try {
            Vec3 pos = cam.getPosition();
            Vector3f look = cam.getLookVector();
            Vector3f left = cam.getLeftVector();
            Vector3f upv = cam.getUpVector();
            // sd positivo = hacia la derecha (lo contrario de "izquierda"); cl positivo = hacia delante (más cerca del jugador)
            Vec3 np = pos.add(look.x() * cl - left.x() * sd + upv.x() * u,
                    look.y() * cl - left.y() * sd + upv.y() * u,
                    look.z() * cl - left.z() * sd + upv.z() * u);
            // que la cámara no atraviese bloques: se mira desde la cabeza hacia la nueva posición
            Vec3 from = ent.getEyePosition(pt);
            Vec3 dir = np.subtract(from);
            double len = dir.length();
            if (len > 1.0E-4) {
                BlockHitResult hit = ent.level().clip(new ClipContext(from, np, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, ent));
                if (hit.getType() != HitResult.Type.MISS) {
                    np = hit.getLocation().subtract(dir.normalize().scale(0.25));
                    len = np.subtract(from).length();
                }
                if (len < 0.8) {
                    np = from.add(dir.normalize().scale(0.8));
                }
            }
            m.invoke(cam, np.x, np.y, np.z);
        } catch (Throwable t) {
            setPos = null; // si falla una vez, se deja de intentar
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (!e.usedConfiguredFov()) {
            return;
        }
        float f = Mth.lerp((float) e.getPartialTick(), fovPrev, fov) + Mth.lerp((float) e.getPartialTick(), kickPrev, kick);
        if (Math.abs(f) > 0.01F) {
            e.setFOV(e.getFOV() + f);
        }
    }
}

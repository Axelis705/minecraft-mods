package com.autoscoleccion.client;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.entity.RcCarEntity;
import com.autoscoleccion.game.CarStats;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.CarMovePacket;
import com.autoscoleccion.net.DrivePacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * Manejar el cochecito a control remoto: W/S aceleran y frenan, A/D giran, Espacio es el freno de mano (derrapa) y Shift lo guarda.
 * El jugador se queda quieto y la cámara sigue al auto desde atrás (una entidad aparte que solo es la cámara mientras se dibuja).
 */
@Mod.EventBusSubscriber(modid = AutosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RcDriving {
    private static int pendingId = -1;
    private static int pendingTicks;
    private static RcCarEntity car;
    private static Marker cam;
    private static boolean swapped;
    private static CameraType savedCamera;
    private static float camYaw;
    private static double camX, camY, camZ;
    private static boolean camInit;
    private static long lastFrame;
    private static EngineSound engine;
    private static int bumpCooldown;

    private RcDriving() {
    }

    public static boolean driving() {
        return car != null;
    }

    /** El servidor dice cuál es mi auto (puede llegar antes que la entidad). */
    public static void onStart(int entityId) {
        pendingId = entityId;
        pendingTicks = 60;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            mc.setScreen(null);
        }
    }

    private static void begin(RcCarEntity e) {
        Minecraft mc = Minecraft.getInstance();
        car = e;
        car.local = true;
        car.speed = 0;
        cam = EntityType.MARKER.create(mc.level);
        camInit = false;
        savedCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        engine = new EngineSound(e);
        mc.getSoundManager().play(engine);
        Sfx.play("garage", 1.3F, 0.6F);
    }

    /** Guarda el auto y devuelve la cámara al jugador. */
    public static void end(boolean tellServer) {
        Minecraft mc = Minecraft.getInstance();
        if (car != null) {
            car.local = false;
        }
        car = null;
        cam = null;
        pendingId = -1;
        if (swapped && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        swapped = false;
        if (savedCamera != null) {
            mc.options.setCameraType(savedCamera);
            savedCamera = null;
        }
        if (engine != null) {
            engine.finish();
            engine = null;
        }
        if (tellServer) {
            AutosNet.toServer(new DrivePacket(-1));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (car != null) {
                end(false);
            }
            return;
        }
        if (e.phase == TickEvent.Phase.START) {
            if (pendingId >= 0) {
                Entity ent = mc.level.getEntity(pendingId);
                if (ent instanceof RcCarEntity rc) {
                    pendingId = -1;
                    begin(rc);
                } else if (--pendingTicks <= 0) {
                    pendingId = -1;
                }
            }
            if (car == null) {
                return;
            }
            if (car.isRemoved() || car.level() != mc.level) {
                end(false);
                return;
            }
            // lo que pide el jugador (las teclas de moverse siguen funcionando aunque el jugador no se mueva)
            boolean open = mc.screen != null;
            float th = 0;
            if (!open && mc.options.keyUp.isDown()) {
                th += 1;
            }
            if (!open && mc.options.keyDown.isDown()) {
                th -= 1;
            }
            float st = 0;
            if (!open && mc.options.keyRight.isDown()) {
                st += 1;
            }
            if (!open && mc.options.keyLeft.isDown()) {
                st -= 1;
            }
            if (RaceClient.frozen()) {
                // cuenta regresiva de la carrera: el auto espera en la parrilla
                th = 0;
            }
            car.throttle = th;
            car.steerIn = st;
            car.handbrake = RaceClient.frozen() || (!open && mc.options.keyJump.isDown());
            if (!open && mc.options.keyShift.isDown()) {
                end(true);
            }
            return;
        }
        // después del tick: mandar la posición, sonidos de choque
        if (car != null) {
            AutosNet.toServer(new CarMovePacket(car.getId(), car.getX(), car.getY(), car.getZ(), car.getYRot(), car.steer));
            if (bumpCooldown > 0) {
                bumpCooldown--;
            }
            if (car.bumped && bumpCooldown == 0) {
                bumpCooldown = 8;
                Sfx.play("bump", 0.9F + mc.level.random.nextFloat() * 0.2F, 0.7F);
            }
            if (car.drifting && mc.level.getGameTime() % 6 == 0) {
                Sfx.play("skid", 0.9F + mc.level.random.nextFloat() * 0.2F, 0.35F);
            }
            if (mc.player.distanceToSqr(car) > 62 * 62) {
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§eEl auto se alejó demasiado y volvió al garaje."), true);
                end(true);
            }
        }
    }

    /** El jugador no camina mientras maneja. */
    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (car != null) {
            Input in = e.getInput();
            in.forwardImpulse = 0;
            in.leftImpulse = 0;
            in.up = in.down = in.left = in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
        }
    }

    /** Cámara de persecución: detrás y un poco arriba del auto, siguiéndolo con suavidad. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase == TickEvent.Phase.START) {
            if (car == null || cam == null || mc.player == null || mc.level == null || mc.getCameraEntity() != mc.player) {
                return;
            }
            float pt = e.renderTickTime;
            long now = System.currentTimeMillis();
            float dt = camInit ? Math.min(0.1F, (now - lastFrame) / 1000.0F) : 1.0F;
            lastFrame = now;
            double x = Mth.lerp(pt, car.xo, car.getX());
            double y = Mth.lerp(pt, car.yo, car.getY());
            double z = Mth.lerp(pt, car.zo, car.getZ());
            float yaw = Mth.rotLerp(pt, car.yRotO, car.getYRot());
            if (!camInit) {
                camYaw = yaw;
            }
            camYaw += Mth.wrapDegrees(yaw - camYaw) * Math.min(1.0F, dt * 5.0F);
            double yr = Math.toRadians(camYaw);
            float spd = Math.abs(car.speed);
            // cerca del auto (es chiquito) y un poco más arriba al ir rápido
            double back = 1.05 + spd * 0.55;
            double tx = x + Math.sin(yr) * back, tz = z - Math.cos(yr) * back, ty = y + 0.5 + spd * 0.15;
            if (!camInit) {
                camX = tx;
                camY = ty;
                camZ = tz;
                camInit = true;
            }
            float k = Math.min(1.0F, dt * 16.0F);
            camX += (tx - camX) * k;
            camY += (ty - camY) * k;
            camZ += (tz - camZ) * k;
            float pitch = 11.0F;
            cam.setPos(camX, camY, camZ);
            cam.xo = cam.xOld = camX;
            cam.yo = cam.yOld = camY;
            cam.zo = cam.zOld = camZ;
            cam.setYRot(camYaw);
            cam.yRotO = camYaw;
            cam.setXRot(pitch);
            cam.xRotO = pitch;
            mc.setCameraEntity(cam);
            swapped = true;
        } else if (swapped) {
            swapped = false;
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
            }
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (car != null) {
            e.setFOV(e.getFOV() + Math.min(1.0F, Math.abs(car.speed) / CarStats.of(car.def()).top) * 7.0);
        }
    }

    /** Si la cámara pasa por donde está el jugador, no se dibuja su cuerpo (tapaba toda la pantalla). */
    @SubscribeEvent
    public static void onRenderPlayer(net.minecraftforge.client.event.RenderPlayerEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (car != null && cam != null && e.getEntity() == mc.player && mc.player.distanceToSqr(camX, camY - 1.0, camZ) < 2.2 * 2.2) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (car != null) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (car != null && e.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            e.setCanceled(true);
        }
    }

    /** Velocímetro y ayuda de controles. */
    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post e) {
        if (car == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        CarStats st = CarStats.of(car.def());
        float frac = Math.min(1, Math.abs(car.speed) / st.top);
        int kmh = Math.round(Math.abs(car.speed) * 20 * 3.6F * 8);
        float cx = W - 52, cy = H - 40, R = 30;
        Gfx.begin(g, false);
        Gfx.circle(cx, cy, R + 4, 0xC00B0B14);
        Gfx.arc(cx, cy, R - 5, R, Math.toRadians(215), Math.toRadians(-35), 0xFF22223A, 0xFF34345A, 30);
        Gfx.arc(cx, cy, R - 5, R, Math.toRadians(215), Math.toRadians(215 - 250 * frac), 0xFF3FE0FF, 0xFFFF4FA0, 30);
        double a = Math.toRadians(215 - 250 * frac);
        Gfx.line(cx, cy, cx + (float) Math.cos(a) * (R - 3), cy - (float) Math.sin(a) * (R - 3), 2, 0xFFFFE8C8);
        Gfx.circle(cx, cy, 3, 0xFFFF6A1A);
        Gfx.end();
        Gfx.text(g, mc.font, String.valueOf(kmh), cx, cy + 6, 1.3F, 0xFFFFFFFF);
        Gfx.text(g, mc.font, "km/h", cx, cy + 18, 0.6F, 0xFF9A9AC0);
        Gfx.text(g, mc.font, car.def().name, cx, cy - R - 14, 0.7F, 0xFF000000 | car.def().rarity.rgb);
        String help = "W/S acelerar · A/D girar · Espacio derrapar · Shift guardar";
        Gfx.textL(g, mc.font, help, 6, H - 12, 0.65F, 0xC0FFFFFF);
        if (car.speed < -0.02F) {
            Gfx.text(g, mc.font, "R", cx, cy - 12, 1.0F, 0xFFFF5A5A);
        }
        String dist = String.format(Locale.ROOT, "%.0f m", Math.sqrt(mc.player.distanceToSqr(car)));
        Gfx.textL(g, mc.font, dist, 6, H - 22, 0.65F, 0xA0FFFFFF);
    }
}

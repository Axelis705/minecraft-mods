package com.showdeaxelis.client;

import com.showdeaxelis.ShowMod;
import com.showdeaxelis.data.Studio;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * La camara de television: la vista salta entre planos del estudio (general, presentador, pantalla, publico, cada podio) con
 * movimiento suave y zoom, como en un programa. Es solo del cliente: cada quien decide si la usa (tecla H) y el presentador
 * puede ponerla a todos los espectadores.
 */
@Mod.EventBusSubscriber(modid = ShowMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TvCamera {
    public static boolean enabled;
    private static boolean engaged;
    private static Entity cam;
    private static boolean failed;
    private static String current = "wide";
    private static float fov = 70f;
    private static float targetFov = 70f;
    private static int shotAge;

    private TvCamera() {
    }

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            release();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(enabled ? "Camara de TV: activada (H para salir)" : "Camara de TV: desactivada"), true);
        }
    }

    /** El presentador pide que los espectadores vean por la camara de TV. */
    public static void forced(boolean on) {
        CompoundTag you = ShowClient.state.getCompound("you");
        if (you.getBoolean("host") || you.getBoolean("player")) {
            return;
        }
        enabled = on;
        if (!on) {
            release();
        }
    }

    public static void shot(String name) {
        current = name;
        shotAge = 0;
    }

    public static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (engaged && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        engaged = false;
        cam = null;
    }

    public static boolean active() {
        return engaged;
    }

    /** Posicion y punto de mira de un plano, en el mundo. Devuelve null si el estudio no existe. */
    private static Vec3[] plan(String name) {
        CompoundTag st = ShowClient.state.getCompound("stage");
        if (!st.contains("x")) {
            return null;
        }
        com.showdeaxelis.data.StageData.Info info = new com.showdeaxelis.data.StageData.Info(st.getInt("x"), st.getInt("y"), st.getInt("z"), st.getInt("d"));
        double px = 0;
        double[] p;
        double[] l;
        if (name.startsWith("pod")) {
            int side = 0;
            try {
                side = Integer.parseInt(name.substring(3));
            } catch (NumberFormatException ignored) {
            }
            px = podiumX(side);
            p = Studio.atD(info, px, 4.4, 10.5);
            l = Studio.atD(info, px, 3.8, 17.5);
            targetFov = 38f;
        } else {
            switch (name) {
                case "host" -> {
                    p = Studio.atD(info, 5, 4.4, 8);
                    l = Studio.atD(info, 11.5, 3.7, 14.5);
                    targetFov = 42f;
                }
                case "board" -> {
                    p = Studio.atD(info, 0, 6, 12);
                    l = Studio.atD(info, 0, 7, 24);
                    targetFov = 60f;
                }
                case "audience" -> {
                    p = Studio.atD(info, 0, 5, 14);
                    l = Studio.atD(info, 0, 4.2, 3);
                    targetFov = 72f;
                }
                default -> {
                    p = Studio.atD(info, 0, 8.5, 3);
                    l = Studio.atD(info, 0, 4, 17);
                    targetFov = 70f;
                }
            }
        }
        // un leve balanceo para que la camara se sienta viva
        double sway = Math.sin(ShowClient.clientTick * 0.03) * 0.35;
        Direction f = Studio.dirOf(info.dir());
        Direction r = f.getClockWise();
        return new Vec3[] {new Vec3(p[0] + r.getStepX() * sway, p[1], p[2] + r.getStepZ() * sway), new Vec3(l[0], l[1], l[2])};
    }

    private static int podiumX(int side) {
        int fmt = ShowClient.state.getInt("format");
        int pod;
        if (fmt == 1) {
            pod = side == 0 ? 0 : 2;
        } else if (fmt == 2) {
            pod = side;
        } else {
            pod = side / 2;
        }
        return Studio.PODIUM_X[Math.max(0, Math.min(2, pod))];
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        ShowClient.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (!enabled || !ShowClient.active() || failed) {
            if (engaged) {
                release();
            }
            return;
        }
        Vec3[] pl = plan(current);
        if (pl == null) {
            if (engaged) {
                release();
            }
            return;
        }
        if (!engaged) {
            try {
                cam = EntityType.MARKER.create(mc.level);
                if (cam == null) {
                    return;
                }
                Vec3 s = mc.player.getEyePosition();
                cam.moveTo(s.x, s.y, s.z, mc.player.getYRot(), mc.player.getXRot());
                cam.setOldPosAndRot();
                mc.setCameraEntity(cam);
                engaged = true;
            } catch (Throwable t) {
                failed = true;
                release();
                return;
            }
        }
        if (cam == null) {
            return;
        }
        if (mc.getCameraEntity() != cam) {
            mc.setCameraEntity(cam);
        }
        shotAge++;
        Vec3 d = pl[1].subtract(pl[0]);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        cam.setOldPosAndRot();
        double k = 0.12;
        cam.setPos(cam.getX() + (pl[0].x - cam.getX()) * k, cam.getY() + (pl[0].y - cam.getY()) * k, cam.getZ() + (pl[0].z - cam.getZ()) * k);
        cam.setYRot(Mth.rotLerp((float) k, cam.getYRot(), yaw));
        cam.setXRot(cam.getXRot() + (pitch - cam.getXRot()) * (float) k);
        fov += (targetFov - fov) * 0.1f;
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (engaged) {
            e.setFOV(fov);
        }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (engaged) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!engaged) {
            return;
        }
        var id = e.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CROSSHAIR.id()) || id.equals(VanillaGuiOverlay.HOTBAR.id())
                || id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) || id.equals(VanillaGuiOverlay.AIR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) || id.equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            e.setCanceled(true);
        }
    }

    /** El juego no dibuja al jugador propio cuando la camara es otra; lo dibujamos para que se vea en su podio. */
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        if (!engaged || e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.getCameraEntity() == p) {
            return;
        }
        try {
            var disp = mc.getEntityRenderDispatcher();
            float pt = e.getPartialTick();
            Vec3 cp = e.getCamera().getPosition();
            double x = Mth.lerp(pt, p.xo, p.getX()) - cp.x;
            double y = Mth.lerp(pt, p.yo, p.getY()) - cp.y;
            double z = Mth.lerp(pt, p.zo, p.getZ()) - cp.z;
            var src = mc.renderBuffers().bufferSource();
            disp.render(p, x, y, z, Mth.lerp(pt, p.yRotO, p.getYRot()), pt, e.getPoseStack(), src, disp.getPackedLightCoords(p, pt));
            src.endBatch();
        } catch (Throwable t) {
            // si falla el dibujo del jugador, simplemente no se ve
        }
    }
}

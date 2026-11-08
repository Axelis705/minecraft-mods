package com.pooltable.client;

import com.pooltable.PoolTable;
import com.pooltable.block.AirHockeyBE;
import com.pooltable.game.HockeyPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * La cámara del hockey: mientras la pantalla está abierta, la vista pasa a un punto sobre un lado de la mesa, mirando hacia abajo,
 * y se ve toda la mesa en grande con los dos jugadores en sus extremos. El jugador de verdad no se mueve.
 */
@Mod.EventBusSubscriber(modid = PoolTable.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HockeyCam {
    public static boolean active = false;
    private static Entity cam = null;
    private static Entity prev = null;
    private static boolean failed = false;

    private HockeyCam() {
    }

    public static void engage(AirHockeyBE be) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || failed) {
            return;
        }
        try {
            prev = mc.getCameraEntity();
            cam = EntityType.MARKER.create(mc.level);
            if (cam == null) {
                return;
            }
            Vec3 start = mc.player.getEyePosition();
            cam.moveTo(start.x, start.y, start.z, mc.player.getYRot(), mc.player.getXRot());
            cam.setOldPosAndRot();
            mc.setCameraEntity(cam);
            active = true;
        } catch (Throwable t) {
            failed = true;
            release();
        }
    }

    public static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (active && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        active = false;
        cam = null;
        prev = null;
    }

    /** Altura de la cámara sobre la mesa y cuánto se echa hacia atrás de tu extremo. */
    private static final double HEIGHT = 1.55;
    private static final double BACK = 0.45;
    /** Ángulo hacia abajo: con esto la mesa llena casi toda la pantalla, de tu extremo (abajo) al del rival (arriba). */
    private static final float PITCH = 47.0F;

    /** Cada jugador ve la mesa desde su propio extremo, mirando hacia el rival (quien solo mira, la ve desde el extremo del jugador 0). */
    private static Vec3 target(AirHockeyBE be, int role) {
        Direction f = be.facing();
        Vec3 c = be.center();
        double d = (HockeyPhysics.HL + BACK) * (role == 1 ? 1.0 : -1.0);
        return new Vec3(c.x + f.getStepX() * d, c.y + HEIGHT, c.z + f.getStepZ() * d);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || cam == null || HockeyScreen.current == null) {
            return;
        }
        AirHockeyBE be = HockeyScreen.current.table();
        if (be == null) {
            return;
        }
        if (mc.getCameraEntity() != cam) {
            mc.setCameraEntity(cam);
        }
        int role = Math.max(0, HockeyScreen.current.roleNow());
        Vec3 t = target(be, role);
        Direction look = role == 1 ? be.facing().getOpposite() : be.facing();
        float yaw = look.toYRot();
        float pitch = PITCH;
        cam.setOldPosAndRot();
        double k = 0.3;
        cam.setPos(cam.getX() + (t.x - cam.getX()) * k, cam.getY() + (t.y - cam.getY()) * k, cam.getZ() + (t.z - cam.getZ()) * k);
        cam.setYRot(Mth.rotLerp((float) k, cam.getYRot(), yaw));
        cam.setXRot(cam.getXRot() + (pitch - cam.getXRot()) * (float) k);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (active) {
            e.setFOV(70.0);
        }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (active) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!active) {
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

    /** La cámara está pegada a tu cabeza: tu cuerpo taparía la mesa, así que no se dibuja mientras juegas. */
    @SubscribeEvent
    public static void onPlayer(RenderPlayerEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (active && e.getEntity() == mc.player) {
            e.setCanceled(true);
        }
    }

    /** El juego no dibuja al jugador propio cuando la cámara es otra; lo dibujamos nosotros para que se vea en su extremo de la mesa. */
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        if (true) {
            // la cámara está justo en tu cabeza, detrás de tu extremo: tu cuerpo taparía la mesa, así que no se dibuja
            return;
        }
        if (!active || e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
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

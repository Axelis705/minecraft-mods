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
    private static boolean autoShot;

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

    /** Cambia el plano. Si es automatico (lo pidio un evento del show), a los 4 segundos vuelve a la pantalla. */
    public static void shot(String name, boolean auto) {
        current = name;
        shotAge = 0;
        autoShot = auto && !name.equals("board");
    }

    public static String currentShot() {
        return current;
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
        double[] p;
        double[] l;
        Vec3[] follow = followShot(name);
        if (follow != null) {
            return follow;
        }
        if (name.startsWith("pod")) {
            int side = 0;
            try {
                side = Integer.parseInt(name.substring(3));
            } catch (NumberFormatException ignored) {
            }
            double px = podiumX(side);
            p = Studio.atD(info, px, 4.9, 47);
            l = Studio.atD(info, px, 4.3, 34.6);
            targetFov = 38f;
        } else {
            switch (name) {
                case "host" -> {
                    p = Studio.atD(info, 14, 4.9, 46);
                    l = Studio.atD(info, 21, 3.8, 33);
                    targetFov = 40f;
                }
                case "face" -> {
                    p = Studio.atD(info, 0, 5.2, 21);
                    l = Studio.atD(info, 0, 3.4, 31);
                    targetFov = 52f;
                }
                case "board" -> {
                    p = Studio.atD(info, 0, 9, 34);
                    l = Studio.atD(info, 0, 11.5, 60);
                    targetFov = 62f;
                }
                case "audience" -> {
                    p = Studio.atD(info, 0, 6.5, 43);
                    l = Studio.atD(info, 0, 4, 12);
                    targetFov = 72f;
                }
                default -> {
                    p = Studio.atD(info, 0, 9, 1);
                    l = Studio.atD(info, 0, 8.5, 45);
                    targetFov = 75f;
                }
            }
        }
        // un leve balanceo para que la camara se sienta viva
        double sway = Math.sin(ShowClient.clientTick * 0.03) * 0.35;
        Direction f = Studio.dirOf(info.dir());
        Direction r = f.getClockWise();
        return new Vec3[] {new Vec3(p[0] + r.getStepX() * sway, p[1], p[2] + r.getStepZ() * sway), new Vec3(l[0], l[1], l[2])};
    }

    private static net.minecraft.world.entity.player.Player findPlayer(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || name == null || name.isEmpty()) {
            return null;
        }
        for (net.minecraft.world.entity.player.Player p : mc.level.players()) {
            if (p.getName().getString().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /** Plano de frente a una persona de verdad (el presentador o quien da la cara de un lado). Null si no esta cerca. */
    private static Vec3[] followShot(String name) {
        String who = "";
        if (name.equals("host")) {
            who = ShowClient.state.getString("hostName");
        } else if (name.startsWith("pod")) {
            try {
                var sd = ShowClient.state.getList("sides", 10).getCompound(Integer.parseInt(name.substring(3)));
                who = sd.getString("rep");
                if (who.isEmpty() && !sd.getList("m", 8).isEmpty()) {
                    who = sd.getList("m", 8).getString(0);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        var pl = findPlayer(who);
        if (pl == null) {
            return null;
        }
        Vec3 eye = pl.getEyePosition();
        double yaw = Math.toRadians(pl.getYRot());
        Vec3 front = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        targetFov = name.equals("host") ? 45f : 40f;
        double sway = Math.sin(ShowClient.clientTick * 0.03) * 0.3;
        Vec3 side = new Vec3(front.z, 0, -front.x);
        Vec3 pos = eye.add(front.scale(3.8)).add(side.scale(sway)).add(0, 0.25, 0);
        return new Vec3[] {pos, eye.add(0, -0.05, 0)};
    }

    /** Donde (x local) esta el lado indicado, segun el formato de la partida. */
    private static double podiumX(int side) {
        int[] pm = Studio.podiumOfSide(com.showdeaxelis.game.Format.byOrdinal(ShowClient.state.getInt("format")), side);
        int cx = Studio.podiumCenter(pm[0]);
        return pm[1] == 2 ? cx : cx + (pm[1] == 0 ? -2.0 : 2.0);
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
        if (autoShot && shotAge > 90) {
            current = "board";
            autoShot = false;
            shotAge = 0;
        }
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

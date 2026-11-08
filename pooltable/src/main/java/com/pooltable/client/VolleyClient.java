package com.pooltable.client;

import com.pooltable.PoolTable;
import com.pooltable.block.VolleyCourtBE;
import com.pooltable.game.VolleyMatch;
import com.pooltable.net.PoolNet;
import com.pooltable.net.VolleyHitPacket;
import com.pooltable.net.VolleySyncPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Todo lo del lado del jugador en una partida de voleibol: saber que estoy jugando (el servidor manda mi id de entidad), la cámara de cancha
 * (detrás de tu línea de fondo, mirando a la red), mover y apuntar con WASD según esa cámara, y los clics (izquierdo = golpear, derecho = colocar).
 * Con la tecla C se vuelve a la cámara normal; entonces WASD sigue la vista normal.
 */
@Mod.EventBusSubscriber(modid = PoolTable.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VolleyClient {
    /** La cancha donde estoy jugando (o null). */
    public static VolleyCourtBE court = null;
    public static int myIdx = -1;
    public static int myTeam = -1;
    public static boolean arcade = true;
    /** Hacia dónde apunto ahora (de WASD): adelante = profundo, a la derecha. */
    public static float aimFwd = 0;
    public static float aimRight = 0;

    /** Barra de fuerza del saque: se llena y vacía mientras mantienes el clic izquierdo. */
    private static boolean charging = false;
    private static long chargeStart = 0;
    private static long lastSync = 0;
    private static boolean camActive = false;
    private static boolean failed = false;
    /** Posición de la cámara de cancha (suavizada en cada tick y repartida entre ticks al dibujar). */
    private static Vec3 camPos = null;
    private static Vec3 camOld = null;
    private static CameraType prevType = null;
    private static java.lang.reflect.Method setPosMethod = null;

    private VolleyClient() {
    }

    public static boolean playing() {
        return court != null && myIdx >= 0;
    }

    /** Fuerza (0 a 1) de la barra si estás cargando el saque, o -1. */
    public static float chargeNow() {
        if (!charging) {
            return -1.0F;
        }
        double t = (System.nanoTime() - chargeStart) / 1.0e9 / 1.3;
        double ph = t - Math.floor(t / 2.0) * 2.0;
        return (float) (ph < 1.0 ? ph : 2.0 - ph);
    }

    /** ¿Me toca sacar ahora? */
    public static boolean iServe() {
        return court != null && court.cState == VolleyMatch.State.SERVE.ordinal() && court.cServerIdx == myIdx;
    }

    public static boolean camOn() {
        return camActive;
    }

    // ------------------------------------------------------------------ estado

    public static void onSync(VolleyCourtBE be, VolleySyncPacket m) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null) {
            return;
        }
        int found = -1;
        if (m.state != VolleyMatch.State.IDLE.ordinal()) {
            for (int i = 0; i < m.count; i++) {
                if (m.eid[i] == me.getId()) {
                    found = i;
                }
            }
        }
        if (found >= 0) {
            boolean first = court != be;
            court = be;
            myIdx = found;
            myTeam = m.team[found];
            lastSync = System.nanoTime();
            if (first) {
                arcade = true;
            }
            if (arcade && !camActive) {
                engage();
            }
        } else if (court == be) {
            leave();
        }
    }

    public static void leave() {
        charging = false;
        court = null;
        myIdx = -1;
        myTeam = -1;
        aimFwd = 0;
        aimRight = 0;
        release();
    }

    // ------------------------------------------------------------------ cámara

    /**
     * La cámara de cancha NO cambia la entidad de cámara del juego (si fuera otra entidad, Minecraft dejaría de moverte y de mandar tu posición
     * al servidor). Tú sigues siendo la cámara, en tercera persona, y en cada fotograma la colocamos nosotros arriba y detrás de tu línea de fondo.
     */
    private static void engage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || failed || camActive) {
            return;
        }
        try {
            if (!findSetPos()) {
                failed = true;
                return;
            }
            prevType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            Vec3 start = mc.gameRenderer.getMainCamera().getPosition();
            camPos = start;
            camOld = start;
            camActive = true;
        } catch (Throwable t) {
            failed = true;
            release();
        }
    }

    private static boolean findSetPos() {
        if (setPosMethod != null) {
            return true;
        }
        // el método de Camera que recibe un Vec3 (es protegido y su nombre cambia entre el desarrollo y el juego, así que se busca por su forma)
        for (java.lang.reflect.Method m : Camera.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Vec3.class && m.getReturnType() == void.class
                    && !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                try {
                    m.setAccessible(true);
                    setPosMethod = m;
                    return true;
                } catch (Throwable t) {
                    return false;
                }
            }
        }
        return false;
    }

    private static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (camActive && prevType != null) {
            mc.options.setCameraType(prevType);
        }
        camActive = false;
        camPos = null;
        camOld = null;
        prevType = null;
    }

    /** Hacia dónde mira mi equipo (la dirección del mundo del lado del rival). */
    private static Direction teamDir() {
        Direction f = court.facing();
        return myTeam == 0 ? f : f.getOpposite();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (court == null) {
            return;
        }
        if (mc.level == null || mc.player == null || court.isRemoved() || (System.nanoTime() - lastSync) > 1_500_000_000L) {
            leave();
            return;
        }
        while (VolleyKeys.MENU.consumeClick()) {
            PoolClient.openVolley(court.getBlockPos());
        }
        if (charging) {
            // se suelta el clic: sale el saque con la fuerza de ese instante
            if (!mc.options.keyAttack.isDown() || !iServe()) {
                float power = chargeNow();
                charging = false;
                if (iServe()) {
                    PoolNet.sendToServer(new VolleyHitPacket(court.getBlockPos(), 0, aimFwd, aimRight, false, Math.max(0.0F, power)));
                }
            }
        }
        while (VolleyKeys.CAM.consumeClick()) {
            arcade = !arcade;
            if (arcade) {
                engage();
            } else {
                release();
            }
        }
        if (!camActive) {
            return;
        }
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        // el cuerpo mira siempre hacia la red mientras se usa la cámara de cancha (también aquí, para que el servidor lo sepa)
        float fy = teamDir().toYRot();
        mc.player.setYRot(fy);
        mc.player.yHeadRot = fy;
        mc.player.yBodyRot = fy;
        Vec3 t = camTarget();
        camOld = camPos;
        double k = 0.25;
        camPos = new Vec3(camPos.x + (t.x - camPos.x) * k, camPos.y + (t.y - camPos.y) * k, camPos.z + (t.z - camPos.z) * k);
    }

    private static Vec3 camTarget() {
        int n = court.cN;
        double hl = VolleyMatch.HL[Math.max(0, Math.min(2, n - 1))];
        double own = myTeam == 0 ? -1.0 : 1.0;
        double h = 5.5 + hl * 0.55;
        double u = own * (hl + 2.6);
        return court.world(u, h, 0.0);
    }

    /** Ángulo hacia abajo de la cámara de cancha. */
    private static float camPitch() {
        int n = court.cN;
        double hl = VolleyMatch.HL[Math.max(0, Math.min(2, n - 1))];
        double own = myTeam == 0 ? -1.0 : 1.0;
        double h = 5.5 + hl * 0.55;
        double u = own * (hl + 2.6);
        double lookU = -own * hl * 0.2;
        return (float) Math.toDegrees(Math.atan2(h - 0.7, Math.abs(lookU - u)));
    }

    /** Cada fotograma, después de que el juego coloque la cámara en tu cabeza, la llevamos a la posición de la cancha. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles e) {
        if (!camActive || court == null || camPos == null || setPosMethod == null) {
            return;
        }
        try {
            double pt = e.getPartialTick();
            Vec3 o = camOld == null ? camPos : camOld;
            Vec3 v = new Vec3(Mth.lerp(pt, o.x, camPos.x), Mth.lerp(pt, o.y, camPos.y), Mth.lerp(pt, o.z, camPos.z));
            setPosMethod.invoke(e.getCamera(), v);
            e.setYaw(teamDir().toYRot());
            e.setPitch(camPitch());
            e.setRoll(0.0F);
        } catch (Throwable t) {
            // si no se puede colocar la cámara, se vuelve a la vista normal para no dejar al jugador con una vista rara
            failed = true;
            arcade = false;
            release();
        }
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (camActive) {
            e.setFOV(70.0);
        }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (camActive) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!playing()) {
            return;
        }
        var id = e.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CROSSHAIR.id()) || id.equals(VanillaGuiOverlay.HOTBAR.id())
                || id.equals(VanillaGuiOverlay.ITEM_NAME.id()) || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            e.setCanceled(true);
        }
    }

    /** Con la cámara de cancha mi cuerpo mira siempre hacia la red (el ratón no lo hace girar). */
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!playing() || p == null) {
            return;
        }
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && camActive) {
            float yaw = teamDir().toYRot();
            p.setYRot(yaw);
            p.yRotO = yaw;
            p.yBodyRot = yaw;
            p.yBodyRotO = yaw;
            p.yHeadRot = yaw;
            p.yHeadRotO = yaw;
            p.setXRot(8.0F);
            p.xRotO = 8.0F;
        }
    }

    // ------------------------------------------------------------------ movimiento y puntería

    /** WASD se interpreta con la cámara de cancha (W = hacia la red, A = a la izquierda de la cámara); la puntería sale de lo mismo. */
    @SubscribeEvent
    public static void onMove(MovementInputUpdateEvent e) {
        if (!playing()) {
            return;
        }
        var in = e.getInput();
        float fwd = in.forwardImpulse;
        float left = in.leftImpulse;
        double base = camActive ? teamDir().toYRot() : e.getEntity().getYRot();
        double bc = Math.toRadians(base);
        double wx = fwd * -Math.sin(bc) + left * Math.cos(bc);
        double wz = fwd * Math.cos(bc) + left * Math.sin(bc);
        // puntería respecto a mi equipo
        double tc = Math.toRadians(teamDir().toYRot());
        double tfx = -Math.sin(tc);
        double tfz = Math.cos(tc);
        double trx = -Math.cos(tc);
        double trz = -Math.sin(tc);
        double len = Math.hypot(wx, wz);
        double nx = len > 1.0 ? wx / len : wx;
        double nz = len > 1.0 ? wz / len : wz;
        aimFwd = (float) Mth.clamp(nx * tfx + nz * tfz, -1.0, 1.0);
        aimRight = (float) Mth.clamp(nx * trx + nz * trz, -1.0, 1.0);
        if (camActive) {
            // lo que mandé con WASD (visto desde la cámara) se convierte a lo que el jugador entiende según hacia dónde mira
            double pc = Math.toRadians(e.getEntity().getYRot());
            in.forwardImpulse = (float) (wx * -Math.sin(pc) + wz * Math.cos(pc));
            in.leftImpulse = (float) (wx * Math.cos(pc) + wz * Math.sin(pc));
        }
    }

    /** Los clics son del voleibol, no de picar o poner bloques. */
    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (!playing()) {
            return;
        }
        if (e.isAttack() || e.isUseItem()) {
            e.setCanceled(true);
            e.setSwingHand(false);
            BlockPos pos = court.getBlockPos();
            if (e.isAttack() && iServe()) {
                // al sacar, el clic mantenido llena la barra (se suelta en la zona verde)
                if (!charging) {
                    charging = true;
                    chargeStart = System.nanoTime();
                }
                return;
            }
            PoolNet.sendToServer(new VolleyHitPacket(pos, e.isUseItem() ? 1 : 0, aimFwd, aimRight, VolleyKeys.DIVE.isDown()));
        }
    }
}

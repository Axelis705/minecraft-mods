package com.pooltable.client;

import com.pooltable.PoolTable;
import com.pooltable.block.VolleyCourtBE;
import com.pooltable.game.VolleyMatch;
import com.pooltable.net.PoolNet;
import com.pooltable.net.VolleyHitPacket;
import com.pooltable.net.VolleySyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
    /**
     * El punto al que apunto, respecto a mi equipo: metros más allá de la red (negativo = en mi campo) y metros a mi derecha. Con la cámara de cancha el
     * ratón mueve la marca por el suelo; con la cámara normal es el punto del suelo al que miras.
     */
    public static float aimDepth = 5.0F;
    public static float aimRight = 0;
    /** Grados de ratón -> metros de la marca. */
    private static final float AIM_YAW = 0.05F;
    private static final float AIM_PITCH = 0.12F;

    /** Barra de fuerza del saque: se llena y vacía mientras mantienes el clic izquierdo. */
    private static boolean charging = false;
    private static long chargeStart = 0;
    private static long lastSync = 0;
    private static Entity cam = null;
    private static boolean camActive = false;
    private static boolean swapped = false;
    private static boolean failed = false;
    /** Lo último que se le puso al cuerpo: lo que el ratón movió desde entonces es lo que mueve la marca. */
    private static float baseYaw = 0;
    private static float basePitch = 0;
    private static boolean baseValid = false;

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
                aimDepth = (float) (VolleyMatch.HL[Math.max(0, Math.min(2, be.cN - 1))] * 0.55);
                aimRight = 0;
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
        aimDepth = 5.0F;
        aimRight = 0;
        release();
    }

    // ------------------------------------------------------------------ cámara

    /**
     * La cámara de cancha es una entidad aparte (un Marker) que se coloca arriba y detrás de tu línea de fondo. Para que Minecraft siga moviéndote y
     * mandando tu posición al servidor, la entidad de cámara es el propio jugador en cada tick del juego y solo es el Marker mientras se dibuja cada
     * fotograma ({@link #onRenderTick}).
     */
    private static void engage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || failed || camActive) {
            return;
        }
        try {
            cam = EntityType.MARKER.create(mc.level);
            if (cam == null) {
                return;
            }
            Vec3 start = mc.player.getEyePosition();
            cam.moveTo(start.x, start.y, start.z, mc.player.getYRot(), mc.player.getXRot());
            cam.setOldPosAndRot();
            camActive = true;
            baseValid = false;
        } catch (Throwable t) {
            failed = true;
            release();
        }
    }

    private static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (swapped || (camActive && mc.player != null)) {
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
            }
        }
        swapped = false;
        camActive = false;
        baseValid = false;
        cam = null;
    }

    /** Durante el dibujado de cada fotograma la cámara es el Marker; el resto del tiempo, el jugador. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase == TickEvent.Phase.START) {
            if (camActive && cam != null && mc.player != null && mc.level != null && mc.getCameraEntity() == mc.player) {
                mc.setCameraEntity(cam);
                swapped = true;
            }
        } else if (swapped) {
            swapped = false;
            if (mc.player != null) {
                mc.setCameraEntity(mc.player);
            }
        }
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
                    PoolNet.sendToServer(new VolleyHitPacket(court.getBlockPos(), 0, aimDepth, aimRight, false, Math.max(0.0F, power)));
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
            aimFromLook(mc.player);
            return;
        }
        applyBody(mc.player);
        int n = court.cN;
        double hl = VolleyMatch.HL[Math.max(0, Math.min(2, n - 1))];
        double own = myTeam == 0 ? -1.0 : 1.0;
        double h = 5.5 + hl * 0.55;
        double u = own * (hl + 2.6);
        Vec3 t = court.world(u, h, 0.0);
        double lookU = -own * hl * 0.2;
        double dist = Math.abs(lookU - u);
        float yaw = teamDir().toYRot();
        float pitch = (float) Math.toDegrees(Math.atan2(h - 0.7, dist));
        cam.setOldPosAndRot();
        double k = 0.25;
        cam.setPos(cam.getX() + (t.x - cam.getX()) * k, cam.getY() + (t.y - cam.getY()) * k, cam.getZ() + (t.z - cam.getZ()) * k);
        cam.setYRot(Mth.rotLerp((float) k, cam.getYRot(), yaw));
        cam.setXRot(cam.getXRot() + (pitch - cam.getXRot()) * (float) k);
    }

    // ------------------------------------------------------------------ puntería

    private static double aimMaxDepth() {
        return VolleyMatch.HL[Math.max(0, Math.min(2, court.cN - 1))] + VolleyMatch.AIM_MARGIN;
    }

    private static double aimMaxRight() {
        return VolleyMatch.HW[Math.max(0, Math.min(2, court.cN - 1))] + VolleyMatch.AIM_MARGIN;
    }

    private static void clampAim() {
        aimDepth = (float) Mth.clamp(aimDepth, -aimMaxDepth(), aimMaxDepth());
        aimRight = (float) Mth.clamp(aimRight, -aimMaxRight(), aimMaxRight());
    }

    /** Punto de la marca en el marco de la cancha (u a lo largo, v a lo ancho). */
    public static double aimU() {
        return VolleyMatch.dir(Math.max(0, myTeam)) * aimDepth;
    }

    public static double aimV() {
        return VolleyMatch.dir(Math.max(0, myTeam)) * aimRight;
    }

    /** ¿La marca está dentro de las líneas? */
    public static boolean aimInside() {
        if (court == null) {
            return true;
        }
        int i = Math.max(0, Math.min(2, court.cN - 1));
        return Math.abs(aimU()) <= VolleyMatch.HL[i] && Math.abs(aimV()) <= VolleyMatch.HW[i];
    }

    /**
     * Con la cámara de cancha el cuerpo queda mirando hacia la marca (hasta 80 grados a cada lado de la red) y lo que el ratón giró desde el último
     * fotograma mueve la marca: derecha/izquierda = a los lados, arriba/abajo = más lejos/más cerca.
     */
    private static void applyBody(LocalPlayer p) {
        if (baseValid) {
            float dy = Mth.wrapDegrees(p.getYRot() - baseYaw);
            float dp = p.getXRot() - basePitch;
            aimRight += dy * AIM_YAW;
            aimDepth -= dp * AIM_PITCH;
            clampAim();
        }
        float team = teamDir().toYRot();
        Vec3 w = court.world(aimU(), 0, aimV());
        double dx = w.x - p.getX();
        double dz = w.z - p.getZ();
        float yaw = team;
        if (Math.hypot(dx, dz) > 0.6) {
            float toAim = (float) Math.toDegrees(Math.atan2(-dx, dz));
            yaw = team + Mth.clamp(Mth.wrapDegrees(toAim - team), -80.0F, 80.0F);
        }
        float pitch = 8.0F;
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
        p.setXRot(pitch);
        p.xRotO = pitch;
        baseYaw = yaw;
        basePitch = pitch;
        baseValid = true;
    }

    /** Con la cámara normal la marca está donde miras: el punto del suelo en esa dirección (lejos si miras al frente). */
    private static void aimFromLook(LocalPlayer p) {
        if (court == null || p == null) {
            return;
        }
        double yaw = Math.toRadians(p.getYRot());
        double pitch = p.getXRot();
        double dist = pitch > 5.0 ? Math.min(16.0, Math.max(2.0, p.getEyeHeight() / Math.tan(Math.toRadians(pitch)))) : 16.0;
        double wx = p.getX() - Math.sin(yaw) * dist;
        double wz = p.getZ() + Math.cos(yaw) * dist;
        Direction f = court.facing();
        Direction r = f.getClockWise();
        double dx = wx - (court.getBlockPos().getX() + 0.5);
        double dz = wz - (court.getBlockPos().getZ() + 0.5);
        double u = dx * f.getStepX() + dz * f.getStepZ();
        double v = dx * r.getStepX() + dz * r.getStepZ();
        double sd = VolleyMatch.dir(Math.max(0, myTeam));
        aimDepth = (float) (sd * u);
        aimRight = (float) (sd * v);
        clampAim();
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

    /** Cada fotograma (antes de dibujar a los jugadores) el cuerpo se pone mirando a la marca y el ratón mueve la marca. */
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!playing() || p == null || !camActive) {
            return;
        }
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            applyBody(p);
        }
    }

    // ------------------------------------------------------------------ movimiento y puntería

    /** WASD solo mueve: con la cámara de cancha, W = hacia la red y A = a la izquierda de la cámara. */
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
            PoolNet.sendToServer(new VolleyHitPacket(pos, e.isUseItem() ? 1 : 0, aimDepth, aimRight, VolleyKeys.DIVE.isDown()));
        }
    }
}

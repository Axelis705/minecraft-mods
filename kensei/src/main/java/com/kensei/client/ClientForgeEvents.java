package com.kensei.client;

import com.kensei.Kensei;
import com.kensei.KenseiConfig;
import com.kensei.combat.KatanaUtil;
import com.kensei.net.DodgePacket;
import com.kensei.net.KenseiNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.kensei.net.DuelKeyPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del cliente: teclas (esquive, crítico, clic izquierdo con katana) y quitar la lentitud al bloquear con katana. */
@Mod.EventBusSubscriber(modid = Kensei.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            ClientLock.tick(Minecraft.getInstance());
            return;
        }
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        ClientState.tick();
        ClientEffects.tick();
        SlashTrail.tick(Minecraft.getInstance());
        ClientCamera.tick(Minecraft.getInstance());
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p != null && p.tickCount % 100 == 0) {
            ClientAnims.prune();
        }
        while (KenseiKeys.DODGE.consumeClick()) {
            if (p == null || mc.screen != null) {
                continue;
            }
            int fwd = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
            int strafe = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);
            KenseiNet.CHANNEL.sendToServer(new DodgePacket(fwd, strafe));
        }
        while (KenseiKeys.DUEL.consumeClick()) {
            if (p != null && mc.screen == null) {
                int tid = -1;
                LivingEntity lt = ClientLock.target();
                if (lt instanceof Player || lt instanceof com.kensei.entity.RivalEntity) {
                    tid = lt.getId();
                } else if (mc.crosshairPickEntity instanceof Player || mc.crosshairPickEntity instanceof com.kensei.entity.RivalEntity) {
                    tid = mc.crosshairPickEntity.getId();
                } else {
                    Player near = nearestLookedPlayer(p);
                    if (near != null) {
                        tid = near.getId();
                    }
                }
                KenseiNet.CHANNEL.sendToServer(new DuelKeyPacket(tid));
            }
        }
        while (KenseiKeys.LOCK.consumeClick()) {
            if (p != null && mc.screen == null && KatanaUtil.holdsKatana(p)) {
                ClientLock.toggle(p);
            }
        }
        while (KenseiKeys.NEXT_TARGET.consumeClick()) {
            if (p != null && mc.screen == null && KatanaUtil.holdsKatana(p)) {
                ClientLock.cycle(p);
            }
        }
        while (KenseiKeys.HEAVY.consumeClick()) {
            if (p == null || mc.screen != null) {
                continue;
            }
            ClientCombat.tryHeavy(p);
        }
        if (p != null && mc.screen == null) {
            ClientCombat.tickBuffer(p);
        }
        // mantener el clic izquierdo repite los golpes del combo
        if (p != null && KenseiConfig.M1_HOLD_TO_CHAIN.get() && ClientCombat.isHolding(mc)) {
            ClientCombat.tryM1(p, false);
        }
    }

    /** El jugador más centrado en la mira dentro de 24 bloques (para desafiar sin tener que apuntar exacto), o null. */
    private static Player nearestLookedPlayer(LocalPlayer p) {
        net.minecraft.world.phys.Vec3 look = p.getLookAngle();
        Player best = null;
        double bestDot = 0.93;
        for (Player o : p.level().players()) {
            if (o == p || o.isSpectator() || p.distanceToSqr(o) > 24.0 * 24.0) {
                continue;
            }
            net.minecraft.world.phys.Vec3 to = o.getEyePosition().subtract(p.getEyePosition()).normalize();
            double dot = look.dot(to);
            if (dot > bestDot) {
                bestDot = dot;
                best = o;
            }
        }
        return best;
    }

    /** Antes de dibujar cada cuadro: gira suave hacia el enemigo fijado. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            ClientLock.frame(Minecraft.getInstance(), e.renderTickTime);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void onCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        ClientLock.overrideCamera(e);
    }

    /**
     * Guarda dónde está la cámara en este cuadro (para dibujar la marca del objetivo en la posición correcta de la pantalla).
     * Ojo: en AFTER_LEVEL Forge entrega la pila de la proyección (sin la rotación de la cámara), por eso se toma en AFTER_PARTICLES,
     * donde la pila trae la rotación de la vista (igual que usa {@link Vfx}).
     */
    @SubscribeEvent
    public static void onLevelStage(net.minecraftforge.client.event.RenderLevelStageEvent e) {
        if (e.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ClientLock.captureView(e.getPoseStack(), e.getProjectionMatrix(), e.getCamera().getPosition());
        }
    }

    /** Con la katana en la mano se oculta la fila de corazones del juego: la vida se ve en el panel de arriba a la izquierda. */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre e) {
        if (e.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() && KenseiConfig.HIDE_VANILLA_HEALTH.get()) {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null && KatanaUtil.holdsKatana(p)) {
                e.setCanceled(true);
            }
        }
    }

    /** Con una katana en la mano el clic izquierdo no hace el ataque normal del juego: lanza el tajo propio del mod. */
    @SubscribeEvent
    public static void onAttackClick(InputEvent.InteractionKeyMappingTriggered e) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !KatanaUtil.mainHandKatana(p)) {
            return;
        }
        if (e.isPickBlock()) {
            e.setCanceled(true); // el clic central con la katana fija objetivo, no copia bloques
            return;
        }
        if (!e.isAttack()) {
            return;
        }
        e.setCanceled(true);
        e.setSwingHand(false);
        ClientCombat.tryM1(p);
    }

    /** Nombres de aliados (verde) y rivales (rojo) del duelo. */
    @SubscribeEvent
    public static void onNameTag(net.minecraftforge.client.event.RenderNameTagEvent e) {
        TeamMarks.onNameTag(e);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientAnims.clear();
        Vfx.clear();
        ClientDuel.clear();
        ClientLock.clear();
        ClientCombat.reset();
    }

    /** Bloquear con una katana no te frena (el juego normalmente multiplica por 0,2 al usar un objeto; aquí se compensa). */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent e) {
        if (KatanaUtil.guarding(e.getEntity()) && !e.getEntity().isPassenger()) {
            Input in = e.getInput();
            in.leftImpulse *= 5.0F;
            in.forwardImpulse *= 5.0F;
        }
    }
}

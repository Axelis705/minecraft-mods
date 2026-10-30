package com.kensei.client;

import com.kensei.KenseiConfig;
import com.kensei.combat.KatanaUtil;
import com.kensei.net.KenseiNet;
import com.kensei.net.MovePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Lado del cliente de los ataques: al hacer clic con katana se pide el golpe al servidor y, sin esperar, se empieza el tajo en pantalla
 * (el servidor es quien decide si conecta). Lleva la misma cuenta de espera y de cadena de golpes que el servidor.
 */
public final class ClientCombat {
    private static long swingReadyAt = 0;
    private static long lastSwingTick = -1000;
    private static int step = 0;
    private static long heavyReadyAt = 0;
    /** Un clic (o la tecla R) que llegó un poco antes de poder atacar: se guarda y se lanza solo en cuanto se puede. */
    private static long queuedM1Until = 0;
    private static long queuedHeavyUntil = 0;
    private static final int BUFFER_TICKS = 5;

    private ClientCombat() {
    }

    public static void tryM1(LocalPlayer p) {
        tryM1(p, true);
    }

    /** @param buffer si el clic llega justo antes de poder atacar, guardarlo (los clics repetidos por mantener apretado no se guardan). */
    public static void tryM1(LocalPlayer p, boolean buffer) {
        if (!KatanaUtil.mainHandKatana(p) || p.isUsingItem() || p.isSpectator() || p.isPassenger()) {
            return;
        }
        long now = p.level().getGameTime();
        if (now < swingReadyAt) {
            if (buffer && swingReadyAt - now <= BUFFER_TICKS) {
                queuedM1Until = swingReadyAt + 1;
            }
            return;
        }
        queuedM1Until = 0;
        boolean chain = step > 0 && now - lastSwingTick <= KenseiConfig.COMBO_WINDOW.get();
        boolean thrust = !chain && p.isSprinting() && p.onGround();
        int nextStep = thrust ? ClientAnims.THRUST : (chain ? step % 4 + 1 : 1);
        float cost = (float) (double) (nextStep == ClientAnims.THRUST ? KenseiConfig.COST_THRUST.get() : nextStep >= 4 ? KenseiConfig.COST_FINISHER.get() : KenseiConfig.COST_LIGHT.get());
        if (!ClientState.canSpend(cost)) {
            return; // sin estamina: ni siquiera empieza el tajo
        }
        ClientState.spendLocal(cost);
        int anim;
        if (thrust) {
            anim = ClientAnims.THRUST; // estocada corriendo
            step = 0;
            swingReadyAt = now + KenseiConfig.THRUST_COOLDOWN.get();
        } else {
            step = chain ? step % 4 + 1 : 1;
            anim = step;
            swingReadyAt = now + (step >= 4 ? KenseiConfig.M1_RECOVERY.get() : KenseiConfig.M1_INTERVAL.get());
        }
        lastSwingTick = now;
        ClientAnims.set(p.getId(), anim, 0.0F);
        p.swing(InteractionHand.MAIN_HAND);
        KenseiNet.CHANNEL.sendToServer(new MovePacket(MovePacket.NORMAL));
    }

    public static void tryHeavy(LocalPlayer p) {
        if (!KatanaUtil.mainHandKatana(p) || p.isUsingItem() || p.isSpectator() || p.isPassenger()) {
            return;
        }
        long now = p.level().getGameTime();
        if (now < swingReadyAt || now < heavyReadyAt) {
            if (Math.max(swingReadyAt, heavyReadyAt) - now <= BUFFER_TICKS) {
                queuedHeavyUntil = Math.max(swingReadyAt, heavyReadyAt) + 1;
            }
            return;
        }
        float heavyCost = (float) (double) KenseiConfig.COST_HEAVY.get();
        if (!ClientState.canSpend(heavyCost)) {
            queuedHeavyUntil = 0;
            return;
        }
        ClientState.spendLocal(heavyCost);
        queuedHeavyUntil = 0;
        queuedM1Until = 0;
        int windup = KenseiConfig.HEAVY_WINDUP.get();
        swingReadyAt = now + windup + KenseiConfig.HEAVY_RECOVERY.get();
        heavyReadyAt = now + windup + KenseiConfig.HEAVY_COOLDOWN.get();
        step = 0;
        KenseiNet.CHANNEL.sendToServer(new MovePacket(MovePacket.HEAVY));
    }

    /** Cada tick: lanza el ataque que se guardó si ya se puede. */
    public static void tickBuffer(LocalPlayer p) {
        long now = p.level().getGameTime();
        if (queuedHeavyUntil > 0) {
            if (now > queuedHeavyUntil || !KatanaUtil.mainHandKatana(p)) {
                queuedHeavyUntil = 0;
            } else if (now >= Math.max(swingReadyAt, heavyReadyAt)) {
                tryHeavy(p);
            }
            return;
        }
        if (queuedM1Until > 0) {
            if (now > queuedM1Until || !KatanaUtil.mainHandKatana(p)) {
                queuedM1Until = 0;
            } else if (now >= swingReadyAt) {
                tryM1(p, false);
            }
        }
    }

    public static void reset() {
        queuedM1Until = 0;
        queuedHeavyUntil = 0;
        swingReadyAt = 0;
        lastSwingTick = -1000;
        step = 0;
        heavyReadyAt = 0;
    }

    public static boolean isHolding(Minecraft mc) {
        return mc.options.keyAttack.isDown() && mc.screen == null && mc.mouseHandler.isMouseGrabbed();
    }
}

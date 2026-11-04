package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.player.Input;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Durante una cinemática: se bloquean los clics (el movimiento sigue libre), se oculta la interfaz y se mueve la cámara. */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CinematicClientEvents {
    private CinematicClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            CinematicClient.renderTick(event.renderTickTime);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!CinematicClient.isActive()) {
            return;
        }
        Input input = event.getInput();
        if (input.forwardImpulse != 0.0F || input.leftImpulse != 0.0F) {
            CinematicClient.vanillaMoveSeen();
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (CinematicClient.isActive()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (CinematicClient.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (CinematicClient.isActive()) {
            event.setCanceled(true);
        }
    }

    /** Oculta la interfaz normal (vida, hotbar, mira...) pero deja las capas de este mod. */
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (CinematicClient.isActive()
                && !MundosDeAxelis.MODID.equals(event.getOverlay().id().getNamespace())) {
            event.setCanceled(true);
        }
    }
}

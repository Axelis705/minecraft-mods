package com.casino.client;

import com.casino.Casino;
import com.casino.registry.ModBlockEntities;
import com.casino.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Casino.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.LUCKY_WHEEL.get(), LuckyWheelRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.PLINKO.get(), PlinkoRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.SLOT.get(), SlotRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.BLACKJACK.get(), BlackjackRenderer::new);
        e.registerEntityRenderer(ModEntities.DEALER.get(), DealerRenderer::new);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("win_banner", WinBanner::render);
    }
}

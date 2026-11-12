package com.casino.client;

import com.casino.Casino;
import com.casino.registry.ModBlockEntities;
import com.casino.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
//#if MC >= 1.20.5
//$$ import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
//#else
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
//#endif
import com.casino.plat.Ids;
import com.casino.plat.client.G;
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
        e.registerBlockEntityRenderer(ModBlockEntities.POKER.get(), PokerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.ROULETTE.get(), RouletteRenderer::new);
        e.registerEntityRenderer(ModEntities.DEALER.get(), DealerRenderer::new);
        e.registerEntityRenderer(com.casino.registry.ModDecor.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    //#if MC >= 1.20.5
    //$$ @SubscribeEvent
    //$$ public static void onOverlays(AddGuiOverlayLayersEvent e) {
    //$$     e.getLayeredDraw().add(Ids.casino("win_banner"), (gg, delta) -> WinBanner.render(new G(gg), gg.guiWidth(), gg.guiHeight()));
    //$$ }
    //#else
    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("win_banner", (gui, gg, pt, w, h) -> WinBanner.render(new G(gg), w, h));
    }
    //#endif
}

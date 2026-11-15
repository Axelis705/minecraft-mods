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
        e.registerBlockEntityRenderer(com.casino.diamond.DiamondReg.SHOP_BE.get(), PrizeShopRenderer::new);
        e.registerBlockEntityRenderer(com.casino.diamond.DiamondReg.RACE_BE.get(), RaceScreenRenderer::new);
        e.registerBlockEntityRenderer(com.casino.diamond.DiamondReg.RECORD_BE.get(), RecordScreenRenderer::new);
        e.registerBlockEntityRenderer(com.casino.heist.HeistReg.CAMERA_BE.get(), HeistRenderers.Camera::new);
        e.registerBlockEntityRenderer(com.casino.heist.HeistReg.LASER_BE.get(), HeistRenderers.Laser::new);
        e.registerBlockEntityRenderer(com.casino.heist.HeistReg.VAULT_BE.get(), HeistRenderers.VaultDoor::new);
        e.registerBlockEntityRenderer(com.casino.west.WestReg.DOOR_BE.get(), WestRenderers.Door::new);
        e.registerBlockEntityRenderer(com.casino.west.WestReg.GATE_BE.get(), WestRenderers.Door::new);
        e.registerBlockEntityRenderer(com.casino.west.WestReg.PIANO_BE.get(), WestRenderers.Piano::new);
        e.registerEntityRenderer(ModEntities.DEALER.get(), DealerRenderer::new);
        e.registerEntityRenderer(com.casino.registry.ModDecor.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent e) {
        e.register(RaceClient.BET);
        e.register(MemberHud.TOGGLE);
        e.register(HeistHud.HELP);
    }

    //#if MC >= 1.20.5
    //$$ @SubscribeEvent
    //$$ public static void onOverlays(AddGuiOverlayLayersEvent e) {
    //$$     e.getLayeredDraw().add(Ids.casino("win_banner"), (gg, delta) -> {
    //$$         MemberHud.render(new G(gg), gg.guiWidth(), gg.guiHeight());
    //$$         HeistHud.render(new G(gg), gg.guiWidth(), gg.guiHeight());
    //$$         RaceHud.render(new G(gg), gg.guiWidth(), gg.guiHeight());
    //$$         WinBanner.render(new G(gg), gg.guiWidth(), gg.guiHeight());
    //$$     });
    //$$ }
    //#else
    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("win_banner", (gui, gg, pt, w, h) -> {
            MemberHud.render(new G(gg), w, h);
            HeistHud.render(new G(gg), w, h);
            RaceHud.render(new G(gg), w, h);
            WinBanner.render(new G(gg), w, h);
        });
    }
    //#endif
}

package com.celular.client;

import com.celular.Celular;
import com.celular.plat.client.G;
import net.minecraftforge.api.distmarker.Dist;
//#if MC >= 1.20.5
//$$ import com.celular.plat.Ids;
//$$ import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
//#else
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
//#endif
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Celular.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent e) {
        e.register(PhoneHud.OPEN);
        e.register(PhoneHud.ANSWER);
        e.register(PhoneHud.HANGUP);
        e.register(PhoneHud.MUSIC_TOGGLE);
        e.register(PhoneHud.MUSIC_NEXT);
        e.register(PhoneHud.MUSIC_PREV);
        e.register(PhoneHud.FLASHLIGHT);
    }

    //#if MC >= 1.20.5
    //$$ @SubscribeEvent
    //$$ public static void onOverlays(AddGuiOverlayLayersEvent e) {
    //$$     e.getLayeredDraw().add(Ids.mod("hud"), (gg, delta) -> PhoneHud.render(new G(gg), gg.guiWidth(), gg.guiHeight()));
    //$$ }
    //#else
    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("hud", (gui, gg, pt, w, h) -> PhoneHud.render(new G(gg), w, h));
    }
    //#endif
}

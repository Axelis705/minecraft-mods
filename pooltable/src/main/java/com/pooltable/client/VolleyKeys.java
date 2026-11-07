package com.pooltable.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pooltable.PoolTable;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Las teclas propias del voleibol (las demás son las de siempre: WASD para moverte y apuntar, espacio para saltar, clics para golpear). */
@Mod.EventBusSubscriber(modid = PoolTable.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VolleyKeys {
    public static final KeyMapping DIVE = new KeyMapping("key.pooltable.voley_dive", InputConstants.KEY_V, "key.categories.pooltable");
    public static final KeyMapping CAM = new KeyMapping("key.pooltable.voley_cam", InputConstants.KEY_C, "key.categories.pooltable");
    public static final KeyMapping MENU = new KeyMapping("key.pooltable.voley_menu", InputConstants.KEY_M, "key.categories.pooltable");

    private VolleyKeys() {
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(DIVE);
        e.register(CAM);
        e.register(MENU);
    }
}

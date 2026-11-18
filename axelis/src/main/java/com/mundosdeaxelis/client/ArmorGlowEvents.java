package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Añade la capa de brillo de las armaduras a los dos modelos de jugador (normal y delgado). */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArmorGlowEvents {
    private ArmorGlowEvents() {
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            EntityRenderer<?> r = event.getPlayerSkin(skin);
            if (r instanceof PlayerRenderer pr) {
                pr.addLayer(new ArmorGlowLayer(pr));
            }
        }
    }
}

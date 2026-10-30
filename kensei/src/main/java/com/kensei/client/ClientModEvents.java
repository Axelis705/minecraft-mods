package com.kensei.client;

import com.kensei.Kensei;
import com.kensei.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registros del cliente (bus del mod): las teclas (esquive y crítico) y las barras en pantalla. */
@Mod.EventBusSubscriber(modid = Kensei.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(KenseiKeys.DODGE);
        e.register(KenseiKeys.HEAVY);
        e.register(KenseiKeys.LOCK);
        e.register(KenseiKeys.NEXT_TARGET);
        e.register(KenseiKeys.DUEL);
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("energy", KatanaHud::render);
        e.registerAboveAll("duel", DuelHud::render);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(com.kensei.registry.ModEntities.RIVAL.get(), RivalRenderer::new);
    }

    @SubscribeEvent
    public static void onParticles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.SLASH.get(), SlashParticle.Provider::new);
        e.registerSpriteSet(ModParticles.LOCK.get(), LockParticle.Provider::new);
    }

    /** Carga el modelo de la katana guardada (no es un objeto, solo se dibuja en la espalda). */
    @SubscribeEvent
    public static void onModels(ModelEvent.RegisterAdditional e) {
        e.register(KatanaSheathLayer.MODEL);
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            EntityRenderer<? extends Player> r = e.getPlayerSkin(skin);
            if (r instanceof PlayerRenderer pr) {
                pr.addLayer(new KatanaSheathLayer(pr));
            }
        }
    }
}

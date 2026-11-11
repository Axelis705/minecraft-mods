package com.autoscoleccion.client;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registro de lo que solo existe en el cliente: los dibujantes de la máquina y la vitrina. */
@Mod.EventBusSubscriber(modid = AutosMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(ModBlockEntities.VENDING_MACHINE.get(), VendingMachineRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.DISPLAY_CASE.get(), DisplayCaseRenderer::new);
    }
}

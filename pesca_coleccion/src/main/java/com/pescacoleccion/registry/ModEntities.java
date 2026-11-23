package com.pescacoleccion.registry;

import com.pescacoleccion.PescaColeccion;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Atributos de los personajes del mod (vida, velocidad...). */
@Mod.EventBusSubscriber(modid = PescaColeccion.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    private ModEntities() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModRegistry.NILO.get(), com.pescacoleccion.nilo.Nilo.createAttributes().build());
    }
}

package com.coleccionista.registry;

import com.coleccionista.Coleccionista;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Atributos de los personajes del mod (vida, velocidad...). */
@Mod.EventBusSubscriber(modid = Coleccionista.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    private ModEntities() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModRegistry.BOT.get(), com.axeliscore.character.Persona.createAttributes().build());
        e.put(ModRegistry.BOTONES.get(), com.coleccionista.hotel.Botones.createAttributes().build());
        e.put(ModRegistry.RATA.get(), com.coleccionista.hotel.Rata.createAttributes().build());
    }
}

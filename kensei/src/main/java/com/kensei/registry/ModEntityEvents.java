package com.kensei.registry;

import com.kensei.Kensei;
import com.kensei.entity.RivalEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Los atributos (vida, velocidad...) de las criaturas del mod. */
@Mod.EventBusSubscriber(modid = Kensei.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {
    private ModEntityEvents() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.RIVAL.get(), RivalEntity.createAttributes().build());
    }
}

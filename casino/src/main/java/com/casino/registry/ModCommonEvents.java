package com.casino.registry;

import com.casino.Casino;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Casino.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCommonEvents {
    private ModCommonEvents() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent e) {
        // alcance para buscar caminos largos (las rondas de los guardias miden hasta 40 bloques)
        e.put(ModEntities.DEALER.get(), Mob.createMobAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 48.0).build());
    }
}

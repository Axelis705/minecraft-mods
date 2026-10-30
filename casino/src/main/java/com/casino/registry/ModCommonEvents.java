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
        e.put(ModEntities.DEALER.get(), Mob.createMobAttributes().build());
    }
}

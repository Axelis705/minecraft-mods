package com.mundosdeaxelis.event;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.world.ModDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** En las salas de los jefes nadie rompe ni coloca bloques salvo en creativo (para que no destruyan la obra por accidente). */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class HubProtection {
    private HubProtection() {
    }

    private static boolean inHub(LevelAccessor level) {
        return level instanceof net.minecraft.world.level.Level l
                && (ModDimensions.isForest(l.dimension())
                || ModDimensions.isForge(l.dimension()) || ModDimensions.isIce(l.dimension()));
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (inHub(event.getLevel()) && !player.isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (inHub(event.getLevel()) && !(event.getEntity() instanceof Player p && p.isCreative())) {
            event.setCanceled(true);
        }
    }
}

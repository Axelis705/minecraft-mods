package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.block.VaultLoot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Etapa 279: las piezas estrella (fragmentos de corazón, llaves, gemas vacías…) que caen al suelo brillan con un contorno que
 * se ve a través de las paredes, como el botín bueno de Diablo: así nunca se pierden entre la hierba, la lava o el lío de un
 * combate.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class BrilloBotin {
    private BrilloBotin() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof ItemEntity ie && isBright(ie.getItem())) {
            ie.setGlowingTag(true);
        }
    }

    /** Lo que brilla en el suelo: las piezas estrella y las gemas (vacías o no). */
    public static boolean isBright(net.minecraft.world.item.ItemStack st) {
        return VaultLoot.isStar(st) || st.getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Gema;
    }
}

package com.kensei.combat;

import com.kensei.item.KatanaItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;

public final class KatanaUtil {
    private KatanaUtil() {
    }

    public static boolean holdsKatana(LivingEntity e) {
        return e.getMainHandItem().getItem() instanceof KatanaItem || e.getOffhandItem().getItem() instanceof KatanaItem;
    }

    public static boolean mainHandKatana(LivingEntity e) {
        return e.getMainHandItem().getItem() instanceof KatanaItem;
    }

    /** ¿Está bloqueando con una katana (clic derecho sostenido)? */
    public static boolean guarding(LivingEntity e) {
        return e.isUsingItem() && e.getUseItem().getItem() instanceof KatanaItem;
    }
}

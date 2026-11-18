package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sin golpes de un solo toque: dentro de las dungeons y arenas de jefe, ningún golpe le quita a un jugador más del 40 % de su vida
 * máxima (ya contadas armadura y efectos). Así hacen falta al menos tres golpes seguidos para caer, y hay tiempo de reaccionar.
 * No toca /kill ni caer al vacío.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class BossHitCap {
    public static final float MAX_FRACTION = 0.40F;

    private BossHitCap() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !ModDimensions.isBoss(p.level().dimension())) {
            return;
        }
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float cap = p.getMaxHealth() * MAX_FRACTION;
        if (e.getAmount() > cap) {
            e.setAmount(cap);
        }
    }
}

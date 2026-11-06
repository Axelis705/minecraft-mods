package com.mundosdeaxelis.cartas;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Vida extra por el album: medio corazon cada 5 % y un corazon por categoria completa (ver AlbumLogic.HEALTH_PER_*). */
public final class AlbumBonuses {
    private static final UUID HP = UUID.fromString("5d7c1a62-3a52-4b8e-9a53-2d9f5f0c7a11");

    private AlbumBonuses() {
    }

    public static void apply(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) {
            return;
        }
        double extra = AlbumCaps.of(p).bonusHealth();
        AttributeModifier old = inst.getModifier(HP);
        if (old != null && Math.abs(old.getAmount() - extra) < 1e-9) {
            return;
        }
        inst.removeModifier(HP);
        if (extra > 0) {
            inst.addPermanentModifier(new AttributeModifier(HP, "Album de cartas", extra, AttributeModifier.Operation.ADDITION));
        }
    }
}

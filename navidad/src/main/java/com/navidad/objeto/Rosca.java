package com.navidad.objeto;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Rosca de Reyes: al comerla, a veces te sale el muñequito (y te toca invitar los tamales el 2 de febrero). */
public class Rosca extends Item {
    public Rosca(Properties p) {
        super(p);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack st, Level l, LivingEntity e) {
        if (!l.isClientSide && e instanceof ServerPlayer sp) {
            if (l.random.nextInt(5) == 0) {
                com.navidad.Logros.dar(sp, "rosca");
                for (ServerPlayer o : sp.serverLevel().players()) {
                    if (o.distanceTo(sp) < 32) {
                        o.sendSystemMessage(Component.translatable("navidad.rosca.muneco", sp.getDisplayName()).withStyle(ChatFormatting.GOLD));
                    }
                }
                Premios.confeti(sp.serverLevel(), sp.getX(), sp.getY() + 1.6, sp.getZ(), false);
            } else {
                sp.displayClientMessage(Component.translatable("navidad.rosca.nada"), true);
            }
        }
        return super.finishUsingItem(st, l, e);
    }
}

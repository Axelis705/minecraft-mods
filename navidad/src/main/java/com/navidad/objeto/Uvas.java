package com.navidad.objeto;

import com.navidad.AnioNuevo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Las uvas de Año Nuevo: se comen rapidito (una por campanada). Durante las 12 campanadas, cada una cuenta como un deseo. */
public class Uvas extends Item {
    public Uvas(Properties p) {
        super(p);
    }

    @Override
    public int getUseDuration(ItemStack s) {
        return 12;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack s, Level l, LivingEntity e) {
        if (!l.isClientSide && e instanceof ServerPlayer sp) {
            AnioNuevo.comioUva(sp);
        }
        return super.finishUsingItem(s, l, e);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.add(Component.translatable("navidad.uvas.ayuda").withStyle(ChatFormatting.GRAY));
    }
}

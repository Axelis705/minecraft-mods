package com.navidad.objeto;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * El regalo misterioso de antes (se abría en la mano). Axelis pidió que los regalos sean solo cajas que se ponen y se abren (y las que
 * se envuelven para regalar): este objeto ya no sale en ningún lado y, si alguien todavía tiene uno, en su inventario se vuelve una caja
 * de regalo envuelta con un premio al azar adentro (una por cada regalo que traía).
 */
public class RegaloMisterioso extends Item {
    public RegaloMisterioso(Properties p) {
        super(p);
    }

    @Override
    public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean seleccionado) {
        if (l.isClientSide || !(e instanceof ServerPlayer sp) || s.isEmpty()) {
            return;
        }
        int n = s.getCount();
        s.setCount(0);
        for (int i = 0; i < n; i++) {
            ItemStack caja = Premios.caja(sp.getRandom(), null, "Santa");
            if (!sp.getInventory().add(caja)) {
                sp.drop(caja, false);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.regalo.ahora_caja").withStyle(ChatFormatting.GRAY));
    }
}

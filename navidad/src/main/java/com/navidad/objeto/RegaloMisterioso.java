package com.navidad.objeto;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** El regalo misterioso: clic derecho para abrirlo (un premio común, raro o épico, con confeti). */
public class RegaloMisterioso extends Item {
    public RegaloMisterioso(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack s = p.getItemInHand(h);
        if (!l.isClientSide && p instanceof ServerPlayer sp) {
            Premios.Rareza rz = Premios.rareza(l.random);
            l.playSound(null, p.blockPosition(), com.navidad.registry.ModRegistry.REGALO_ABRE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1F);
            Premios.dar(sp, Premios.sacar(rz, l.random), rz, "navidad.regalo.te_toco");
            if (!p.getAbilities().instabuild) {
                s.shrink(1);
            }
            p.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(s, l.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.regalo.ayuda").withStyle(ChatFormatting.GRAY));
    }
}

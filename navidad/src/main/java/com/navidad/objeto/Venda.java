package com.navidad.objeto;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** La venda de la piñata: te tapa los ojos 15 segundos (como en las posadas, para pegarle a ciegas). */
public class Venda extends Item {
    public Venda(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        if (!l.isClientSide) {
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 15 * 20, 0, false, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 6 * 20, 0, false, false, false));
            l.playSound(null, p.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1F, 1.2F);
            p.displayClientMessage(Component.translatable("navidad.venda.puesta"), true);
            p.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(h), l.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.venda.ayuda").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

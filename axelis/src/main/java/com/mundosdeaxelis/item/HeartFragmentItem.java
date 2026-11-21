package com.mundosdeaxelis.item;

import com.mundosdeaxelis.world.Corazones;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Fragmento de corazón (etapa 269): clic derecho para absorberlo; cuatro = un corazón más de vida máxima para siempre. */
public class HeartFragmentItem extends Item {
    public HeartFragmentItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (!Corazones.absorb(sp)) {
                return InteractionResultHolder.fail(held);
            }
            if (!sp.getAbilities().instabuild) {
                held.shrink(1);
            }
            sp.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.heart_fragment").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.heart_fragment.use", Corazones.PER_HEART).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.heart_fragment.cap").withStyle(ChatFormatting.DARK_GRAY));
    }
}

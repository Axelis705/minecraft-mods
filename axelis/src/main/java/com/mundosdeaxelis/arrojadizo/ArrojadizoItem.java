package com.mundosdeaxelis.arrojadizo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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

/** Un arrojadizo (etapa 273): clic derecho para lanzarlo. Se apilan de 16 y tienen una pequeña espera entre lanzamiento y lanzamiento. */
public class ArrojadizoItem extends Item {
    public final Arrojadizos.Tipo tipo;

    public ArrojadizoItem(Arrojadizos.Tipo tipo) {
        super(new Item.Properties().stacksTo(16).rarity(tipo.tier >= 5 ? Rarity.RARE : tipo.tier >= 3 ? Rarity.UNCOMMON : Rarity.COMMON));
        this.tipo = tipo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            ArrojadizoEntity e = new ArrojadizoEntity(level, p);
            e.setItem(st.copyWithCount(1));
            e.shootFromRotation(p, p.getXRot(), p.getYRot(), -8.0F, 1.1F, 0.6F);
            level.addFreshEntity(e);
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.mundosdeaxelis.encargo.Encargos.progress(sp, com.mundosdeaxelis.encargo.Encargos.Tipo.ARROJADIZOS, 1);
            }
        }
        p.getCooldowns().addCooldown(this, 14);
        p.awardStat(Stats.ITEM_USED.get(this));
        if (!p.getAbilities().instabuild) {
            st.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mundos_de_axelis." + tipo.id).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.mundos_de_axelis.arrojadizo.use").withStyle(ChatFormatting.DARK_AQUA));
    }
}

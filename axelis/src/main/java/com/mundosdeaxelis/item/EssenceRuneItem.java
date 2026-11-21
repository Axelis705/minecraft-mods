package com.mundosdeaxelis.item;

import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.table.Essence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Runa de Esencia I a VIII (etapa 269, como las runas doradas de Elden Ring): sale en los cofres de las estructuras de su tier y se
 * rompe (clic derecho) para recibir Esencia, la moneda de todo (llaves, cajas, subir la Mesa y el equipo). Agachado: rompe todas
 * las de la pila. Tiene el color de la rareza de su tier.
 */
public class EssenceRuneItem extends Item {
    /** Esencia que da cada runa según su tier (1 a 8). */
    public static final int[] VALUE = {0, 25, 40, 60, 90, 130, 180, 250, 340};
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII"};

    public final int tier;

    public EssenceRuneItem(int tier) {
        super(new Item.Properties().stacksTo(64).rarity(Tier.values()[tier - 1].rarity));
        this.tier = tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.mundos_de_axelis.essence_rune", ROMAN[tier]).withStyle(Tier.values()[tier - 1].style());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier >= 5;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            int n = player.isShiftKeyDown() ? held.getCount() : 1;
            int gain = VALUE[tier] * n;
            Essence.add(sp, gain);
            Essence.sync(sp);
            com.mundosdeaxelis.world.Titles.lootEssence(sp, gain);
            ServerLevel sl = sp.serverLevel();
            sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F + 0.06F * tier);
            sl.playSound(null, sp.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.8F);
            sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.1, sp.getZ(), 10 + 2 * tier, 0.3, 0.5, 0.3, 0.06);
            sl.sendParticles(ParticleTypes.WAX_OFF, sp.getX(), sp.getY() + 1.0, sp.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
            if (!sp.getAbilities().instabuild) {
                held.shrink(n);
            }
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.essence_rune").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.essence_rune.use", VALUE[tier]).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.essence_rune.all").withStyle(ChatFormatting.DARK_GRAY));
    }
}

package com.casino.item;

import com.casino.client.ClientHooks;
import com.casino.game.ScratchCards;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Un boleto de rasca y gana. El resultado (los 6 símbolos) se sortea al comprarlo y se guarda en el objeto; con clic derecho se abre el
 * boleto y se rasca con el ratón. Cada casilla descubierta se anota en el boleto (máscara de 6 bits); al descubrir las 6, el servidor
 * paga el premio (si lo hay) y marca el boleto como cobrado. El premio se calcula siempre desde los símbolos, no se guarda aparte.
 */
public class ScratchCardItem extends Item {
    public static final String SYM = "Sym";
    public static final String MASK = "Mask";
    public static final String CLAIMED = "Claimed";
    public static final int ALL = (1 << ScratchCards.SPOTS) - 1;

    public ScratchCardItem(Properties props) {
        super(props);
    }

    /** Un boleto nuevo con el resultado ya sorteado. */
    public static ItemStack create(net.minecraft.world.item.Item item, Random rnd) {
        ItemStack st = new ItemStack(item);
        init(st, rnd);
        return st;
    }

    public static void init(ItemStack st, Random rnd) {
        CompoundTag t = st.getOrCreateTag();
        t.putIntArray(SYM, ScratchCards.generate(rnd));
        t.putInt(MASK, 0);
        t.putBoolean(CLAIMED, false);
    }

    public static boolean hasCard(ItemStack st) {
        return st.hasTag() && st.getTag().getIntArray(SYM).length == ScratchCards.SPOTS;
    }

    public static int[] symbols(ItemStack st) {
        int[] s = hasCard(st) ? st.getTag().getIntArray(SYM) : new int[ScratchCards.SPOTS];
        return s.clone();
    }

    public static int mask(ItemStack st) {
        return st.hasTag() ? st.getTag().getInt(MASK) & ALL : 0;
    }

    public static boolean claimed(ItemStack st) {
        return st.hasTag() && st.getTag().getBoolean(CLAIMED);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return claimed(stack) ? Rarity.COMMON : Rarity.UNCOMMON;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // un boleto sacado del menú creativo todavía no tiene resultado: se sortea la primera vez que está en un inventario
        if (!level.isClientSide && !hasCard(stack)) {
            init(stack, new Random());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide && hasCard(st)) {
            boolean off = hand == InteractionHand.OFF_HAND;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openScratchCard(off));
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (!hasCard(stack)) {
            tip.add(Component.translatable("casino.scratch.tip_new").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (claimed(stack)) {
            int prize = ScratchCards.prizeOf(symbols(stack));
            tip.add(Component.translatable(prize > 0 ? "casino.scratch.tip_paid" : "casino.scratch.tip_lost", prize).withStyle(prize > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        } else if (mask(stack) == 0) {
            tip.add(Component.translatable("casino.scratch.tip_unscratched").withStyle(ChatFormatting.YELLOW));
            tip.add(Component.translatable("casino.scratch.tip_use").withStyle(ChatFormatting.GRAY));
        } else {
            tip.add(Component.translatable("casino.scratch.tip_partial", Integer.bitCount(mask(stack)), ScratchCards.SPOTS).withStyle(ChatFormatting.YELLOW));
        }
    }
}

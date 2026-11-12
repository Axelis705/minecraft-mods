package com.casino.item;

import com.casino.plat.Net;
import com.casino.plat.Nbt;
import com.casino.plat.ItemTag;
import com.casino.client.ClientHooks;
import com.casino.game.ScratchCards;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;

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
        int[] sym = ScratchCards.generate(rnd);
        ItemTag.edit(st, t -> {
            t.putIntArray(SYM, sym);
            t.putInt(MASK, 0);
            t.putBoolean(CLAIMED, false);
        });
    }

    public static boolean hasCard(ItemStack st) {
        return Nbt.ia(ItemTag.get(st), SYM).length == ScratchCards.SPOTS;
    }

    public static int[] symbols(ItemStack st) {
        int[] s = hasCard(st) ? Nbt.ia(ItemTag.get(st), SYM) : new int[ScratchCards.SPOTS];
        return s.clone();
    }

    public static int mask(ItemStack st) {
        return Nbt.i(ItemTag.get(st), MASK) & ALL;
    }

    public static boolean claimed(ItemStack st) {
        return Nbt.b(ItemTag.get(st), CLAIMED);
    }

    //#if MC < 1.20.5
    @Override
    public Rarity getRarity(ItemStack stack) {
        return claimed(stack) ? Rarity.COMMON : Rarity.UNCOMMON;
    }
    //#endif

    // un boleto sacado del menú creativo todavía no tiene resultado: se sortea la primera vez que está en un inventario
    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
    //$$     if (!hasCard(stack)) {
    //$$         init(stack, new Random());
    //$$     }
    //$$ }
    //#else
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && !hasCard(stack)) {
            init(stack, new Random());
        }
    }
    //#endif

    private void openCard(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide() && hasCard(st)) {
            boolean off = hand == InteractionHand.OFF_HAND;
            Net.onClient(() -> ClientHooks.openScratchCard(off));
        }
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$$     openCard(level, player, hand);
    //$$     return InteractionResult.SUCCESS;
    //$$ }
    //#else
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        openCard(level, player, hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     tips(stack, add);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tips(stack, tip::add);
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tips(stack, tip::add);
    }
    //#endif

    private static void tips(ItemStack stack, java.util.function.Consumer<Component> tip) {
        if (!hasCard(stack)) {
            tip.accept(Component.translatable("casino.scratch.tip_new").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (claimed(stack)) {
            int prize = ScratchCards.prizeOf(symbols(stack));
            tip.accept(Component.translatable(prize > 0 ? "casino.scratch.tip_paid" : "casino.scratch.tip_lost", prize).withStyle(prize > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        } else if (mask(stack) == 0) {
            tip.accept(Component.translatable("casino.scratch.tip_unscratched").withStyle(ChatFormatting.YELLOW));
            tip.accept(Component.translatable("casino.scratch.tip_use").withStyle(ChatFormatting.GRAY));
        } else {
            tip.accept(Component.translatable("casino.scratch.tip_partial", Integer.bitCount(mask(stack)), ScratchCards.SPOTS).withStyle(ChatFormatting.YELLOW));
        }
    }
}

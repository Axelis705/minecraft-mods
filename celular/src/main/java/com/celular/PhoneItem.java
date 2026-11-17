package com.celular;

import com.celular.client.ClientHooks;
import com.celular.plat.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** El celular: clic derecho para desbloquearlo y usar sus apps. */
public class PhoneItem extends Item {
    public final int color;

    public PhoneItem(Properties props, int color) {
        super(props);
        this.color = color;
    }

    private void open(Level level) {
        if (level.isClientSide()) {
            Net.onClient(() -> ClientHooks.openPhone(color));
        }
    }

    /** La pose del brazo en tercera persona (escribiendo o alumbrando) según la pantalla que lleva el celular. */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.celular.client.PhonePoses.EXT);
    }

    /** Cambiar de app cambia el número de pantalla del celular: eso no debe hacer que la mano lo vuelva a sacar. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$$     open(level);
    //$$     return InteractionResult.SUCCESS;
    //$$ }
    //#else
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        open(level);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     add.accept(Component.translatable("celular.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tip.add(Component.translatable("celular.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("celular.tooltip").withStyle(ChatFormatting.GRAY));
    }
    //#endif
}

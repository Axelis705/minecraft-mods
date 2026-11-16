package com.casino.west;

import com.casino.plat.ItemTag;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
//#if MC < 1.21.2
import net.minecraft.world.InteractionResultHolder;
//#endif

/**
 * Revólver de feria: lo presta la galería de tiro mientras juegas. Clic derecho = disparo (lo revisa el servidor). Fuera de una
 * partida no sirve y se devuelve solo.
 */
public class RevolverItem extends Item {
    public RevolverItem(Properties props) {
        super(props);
    }

    private static BlockPos gallery(ItemStack st) {
        var t = ItemTag.get(st);
        if (!Nbt.has(t, "GalleryY")) {
            return null;
        }
        return new BlockPos(Nbt.i(t, "GalleryX"), Nbt.i(t, "GalleryY"), Nbt.i(t, "GalleryZ"));
    }

    private static void fire(Level level, Player player, ItemStack st) {
        BlockPos pos = gallery(st);
        if (level.isClientSide()) {
            com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.recoil(player));
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof GalleryBE be && be.isShooter(sp.getUUID())) {
            be.shoot(sp);
        } else {
            st.setCount(0);
            Plat.msg(sp, Component.translatable("casino.gallery.gun_back").withStyle(ChatFormatting.GRAY), true);
        }
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$$     fire(level, player, player.getItemInHand(hand));
    //$$     return InteractionResult.SUCCESS;
    //$$ }
    //#else
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        fire(level, player, st);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide());
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     add.accept(Component.translatable("casino.gallery.gun_tip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tip.add(Component.translatable("casino.gallery.gun_tip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("casino.gallery.gun_tip").withStyle(ChatFormatting.GRAY));
    }
    //#endif
}

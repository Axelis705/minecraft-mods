package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Mochila de bolsillo: tiene su propio inventario de 27 espacios, guardado dentro del objeto. */
public class PocketBackpackItem extends Item {
    public PocketBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        BackpackContainer container = new BackpackContainer(stack);
        sp.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new BackpackMenu(id, inv, container, stack),
                Component.translatable("item.mundos_de_axelis.pocket_backpack")));
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 0.9F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.8F);
        sl.sendParticles(Fx.spark(0xE3C8FF, 0.9F), sp.getX(), sp.getY() + 1.1, sp.getZ(), 6, 0.35, 0.4, 0.35, 0.0);
        sl.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.2, sp.getZ(), 10, 0.5, 0.4, 0.5, 0.2);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xD8B8FF, 18);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "pocket_backpack", 2);
        int n = BackpackContainer.filledSlots(stack);
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.pocket_backpack.slots", n, BackpackContainer.SIZE)
                .withStyle(ChatFormatting.AQUA));
    }
}

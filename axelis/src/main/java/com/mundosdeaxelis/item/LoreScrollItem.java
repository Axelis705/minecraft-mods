package com.mundosdeaxelis.item;

import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.world.Cronicas;
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

/**
 * Pergamino de la crónica (etapa 269): clic derecho para leerlo (se abre como un libro). La primera vez queda apuntado en tu
 * Archivo; completar los 3 de un tier da un premio. No se gasta: puedes volver a leerlo cuando quieras.
 */
public class LoreScrollItem extends Item {
    public LoreScrollItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = Cronicas.idOf(stack);
        int t = Cronicas.tierOf(id);
        if (t == 0) {
            return super.getName(stack);
        }
        return Component.translatable("item.mundos_de_axelis.lore_scroll.named", Component.translatable("lore.mundos_de_axelis." + id + ".title"))
                .withStyle(Tier.values()[t - 1].style());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        String id = Cronicas.idOf(held);
        if (Cronicas.tierOf(id) == 0) {
            return InteractionResultHolder.pass(held);
        }
        if (level.isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.mundosdeaxelis.client.LoreClient.open(id));
        } else if (player instanceof ServerPlayer sp) {
            Cronicas.read(sp, id);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int t = Cronicas.tierOf(Cronicas.idOf(stack));
        if (t > 0) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.lore_scroll.of", Component.translatable("cronica.mundos_de_axelis." + t)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.lore_scroll.use").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.lore_scroll.reward").withStyle(ChatFormatting.DARK_GRAY));
    }
}

package com.autoscoleccion.item;

import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.game.Rarity;
import com.autoscoleccion.server.Unboxing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/** Una caja cerrada de una serie. Clic derecho para abrirla (con la animación). */
public class BoxItem extends Item {
    public final int box;

    public BoxItem(int box, Properties props) {
        super(props);
        this.box = box;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Caja " + Boxes.NAME[box]).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Boxes.COLOR[box])).withBold(box == 2));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            Unboxing.open(sp, hand, box);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(Boxes.TAGLINE[box] + " · " + Boxes.size(box) + " autos posibles").withStyle(ChatFormatting.GRAY));
        for (Rarity r : Rarity.values()) {
            double p = 0;
            for (CarDef c : Cars.all()) {
                if (c.rarity == r) {
                    p += Boxes.chance(c, box);
                }
            }
            if (p > 0) {
                tip.add(Component.literal(String.format(Locale.ROOT, "  %s  %.1f%%", r.label, p * 100))
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(r.rgb))));
            }
        }
        tip.add(Component.literal("Clic derecho para abrir").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}

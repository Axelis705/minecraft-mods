package com.casino.item;

import com.casino.plat.Plat;
import com.casino.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Tarjeta VIP del casino. Plata (nivel 1) abre la zona VIP; Oro (nivel 2) además el penthouse y la azotea. Se compra en el cajero. */
public class VipCardItem extends Item {
    public final int level;

    public VipCardItem(Properties props, int level) {
        super(props);
        this.level = level;
    }

    /** El nivel VIP más alto que lleva el jugador (0 = ninguno). Operadores y modo creativo pasan siempre. */
    public static int tier(Player p) {
        int best = 0;
        for (ItemStack s : Plat.mainAndOffhand(p.getInventory())) {
            if (s.getItem() instanceof VipCardItem c) {
                best = Math.max(best, c.level);
            }
        }
        return best;
    }

    public static boolean canEnter(Player p, int need) {
        return need <= 0 || p.isCreative() || p.isSpectator() || tier(p) >= need;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return level >= 2;
    }

    private void tips(java.util.function.Consumer<Component> add) {
        add.accept(Component.translatable(level >= 2 ? "casino.vip.tooltip_gold" : "casino.vip.tooltip_silver").withStyle(ChatFormatting.GRAY));
    }

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     tips(add);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tips(tip::add);
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tips(tip::add);
    }
    //#endif

    static Item silver() {
        return ModItems.VIP_SILVER.get();
    }
}

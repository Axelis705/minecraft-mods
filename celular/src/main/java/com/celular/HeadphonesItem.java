package com.celular;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Audífonos: se ponen en la cabeza (clic derecho) y se ven en el jugador. Con ellos puestos la música es solo tuya: el altavoz no suena
 * para los demás y no oyes los altavoces de otros (el Jam sí, porque es de todos los que están en él).
 */
public class HeadphonesItem extends Item
        //#if MC < 1.21.2
        implements net.minecraft.world.item.Equipable
        //#endif
{
    public HeadphonesItem(Properties props) {
        super(props);
    }

    /** ¿Lleva audífonos puestos? */
    public static boolean wearing(LivingEntity e) {
        return e != null && e.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof HeadphonesItem;
    }

    //#if MC < 1.21.2
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         java.util.function.Consumer<Component> add, TooltipFlag flag) {
    //$$     add.accept(Component.translatable("celular.headphones.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
    //$$     tip.add(Component.translatable("celular.headphones.tooltip").withStyle(ChatFormatting.GRAY));
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("celular.headphones.tooltip").withStyle(ChatFormatting.GRAY));
    }
    //#endif
}

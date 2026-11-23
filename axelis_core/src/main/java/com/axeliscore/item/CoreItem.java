package com.axeliscore.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * Objeto base de los mods del Core: la ayuda al pasar el ratón y el clic derecho se escriben una sola vez y funcionan igual en
 * todas las versiones (cada versión de Minecraft cambió cómo se llaman).
 * <pre>
 *     public class LibroItem extends CoreItem {
 *         protected void tooltip(ItemStack st, Consumer&lt;Component&gt; add) { add.accept(Component.translatable("...")); }
 *         protected boolean onUse(Level level, Player p, InteractionHand hand) { ...; return true; }
 *     }
 * </pre>
 */
public class CoreItem extends Item {
    public CoreItem(Properties props) {
        super(props);
    }

    /** Líneas de ayuda debajo del nombre. */
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
    }

    /** Clic derecho con el objeto (en el cliente y en el servidor). true = se usó (el brazo se mueve y no hace lo normal). */
    protected boolean onUse(Level level, Player player, InteractionHand hand) {
        return false;
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$$     return onUse(level, player, hand) ? InteractionResult.SUCCESS : super.use(level, player, hand);
    //$$ }
    //#else
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (onUse(level, player, hand)) {
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        }
        return super.use(level, player, hand);
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         Consumer<Component> add, net.minecraft.world.item.TooltipFlag flag) {
    //$$     tooltip(stack, add);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
    //$$     tooltip(stack, tip::add);
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip(stack, tip::add);
    }
    //#endif
}

package com.mundosdeaxelis.item;

import com.mundosdeaxelis.cape.CapeDefs;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiered;
import com.mundosdeaxelis.relic.Equipment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Una capa: se lleva en la ranura "Capa" del menú de equipo (tecla G). Clic derecho con ella en la mano la equipa. */
public class CapeItem extends Item implements Tiered {
    private final CapeDefs.Def def;

    public CapeItem(CapeDefs.Def def) {
        super(new Item.Properties().stacksTo(1).rarity(def.tier().rarity));
        this.def = def;
    }

    public CapeDefs.Def def() {
        return def;
    }

    @Override
    public Tier tier() {
        return def.tier();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(held);
        }
        ItemStack old = Equipment.get(sp).capes[0];
        ItemStack one = held.copy();
        one.setCount(1);
        Equipment.get(sp).capes[0] = one;
        Equipment.save(sp);
        Equipment.sync(sp);
        ItemStack back = old.isEmpty() ? ItemStack.EMPTY : old;
        held.shrink(1);
        if (!back.isEmpty()) {
            if (held.isEmpty()) {
                player.setItemInHand(hand, back);
            } else if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 0.8F, 1.1F);
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.cape_equipped",
                Component.translatable(getDescriptionId())).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.tier", com.mundosdeaxelis.crate.Tiers.shown(this).label()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.cape").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.cape_from",
                Component.translatable(def.crate().titleKey()).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(def.crate().color)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.cape_armor", def.armor()).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.cape_use").withStyle(ChatFormatting.DARK_GRAY));
    }
}

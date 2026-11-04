package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class GearShovel extends ShovelItem implements GearTool {
    private final Ability ability;
    private final com.mundosdeaxelis.crate.Tier rarity;

    public GearShovel(Ability ability, net.minecraft.world.item.Tier tier, float dmg, float spd, Item.Properties props) {
        super(tier, dmg, spd, props);
        this.ability = ability;
        this.rarity = com.mundosdeaxelis.crate.Tiers.of(ability.id);
    }

    @Override
    public GearDef def() {
        return null;
    }

    @Override
    public Ability ability() {
        return ability;
    }

    @Override
    public com.mundosdeaxelis.crate.Tier tier() {
        return rarity;
    }

    @Override
    public Component getName(ItemStack stack) {
        return com.mundosdeaxelis.crate.Tiers.shown(this).paint(super.getName(stack));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return rarity.ordinal() >= com.mundosdeaxelis.crate.Tier.EPIC.ordinal() || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        Gear.tooltip(this, tooltip);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (selected && entity instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            Gear.passive(this, sp, sl);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ability.hasActive() && player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            if (Gear.tryActive(this, sp, sl, stack)) {
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (ability.hasActive() && level.isClientSide) {
            return player.getCooldowns().isOnCooldown(this) ? InteractionResultHolder.fail(stack) : InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // con habilidad activa: agachado + clic derecho en un bloque la usa (sin agacharse sigue el uso normal: pelar troncos, allanar...)
        if (ability.hasActive() && ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return super.useOn(ctx);
    }
}

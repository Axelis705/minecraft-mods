package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;
import net.minecraft.network.chat.Component;
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

/** Pala de la escalera de un set: sus capacidades salen de la ficha ({@link GearDef}). */
public class LadderShovel extends ShovelItem implements GearTool {
    private final GearDef def;

    public LadderShovel(GearDef def) {
        super(new GearMaterial(def), 2.0F, def.attackSpeed(), GearItems.props(def));
        this.def = def;
    }

    @Override
    public GearDef def() {
        return def;
    }

    @Override
    public Ability ability() {
        return null;
    }

    @Override
    public Tier tier() {
        return def.tier();
    }

    @Override
    public Component getName(ItemStack stack) {
        return GearItems.name(def, super.getName(stack));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return GearItems.foil(def) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Gear.tooltip(this, tooltip);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        GearItems.tick(this, level, entity, selected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> r = GearItems.use(this, def, level, player, hand);
        return r != null ? r : super.use(level, player, hand);
    }
}

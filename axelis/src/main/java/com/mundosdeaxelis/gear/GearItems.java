package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Piezas comunes de las herramientas de la escalera (las cuatro clases solo cambian de padre). */
final class GearItems {
    private GearItems() {
    }

    static Item.Properties props(GearDef d) {
        Item.Properties p = new Item.Properties().rarity(d.tier().rarity);
        if (d.set().fireproof) {
            p.fireResistant();
        }
        return p;
    }

    static Component name(GearDef d, Component base) {
        return Tiers.shown(d.id()).paint(base);
    }

    static boolean foil(GearDef d) {
        return d.tier().ordinal() >= Tier.EPIC.ordinal();
    }

    static boolean hasActive(GearDef d) {
        return d.has(Perk.SKILL) || d.has(Perk.RADAR);
    }

    static void tick(GearTool t, Level level, Entity entity, boolean selected) {
        if (selected && entity instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            Gear.passive(t, sp, sl);
        }
    }

    /** Clic derecho: habilidad activa si la tiene. Devuelve null para dejar el uso normal de la herramienta. */
    static InteractionResultHolder<ItemStack> use(GearTool t, GearDef d, Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasActive(d)) {
            return null;
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            return Gear.tryActive(t, sp, sl, stack) ? InteractionResultHolder.sidedSuccess(stack, false) : InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return player.getCooldowns().isOnCooldown(stack.getItem()) ? InteractionResultHolder.fail(stack) : InteractionResultHolder.success(stack);
        }
        return null;
    }
}

package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

/**
 * Anillo del Herrero (se crea en la Mesa del Artífice): repara poco a poco todo lo que llevas puesto o en las manos.
 * Activo (R): Temple, repara de golpe un porcentaje de cada pieza y te da Absorción.
 */
public class SmithRingItem extends RelicItem {
    public SmithRingItem() {
        super("smith_ring", 0xFF9A3C, 20 * 150, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 40;
    }

    /** Piezas que se pueden reparar: mano principal, mano secundaria y las cuatro de armadura. */
    private static List<ItemStack> worn(ServerPlayer sp) {
        List<ItemStack> l = new ArrayList<>(6);
        l.add(sp.getMainHandItem());
        l.add(sp.getOffhandItem());
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            l.add(sp.getItemBySlot(s));
        }
        return l;
    }

    private static boolean damaged(ItemStack s) {
        return !s.isEmpty() && s.isDamageableItem() && s.isDamaged() && !(s.getItem() instanceof RelicItem);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        // cada 2 segundos arregla un poco la pieza más gastada (1, 1, 2, 2 y 3 puntos según el nivel)
        ItemStack worst = ItemStack.EMPTY;
        float worstFrac = 0.0F;
        for (ItemStack s : worn(sp)) {
            if (damaged(s)) {
                float frac = s.getDamageValue() / (float) s.getMaxDamage();
                if (frac > worstFrac) {
                    worstFrac = frac;
                    worst = s;
                }
            }
        }
        if (worst.isEmpty()) {
            return;
        }
        int amount = 1 + (equippedLevel - 1) / 2;
        worst.setDamageValue(Math.max(0, worst.getDamageValue() - amount));
        sl.sendParticles(Fx.spark(0xFFB347, 0.5F), sp.getX(), sp.getY() + 1.0, sp.getZ(), 2, 0.35, 0.45, 0.35, 0.0);
        if (time % 120 == 0) {
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.25F, 1.7F);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        List<ItemStack> hurt = new ArrayList<>();
        for (ItemStack s : worn(sp)) {
            if (damaged(s)) {
                hurt.add(s);
            }
        }
        if (hurt.isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.smith_ring_nothing").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        int lv = level(stack);
        double fraction = 0.25 + 0.05 * (lv - 1);
        for (ItemStack s : hurt) {
            int amount = Math.max(1, (int) Math.round(s.getMaxDamage() * fraction));
            s.setDamageValue(Math.max(0, s.getDamageValue() - amount));
        }
        sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 10, lv >= 3 ? 1 : 0, true, true, true));
        double x = sp.getX(), y = sp.getY(), z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.25F);
        Fx.sound(sl, x, y, z, SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.0F);
        Fx.ring(sl, Fx.spark(0xFFB347, 0.8F), x, y + 1.0, z, 1.1, 18, 0.0);
        Fx.ring(sl, Fx.ember(0xFF7A20, 0.7F), x, y + 0.4, z, 0.8, 14, 0.4);
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.smith_ring_temper", hurt.size()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}

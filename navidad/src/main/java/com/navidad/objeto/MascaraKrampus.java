package com.navidad.objeto;

import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * La máscara del Krampus (de su botín): cara de cuero negro con cuernos, colmillos y la lengua de fuera. Se pone en la cabeza (clic
 * derecho o en el espacio del casco). Con ella, los krampusillos creen que eres de los suyos y no te atacan, y el apagón del Krampus no
 * te deja a oscuras.
 */
public class MascaraKrampus extends Item implements Equipable {
    public MascaraKrampus(Properties p) {
        super(p);
    }

    /** ¿Trae puesta la máscara? */
    public static boolean lleva(LivingEntity e) {
        return e != null && e.getItemBySlot(EquipmentSlot.HEAD).is(ModRegistry.MASCARA_KRAMPUS.get());
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        return swapWithEquipmentSlot(this, l, p, h);
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.navidad.mascara_krampus.ayuda").withStyle(ChatFormatting.GRAY));
    }
}

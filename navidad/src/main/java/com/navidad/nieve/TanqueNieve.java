package com.navidad.nieve;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * El tanque de nieve: se pone en la espalda (va en el lugar del peto, como las alas de élitro) y guarda la nieve del lanzanieves en sus
 * {@link #RANURAS} ranuras: bolas de nieve (1 tiro cada una) o bloques de nieve (4 tiros cada uno), más la nieve suelta que aspira el
 * lanzanieves del suelo. Agachado y con clic derecho (o con la tecla del tanque si lo traes puesto) se abren sus ranuras.
 */
public class TanqueNieve extends Item implements Equipable {
    public static final int RANURAS = 6, MAX_SUELTA = 256;
    /** Lo que se considera "lleno" para dibujar la ventanita (la nieve puede ser más). */
    public static final int LLENO = 192;

    public TanqueNieve(Properties p) {
        super(p);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        if (p.isShiftKeyDown()) {
            if (p instanceof ServerPlayer sp) {
                TanqueMenu.abrir(sp, h == InteractionHand.MAIN_HAND ? TanqueMenu.MANO : TanqueMenu.MANO_IZQ);
            }
            return InteractionResultHolder.sidedSuccess(st, l.isClientSide);
        }
        return swapWithEquipmentSlot(this, l, p, h);
    }

    // ------------------------------------------------------------------ lo que tiene adentro

    public static NonNullList<ItemStack> ranuras(ItemStack tanque) {
        NonNullList<ItemStack> l = NonNullList.withSize(RANURAS, ItemStack.EMPTY);
        CompoundTag t = tanque.getTag();
        if (t != null && t.contains("Items")) {
            ContainerHelper.loadAllItems(t, l);
        }
        return l;
    }

    public static void guardar(ItemStack tanque, NonNullList<ItemStack> l) {
        ContainerHelper.saveAllItems(tanque.getOrCreateTag(), l, true);
    }

    public static int suelta(ItemStack tanque) {
        CompoundTag t = tanque.getTag();
        return t == null ? 0 : t.getInt("Suelta");
    }

    public static void setSuelta(ItemStack tanque, int n) {
        tanque.getOrCreateTag().putInt("Suelta", Math.max(0, Math.min(MAX_SUELTA, n)));
    }

    /** Cuántos tiros vale un objeto en el tanque (0 = no se puede meter). */
    public static int tiros(ItemStack st) {
        if (st.is(Items.SNOWBALL)) {
            return 1;
        }
        if (st.is(Items.SNOW_BLOCK)) {
            return 4;
        }
        return 0;
    }

    /** Cuántos tiros tiene el tanque. */
    public static int carga(ItemStack tanque) {
        int n = suelta(tanque);
        for (ItemStack s : ranuras(tanque)) {
            n += tiros(s) * s.getCount();
        }
        return n;
    }

    /** Gasta {@code n} tiros (primero la nieve suelta, luego las bolas y al final muele bloques). Devuelve cuántos pudo gastar. */
    public static int gastar(ItemStack tanque, int n) {
        int hechos = 0;
        int s = suelta(tanque);
        int de = Math.min(s, n);
        s -= de;
        hechos += de;
        NonNullList<ItemStack> l = ranuras(tanque);
        boolean cambio = false;
        for (int pasada = 0; pasada < 2 && hechos < n; pasada++) {
            for (ItemStack st : l) {
                while (hechos < n && !st.isEmpty() && (pasada == 0 ? st.is(Items.SNOWBALL) : st.is(Items.SNOW_BLOCK))) {
                    st.shrink(1);
                    cambio = true;
                    if (pasada == 0) {
                        hechos++;
                    } else {
                        // un bloque de nieve molido da 4 tiros: uno ahora y el resto queda suelto
                        int falta = n - hechos;
                        int usa = Math.min(4, falta);
                        hechos += usa;
                        s += 4 - usa;
                    }
                }
            }
        }
        setSuelta(tanque, s);
        if (cambio) {
            guardar(tanque, l);
        }
        return hechos;
    }

    /** Mete nieve suelta (lo que aspira el lanzanieves). Devuelve cuánta cupo. */
    public static int meter(ItemStack tanque, int n) {
        int s = suelta(tanque);
        int cabe = Math.max(0, Math.min(n, MAX_SUELTA - s));
        setSuelta(tanque, s + cabe);
        return cabe;
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.tanque.carga", carga(st)).withStyle(ChatFormatting.AQUA));
        t.add(Component.translatable("navidad.tanque.ayuda").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack st) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack st) {
        return Math.round(13F * Math.min(1F, carga(st) / (float) LLENO));
    }

    @Override
    public int getBarColor(ItemStack st) {
        return 0x9CD8FF;
    }
}

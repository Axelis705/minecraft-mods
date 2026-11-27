package com.navidad.nieve;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** De dónde saca nieve el lanzanieves: del tanque que traes en la espalda y, si no alcanza, de las bolas de nieve de tu inventario. */
public final class Municion {
    private Municion() {
    }

    /** El tanque que traes puesto (o vacío). */
    public static ItemStack tanque(Player p) {
        ItemStack t = p.getItemBySlot(EquipmentSlot.CHEST);
        return t.getItem() instanceof TanqueNieve ? t : ItemStack.EMPTY;
    }

    public static int bolasSueltas(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(Items.SNOWBALL)) {
                n += s.getCount();
            }
        }
        return n;
    }

    public static int total(Player p) {
        ItemStack t = tanque(p);
        return (t.isEmpty() ? 0 : TanqueNieve.carga(t)) + bolasSueltas(p);
    }

    /** Gasta {@code n} tiros. En creativo no gasta. Devuelve si alcanzó. */
    public static boolean gastar(Player p, int n) {
        if (p.getAbilities().instabuild) {
            return true;
        }
        if (total(p) < n) {
            return false;
        }
        ItemStack t = tanque(p);
        int falta = n;
        if (!t.isEmpty()) {
            falta -= TanqueNieve.gastar(t, falta);
        }
        for (ItemStack s : p.getInventory().items) {
            if (falta <= 0) {
                break;
            }
            if (s.is(Items.SNOWBALL)) {
                int k = Math.min(falta, s.getCount());
                s.shrink(k);
                falta -= k;
            }
        }
        return falta <= 0;
    }
}

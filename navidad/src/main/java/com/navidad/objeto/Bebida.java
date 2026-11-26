package com.navidad.objeto;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Una bebida caliente (chocolate, ponche, champurrado): se bebe, quita el frío de la nieve polvo y calienta un rato. */
public class Bebida extends Item {
    public Bebida(Properties p) {
        super(p);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack s) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack s, Level l, LivingEntity e) {
        e.setTicksFrozen(0);
        e.clearFire();
        return super.finishUsingItem(s, l, e);
    }
}

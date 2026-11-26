package com.navidad.objeto;

import com.axeliscore.item.CoreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Luz de bengala: clic derecho para encenderla; chisporrotea en tu mano unos segundos y se acaba. */
public class Bengala extends CoreItem {
    public Bengala(Properties p) {
        super(p);
    }

    @Override
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
        add.accept(Component.translatable("navidad.bengala.ayuda").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected boolean onUse(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (!level.isClientSide && st.getOrCreateTag().getInt("Luz") <= 0) {
            st.getOrCreateTag().putInt("Luz", 240);
            level.playSound(null, p.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1F, 1.2F);
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack st, Level l, Entity e, int slot, boolean enMano) {
        if (l.isClientSide || !st.hasTag()) {
            return;
        }
        int luz = st.getTag().getInt("Luz");
        if (luz <= 0) {
            return;
        }
        st.getTag().putInt("Luz", luz - 1);
        if (e instanceof Player p && (enMano || p.getOffhandItem() == st) && l instanceof ServerLevel sl) {
            Vec3 m = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(Vec3.directionFromRotation(0, p.getYRot() + 90).scale(0.35))
                    .add(0, -0.45, 0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.x, m.y, m.z, 4, 0.08, 0.08, 0.08, 0.15);
            if (luz % 4 == 0) {
                sl.sendParticles(ParticleTypes.FIREWORK, m.x, m.y, m.z, 1, 0.05, 0.05, 0.05, 0.05);
            }
            if (luz % 20 == 0) {
                sl.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.15F, 2F);
            }
        }
        if (luz - 1 <= 0) {
            st.shrink(1);
        }
    }
}

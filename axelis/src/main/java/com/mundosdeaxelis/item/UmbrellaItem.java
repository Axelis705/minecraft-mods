package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Paraguas planeador: mantén clic derecho en el aire y bajas planeando hacia donde miras. */
public class UmbrellaItem extends Item {
    public UmbrellaItem(Properties properties) {
        super(properties);
    }

    public static boolean isOpen(LivingEntity e, ItemStack stack) {
        return e != null && e.isUsingItem() && e.getUseItem() == stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            ServerLevel sl = sp.serverLevel();
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 1.0F, 1.4F);
            if (!sp.onGround()) {
                Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xBFE6FF, 26);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp) || sp.onGround() || sp.isInWater()) {
            return;
        }
        int t = 72000 - remaining;
        Vec3 look = sp.getLookAngle();
        // Caída suave (efecto normal del juego: se siente fluido) y empuje hacia adelante.
        MobEffectInstance cur = sp.getEffect(MobEffects.SLOW_FALLING);
        if (cur == null || cur.getDuration() < 6) {
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 12, 0, false, false, false));
        }
        sp.fallDistance = 0.0F;
        if (t % 2 == 0) {
            Vec3 m = sp.getDeltaMovement();
            double fx = look.x, fz = look.z;
            double len = Math.sqrt(fx * fx + fz * fz);
            if (len > 0.01) {
                fx /= len;
                fz /= len;
                double speed = Math.sqrt(m.x * m.x + m.z * m.z);
                double add = speed < 0.55 ? 0.09 : 0.0;
                sp.setDeltaMovement(m.x * 0.98 + fx * add, Math.max(m.y, -0.09), m.z * 0.98 + fz * add);
                sp.hurtMarked = true;
            }
        }
        ServerLevel sl = sp.serverLevel();
        if (t % 3 == 0) {
            sl.sendParticles(Fx.spark(0xFFFFFF, 0.6F), sp.getX(), sp.getY() + 2.1, sp.getZ(), 1, 0.35, 0.05, 0.35, 0.0);
            sl.sendParticles(Fx.dust(0xDDF1FF, 0.7F), sp.getX(), sp.getY() - 0.1, sp.getZ(), 2, 0.25, 0.05, 0.25, 0.0);
        }
        if (t % 12 == 0) {
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.25F, 1.5F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "umbrella", 2);
    }
}

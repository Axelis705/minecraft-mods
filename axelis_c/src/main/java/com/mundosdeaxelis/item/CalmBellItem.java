package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/** Campana de la Calma: su sonido apacigua a los monstruos cercanos y los deja sin ganas de pelear. */
public class CalmBellItem extends Item {
    private static final double RANGE = 14.0;

    public CalmBellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        sp.getCooldowns().addCooldown(this, 20 * 12);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.4F, 1.0F);
        Fx.sound(sl, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0x9ADFFF, 50);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xCFEFFF, 50);
        for (int i = 1; i <= 7; i++) {
            final int r = i * 2;
            Scheduler.runLater(i * 2, () -> {
                Fx.ring(sl, Fx.dust(0x9ADFFF, 1.4F), x, y + 0.2, z, r, 12 + r * 3, 0);
                Fx.ring(sl, Fx.spark(0xFFFFFF, 0.8F), x, y + 0.9, z, r, 8 + r * 2, r * 0.4);
            });
        }
        List<Mob> mobs = sl.getEntitiesOfClass(Mob.class, new AABB(x - RANGE, y - RANGE, z - RANGE, x + RANGE, y + RANGE, z + RANGE),
                m -> m instanceof Enemy && m.isAlive());
        for (Mob m : mobs) {
            double d = Math.sqrt(m.distanceToSqr(sp));
            Scheduler.runLater(2 + (int) d, () -> {
                if (m.isAlive()) {
                    m.setTarget(null);
                    m.getNavigation().stop();
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 10, 3));
                    m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 10, 2));
                    sl.sendParticles(Fx.heart(0x9ADFFF, 0.9F), m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
                }
            });
        }
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.bell_used", mobs.size()), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "calm_bell", 2);
    }
}

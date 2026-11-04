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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/** Ojo Vigía: una onda de luz revela (resalta) a los monstruos cercanos a través de las paredes. */
public class WatcherEyeItem extends Item {
    private static final double RANGE = 28.0;

    public WatcherEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        sp.getCooldowns().addCooldown(this, 20 * 8);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
        Fx.sound(sl, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xC79BFF, 40);
        for (int i = 1; i <= 14; i++) {
            final int r = i * 2;
            Scheduler.runLater(i, () -> {
                Fx.ring(sl, Fx.dust(0xC79BFF, 1.3F), x, y + 0.15, z, r, 10 + r * 3, r * 0.2);
                if (r % 4 == 0) {
                    Fx.ring(sl, Fx.spark(0xFFFFFF, 0.9F), x, y + 0.3, z, r, 6 + r, -r * 0.3);
                }
            });
        }
        List<LivingEntity> found = sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - RANGE, y - RANGE, z - RANGE, x + RANGE, y + RANGE, z + RANGE),
                e -> e instanceof Enemy && e.isAlive());
        for (LivingEntity e : found) {
            double d = Math.sqrt(e.distanceToSqr(sp));
            Scheduler.runLater(1 + (int) (d / 2.0), () -> {
                if (e.isAlive()) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 12, 0, false, false, false));
                    sl.sendParticles(Fx.spark(0xC79BFF, 1.4F), e.getX(), e.getY() + e.getBbHeight() + 0.4, e.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
                }
            });
        }
        sp.displayClientMessage(Component.translatable(found.isEmpty() ? "message.mundos_de_axelis.eye_none"
                : "message.mundos_de_axelis.eye_found", found.size()), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "watcher_eye", 2);
    }
}

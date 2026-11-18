package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Reloj estelar: si es de noche, hace pasar las horas hasta que sale el sol (y despeja el cielo). */
public class StarClockItem extends Item {
    private static final int STEPS = 70;

    public StarClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        if (!sl.dimensionType().natural() || sl.dimensionType().hasFixedTime()) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.clock_none"), true);
            sp.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.fail(stack);
        }
        long day = sl.getDayTime();
        long inDay = day % 24000L;
        boolean night = inDay >= 12600L || inDay < 23000L && inDay > 12000L;
        if (inDay < 12000L || inDay >= 23000L) {
            night = false;
        }
        if (!night) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.clock_day"), true);
            sp.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.fail(stack);
        }
        final long start = day;
        final long target = (day / 24000L + 1L) * 24000L;
        sp.getCooldowns().addCooldown(this, 20 * 20);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        for (ServerPlayer p : sl.players()) {
            Fx.screen(p, ScreenFxPacket.SUNRISE, 0xFFC36B, STEPS + 24);
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.clock_used", sp.getDisplayName()), true);
        }
        sl.sendParticles(Fx.spark(0xFFE9A0, 1.4F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 14, 0.6, 0.7, 0.6, 0.0);
        Fx.ring(sl, Fx.dust(0xFFC36B, 1.2F), sp.getX(), sp.getY() + 0.2, sp.getZ(), 1.2, 24, 0);

        for (int i = 1; i <= STEPS; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> {
                float u = k / (float) STEPS;
                float e = u * u * (3.0F - 2.0F * u);
                sl.setDayTime(start + (long) ((target - start) * e));
                if (sp.isAlive() && !sp.hasDisconnected() && k % 4 == 0) {
                    ServerLevel w = sp.serverLevel();
                    Fx.helix(w, Fx.spark(0xFFE9A0, 0.9F), sp.getX(), sp.getY() + 0.1, sp.getZ(), 0.9, 2.2, 4, k * 0.3);
                    w.sendParticles(ParticleTypes.FIREWORK, sp.getX(), sp.getY() + 2.2, sp.getZ(), 1, 0.3, 0.1, 0.3, 0.02);
                }
                if (k % 14 == 0) {
                    sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS,
                            0.8F, 0.7F + u);
                }
            });
        }
        Scheduler.runLater(STEPS + 2, () -> {
            sl.setDayTime(target);
            sl.setWeatherParameters(24000, 0, false, false);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.3F);
        });
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "star_clock", 2);
    }
}

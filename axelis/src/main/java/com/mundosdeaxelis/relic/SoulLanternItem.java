package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Farol de Almas: guarda las almas de lo que derrotas y las lanza como espíritus que persiguen enemigos. */
public class SoulLanternItem extends RelicItem {
    private static final int COLOR = 0x4FE8FF;
    private static final int MAX = 30;

    public SoulLanternItem() {
        super("soul_lantern", COLOR, 20 * 18, Rarity.EPIC);
    }

    public static int souls(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt("souls") : 0;
    }

    private static void setSouls(ItemStack stack, int n) {
        stack.getOrCreateTag().putInt("souls", Math.max(0, Math.min(MAX, n)));
    }

    /** Llamado desde RelicEvents cuando el portador derrota algo. */
    public static void onKill(ServerPlayer sp, LivingEntity victim) {
        ItemStack stack = RelicItem.carried(sp, ModItems.SOUL_LANTERN);
        if (stack.isEmpty() || victim instanceof net.minecraft.world.entity.player.Player) {
            return;
        }
        ServerLevel sl = sp.serverLevel();
        Vec3 start = victim.getBoundingBox().getCenter();
        Fx.sound(sl, start.x, start.y, start.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6F, 1.2F);
        RelicFx.burst(sl, Fx.wisp(COLOR, 1.0F), start, 6, 0.12);
        collect(sp, sl, stack, start, 0);
    }

    private static void collect(ServerPlayer sp, ServerLevel sl, ItemStack stack, Vec3 start, int step) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        Vec3 end = RelicFx.hand(sp);
        double u = (step + 1) / 8.0;
        Vec3 p = start.add(end.subtract(start).scale(u)).add(Math.sin(u * Math.PI) * 0.8 * Math.cos(step), Math.sin(u * Math.PI) * 1.0, Math.sin(u * Math.PI) * 0.8 * Math.sin(step));
        sl.sendParticles(Fx.wisp(COLOR, 1.0F), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 2, 0.04, 0.04, 0.04, 0.0);
        sl.sendParticles(Fx.spark(0xC8F8FF, 0.5F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        if (step >= 7) {
            if (!stack.isEmpty()) {
                setSouls(stack, souls(stack) + 1);
            }
            RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), end, 8, 0.15);
            RelicSounds.play(sl, end, "soul_lantern", "collect", 0.8F, 0.95F + sl.random.nextFloat() * 0.15F);
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.souls", souls(stack), MAX), true);
            return;
        }
        Scheduler.runLater(1, () -> collect(sp, sl, stack, start, step + 1));
    }

    @Override
    protected int interval() {
        return 4;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        ItemStack stack = carried(sp, ModItems.SOUL_LANTERN);
        int n = stack.isEmpty() ? 0 : souls(stack);
        if (n >= 5) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, true, false, true));
        }
        int orbs = Math.min(6, n);
        for (int i = 0; i < orbs; i++) {
            double a = time * 0.09 + i * Math.PI * 2.0 / orbs;
            sl.sendParticles(Fx.wisp(COLOR, 0.5F), sp.getX() + Math.cos(a) * 0.95, sp.getY() + 1.1 + Math.sin(time * 0.05 + i) * 0.4, sp.getZ() + Math.sin(a) * 0.95, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        int have = souls(stack);
        if (have < 1) {
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.souls_low"), true);
            return false;
        }
        List<LivingEntity> foes = RelicFx.foes(sl, sp.position(), 18, sp);
        if (foes.isEmpty()) {
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.no_target"), true);
            return false;
        }
        int n = Math.min(have, 8);
        setSouls(stack, have - n);
        Vec3 h = RelicFx.hand(sp);
        Fx.sound(sl, h.x, h.y, h.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.7F, 0.6F);
        Scheduler.runLater(6, () -> RelicSounds.play(sl, RelicFx.hand(sp), "soul_lantern", "release", 0.9F, 1.0F));
        RelicFx.burst(sl, Fx.wisp(COLOR, 1.2F), h, 12, 0.15);
        for (int k = 0; k < n; k++) {
            final LivingEntity t = foes.get(k % foes.size());
            final int kk = k;
            Scheduler.runLater(k * 2, () -> seek(sp, sl, RelicFx.hand(sp), t, 0, kk));
        }
        return true;
    }

    private void seek(ServerPlayer sp, ServerLevel sl, Vec3 pos, LivingEntity target, int step, int k) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        Vec3 aim = target.isAlive() ? target.getBoundingBox().getCenter() : pos.add(sp.getLookAngle().scale(6));
        Vec3 toward = aim.subtract(pos);
        double dist = toward.length();
        double speed = 0.9 + step * 0.08;
        Vec3 swirl = new Vec3(Math.cos(step * 0.9 + k), Math.sin(step * 0.7 + k) * 0.6, Math.sin(step * 0.9 + k)).scale(0.45 * Math.max(0, 1 - step / 14.0));
        Vec3 next = dist <= speed ? aim : pos.add(toward.normalize().scale(speed)).add(swirl);
        RelicFx.line(sl, Fx.wisp(COLOR, 0.8F), pos, next, 0.6);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, next.x, next.y, next.z, 2, 0.05, 0.05, 0.05, 0.0);
        sl.sendParticles(Fx.spark(0xC8F8FF, 0.6F), next.x, next.y, next.z, 1, 0.05, 0.05, 0.05, 0.0);
        if (dist <= speed || step > 30) {
            if (target.isAlive() && dist <= speed) {
                RelicFx.hurt(sl, sp, target, 5.0F);
                target.invulnerableTime = 0;
                sp.heal(0.5F);
                RelicFx.burst(sl, Fx.spark(COLOR, 0.9F), aim, 10, 0.25);
                RelicFx.burst(sl, Fx.wisp(COLOR, 1.1F), aim, 5, 0.1);
                Fx.sound(sl, aim.x, aim.y, aim.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.4F, 1.5F);
            }
            return;
        }
        Scheduler.runLater(1, () -> seek(sp, sl, next, target, step + 1, k));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(1, Component.translatable("tooltip.mundos_de_axelis.soul_lantern.souls", souls(stack), MAX).withStyle(ChatFormatting.AQUA));
    }
}

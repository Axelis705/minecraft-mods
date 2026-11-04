package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/** Lámpara del Genio: al frotarla sale un genio de humo que cumple un deseo al azar. */
public class GenieLampItem extends RelicItem {
    private static final int COLOR = 0x4AE0D0;
    private static final int WISHES = 7;

    public GenieLampItem() {
        super("genie_lamp", COLOR, 20 * 90, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, false, true));
        if (time % 60 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.wisp(COLOR, 0.5F), h.x, h.y + 0.1, h.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 look = sp.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 base = sp.position().add(flat.scale(1.8));
        Fx.sound(sl, base.x, base.y, base.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.7F);
        smoke(sp, sl, base, 0);
        return true;
    }

    private void smoke(ServerPlayer sp, ServerLevel sl, Vec3 base, int t) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        if (t < 40) {
            double u = t / 40.0;
            double height = 0.4 + u * 2.8;
            for (int i = 0; i < 4; i++) {
                double h = height * (i + 1) / 4.0;
                double a = t * 0.35 + i * 1.6;
                double r = 0.25 + 0.5 * Math.sin(h / 3.2 * Math.PI) + 0.1;
                sl.sendParticles(Fx.wisp(i % 2 == 0 ? COLOR : 0xA070FF, 1.1F), base.x + Math.cos(a) * r, base.y + h, base.z + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.0);
            }
            // Cabeza y brazos del genio
            if (t > 14) {
                Vec3 head = base.add(0, 3.0, 0);
                sl.sendParticles(Fx.wisp(0xC0F0FF, 1.6F), head.x, head.y, head.z, 1, 0.1, 0.1, 0.1, 0.0);
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 arm = base.add(Math.cos(t * 0.25) * 0.2 + s * 0.9, 1.9 + Math.sin(t * 0.3) * 0.15, s * 0.1);
                    sl.sendParticles(Fx.wisp(0x80D8FF, 0.9F), arm.x, arm.y, arm.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            if (t % 3 == 0) {
                sl.sendParticles(Fx.spark(0xFFF0A8, 0.7F), base.x, base.y + sl.random.nextDouble() * 3.4, base.z, 2, 0.6, 0.2, 0.6, 0.0);
            }
            if (t % 5 == 0) {
                Fx.ring(sl, Fx.dust(COLOR, 0.7F), base.x, base.y + 0.05, base.z, 0.6 + u * 0.8, 14, t);
            }
            Scheduler.runLater(1, () -> smoke(sp, sl, base, t + 1));
            return;
        }
        wish(sp, sl, base);
    }

    private void wish(ServerPlayer sp, ServerLevel sl, Vec3 base) {
        int w = sl.random.nextInt(WISHES);
        switch (w) {
            case 0 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 180, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 180, 0));
            }
            case 1 -> {
                sp.setHealth(sp.getMaxHealth());
                sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 60, 3));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 1));
            }
            case 2 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0));
                sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 600, 0));
                sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 300, 0));
            }
            case 3 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 600, 1));
                sp.getFoodData().setFoodLevel(20);
                sp.getFoodData().setSaturation(20.0F);
                sp.giveExperiencePoints(300);
            }
            case 4 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 180, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 180, 1));
                sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 180, 0));
            }
            case 5 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0));
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 180, 1));
            }
            default -> {
                sp.spawnAtLocation(new ItemStack(Items.DIAMOND, 2 + sl.random.nextInt(3)));
                sp.spawnAtLocation(new ItemStack(Items.EMERALD, 4 + sl.random.nextInt(5)));
                sp.spawnAtLocation(new ItemStack(Items.GOLD_INGOT, 6 + sl.random.nextInt(7)));
            }
        }
        sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.genie." + w), true);
        Vec3 top = base.add(0, 3.0, 0);
        RelicSounds.play(sl, base.add(0, 1.5, 0), relicId(), "wish", 1.1F, 1.0F);
        Fx.sound(sl, base.x, base.y, base.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.3F);
        Fx.confetti(sl, top.x, top.y, top.z, 40, 3.0);
        RelicFx.burst(sl, Fx.spark(0xFFE070, 1.1F), top, 36, 0.35);
        RelicFx.burst(sl, Fx.wisp(COLOR, 1.4F), top, 14, 0.15);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xFFE070, 40);
        for (int i = 0; i < 4; i++) {
            final int k = i;
            Scheduler.runLater(i * 2, () -> Fx.ring(sl, Fx.dust(0xFFE070, 1.0F), top.x, top.y - 0.5 * k, top.z, 0.8 + k * 0.5, 18, k));
        }
    }
}

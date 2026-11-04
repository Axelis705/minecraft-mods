package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

/** Cuerno de Guerra: lo soplas y una onda de sonido sacude el campo, fortalece a los aliados y asusta a los enemigos. */
public class WarHornItem extends RelicItem {
    private static final int COLOR = 0xE8C070;

    public WarHornItem() {
        super("war_horn", COLOR, 20 * 45, Rarity.EPIC);
    }

    @Override
    protected int channelTicks() {
        return 30;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.TOOT_HORN;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        int n = RelicFx.foes(sl, sp.position(), 8, sp).size();
        if (n >= 2) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            if (time % 20 == 0) {
                sl.sendParticles(Fx.ember(0xFF5A2A, 0.8F), sp.getX(), sp.getY() + 1.0, sp.getZ(), 4, 0.4, 0.6, 0.4, 0.02);
            }
        }
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 1.3F, 0.9F);
        RelicSounds.play(sl, sp, relicId(), "charge", 1.0F, 1.0F);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        if (used % 3 == 0) {
            double d = 1.0 + used * 0.22;
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_BOOM.get(), SoundSource.PLAYERS, 0.15F, 1.5F);
            RelicFx.ringFacing(sl, Fx.dust(COLOR, 0.9F), eye.add(look.scale(d)), look, 0.4 + used * 0.05, 16, used);
            RelicFx.ringFacing(sl, Fx.spark(0xFFF3C0, 0.6F), eye.add(look.scale(d + 0.4)), look, 0.3 + used * 0.04, 8, -used);
        }
        if (used == 1) {
            RelicFx.shake(sp, 0.5F, 30);
        }
        if (used == 14) {
            RelicSounds.play(sl, sp, relicId(), "charge", 1.0F, 1.25F);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        if (!completed) {
            return;
        }
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, ModSounds.RELIC_BOOM.get(), SoundSource.PLAYERS, 1.4F, 0.7F);
        Fx.sound(sl, x, y, z, SoundEvents.GOAT_HORN_BREAK, SoundSource.PLAYERS, 0.6F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.SUNRISE, COLOR, 40);
        RelicFx.shakeNear(sl, sp.position(), 18, 2.5F, 22);
        for (int i = 1; i <= 10; i++) {
            final int r = i;
            Scheduler.runLater(i, () -> {
                Fx.ring(sl, Fx.dust(COLOR, 1.4F), x, y + 0.2, z, r, 12 + r * 5, r * 0.25);
                Fx.ring(sl, Fx.ember(0xFFB040, 1.0F), x, y + 0.5, z, r, 6 + r * 2, -r * 0.3);
                if (r % 3 == 0) {
                    Fx.ring(sl, Fx.spark(0xFFFFFF, 0.8F), x, y + 1.2, z, r, 10, r);
                }
            });
        }
        // Columna de estandartes dorados
        Fx.helix(sl, Fx.spark(COLOR, 0.8F), x, y, z, 1.0, 3.5, 20, 0);
        for (LivingEntity e : RelicFx.foes(sl, sp.position(), 11, sp)) {
            RelicFx.hurt(sl, sp, e, 3.0F);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            double dx = e.getX() - x;
            double dz = e.getZ() - z;
            double len = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            e.push(dx / len * 1.1, 0.4, dz / len * 1.1);
            e.hurtMarked = true;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceToSqr(sp.position()) <= 16 * 16) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 0));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 0));
                RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), p.position().add(0, 1, 0), 10, 0.2);
            }
        }
        spend(sp, stack, cooldownTicks(), true);
    }
}

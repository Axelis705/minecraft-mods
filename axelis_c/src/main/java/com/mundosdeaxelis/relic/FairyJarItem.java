package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Frasco de Hadas: tres hadas acompañan al jugador, curan a los aliados y disparan chispas a los enemigos. */
public class FairyJarItem extends RelicItem {
    private static final int COLOR = 0xFF9BE8;
    private static final int[] FAIRY = {0xFFB6EE, 0xB8E8FF, 0xFFF0A8};

    public FairyJarItem() {
        super("fairy_jar", COLOR, 20 * 70, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 40;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, true, false, true));
        for (LivingEntity a : RelicFx.allies(sl, sp.position(), 8, sp, false)) {
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, true, false, true));
        }
        Vec3 h = RelicFx.hand(sp);
        sl.sendParticles(Fx.spark(FAIRY[(int) ((time / 40) % 3)], 0.4F), h.x, h.y, h.z, 2, 0.1, 0.1, 0.1, 0.0);
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = power(stack);
        Vec3 h = RelicFx.hand(sp);
        Vec3 look = sp.getLookAngle();
        // se abre el frasco: tres destellos de colores salen volando
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Scheduler.runLater(i * 3, () -> {
                Vec3 side = look.cross(new Vec3(0, 1, 0));
                side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
                Vec3 p = h.add(side.scale((k - 1) * 0.5)).add(0, 0.4 * k, 0);
                Vfx.flash(sl, p, FAIRY[k], 1.6, 9);
                RelicFx.burst(sl, Fx.spark(FAIRY[k], 0.8F), p, 14, 0.25);
                RelicFx.burst(sl, Fx.heart(FAIRY[k], 0.6F), p, 2, 0.1);
                sl.playSound(null, p.x, p.y, p.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.4F + k * 0.25F);
            });
        }
        Vfx.rune(sl, sp.position(), COLOR, 3.4, 36);
        Vfx.ring(sl, sp.position().add(0, 0.1, 0), COLOR, 4.0, 16);
        RelicFx.sphere(sl, Fx.spark(COLOR, 0.6F), sp.position().add(0, 1, 0), 1.6, 28, 0);
        Familiars.spawnFairies(sp, (int) (20 * 26 * power), power);
        return true;
    }
}

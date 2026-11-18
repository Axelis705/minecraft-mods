package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.ScreenFxPacket;
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

/** Huevo de Dragoncito: eclosiona un dragoncito que te acompaña y escupe fuego a tus enemigos. */
public class HatchlingEggItem extends RelicItem {
    private static final int COLOR = 0xFF8A2E;

    public HatchlingEggItem() {
        super("hatchling_egg", COLOR, 20 * 80, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    /** Los enemigos en llamas reciben más daño de quien lleva el huevo (se aplica desde RelicEvents). */
    public static final float BURN_BONUS = 1.25F;

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (time % 40 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.ember(COLOR, 0.5F), h.x, h.y + 0.1, h.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
        if (sp.isOnFire()) {
            sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        float power = power(stack);
        Vec3 look = sp.getLookAngle();
        Vec3 spot = sp.getEyePosition().add(look.x * 1.6, -0.2, look.z * 1.6);
        RelicSounds.play(sl, spot, relicId(), "hatch", 1.0F, 1.0F);
        // el huevo se agrieta
        Vfx.flash(sl, spot, COLOR, 1.6, 8);
        RelicFx.burst(sl, Fx.shard(0xFFE0B0, 0.7F), spot, 16, 0.2);
        RelicFx.shake(sp, 0.5F, 10);
        Scheduler.runLater(10, () -> {
            if (!sp.isAlive()) {
                return;
            }
            Vec3 l2 = sp.getLookAngle();
            Vec3 s2 = sp.getEyePosition().add(l2.x * 1.6, -0.2, l2.z * 1.6);
            Vfx.flash(sl, s2, 0xFFFFFF, 3.4, 12);
            Vfx.ring(sl, s2, COLOR, 4.0, 14);
            Vfx.ringFacing(sl, s2, l2, COLOR, 3.0, 12);
            RelicFx.burst(sl, Fx.shard(0xFFE0B0, 0.9F), s2, 28, 0.35);
            RelicFx.burst(sl, Fx.ember(COLOR, 0.8F), s2, 24, 0.3);
            RelicFx.burst(sl, ParticleTypes.FLAME, s2, 20, 0.15);
            RelicSounds.play(sl, s2, relicId(), "roar", 1.0F, 1.0F);
            Fx.screen(sp, ScreenFxPacket.FLASH, COLOR, 10);
            Familiars.spawnDragon(sp, (int) (20 * 32 * power), power);
        });
        return true;
    }
}

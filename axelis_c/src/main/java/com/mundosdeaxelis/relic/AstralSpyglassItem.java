package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.vfx.VfxType;
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

/** Catalejo Astral: al mirar lejos marca a los enemigos; los marcados reciben más daño. */
public class AstralSpyglassItem extends RelicItem {
    private static final int COLOR = 0xB38CFF;

    public AstralSpyglassItem() {
        super("astral_spyglass", COLOR, 20 * 20, Rarity.RARE);
    }

    @Override
    protected int channelTicks() {
        return 40;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.SPYGLASS;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (time % 40 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.spark(COLOR, 0.45F), h.x, h.y, h.z, 1, 0.12, 0.12, 0.12, 0.0);
        }
    }

    /** Enemigos dentro del cono de visión. */
    private static List<LivingEntity> inCone(ServerLevel sl, ServerPlayer sp, double range, double minDot) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : RelicFx.foes(sl, eye, range, sp)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > 1.0 && to.scale(1.0 / d).dot(look) > minDot) {
                list.add(e);
            }
        }
        return list;
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, sp, relicId(), "scan", 0.7F, 1.0F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, COLOR, 40);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        if (used % 4 == 0) {
            double d = 4 + (used * 1.3) % 40;
            Vec3 p = eye.add(look.scale(d));
            sl.sendParticles(Fx.spark(COLOR, 0.5F), p.x, p.y, p.z, 1, 0.5, 0.5, 0.5, 0.0);
        }
        if (used >= 10 && used % 5 == 0) {
            for (LivingEntity e : inCone(sl, sp, 48, 0.94)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 12, 0, false, false));
            }
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        if (used < 20) {
            return;
        }
        List<LivingEntity> targets = inCone(sl, sp, 48, 0.9);
        if (targets.isEmpty()) {
            RelicFx.sendActionBar(sp, "message.mundos_de_axelis.relic_no_target");
            return;
        }
        long end = sl.getGameTime() + (long) (240 * power(stack));
        int count = 0;
        for (LivingEntity e : targets) {
            if (count++ >= 10) {
                break;
            }
            e.getPersistentData().putLong("mdc_mark", end);
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) (end - sl.getGameTime()), 0, false, false));
            Vfx.runeFollow(sl, e, COLOR, 1.3, 240);
            Vfx.make(sl, VfxType.ORB).follow(e, 0, e.getBbHeight() + 0.6, 0).rgb(0xFFFFFF).size(0.2).flags(VfxType.F_FLICKER).life(240).send();
            Vfx.flash(sl, e.getBoundingBox().getCenter(), COLOR, 1.6, 10);
        }
        RelicSounds.play(sl, sp, relicId(), "mark", 0.9F, 1.0F);
        spend(sp, stack, cooldownTicks(), true);
    }

    /** Multiplicador de daño que recibe una entidad marcada. */
    public static float bonus(LivingEntity target) {
        return target.getPersistentData().getLong("mdc_mark") > target.level().getGameTime() ? 1.4F : 1.0F;
    }
}

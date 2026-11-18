package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxType;
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

/** Arco de las Constelaciones: estrellas teledirigidas que, al caer, se unen con hilos de luz como una constelación. */
public class StarBowItem extends RelicItem {
    private static final int COLOR = 0x7FB2FF;

    public StarBowItem() {
        super("star_bow", COLOR, 20 * 14, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected int channelTicks() {
        return 30;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.BOW;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (RelicFx.isNight(sl) && sl.canSeeSky(sp.blockPosition())) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 0, true, false, true));
            if (time % 40 == 0) {
                Vec3 h = RelicFx.hand(sp);
                sl.sendParticles(Fx.spark(COLOR, 0.5F), h.x, h.y, h.z, 2, 0.15, 0.15, 0.15, 0.0);
            }
        }
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, sp, relicId(), "charge", 0.7F, 1.0F);
        Vec3 look = sp.getLookAngle();
        Vfx.runeFacing(sl, sp.getEyePosition().add(look.scale(1.7)), look, COLOR, 1.0, 34);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        float k = used / 30.0F;
        Vec3 h = RelicFx.hand(sp);
        double r = 1.7 * (1.0 - k) + 0.3;
        for (int i = 0; i < 2 + used / 10; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0;
            double e = (sl.random.nextDouble() - 0.5) * 2.0;
            Vec3 from = h.add(Math.cos(a) * r, e * r * 0.8, Math.sin(a) * r);
            Vec3 v = h.subtract(from).scale(0.14);
            sl.sendParticles(Fx.spark(i % 2 == 0 ? COLOR : 0xFFFFFF, 0.5F + k * 0.5F), from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (used % 6 == 0) {
            Vec3 look = sp.getLookAngle();
            Vfx.ringFacing(sl, h.add(look.scale(0.5)), look, COLOR, 0.35 + 0.5 * k, 9);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        if (used < 8) {
            return;
        }
        float charge = Math.min(1.0F, used / 30.0F);
        float power = power(stack);
        int n = 3 + (int) (charge * 4);
        Vec3 look = sp.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        final Vec3 rightV = right;
        Vec3 h = RelicFx.hand(sp);
        List<LivingEntity> foes = RelicFx.foes(sl, sp.position(), 28, sp);
        LivingEntity aimed = RelicFx.aimedFoe(sl, sp, 32, 0.85);
        if (aimed != null) {
            foes.remove(aimed);
            foes.add(0, aimed);
        }
        RelicSounds.play(sl, h, relicId(), "shoot", 0.9F, 1.0F);
        RelicFx.shake(sp, 0.7F, 8);
        final List<Vec3> hits = new ArrayList<>();
        final int[] landed = {0};
        for (int i = 0; i < n; i++) {
            final int idx = i;
            double off = i - (n - 1) / 2.0;
            Vec3 dir = look.add(right.scale(off * 0.17)).add(up.scale(0.10 + 0.045 * Math.abs(off))).normalize();
            LivingEntity tgt = foes.isEmpty() ? null : foes.get(i % foes.size());
            Scheduler.runLater(1 + i * 2, () -> {
                Missiles.Spec m = new Missiles.Spec(sp);
                m.pos = h.add(rightV.scale(off * 0.3));
                m.dir = dir;
                m.speed = 1.45;
                m.turn = tgt != null ? 0.2 : 0.0;
                m.homing = tgt;
                m.life = 55;
                m.rgb = idx % 2 == 0 ? COLOR : 0xE6F0FF;
                m.size = 0.3;
                m.particles = Fx.spark(COLOR, 0.35F);
                m.onHit = (pos, target) -> {
                    Missiles.spark(sl, pos, COLOR, 12);
                    Vfx.flash(sl, pos, COLOR, 1.7, 9);
                    RelicSounds.playVaried(sl, pos, relicId(), "hit", 0.75F);
                    if (target != null) {
                        RelicFx.damage(sl, sp, target, (5.0F + 3.0F * charge) * power);
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                    }
                    if (!hits.isEmpty()) {
                        Vec3 prev = hits.get(hits.size() - 1);
                        Vfx.beam(sl, prev, pos, 0xBFD8FF, 0.03, 30, VfxType.F_CAPS | VfxType.F_FLICKER);
                    }
                    hits.add(pos);
                    landed[0]++;
                    if (landed[0] == n && n >= 4) {
                        Vec3 c = Vec3.ZERO;
                        for (Vec3 p : hits) {
                            c = c.add(p);
                        }
                        c = c.scale(1.0 / hits.size());
                        Vfx.flash(sl, c, 0xFFFFFF, 2.4, 14);
                        Vfx.rune(sl, c, COLOR, 3.2, 34);
                        RelicSounds.play(sl, c, relicId(), "hit", 0.8F, 0.75F);
                    }
                };
                Missiles.launch(m);
            });
        }
        spend(sp, stack, cooldownTicks(), true);
    }
}

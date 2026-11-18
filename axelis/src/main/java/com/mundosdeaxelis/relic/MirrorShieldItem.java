package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
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

import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Escudo Espejo: mientras lo mantienes, refleja proyectiles, reduce el daño y devuelve el golpe. */
public class MirrorShieldItem extends RelicItem {
    private static final int COLOR = 0xBFE8FF;

    public MirrorShieldItem() {
        super("mirror_shield", COLOR, 20 * 18, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected int channelTicks() {
        return 100;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.BLOCK;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.getHealth() <= sp.getMaxHealth() * 0.5F) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.spark(COLOR, 0.5F), h.x, h.y, h.z, 2, 0.2, 0.2, 0.2, 0.0);
        }
    }

    /** ¿Tiene el escudo levantado ahora mismo? */
    public static boolean isShielding(ServerPlayer sp) {
        return sp.isUsingItem() && sp.getUseItem().getItem() instanceof MirrorShieldItem
                && sp.getPersistentData().getLong("mdc_mirror") > sp.level().getGameTime();
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        RelicSounds.play(sl, sp, relicId(), "raise", 0.85F, 1.0F);
        sp.getPersistentData().putLong("mdc_mirror", sl.getGameTime() + 6);
        sp.getPersistentData().putInt("mdc_mirror_vfx", Vfx.domeFollow(sl, sp, COLOR, 1.75, 105));
        Vfx.ring(sl, sp.position().add(0, 0.1, 0), COLOR, 2.4, 14);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        sp.getPersistentData().putLong("mdc_mirror", sl.getGameTime() + 6);
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 1, true, false, false));
        Vec3 c = sp.position().add(0, sp.getBbHeight() * 0.5, 0);
        for (Projectile pr : sl.getEntitiesOfClass(Projectile.class, new AABB(c.x - 3.2, c.y - 3.2, c.z - 3.2, c.x + 3.2, c.y + 3.2, c.z + 3.2),
                p -> p.getOwner() != sp && p.tickCount > 0)) {
            Vec3 toMe = c.subtract(pr.position());
            if (pr.getDeltaMovement().dot(toMe) > 0 && pr.position().distanceTo(c) < 3.0) {
                reflect(sl, sp, pr);
            }
        }
        if (used % 5 == 0) {
            double a = used * 0.4;
            sl.sendParticles(Fx.spark(COLOR, 0.45F), c.x + Math.cos(a) * 1.7, c.y + Math.sin(a * 0.7) * 1.2, c.z + Math.sin(a) * 1.7, 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        sp.getPersistentData().putLong("mdc_mirror", 0);
        Vfx.remove(sl, sp.getPersistentData().getInt("mdc_mirror_vfx"));
        if (used < 10) {
            return;
        }
        RelicSounds.play(sl, sp, relicId(), "reflect", 0.4F, 0.7F);
        Vfx.flash(sl, sp.position().add(0, 1.1, 0), COLOR, 2.0, 8);
        spend(sp, stack, cooldownTicks(), false);
    }

    /** Devuelve un proyectil al que lo lanzó. */
    public static void reflect(ServerLevel sl, ServerPlayer sp, Projectile pr) {
        if (pr.getPersistentData().getLong("mdc_refl") > sl.getGameTime() - 5) {
            return;
        }
        pr.getPersistentData().putLong("mdc_refl", sl.getGameTime());
        Vec3 v = pr.getDeltaMovement();
        Vec3 back = v.lengthSqr() < 1.0E-4 ? sp.getLookAngle() : v.scale(-1.2);
        pr.setDeltaMovement(back);
        pr.setOwner(sp);
        pr.hurtMarked = true;
        Vec3 at = pr.position();
        RelicSounds.play(sl, at, "mirror_shield", "reflect", 0.9F, 0.9F + sl.random.nextFloat() * 0.3F);
        Vfx.flash(sl, at, COLOR, 1.6, 8);
        Vec3 n = sp.position().add(0, 1, 0).subtract(at);
        Vfx.ringFacing(sl, at, n, COLOR, 1.0, 10);
        RelicFx.burst(sl, Fx.spark(COLOR, 0.6F), at, 10, 0.3);
        int id = sp.getPersistentData().getInt("mdc_mirror_vfx");
        if (id > 0) {
            Vfx.make(sl, VfxType.DOME).follow(sp, 0, sp.getBbHeight() * 0.5, 0).rgb(COLOR).size(1.75).a(1.0).life(0).update(id);
        }
    }

    /** Daño recibido con el escudo arriba: casi todo se apaga y parte vuelve al atacante. */
    public static void onHurt(LivingHurtEvent event, ServerPlayer sp) {
        ServerLevel sl = sp.serverLevel();
        float original = event.getAmount();
        event.setAmount(original * 0.25F);
        Vec3 at = sp.position().add(0, sp.getBbHeight() * 0.6, 0);
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != sp && attacker.distanceTo(sp) < 9) {
            attacker.hurt(sl.damageSources().thorns(sp), Math.min(10.0F, original * 0.6F));
            RelicFx.knock(attacker, sp.position(), 0.7, 0.3);
            Vec3 to = attacker.getBoundingBox().getCenter();
            Vfx.beam(sl, at, to, COLOR, 0.05, 8, VfxType.F_CAPS | VfxType.F_FLICKER);
            Vfx.flash(sl, to, COLOR, 1.6, 8);
        }
        RelicSounds.play(sl, at, "mirror_shield", "reflect", 0.9F, 1.0F);
        Vfx.flash(sl, at, COLOR, 1.8, 8);
        int id = sp.getPersistentData().getInt("mdc_mirror_vfx");
        if (id > 0) {
            Vfx.make(sl, VfxType.DOME).follow(sp, 0, sp.getBbHeight() * 0.5, 0).rgb(COLOR).size(1.75).a(1.0).life(0).update(id);
        }
    }
}

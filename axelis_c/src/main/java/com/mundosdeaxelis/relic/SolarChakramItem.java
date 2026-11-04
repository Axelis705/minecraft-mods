package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Chakram Solar: un disco de fuego que giras hacia delante y vuelve a tu mano cortando todo a su paso. */
public class SolarChakramItem extends RelicItem {
    private static final int COLOR = 0xFFB22E;

    public SolarChakramItem() {
        super("solar_chakram", COLOR, 20 * 10, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false, true));
        if (time % 30 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.ember(COLOR, 0.6F), h.x, h.y, h.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 look = sp.getLookAngle();
        Vec3 start = RelicFx.hand(sp);
        Fx.sound(sl, start.x, start.y, start.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        fly(sp, sl, start, look, 0, false, new HashSet<>());
        return true;
    }

    private void fly(ServerPlayer sp, ServerLevel sl, Vec3 pos, Vec3 dir, int tick, boolean back, Set<UUID> hit) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        boolean returning = back;
        Vec3 d = dir;
        Set<UUID> hs = hit;
        if (!returning) {
            BlockPos bp = BlockPos.containing(pos);
            if (tick >= 15 || !sl.getBlockState(bp).getCollisionShape(sl, bp).isEmpty()) {
                returning = true;
                hs = new HashSet<>();
                sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                RelicFx.burst(sl, Fx.ember(COLOR, 1.0F), pos, 14, 0.25);
            }
        }
        double speed = returning ? 1.35 : 1.15;
        if (returning) {
            Vec3 to = sp.getEyePosition().add(0, -0.3, 0).subtract(pos);
            if (to.length() < 1.6 || tick > 70) {
                Vec3 e = sp.getEyePosition();
                RelicSounds.play(sl, e, "solar_chakram", "catch", 1.0F, 1.0F);
                Fx.ring(sl, Fx.spark(COLOR, 0.7F), e.x, e.y - 0.4, e.z, 0.6, 10, 0);
                RelicFx.burst(sl, Fx.ember(COLOR, 0.9F), e, 8, 0.15);
                return;
            }
            d = to.normalize();
        }
        Vec3 next = pos.add(d.scale(speed));
        draw(sl, next, pos, tick);
        for (LivingEntity e : RelicFx.foes(sl, next, 1.6, sp)) {
            if (hs.add(e.getUUID())) {
                RelicFx.hurt(sl, sp, e, returning ? 5.0F : 7.0F);
                e.setSecondsOnFire(3);
                Vec3 c = e.getBoundingBox().getCenter();
                RelicFx.burst(sl, Fx.ember(COLOR, 1.0F), c, 10, 0.25);
                sl.sendParticles(Fx.slash(0xFFD36B, 1.4F), c.x, c.y, c.z, 1, 0, 0, 0, 0);
                Fx.sound(sl, c.x, c.y, c.z, ModSounds.KATANA_SLASH.get(), SoundSource.PLAYERS, 0.5F, 1.5F);
            }
        }
        if (tick % 9 == 0) {
            RelicSounds.play(sl, next, "solar_chakram", "spin", 0.7F, returning ? 1.15F : 1.0F);
        }
        final boolean r2 = returning;
        final Vec3 d2 = d;
        final Set<UUID> h2 = hs;
        Scheduler.runLater(1, () -> fly(sp, sl, next, d2, tick + 1, r2, h2));
    }

    private void draw(ServerLevel sl, Vec3 p, Vec3 prev, int tick) {
        Fx.ring(sl, Fx.dust(COLOR, 0.9F), p.x, p.y, p.z, 0.75, 16, tick * 0.8);
        Fx.ring(sl, Fx.dust(0xFFE9A0, 0.6F), p.x, p.y, p.z, 0.42, 8, -tick * 0.8);
        for (int k = 0; k < 4; k++) {
            double a = tick * 1.0 + k * Math.PI / 2.0;
            sl.sendParticles(Fx.spark(0xFFFFFF, 0.7F), p.x + Math.cos(a) * 0.95, p.y, p.z + Math.sin(a) * 0.95, 1, 0, 0, 0, 0);
        }
        sl.sendParticles(Fx.dust(0xFFF3C0, 1.3F), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        sl.sendParticles(Fx.ember(COLOR, 0.9F), prev.x, prev.y, prev.z, 3, 0.15, 0.1, 0.15, 0.02);
        sl.sendParticles(ParticleTypes.FLAME, prev.x, prev.y, prev.z, 2, 0.12, 0.05, 0.12, 0.01);
        sl.sendParticles(Fx.wisp(0x40200A, 0.7F), prev.x, prev.y, prev.z, 1, 0.1, 0.05, 0.1, 0.0);
    }
}

package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Singularidad: abre un agujero negro donde miras que arrastra a los enemigos y luego implosiona. */
public class VoidSingularityItem extends RelicItem {
    private static final int COLOR = 0x9A2BFF;
    private static final int LIFE = 64;

    public VoidSingularityItem() {
        super("void_singularity", COLOR, 20 * 40, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 4;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.isCrouching()) {
            return;
        }
        Vec3 c = sp.position().add(0, 0.8, 0);
        for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, new AABB(c.x - 5, c.y - 3, c.z - 5, c.x + 5, c.y + 3, c.z + 5))) {
            if (it.hasPickUpDelay() || !it.isAlive()) {
                continue;
            }
            Vec3 d = c.subtract(it.position());
            if (d.lengthSqr() < 0.6) {
                continue;
            }
            it.setDeltaMovement(it.getDeltaMovement().scale(0.4).add(d.normalize().scale(0.28)));
            if (time % 8 == 0) {
                sl.sendParticles(Fx.dust(COLOR, 0.4F), it.getX(), it.getY() + 0.2, it.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 t = RelicFx.target(sp, 22);
        Vec3 c = new Vec3(t.x, t.y + 1.3, t.z);
        Fx.sound(sl, c.x, c.y, c.z, ModSounds.RELIC_DARK.get(), SoundSource.PLAYERS, 1.2F, 0.5F);
        Fx.sound(sl, c.x, c.y, c.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.4F, 1.8F);
        step(sp, sl, c, 0);
        return true;
    }

    private void step(ServerPlayer sp, ServerLevel sl, Vec3 c, int t) {
        if (t < LIFE) {
            // Núcleo oscuro
            sl.sendParticles(Fx.dust(0x06000C, 2.2F), c.x, c.y, c.z, 2, 0.1, 0.1, 0.1, 0.0);
            sl.sendParticles(Fx.dust(0x06000C, 1.4F), c.x, c.y, c.z, 2, 0.25, 0.25, 0.25, 0.0);
            // Brazos espirales que caen hacia el centro
            for (int arm = 0; arm < 3; arm++) {
                for (int j = 0; j < 4; j++) {
                    double u = (t * 0.045 + j * 0.25) % 1.0;
                    double rad = 4.3 * (1.0 - u) + 0.3;
                    double ang = u * 6.5 + arm * 2.094 + t * 0.15;
                    Vec3 p = c.add(Math.cos(ang) * rad, Math.sin(ang * 0.7) * 0.35 * rad * 0.3, Math.sin(ang) * rad);
                    sl.sendParticles(Fx.dust(arm == 0 ? 0xC060FF : (arm == 1 ? COLOR : 0x6A20D0), 0.5F + (float) (1 - u) * 0.5F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    if (j == 0 && t % 3 == 0) {
                        sl.sendParticles(Fx.spark(0xE8C8FF, 0.5F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                }
            }
            if (t % 6 == 0) {
                Fx.ring(sl, Fx.dust(0xE8A0FF, 0.5F), c.x, c.y, c.z, 1.1 + Math.sin(t * 0.3) * 0.2, 16, t);
            }
            if (t == 0) {
                RelicFx.sigil(sl, new Vec3(c.x, c.y - 1.2, c.z), 3.0, COLOR, 0);
            }
            if (t % 8 == 0) {
                Fx.sound(sl, c.x, c.y, c.z, ModSounds.RELIC_DARK.get(), SoundSource.PLAYERS, 0.5F, 0.4F + t * 0.01F);
                RelicFx.shakeNear(sl, c, 12, 0.5F + t * 0.01F, 8);
            }
            // Atracción
            for (LivingEntity e : RelicFx.foes(sl, c, 9, sp)) {
                Vec3 d = c.subtract(e.getBoundingBox().getCenter());
                double dist = Math.max(0.5, d.length());
                double pull = Math.min(0.5, 0.12 + (9 - dist) * 0.035);
                e.setDeltaMovement(e.getDeltaMovement().scale(0.55).add(d.scale(pull / dist)));
                e.hurtMarked = true;
                e.fallDistance = 0.0F;
                if (t % 10 == 0 && dist < 4.5) {
                    RelicFx.hurt(sl, sp, e, 2.5F);
                    e.invulnerableTime = 0;
                }
            }
            for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, new AABB(c.x - 9, c.y - 9, c.z - 9, c.x + 9, c.y + 9, c.z + 9))) {
                Vec3 d = c.subtract(it.position());
                if (d.lengthSqr() > 1.0) {
                    it.setDeltaMovement(it.getDeltaMovement().scale(0.5).add(d.normalize().scale(0.25)));
                }
            }
            Scheduler.runLater(1, () -> step(sp, sl, c, t + 1));
            return;
        }
        // Implosión
        Fx.sound(sl, c.x, c.y, c.z, ModSounds.RELIC_BOOM.get(), SoundSource.PLAYERS, 0.9F, 0.5F);
        RelicSounds.play(sl, c, "void_singularity", "implode", 1.2F, 1.0F);
        RelicFx.shakeNear(sl, c, 18, 3.0F, 24);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        RelicFx.burst(sl, Fx.spark(0xF0D0FF, 1.2F), c, 50, 0.7);
        RelicFx.burst(sl, Fx.dust(COLOR, 1.4F), c, 40, 0.5);
        RelicFx.burst(sl, Fx.wisp(0x1A0A30, 1.4F), c, 16, 0.2);
        for (int r = 1; r <= 7; r++) {
            final int rr = r;
            Scheduler.runLater(r, () -> {
                Fx.ring(sl, Fx.dust(0xE8A0FF, 1.3F), c.x, c.y, c.z, rr, 14 + rr * 5, rr * 0.4);
                RelicFx.ringFacing(sl, Fx.spark(0xFFFFFF, 0.8F), c, new Vec3(0.3, 1, 0.2), rr * 0.9, 12 + rr * 3, rr);
            });
        }
        for (LivingEntity e : RelicFx.foes(sl, c, 6.5, sp)) {
            RelicFx.hurt(sl, sp, e, 10.0F);
            Vec3 d = e.getBoundingBox().getCenter().subtract(c);
            Vec3 n = d.lengthSqr() < 0.01 ? new Vec3(0, 1, 0) : d.normalize();
            e.setDeltaMovement(n.scale(1.1).add(0, 0.4, 0));
            e.hurtMarked = true;
        }
    }
}

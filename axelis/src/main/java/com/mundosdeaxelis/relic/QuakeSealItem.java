package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
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

/** Sello del Terremoto: la tierra se abre en ondas que lanzan por los aires a los enemigos. */
public class QuakeSealItem extends RelicItem {
    private static final int COLOR = 0xC89A5A;
    private static final int RADIUS = 10;

    public QuakeSealItem() {
        super("quake_seal", COLOR, 20 * 22, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.getY() < 50 && !sl.canSeeSky(sp.blockPosition())) {
            sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, false, true));
            if (time % 40 == 0) {
                sl.sendParticles(Fx.dust(COLOR, 0.5F), sp.getX(), sp.getY() + 0.1, sp.getZ(), 3, 0.4, 0.05, 0.4, 0.0);
            }
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 c = sp.position();
        Fx.sound(sl, c.x, c.y, c.z, ModSounds.RELIC_BOOM.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
        RelicSounds.play(sl, c, relicId(), "wave", 1.2F, 1.0F);
        RelicFx.shake(sp, 3.0F, 36);
        RelicFx.shakeNear(sl, c, 18, 2.5F, 32);
        Set<UUID> hit = new HashSet<>();
        wave(sp, sl, c, 1, hit);
        return true;
    }

    private void wave(ServerPlayer sp, ServerLevel sl, Vec3 c, int r, Set<UUID> hit) {
        if (r > RADIUS) {
            return;
        }
        int n = 10 + r * 7;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0 * i / n + r * 0.3;
            double px = c.x + Math.cos(a) * r;
            double pz = c.z + Math.sin(a) * r;
            sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, px, c.y, pz)), px, c.y + 0.25, pz, 3, 0.12, 0.3, 0.12, 0.2);
            if (i % 2 == 0) {
                sl.sendParticles(Fx.wisp(0xB09870, 0.9F), px, c.y + 0.2, pz, 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
        // Grietas que brillan
        for (int s = 0; s < 8; s++) {
            double a = Math.PI * 2.0 * s / 8 + 0.2;
            for (double d = Math.max(0, r - 1); d <= r; d += 0.35) {
                sl.sendParticles(Fx.ember(0xFFB040, 0.7F), c.x + Math.cos(a) * d, c.y + 0.1, c.z + Math.sin(a) * d, 1, 0.03, 0.0, 0.03, 0.0);
            }
        }
        if (r % 2 == 0) {
            Fx.sound(sl, c.x + r, c.y, c.z, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
        for (LivingEntity e : RelicFx.foes(sl, c, r + 1.4, sp)) {
            double d = Math.sqrt((e.getX() - c.x) * (e.getX() - c.x) + (e.getZ() - c.z) * (e.getZ() - c.z));
            if (Math.abs(d - r) <= 1.4 && hit.add(e.getUUID())) {
                RelicFx.hurt(sl, sp, e, 5.0F + (RADIUS - r) * 0.3F);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                sl.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.3);
            }
        }
        Scheduler.runLater(1, () -> wave(sp, sl, c, r + 1, hit));
    }
}

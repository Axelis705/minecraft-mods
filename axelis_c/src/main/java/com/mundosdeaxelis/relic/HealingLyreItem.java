package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Lira Sanadora: tócala (mantén el clic) y su melodía cura a quien esté cerca. */
public class HealingLyreItem extends RelicItem {
    private static final int COLOR = 0x7CFFB0;
    private static final int[] MELODY = {0, 2, 4, 7, 9, 7, 4, 2, 0, 4, 7, 12, 9, 7, 4, 7};

    public HealingLyreItem() {
        super("healing_lyre", COLOR, 20 * 25, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        for (ServerPlayer p : sl.players()) {
            if (p != sp && p.position().distanceToSqr(sp.position()) <= 64.0) {
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                if (time % 60 == 0) {
                    sl.sendParticles(Fx.heart(0xFF8AB0, 0.9F), sp.getX(), sp.getY() + 2.1, sp.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                    sl.sendParticles(Fx.heart(0xFF8AB0, 0.9F), p.getX(), p.getY() + 2.1, p.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }
    }

    @Override
    protected int channelTicks() {
        return 160;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.BRUSH;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        Vec3 h = RelicFx.hand(sp);
        if (used % 8 == 0) {
            int semi = MELODY[(used / 8) % MELODY.length];
            float pitch = (float) (0.5 * Math.pow(2.0, semi / 12.0));
            RelicSounds.play(sl, sp, relicId(), "note", 1.0F, (float) Math.pow(2.0, semi / 12.0));
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.25F, pitch * 2.0F);
            int hue = Fx.rainbow(semi / 14.0F);
            sl.sendParticles(Fx.note(hue, 1.2F), h.x, h.y + 0.2, h.z, 0, (sl.random.nextDouble() - 0.5) * 0.1, 0.07, (sl.random.nextDouble() - 0.5) * 0.1, 1.0);
            sl.sendParticles(ParticleTypes.NOTE, sp.getX(), sp.getY() + 2.2, sp.getZ(), 0, semi / 24.0, 0, 0, 1.0);
            Fx.ring(sl, Fx.spark(COLOR, 0.6F), h.x, h.y, h.z, 0.45, 8, used);
        }
        if (used % 4 == 0) {
            double a = used * 0.2;
            sl.sendParticles(Fx.dust(COLOR, 0.7F), sp.getX() + Math.cos(a) * 0.9, sp.getY() + 0.1 + (used % 40) * 0.05, sp.getZ() + Math.sin(a) * 0.9, 1, 0, 0, 0, 0);
        }
        if (used % 40 == 0) {
            RelicFx.sigil(sl, sp.position(), 3.5, COLOR, used * 0.05);
        }
        if (used == 4) {
            Fx.screen(sp, ScreenFxPacket.HEARTS, 0x7CFFB0, 150);
        }
        if (used % 20 == 10) {
            heal(sp, sl);
        }
    }

    private void heal(ServerPlayer sp, ServerLevel sl) {
        Vec3 c = sp.position();
        AABB box = new AABB(c.x - 8, c.y - 4, c.z - 8, c.x + 8, c.y + 4, c.z + 8);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && (e instanceof net.minecraft.world.entity.player.Player
                || (e instanceof TamableAnimal t && t.isTame())))) {
            if (e.getHealth() < e.getMaxHealth()) {
                e.heal(e == sp ? 1.0F : 2.0F);
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false, true));
                sl.sendParticles(Fx.heart(COLOR, 0.9F), e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 2, 0.25, 0.1, 0.25, 0.0);
                sl.sendParticles(Fx.spark(COLOR, 0.7F), e.getX(), e.getY() + 0.5, e.getZ(), 5, 0.3, 0.5, 0.3, 0.0);
            }
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        if (used < 12) {
            return;
        }
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.2F, 1.5F);
        spend(sp, stack, cooldownTicks(), true);
    }
}

package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Glaive Umbral: seis hojas de sombra giran a tu alrededor y salen disparadas a cazar enemigos. */
public class UmbralGlaiveItem extends RelicItem {
    private static final int COLOR = 0x8A4AFF;
    private static final int BLADES = 6;
    private static final int ORBIT = 20;

    public UmbralGlaiveItem() {
        super("umbral_glaive", COLOR, 20 * 16, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.getMaxLocalRawBrightness(sp.blockPosition()) < 6) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, false, true));
            sl.sendParticles(Fx.wisp(0x3A1A6A, 0.8F), sp.getX(), sp.getY() + 0.4, sp.getZ(), 1, 0.4, 0.3, 0.4, 0.0);
        }
    }

    private static Vec3 bladePos(Vec3 c, int k, int tick) {
        double ang = tick * 0.5 + k * Math.PI * 2.0 / BLADES;
        double r = 1.2 + 0.7 * Math.sin(Math.min(1.0, tick / 10.0) * Math.PI * 0.5);
        double h = 1.0 + 0.45 * Math.sin(tick * 0.35 + k * 1.3);
        return c.add(Math.cos(ang) * r, h, Math.sin(ang) * r);
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        List<LivingEntity> foes = RelicFx.foes(sl, sp.position(), 14, sp);
        if (foes.isEmpty()) {
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.no_target"), true);
            return false;
        }
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_DARK.get(), SoundSource.PLAYERS, 0.9F, 1.1F);
        Fx.screen(sp, ScreenFxPacket.DARK, 0x6A2AD0, ORBIT + 10);
        orbit(sp, sl, 0);
        return true;
    }

    private void orbit(ServerPlayer sp, ServerLevel sl, int tick) {
        if (!sp.isAlive() || sp.isRemoved()) {
            return;
        }
        Vec3 c = sp.position();
        if (tick < ORBIT) {
            for (int k = 0; k < BLADES; k++) {
                Vec3 p = bladePos(c, k, tick);
                sl.sendParticles(Fx.slash(COLOR, 0.7F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                sl.sendParticles(Fx.wisp(0x2A1050, 0.7F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                sl.sendParticles(Fx.dust(COLOR, 0.6F), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
            if (tick % 5 == 0) {
                RelicFx.sigil(sl, c, 1.8, COLOR, tick * 0.2);
                Fx.sound(sl, c.x, c.y, c.z, ModSounds.RELIC_WHOOSH.get(), SoundSource.PLAYERS, 0.35F, 1.2F + tick * 0.02F);
            }
            Scheduler.runLater(1, () -> orbit(sp, sl, tick + 1));
            return;
        }
        // Lanzamiento
        List<LivingEntity> foes = RelicFx.foes(sl, c, 14, sp);
        Vec3 look = sp.getLookAngle();
        for (int k = 0; k < BLADES; k++) {
            Vec3 start = bladePos(c, k, ORBIT);
            LivingEntity t = foes.isEmpty() ? null : foes.get(k % foes.size());
            Vec3 end = t != null ? t.getBoundingBox().getCenter() : c.add(0, 1, 0).add(look.scale(9)).add((k - 2.5) * 0.8, 0, 0);
            fly(sp, sl, start, end, t, 1, k);
        }
        Fx.sound(sl, c.x, c.y, c.z, ModSounds.RELIC_WHOOSH.get(), SoundSource.PLAYERS, 0.6F, 1.6F);
        RelicSounds.play(sl, c, relicId(), "blade", 1.0F, 1.0F);
        RelicFx.shake(sp, 0.8F, 8);
    }

    private void fly(ServerPlayer sp, ServerLevel sl, Vec3 start, Vec3 end, LivingEntity target, int step, int k) {
        Vec3 aim = target != null && target.isAlive() ? target.getBoundingBox().getCenter() : end;
        Vec3 p = start.add(aim.subtract(start).scale(step / 5.0));
        RelicFx.line(sl, Fx.dust(COLOR, 0.6F), start.add(aim.subtract(start).scale((step - 1) / 5.0)), p, 0.35);
        sl.sendParticles(Fx.slash(0xC9A8FF, 0.9F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        sl.sendParticles(Fx.wisp(0x2A1050, 0.8F), p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
        if (step >= 5) {
            if (target != null && target.isAlive()) {
                RelicFx.hurt(sl, sp, target, 5.0F);
                RelicFx.burst(sl, Fx.spark(COLOR, 0.9F), aim, 8, 0.25);
                sl.sendParticles(Fx.slash(0xFFFFFF, 1.5F), aim.x, aim.y, aim.z, 2, 0.2, 0.2, 0.2, 0.0);
                RelicFx.burst(sl, Fx.wisp(0x1A0A30, 1.0F), aim, 4, 0.15);
                RelicSounds.playVaried(sl, aim, "umbral_glaive", "blade", 0.55F);
            } else {
                RelicFx.burst(sl, Fx.wisp(COLOR, 0.8F), aim, 5, 0.1);
            }
            return;
        }
        Scheduler.runLater(1, () -> fly(sp, sl, start, end, target, step + 1, k));
    }
}

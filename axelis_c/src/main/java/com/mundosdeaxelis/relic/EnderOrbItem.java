package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Orbe de Ender: en El Fin te protege; activo: intercambias lugar con lo que miras. */
public class EnderOrbItem extends RelicItem {
    public EnderOrbItem() {
        super("ender_orb", 0xB45CFF, 20 * 15, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.dimension() == Level.END) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Entity best = null;
        double bestD = 1e9;
        for (Entity e : sl.getEntities(sp, new AABB(eye, eye.add(look.scale(24.0))).inflate(2.0), e -> e instanceof LivingEntity && e.isAlive())) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            double t = d.dot(look);
            if (t < 1.0 || t > 24.0) {
                continue;
            }
            double off = d.subtract(look.scale(t)).length();
            if (off < 1.2 + e.getBbWidth() * 0.5 && t < bestD) {
                best = e;
                bestD = t;
            }
        }
        if (best == null) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.no_target"), true);
            return false;
        }
        Vec3 a = sp.position();
        Vec3 b = best.position();
        for (int i = 0; i <= 14; i++) {
            Vec3 p = a.lerp(b, i / 14.0);
            sl.sendParticles(Fx.dust(0xB45CFF, 1.2F), p.x, p.y + 1.0, p.z, 1, 0.1, 0.3, 0.1, 0.0);
            sl.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 1.0, p.z, 3, 0.2, 0.4, 0.2, 0.3);
        }
        best.teleportTo(a.x, a.y, a.z);
        sp.teleportTo(b.x, b.y, b.z);
        sp.fallDistance = 0.0F;
        Fx.sound(sl, a.x, a.y, a.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        Fx.sound(sl, b.x, b.y, b.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.3F);
        Fx.ring(sl, Fx.spark(0xE0B0FF, 1.1F), a.x, a.y + 0.4, a.z, 1.0, 14, 0);
        Fx.ring(sl, Fx.spark(0xE0B0FF, 1.1F), b.x, b.y + 0.4, b.z, 1.0, 14, 0);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xB45CFF, 30);
        return true;
    }
}

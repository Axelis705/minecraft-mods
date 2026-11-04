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

/** Daga del Eclipse: te teletransportas a la espalda del enemigo y lo rematas con tres cortes. */
public class EclipseDaggerItem extends RelicItem {
    private static final int COLOR = 0x9B5BFF;

    public EclipseDaggerItem() {
        super("eclipse_dagger", COLOR, 20 * 12, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.isCrouching()) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, true, false, true));
            sl.sendParticles(Fx.wisp(0x1A0A30, 0.6F), sp.getX(), sp.getY() + 0.2, sp.getZ(), 1, 0.3, 0.05, 0.3, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        LivingEntity t = RelicFx.aimedFoe(sl, sp, 16, 0.8);
        if (t == null) {
            var near = RelicFx.foes(sl, sp.position(), 8, sp);
            t = near.isEmpty() ? null : near.get(0);
        }
        if (t == null) {
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.no_target"), true);
            return false;
        }
        final LivingEntity target = t;
        Vec3 from = sp.position();
        RelicFx.burst(sl, Fx.wisp(0x1A0A30, 1.4F), from.add(0, 1, 0), 14, 0.2);
        RelicFx.burst(sl, Fx.spark(COLOR, 0.9F), from.add(0, 1, 0), 14, 0.3);
        Fx.ring(sl, Fx.dust(0x0A0010, 1.4F), from.x, from.y + 0.1, from.z, 1.0, 18, 0);
        Fx.sound(sl, from.x, from.y, from.z, ModSounds.RELIC_DARK.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
        // Estela de imagen residual entre el origen y el destino
        Vec3 dir = Vec3.directionFromRotation(0, target.getYRot());
        Vec3[] tries = {target.position().subtract(dir.scale(1.4)), target.position().add(dir.cross(new Vec3(0, 1, 0)).scale(1.3)), target.position().add(dir.cross(new Vec3(0, 1, 0)).scale(-1.3))};
        Vec3 dest = null;
        for (Vec3 c : tries) {
            if (sl.noCollision(sp, sp.getBoundingBox().move(c.subtract(sp.position())))) {
                dest = c;
                break;
            }
        }
        if (dest != null) {
            RelicFx.line(sl, Fx.dust(COLOR, 0.9F), from.add(0, 1, 0), dest.add(0, 1, 0), 0.4);
            RelicFx.line(sl, Fx.wisp(0x1A0A30, 0.8F), from.add(0, 1.2, 0), dest.add(0, 1.2, 0), 0.8);
            Vec3 face = target.position().subtract(dest);
            float yaw = (float) (Math.toDegrees(Math.atan2(-face.x, face.z)));
            sp.teleportTo(sl, dest.x, dest.y, dest.z, yaw, 0.0F);
        }
        Fx.screen(sp, ScreenFxPacket.DARK, 0x6A2AD0, 22);
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 view = target.position().subtract(dest != null ? dest : from).normalize();
        // Eclipse: anillo oscuro con corona dorada detrás del enemigo
        for (int i = 0; i < 5; i++) {
            final int k = i;
            Scheduler.runLater(i * 2, () -> {
                RelicFx.ringFacing(sl, Fx.dust(0x0A0010, 1.5F), center.add(view.scale(0.4)), view, 1.0, 20, k);
                RelicFx.ringFacing(sl, Fx.spark(0xFFD36B, 0.8F), center.add(view.scale(0.45)), view, 1.25 + k * 0.05, 18, -k);
            });
        }
        float[] dmg = {4.0F, 4.0F, 7.0F};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            Scheduler.runLater(2 + i * 3, () -> {
                if (!target.isAlive()) {
                    return;
                }
                Vec3 c = target.getBoundingBox().getCenter();
                sl.sendParticles(Fx.slash(k == 2 ? 0xFFFFFF : 0xC9A8FF, k == 2 ? 2.0F : 1.4F), c.x, c.y, c.z, 2, 0.25, 0.25, 0.25, 0.0);
                sl.sendParticles(Fx.slash(COLOR, 1.2F), c.x, c.y, c.z, 1, 0.3, 0.3, 0.3, 0.0);
                RelicFx.burst(sl, Fx.spark(0xFFFFFF, 0.7F), c, 8, 0.3);
                RelicSounds.play(sl, c, "eclipse_dagger", "slash", k == 2 ? 1.1F : 0.8F, 0.95F + k * 0.15F);
                RelicFx.hurt(sl, sp, target, dmg[k]);
                target.invulnerableTime = 0;
            });
        }
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2));
        sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 50, 0, true, false, true));
        RelicFx.shake(sp, 1.0F, 12);
        return true;
    }
}

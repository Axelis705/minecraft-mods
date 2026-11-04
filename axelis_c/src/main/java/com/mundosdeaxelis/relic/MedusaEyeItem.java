package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Ojo de Medusa: su mirada convierte en piedra (inmóviles) a los enemigos frente a ti. */
public class MedusaEyeItem extends RelicItem {
    private static final int COLOR = 0x7DFF6A;

    public MedusaEyeItem() {
        super("medusa_eye", COLOR, 20 * 30, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.isCrouching() && sp.getDeltaMovement().horizontalDistanceSqr() < 0.002) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, true, false, true));
            sl.sendParticles(RelicFx.blockDust(Blocks.STONE.defaultBlockState()), sp.getX(), sp.getY() + 0.9, sp.getZ(), 4, 0.35, 0.7, 0.35, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity e : RelicFx.foes(sl, eye, 14, sp)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye).normalize();
            if (to.dot(look) > 0.82 && sp.hasLineOfSight(e)) {
                hit.add(e);
            }
        }
        if (hit.isEmpty()) {
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.medusa_none"), true);
            return false;
        }
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.6F, 0.6F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), ModSounds.RELIC_DARK.get(), SoundSource.PLAYERS, 0.9F, 0.7F);
        Fx.screen(sp, ScreenFxPacket.DARK, COLOR, 30);
        RelicFx.shake(sp, 1.0F, 12);
        // El cono verde se abre hacia delante.
        for (int i = 1; i <= 7; i++) {
            final int d = i;
            Scheduler.runLater(i, () -> {
                RelicFx.ringFacing(sl, Fx.dust(COLOR, 1.0F), eye.add(look.scale(d * 2.0)), look, d * 0.5, 10 + d * 3, d);
                RelicFx.ringFacing(sl, Fx.wisp(0x2A7A2A, 0.8F), eye.add(look.scale(d * 2.0)), look, d * 0.4, 6, -d);
            });
        }
        for (LivingEntity e : hit) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 8, 9));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 3));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 8, 0));
            RelicFx.hurt(sl, sp, e, 3.0F);
            Vec3 c = e.getBoundingBox().getCenter();
            RelicSounds.playVaried(sl, c, "medusa_eye", "crack", 0.85F);
            RelicFx.burst(sl, RelicFx.blockDust(Blocks.STONE.defaultBlockState()), c, 24, 0.25);
            RelicFx.burst(sl, Fx.spark(COLOR, 0.8F), c, 8, 0.2);
            petrify(sl, e, 0);
        }
        return true;
    }

    private static void petrify(ServerLevel sl, LivingEntity e, int step) {
        if (step > 15 || !e.isAlive()) {
            return;
        }
        Vec3 c = e.getBoundingBox().getCenter();
        sl.sendParticles(RelicFx.blockDust(Blocks.STONE.defaultBlockState()), c.x, c.y, c.z, 5, e.getBbWidth() * 0.4, e.getBbHeight() * 0.4, e.getBbWidth() * 0.4, 0.0);
        if (step % 3 == 0) {
            sl.sendParticles(Fx.dust(0x8F9A8F, 0.8F), c.x, c.y, c.z, 4, e.getBbWidth() * 0.4, e.getBbHeight() * 0.4, e.getBbWidth() * 0.4, 0.0);
        }
        Scheduler.runLater(10, () -> petrify(sl, e, step + 1));
    }
}

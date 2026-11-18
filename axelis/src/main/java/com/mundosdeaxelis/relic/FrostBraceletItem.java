package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;

/** Pulsera de Hielo: congela el agua bajo tus pies al caminar; activo: onda helada que ralentiza a los enemigos. */
public class FrostBraceletItem extends RelicItem {
    public FrostBraceletItem() {
        super("frost_bracelet", 0x9FE6FF, 20 * 20, Rarity.RARE);
    }

    @Override
    protected int interval() {
        return 3;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (!sp.onGround() || sp.isShiftKeyDown()) {
            return;
        }
        BlockPos base = sp.blockPosition().below();
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, 0, -2), base.offset(2, 0, 2))) {
            if (p.distToCenterSqr(sp.getX(), sp.getY(), sp.getZ()) > 6.0) {
                continue;
            }
            if (sl.getBlockState(p) == Blocks.WATER.defaultBlockState() && sl.getBlockState(p.above()).isAir() && ice.canSurvive(sl, p)) {
                sl.setBlockAndUpdate(p, ice);
                sl.scheduleTick(p, Blocks.FROSTED_ICE, Mth.nextInt(sl.getRandom(), 60, 120));
                sl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.01);
            }
        }
        if (sp.isOnFire()) {
            sp.clearFire();
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F);
        Fx.sound(sl, x, y, z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0x9FE6FF, 60);
        for (int i = 1; i <= 7; i++) {
            final int r = i * 2;
            com.mundosdeaxelis.world.Scheduler.runLater(i * 2, () -> {
                Fx.ring(sl, Fx.dust(0x9FE6FF, 1.4F), x, y + 0.2, z, r, 12 + r * 3, 0);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.6, z, 20, r * 0.5, 0.3, r * 0.5, 0.02);
            });
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 14, y - 6, z - 14, x + 14, y + 6, z + 14), e -> e instanceof Enemy && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 8, 3));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 1));
            e.setTicksFrozen(e.getTicksRequiredToFreeze() + 100);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + 1.0, e.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        return true;
    }
}

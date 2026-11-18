package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lava falsa (cráter del volcán): se ve y brilla como lava, pero no quema ni tiene suelo. Quien se tira dentro atraviesa la capa
 * y cae despacio (caída lenta unos segundos) hacia la dungeon que hay debajo.
 */
public class LavaFalsaBlock extends Block {
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 14, 16);

    public LavaFalsaBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity le)) {
            return;
        }
        le.resetFallDistance();
        // solo si debajo hay un hueco (el lago del cráter); en los ríos de las laderas se camina sin más
        if (!level.getBlockState(pos.below()).isAir()) {
            return;
        }
        if (!le.hasEffect(MobEffects.SLOW_FALLING) || le.getEffect(MobEffects.SLOW_FALLING).getDuration() < 60) {
            le.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0, false, false, true));
            if (!level.isClientSide) {
                level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.2F, 0.6F);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 0.7F);
                if (le instanceof ServerPlayer sp) {
                    com.mundosdeaxelis.world.Titles.actionBar(sp, "¡La lava era falsa! Caes al corazón del volcán...");
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (r.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + r.nextDouble(), pos.getY() + 0.95, pos.getZ() + r.nextDouble(), 0.0, 0.0, 0.0);
        }
        if (r.nextInt(220) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.3F,
                    0.8F + r.nextFloat() * 0.3F, false);
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}

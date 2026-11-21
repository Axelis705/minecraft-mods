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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lava falsa (cráter del volcán): se ve y brilla como lava, pero no quema ni tiene suelo. Quien se tira dentro atraviesa la capa
 * y cae despacio (caída lenta unos segundos) hacia la dungeon que hay debajo.
 * <p>Etapa 263: {@link #FLUJO} dice hacia dónde corre (para que los ríos de las laderas y las cascadas se vean moverse): {@code up} es
 * lava quieta (el lago), {@code down} es una cascada (los lados caen) y las cuatro direcciones son ríos que corren hacia allí.
 */
public class LavaFalsaBlock extends Block {
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 14, 16);
    public static final DirectionProperty FLUJO = DirectionProperty.create("flujo");

    public LavaFalsaBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FLUJO, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FLUJO);
    }

    /** Al girar una estructura, el río sigue corriendo ladera abajo. */
    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rot) {
        Direction f = state.getValue(FLUJO);
        return f.getAxis().isHorizontal() ? state.setValue(FLUJO, rot.rotate(f)) : state;
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        Direction f = state.getValue(FLUJO);
        return f.getAxis().isHorizontal() ? state.setValue(FLUJO, mirror.mirror(f)) : state;
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
        Direction f = state.getValue(FLUJO);
        if (f == Direction.DOWN) {
            // cascada: gotas que caen por debajo y algo de humo
            if (r.nextInt(6) == 0 && level.getBlockState(pos.below()).isAir()) {
                level.addParticle(ParticleTypes.DRIPPING_LAVA, pos.getX() + r.nextDouble(), pos.getY() - 0.05, pos.getZ() + r.nextDouble(), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (f != Direction.UP) {
            // río: humo que sube y alguna chispa que salta en el sentido de la corriente
            if (r.nextInt(9) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 0.9, pos.getZ() + r.nextDouble(), 0.0, 0.03, 0.0);
            }
            if (r.nextInt(40) == 0) {
                level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
            }
            if (r.nextInt(400) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.25F,
                        0.8F + r.nextFloat() * 0.3F, false);
            }
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

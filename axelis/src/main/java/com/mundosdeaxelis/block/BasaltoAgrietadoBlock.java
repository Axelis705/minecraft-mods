package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Basalto agrietado (dungeon del volcán): suelo que se derrumba. Al pisarlo cruje (grietas que brillan, chispas y polvo), al
 * segundo se rompe y cae dejando un hueco, y a los 10 s vuelve a formarse. Los bloques vecinos del mismo tipo también empiezan a
 * crujir un poco después, así que no conviene quedarse quieto. No se puede romper ni recoger.
 * <p>Etapas: 0 entero, 1 cruje, 2 a punto de caer, 3 caído (sin forma: no se ve ni se pisa).
 */
public class BasaltoAgrietadoBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    private static final int CRACK_TICKS = 10;
    private static final int FALL_TICKS = 10;
    private static final int REFORM_TICKS = 200;

    public BasaltoAgrietadoBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(STAGE) == 3 ? Shapes.empty() : Shapes.block();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(STAGE) == 3 ? Shapes.empty() : Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(STAGE) == 3 ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(STAGE) == 3;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && state.getValue(STAGE) == 0 && entity instanceof Player p && !p.isCreative() && !p.isSpectator()) {
            crack((ServerLevel) level, pos, state);
        }
        super.stepOn(level, pos, state, entity);
    }

    private void crack(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, CRACK_TICKS);
        level.playSound(null, pos, SoundEvents.BASALT_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BASALT.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 1.0,
                pos.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.05);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        int s = state.getValue(STAGE);
        if (s == 1) {
            level.setBlock(pos, state.setValue(STAGE, 2), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, FALL_TICKS);
            level.playSound(null, pos, SoundEvents.BASALT_BREAK, SoundSource.BLOCKS, 1.2F, 0.45F);
            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.0, 0.3, 0.0);
            // el crujido se contagia a los vecinos
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = pos.relative(d);
                BlockState ns = level.getBlockState(n);
                if (ns.is(this) && ns.getValue(STAGE) == 0 && r.nextInt(3) == 0) {
                    crack(level, n, ns);
                }
            }
        } else if (s == 2) {
            level.setBlock(pos, state.setValue(STAGE, 3), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, REFORM_TICKS + r.nextInt(40));
            level.playSound(null, pos, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.BLOCKS, 1.4F, 0.5F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BASALT.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.1);
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.BASALT.defaultBlockState()), pos.getX() + 0.5,
                    pos.getY(), pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.0);
        } else if (s == 3) {
            // vuelve a formarse si no hay nadie en el hueco
            if (!level.getEntitiesOfClass(Entity.class, new AABB(pos)).isEmpty()) {
                level.scheduleTick(pos, this, 20);
                return;
            }
            level.setBlock(pos, state.setValue(STAGE, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BASALT_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.01);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        // si se guardó a medio caer (o el comando lo puso así), que siga su ciclo
        // (no cuando cambia de etapa él mismo: su propio tick ya programa el siguiente y uno duplicado se ignoraría)
        if (!level.isClientSide && !old.is(this) && state.getValue(STAGE) != 0 && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        int s = state.getValue(STAGE);
        if (s == 1 || s == 2) {
            for (int i = 0; i < s * 2; i++) {
                level.addParticle(r.nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 1.02,
                        pos.getZ() + r.nextDouble(), 0.0, 0.02, 0.0);
            }
        } else if (s == 0 && r.nextInt(40) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 1.02, pos.getZ() + r.nextDouble(), 0.0, 0.01, 0.0);
        }
    }
}

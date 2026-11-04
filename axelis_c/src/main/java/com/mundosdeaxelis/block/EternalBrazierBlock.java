package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Brasero Eterno: cuenco de hierro con borde dorado sobre un pedestal de piedra, con llama animada. Encendido da luz 15 y protege del
 * frío: dentro de 5 bloques el medidor de frío baja muy rápido y el hielo que te atrapa se rompe solo ({@code Cold.heat}).
 * Se enciende con yesquero o bola de fuego; la ventisca del Lobo Ártico puede apagarlo ({@link #setLit}). Al ponerlo nace encendido.
 * Alrededor del suelo flota un anillo de chispas naranjas que marca hasta dónde llega su calor.
 */
public class EternalBrazierBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Radio (horizontal) en bloques en el que da calor. */
    public static final double WARM_RADIUS = 5.0;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(5, 2, 5, 11, 10, 11),
            Block.box(2.5, 10, 2.5, 13.5, 16, 13.5));

    public EternalBrazierBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(LIT, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static boolean isLit(BlockState state) {
        return state.getBlock() instanceof EternalBrazierBlock && state.getValue(LIT);
    }

    /** Enciende o apaga un brasero (con sonido). */
    public static void setLit(Level level, BlockPos pos, boolean lit) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof EternalBrazierBlock && st.getValue(LIT) != lit) {
            level.setBlock(pos, st.setValue(LIT, lit), 3);
            level.playSound(null, pos, lit ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    lit ? 1.0F : 0.8F, lit ? 0.8F : 1.0F);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!state.getValue(LIT) && (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE))) {
            if (!level.isClientSide) {
                setLit(level, pos, true);
                if (!player.getAbilities().instabuild) {
                    if (held.is(Items.FLINT_AND_STEEL)) {
                        held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                    } else {
                        held.shrink(1);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.5, y + 0.95, z + (random.nextDouble() - 0.5) * 0.5,
                    0.0, 0.02 + random.nextDouble() * 0.02, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.4, y + 1.2, z + (random.nextDouble() - 0.5) * 0.4,
                    (random.nextDouble() - 0.5) * 0.02, 0.05, (random.nextDouble() - 0.5) * 0.02);
        }
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.LAVA, x + (random.nextDouble() - 0.5) * 0.4, y + 1.0, z + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5F + random.nextFloat() * 0.3F,
                    random.nextFloat() * 0.7F + 0.6F, false);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1.3, z, 0.0, 0.04, 0.0);
        }
        // anillo de chispas naranjas a ras de suelo: hasta aquí llega el calor
        if (random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2.0;
            level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.6F, 0.25F), 0.9F),
                    x + Math.cos(a) * WARM_RADIUS, y + 0.1, z + Math.sin(a) * WARM_RADIUS, 0.0, 0.02, 0.0);
        }
    }
}

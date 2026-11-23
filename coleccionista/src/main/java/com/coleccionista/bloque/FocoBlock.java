package com.coleccionista.bloque;

import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Un foco colgando del techo que el evento prende y apaga (el "¡pum!" del despertar, los parpadeos, los apagones). Sin redstone: lo
 * cambia el mod directamente. Algunos están "malos" (fijo según dónde están): cuando están prendidos fallan solos, con rachas de
 * parpadeos y su chispazo.
 */
public class FocoBlock extends Block {
    public static final BooleanProperty ENCENDIDO = BooleanProperty.create("encendido");
    /** Se apagó solo un instante (falla del foco viejo). */
    public static final BooleanProperty FALLA = BooleanProperty.create("falla");
    private static final VoxelShape FORMA = Block.box(5, 2, 5, 11, 16, 11);
    /** Qué parte de los focos están malos. */
    public static int porcentajeMalos = 40;

    public FocoBlock(Properties p) {
        super(p.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(ENCENDIDO, false).setValue(FALLA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ENCENDIDO, FALLA);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    /** ¿Da luz ahora? */
    public static boolean prendido(BlockState s) {
        return s.getValue(ENCENDIDO) && !s.getValue(FALLA);
    }

    public static boolean malo(BlockPos p) {
        return Math.floorMod(Mth.getSeed(p) >> 9, 100) < porcentajeMalos;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moving) {
        super.onPlace(s, l, p, old, moving);
        if (l instanceof ServerLevel sl && s.getValue(ENCENDIDO) && malo(p) && (!old.is(this) || !old.getValue(ENCENDIDO))
                && !sl.getBlockTicks().hasScheduledTick(p, this)) {
            sl.scheduleTick(p, this, 40 + sl.random.nextInt(200));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        if (s.getValue(ENCENDIDO) && malo(p) && !l.getBlockTicks().hasScheduledTick(p, this)) {
            l.scheduleTick(p, this, 1 + r.nextInt(40));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        boolean falla = s.getValue(FALLA);
        if (!s.getValue(ENCENDIDO)) {
            if (falla) {
                l.setBlock(p, s.setValue(FALLA, false), Block.UPDATE_CLIENTS);
            }
            return;
        }
        if (falla) {
            l.setBlock(p, s.setValue(FALLA, false), Block.UPDATE_CLIENTS);
            l.scheduleTick(p, this, r.nextFloat() < 0.5F ? 2 + r.nextInt(4) : 80 + r.nextInt(400));
        } else if (malo(p)) {
            l.setBlock(p, s.setValue(FALLA, true), Block.UPDATE_CLIENTS);
            l.playSound(null, p, ModRegistry.CHISPA.get(), SoundSource.BLOCKS, 0.35F + r.nextFloat() * 0.2F, 0.9F + r.nextFloat() * 0.3F);
            if (r.nextFloat() < 0.25F) {
                l.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.4, p.getZ() + 0.5, 2 + r.nextInt(3), 0.15, 0.1, 0.15,
                        0.05);
            }
            l.scheduleTick(p, this, r.nextFloat() < 0.85F ? 1 + r.nextInt(4) : 12 + r.nextInt(40));
        }
    }
}

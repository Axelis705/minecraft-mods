package com.navidad.bloque;

import com.navidad.Navidad;
import com.navidad.entidad.Krampus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El farol de Nochebuena: un farol dorado con vidrios y su vela, en un poste. Alumbra mucho; con clic derecho se enciende (y agachado,
 * se apaga). En el apagón del Krampus es lo que lo saca de la oscuridad: cerca de su luz se le ve y se lastima más, y si lo enciendes
 * junto a él, se deslumbra. Los que pone el Krampus solo ({@code temporal}) se quitan solos cuando ya no está.
 */
public class FarolNochebuena extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty TEMPORAL = BooleanProperty.create("temporal");
    private static final VoxelShape FORMA = Shapes.or(Block.box(7, 0, 7, 9, 8, 9), Block.box(4.5, 8, 4.5, 11.5, 16, 11.5));
    /** Para la prueba con fotos. */
    public static int encendidos;

    public FarolNochebuena(Properties p) {
        super(p.lightLevel(s -> s.getValue(LIT) ? 15 : 0).randomTicks());
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(TEMPORAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT, TEMPORAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        if (h != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        boolean lit = s.getValue(LIT);
        if (lit && !p.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        l.setBlock(pos, s.setValue(LIT, !lit), 3);
        if (!lit) {
            encender((ServerLevel) l, pos, p);
        } else {
            l.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1F, 1F);
        }
        return InteractionResult.CONSUME;
    }

    private static void encender(ServerLevel l, BlockPos pos, Player p) {
        encendidos++;
        l.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1F, 1F);
        l.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.6F);
        com.axeliscore.fx.Efectos.lanzar(l, Navidad.MODID + ":farol_enciende", net.minecraft.world.phys.Vec3.atCenterOf(pos));
        for (Krampus k : l.getEntitiesOfClass(Krampus.class, new net.minecraft.world.phys.AABB(pos).inflate(12))) {
            k.alEncenderFarol(pos, p);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState s, ServerLevel l, BlockPos pos, RandomSource r) {
        if (s.getValue(TEMPORAL) && l.getEntitiesOfClass(Krampus.class, new net.minecraft.world.phys.AABB(pos).inflate(48)).isEmpty()) {
            l.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos pos, RandomSource r) {
        if (s.getValue(LIT) && r.nextInt(3) == 0) {
            l.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.72, pos.getZ() + 0.5, 0, 0.005, 0);
        }
    }
}

package com.celular;

import com.celular.plat.Plat;
import com.celular.plat.UseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bocina Bluetooth: clic derecho para conectar tu celular (otra vez para desconectarlo). Lo que suena en tu celular sale desde la bocina,
 * más fuerte y con más alcance (40 bloques); las canciones del mundo las oyen todos los de cerca. Mientras suena se enciende: luces,
 * el bajo que late (textura animada) y notas musicales.
 */
public class SpeakerBlock extends UseBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ON = BooleanProperty.create("on");
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // la caja mide 10 × 14 × 8 (más el asa); el frente mira hacia FACING
        SHAPES.put(Direction.NORTH, Shapes.or(Block.box(3, 0, 3.5, 13, 14, 12), Block.box(5, 14, 7.5, 11, 16, 8.5)));
        SHAPES.put(Direction.SOUTH, Shapes.or(Block.box(3, 0, 4, 13, 14, 12.5), Block.box(5, 14, 7.5, 11, 16, 8.5)));
        SHAPES.put(Direction.WEST, Shapes.or(Block.box(3.5, 0, 3, 12, 14, 13), Block.box(7.5, 14, 5, 8.5, 16, 11)));
        SHAPES.put(Direction.EAST, Shapes.or(Block.box(4, 0, 3, 12.5, 14, 13), Block.box(7.5, 14, 5, 8.5, 16, 11)));
    }

    public SpeakerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ON);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            com.celular.radio.Speakers.pair(sp, level, pos);
        }
        return Plat.ok(level.isClientSide());
    }

    /** Si quedó encendida sin que nadie la use (por ejemplo, el servidor se cerró mientras sonaba), se apaga sola. */
    @Override
    public void randomTick(BlockState s, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource r) {
        if (s.getValue(ON) && !com.celular.radio.Speakers.active(level, pos)) {
            level.setBlock(pos, s.setValue(ON, false), 3);
        }
    }

    /** Encendida: notas musicales que salen de la bocina (solo se dibujan, no hacen nada). */
    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (!s.getValue(ON)) {
            return;
        }
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 1.05, pos.getZ() + 0.3 + r.nextDouble() * 0.4,
                    r.nextInt(25) / 24.0, 0, 0);
        }
    }
}

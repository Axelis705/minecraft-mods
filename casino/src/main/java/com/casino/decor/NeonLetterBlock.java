package com.casino.decor;

import com.casino.client.ClientHooks;
import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Letra de neón: varias juntas forman palabras ("CASINO", "BAR"...). Clic derecho abre una pantalla para elegir la letra y el
 * color, y para escribir con el teclado (salta sola a la letra de la derecha).
 */
public class NeonLetterBlock extends UseBlock {
    /** Mismo orden que en herramientas/neon_tex.py. */
    public static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$&♥♠♦♣★- ";
    public static final String[] COLORS = {"red", "pink", "gold", "green", "cyan", "white"};
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, CHARS.length() - 1);
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, COLORS.length - 1);
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{0, 0, 14, 16, 16, 16});

    public NeonLetterBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GLYPH, 0).setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, GLYPH, COLOR);
    }

    /** Si pones una letra al lado de otra, sigue con el mismo color. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = WallDecorBlock.wallFacing(ctx);
        BlockState s = defaultBlockState().setValue(FACING, f);
        for (Direction side : new Direction[]{f.getClockWise(), f.getCounterClockWise()}) {
            BlockState n = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(side));
            if (n.is(this) && n.getValue(FACING) == f) {
                return s.setValue(COLOR, n.getValue(COLOR));
            }
        }
        return s;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    /** La letra que sigue al leer (a la derecha de quien mira el letrero). */
    public static BlockPos next(BlockState s, BlockPos pos) {
        return pos.relative(s.getValue(FACING).getCounterClockWise());
    }

    public static BlockPos previous(BlockState s, BlockPos pos) {
        return pos.relative(s.getValue(FACING).getClockWise());
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            com.casino.plat.Net.onClient(() -> ClientHooks.openNeon(pos));
        }
        return Plat.ok(level.isClientSide());
    }
}

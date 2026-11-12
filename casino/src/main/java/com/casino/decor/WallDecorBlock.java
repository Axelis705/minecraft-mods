package com.casino.decor;

import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Panel de pared con varios dibujos (letrero de neón, pantalla). Se pega a la pared donde haces clic. Clic derecho cambia el
 * dibujo (agachado, hacia atrás).
 */
public abstract class WallDecorBlock extends UseBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final Map<Direction, VoxelShape> shapes;

    protected WallDecorBlock(Properties props, double depth) {
        super(props);
        this.shapes = Shapes.rotated(new double[]{0, 0, 16 - depth, 16, 16, 16});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(design(), 0));
    }

    /** La propiedad de dibujo (cada clase tiene la suya, según cuántos dibujos hay). */
    protected abstract IntegerProperty design();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, design());
    }

    /** Letreros de neón con dibujo (12). */
    public static class Sign extends WallDecorBlock {
        public static final IntegerProperty DESIGN = IntegerProperty.create("design", 0, 11);

        public Sign(Properties p) {
            super(p, 2);
        }

        @Override
        protected IntegerProperty design() {
            return DESIGN;
        }
    }

    /** Pantallas de pared animadas (4). */
    public static class Screen extends WallDecorBlock {
        public static final IntegerProperty DESIGN = IntegerProperty.create("design", 0, 3);

        public Screen(Properties p) {
            super(p, 3);
        }

        @Override
        protected IntegerProperty design() {
            return DESIGN;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, wallFacing(ctx));
    }

    /** Si haces clic en una pared, el panel queda pegado a ella; si no, mirando hacia ti. */
    public static Direction wallFacing(BlockPlaceContext ctx) {
        Direction f = ctx.getClickedFace();
        return f.getAxis().isHorizontal() ? f : ctx.getHorizontalDirection().getOpposite();
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return shapes.get(s.getValue(FACING));
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            IntegerProperty prop = design();
            int count = prop.getPossibleValues().size();
            int d = Math.floorMod(state.getValue(prop) + (player.isShiftKeyDown() ? -1 : 1), count);
            level.setBlock(pos, state.setValue(prop, d), 3);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 1.4F);
        }
        return Plat.ok(level.isClientSide());
    }
}

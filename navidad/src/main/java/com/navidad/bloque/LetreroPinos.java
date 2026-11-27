package com.navidad.bloque;

import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El letrero del puesto de árboles de Navidad: una tabla con un pinito pintado. El que sale con el puesto en el mundo trae (y cuida) a
 * Don Abeto, el vendedor: si no está cerca, lo vuelve a poner en su lugar detrás del mostrador.
 */
public class LetreroPinos extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape FORMA = Shapes.or(Block.box(1, 0, 7, 2.5, 13, 8.5), Block.box(13.5, 0, 7, 15, 13, 8.5),
            Block.box(0.5, 6, 6.7, 15.5, 15.8, 8.4));

    public LetreroPinos(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new LetreroPinosBE(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide || t != ModRegistry.LETRERO_PINOS_BE.get() ? null : (lv, p, st, be) -> ((LetreroPinosBE) be).tick(lv);
    }
}

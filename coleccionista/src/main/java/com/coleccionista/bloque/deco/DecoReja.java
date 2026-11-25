package com.coleccionista.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La puerta corrediza de reja de las celdas. Es una puerta de dos bloques (la de hierro: nadie la abre con la mano, solo el evento), pero:
 * <ul>
 *   <li>va centrada en la línea de los barrotes y los barrotes de al lado se unen a ella;</li>
 *   <li>cuando se abre no gira: la reja sale un poco hacia el pasillo y se corre a un lado (la dibuja {@code RejaRenderer} con animación
 *   y sonido de riel); abierta no estorba nada;</li>
 *   <li>no le hace caso a la redstone.</li>
 * </ul>
 */
public class DecoReja extends DoorBlock implements EntityBlock {
    private static final VoxelShape NS = Block.box(0, 0, 7, 16, 16, 9), EW = Block.box(7, 0, 0, 9, 16, 16);

    /** Puerta de ascensor o de acero (opaca): no sale hacia el pasillo, solo se corre de lado y se mete en el muro. */
    public final boolean ascensor;

    public DecoReja(BlockBehaviour.Properties p) {
        this(p, false);
    }

    public DecoReja(BlockBehaviour.Properties p, boolean ascensor) {
        super(p, BlockSetType.IRON);
        this.ascensor = ascensor;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        if (s.getValue(OPEN)) {
            return Shapes.empty();
        }
        return s.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }

    /** Para que los barrotes de los lados se unan a la reja (como a un muro). */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getBlockSupportShape(BlockState s, BlockGetter l, BlockPos p) {
        return Shapes.block();
    }

    @Override
    public boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
        return true;
    }

    /** Solo la abre el evento: la redstone no le hace nada. */
    @Override
    public void neighborChanged(BlockState s, Level l, BlockPos p, Block b, BlockPos from, boolean moving) {
    }

    /** La mitad de abajo la dibuja su animación (las dos mitades juntas); la de arriba no se dibuja sola. */
    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState s) {
        return s.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return s.getValue(HALF) == DoubleBlockHalf.LOWER ? new RejaBE(p, s) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide && t == Deco.REJA_BE ? (lv, p, st, be) -> ((RejaBE) be).tickCliente(lv, p, st) : null;
    }
}

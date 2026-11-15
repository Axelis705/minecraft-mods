package com.casino.heist;

import com.casino.decor.Shapes;
import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Los bloques del robo a la bóveda. Todos se pegan a una pared o al piso mirando hacia {@link #FACING}; lo que pasa al usarlos lo
 * decide {@link HeistManager} (fuera de un robo solo dicen "solo personal autorizado").
 */
public final class HeistBlocks {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private HeistBlocks() {
    }

    /** Base: mira hacia FACING (lo opuesto a la pared donde se pega) con una forma dada para el norte. */
    public abstract static class Facing extends UseBlock {
        private final Map<Direction, VoxelShape> shapes;

        protected Facing(Properties p, double[]... shape) {
            super(p);
            this.shapes = Shapes.rotated(shape);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
            extra(b);
        }

        protected void extra(StateDefinition.Builder<Block, BlockState> b) {
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            Direction f = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
            return defaultBlockState().setValue(FACING, f);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return shapes.get(s.getValue(FACING));
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                use(state, level, pos, (net.minecraft.server.level.ServerPlayer) player);
            }
            return Plat.ok(level.isClientSide());
        }

        protected void use(BlockState state, Level level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
        }
    }

    // ------------------------------------------------------------------ cámara de seguridad

    public static class Camera extends Facing implements EntityBlock {
        public Camera(Properties p) {
            super(p, new double[]{5, 9, 9, 11, 15, 16});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CameraBE(pos, state);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != HeistReg.CAMERA_BE.get() || level.isClientSide()) {
                return null;
            }
            return (lvl, p, st, be) -> ((CameraBE) be).serverTick();
        }
    }

    // ------------------------------------------------------------------ emisor láser

    public static class Laser extends Facing implements EntityBlock {
        public static final IntegerProperty PATTERN = IntegerProperty.create("pattern", 0, 3);

        public Laser(Properties p) {
            super(p, new double[]{5, 5, 13, 11, 11, 16});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PATTERN, 0));
        }

        @Override
        protected void extra(StateDefinition.Builder<Block, BlockState> b) {
            b.add(PATTERN);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LaserBE(pos, state);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != HeistReg.LASER_BE.get() || level.isClientSide()) {
                return null;
            }
            return (lvl, p, st, be) -> ((LaserBE) be).serverTick();
        }
    }

    // ------------------------------------------------------------------ lector de tarjeta

    public static class Reader extends Facing {
        public Reader(Properties p) {
            super(p, new double[]{5, 3, 14, 11, 13, 16});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
        }

        @Override
        protected void extra(StateDefinition.Builder<Block, BlockState> b) {
            b.add(LIT);
        }

        @Override
        protected void use(BlockState state, Level level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
            HeistManager.useReader(player, pos);
        }
    }

    // ------------------------------------------------------------------ caja fuerte de pared

    public static class Safe extends Facing {
        public Safe(Properties p) {
            super(p, new double[]{2, 0, 6, 14, 14, 16});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
        }

        @Override
        protected void extra(StateDefinition.Builder<Block, BlockState> b) {
            b.add(OPEN);
        }

        @Override
        protected void use(BlockState state, Level level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
            HeistManager.useSafe(player, pos);
        }
    }

    // ------------------------------------------------------------------ nota con pista (sobre una mesa)

    public static class Note extends Facing {
        public Note(Properties p) {
            super(p, new double[]{3, 0, 4, 13, 1, 12});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void use(BlockState state, Level level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
            HeistManager.useClue(player, pos);
        }
    }

    // ------------------------------------------------------------------ consola de cámaras (cuarto de seguridad)

    public static class Console extends Facing {
        public Console(Properties p) {
            super(p, new double[]{1, 0, 4, 15, 6, 16});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void use(BlockState state, Level level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
            HeistManager.useConsole(player, pos);
        }
    }

    // ------------------------------------------------------------------ decoración: monitores y cajas de seguridad

    public static class Decor extends Facing {
        public Decor(Properties p, double[]... shape) {
            super(p, shape);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return InteractionResult.PASS;
        }
    }

    // ------------------------------------------------------------------ puerta de la bóveda

    public static class VaultDoor extends Facing implements EntityBlock {
        public VaultDoor(Properties p) {
            super(p, new double[]{0, 0, 0, 16, 16, 16});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
        }

        @Override
        protected void extra(StateDefinition.Builder<Block, BlockState> b) {
            b.add(OPEN);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return s.getValue(OPEN) ? net.minecraft.world.phys.shapes.Shapes.empty() : net.minecraft.world.phys.shapes.Shapes.block();
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
            return s.getValue(OPEN) ? net.minecraft.world.phys.shapes.Shapes.empty() : net.minecraft.world.phys.shapes.Shapes.block();
        }

        @Override
        public RenderShape getRenderShape(BlockState s) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected void use(BlockState state, Level level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
            HeistManager.useVaultDoor(player, pos);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new VaultDoorBE(pos, state);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (type != HeistReg.VAULT_BE.get()) {
                return null;
            }
            if (level.isClientSide()) {
                return (lvl, p, st, be) -> ((VaultDoorBE) be).clientTick();
            }
            return (lvl, p, st, be) -> ((VaultDoorBE) be).serverTick();
        }
    }

    /** El resto del hueco de la puerta: invisible y sólido mientras está cerrada (la puerta lo quita al abrir). */
    public static class Seal extends Block {
        public Seal(Properties p) {
            super(p);
        }

        @Override
        public RenderShape getRenderShape(BlockState s) {
            return RenderShape.INVISIBLE;
        }
    }

    // ------------------------------------------------------------------ carrito de botín

    public enum Kind implements StringRepresentable {
        CASH("cash"), GOLD("gold"), DIAMOND("diamond"), CHIPS("chips");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static class Cart extends Facing {
        public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
        public static final IntegerProperty LEFT = IntegerProperty.create("left", 0, 3);

        public Cart(Properties p) {
            super(p, new double[]{1, 0, 2, 15, 13, 14});
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, Kind.CASH).setValue(LEFT, 3));
        }

        @Override
        protected void extra(StateDefinition.Builder<Block, BlockState> b) {
            b.add(KIND, LEFT);
        }

        @Override
        protected void use(BlockState state, Level level, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
            HeistManager.useCart(player, pos);
        }
    }

    // ------------------------------------------------------------------ ducto de gas (techo de la bóveda)

    public static class Vent extends Block {
        public static final BooleanProperty GAS = BooleanProperty.create("gas");

        public Vent(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(GAS, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(GAS);
        }

        @Override
        public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
            if (!s.getValue(GAS)) {
                return;
            }
            for (int i = 0; i < 6; i++) {
                l.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX() + 0.2 + r.nextDouble() * 0.6, p.getY() - 0.1, p.getZ() + 0.2 + r.nextDouble() * 0.6,
                        (r.nextDouble() - 0.5) * 0.06, -0.03 - r.nextDouble() * 0.04, (r.nextDouble() - 0.5) * 0.06);
                l.addParticle(ParticleTypes.SNEEZE, p.getX() + r.nextDouble(), p.getY() - 0.3 - r.nextDouble() * 2, p.getZ() + r.nextDouble(),
                        (r.nextDouble() - 0.5) * 0.04, -0.02, (r.nextDouble() - 0.5) * 0.04);
            }
        }
    }
}

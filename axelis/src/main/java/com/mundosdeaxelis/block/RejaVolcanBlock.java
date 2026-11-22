package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Reja del volcán (etapa 255): barrotes de piedra negra que cierran el paso. No se rompen. Se abren por canal: el 1 al vencer a
 * la Gran Salamandra (paso al santuario), el 2 al encender a la vez los cuatro braseros de la sala de erupciones (sala del
 * tesoro) y el 3 (etapa 287) al vencer las oleadas del Altar Maldito de la sala de erupciones (paso al guardián). Abierta no se ve ni estorba; cuando el volcán se reinicia, se vuelve a cerrar.
 */
public class RejaVolcanBlock extends Block {
    /** Eje a lo largo del cual se extiende la reja (el plano de los barrotes contiene este eje y la vertical). */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 3);
    private static final VoxelShape ALONG_Z = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    private static final VoxelShape ALONG_X = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public RejaVolcanBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(OPEN, false).setValue(CHANNEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS, OPEN, CHANNEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (state.getValue(OPEN)) {
            return Shapes.empty();
        }
        return state.getValue(AXIS) == Direction.Axis.Z ? ALONG_Z : ALONG_X;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(OPEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return state.getValue(OPEN);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) {
            return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(OPEN)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int ch = state.getValue(CHANNEL);
            player.displayClientMessage(Component.translatable("message.mundos_de_axelis.reja_volcan." + (ch == 1 ? "guardian" : ch == 2 ? "braziers" : ch == 3 ? "altar" : "closed")), true);
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.7F, 0.7F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Abre o cierra todas las rejas de un canal cerca de {@code center} (en un cubo de lado 2 * radio + 1, y +-vy en altura).
     * Devuelve cuántas cambiaron.
     */
    public static int setChannel(ServerLevel level, BlockPos center, int channel, int radius, int vy, boolean open) {
        int n = 0;
        double sx = 0, sy = 0, sz = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -vy; y <= vy; y++) {
                    p.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof RejaVolcanBlock && s.getValue(CHANNEL) == channel && s.getValue(OPEN) != open) {
                        level.setBlock(p, s.setValue(OPEN, open), Block.UPDATE_ALL);
                        if (open) {
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.2, 0.3, 0.2, 0.01);
                            level.sendParticles(ParticleTypes.LAVA, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.2, 0.3, 0.2, 0.0);
                        }
                        sx += p.getX();
                        sy += p.getY();
                        sz += p.getZ();
                        n++;
                    }
                }
            }
        }
        if (n > 0) {
            BlockPos c = BlockPos.containing(sx / n + 0.5, sy / n + 0.5, sz / n + 0.5);
            level.playSound(null, c, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.2F, 0.5F);
            level.playSound(null, c, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
            if (open) {
                level.playSound(null, c, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8F, 0.5F);
            }
        }
        return n;
    }
}

package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gran Yunque de Fundición (etapa 270): un bloque de dos partes, como una cama. A un lado, un yunque enorme con una llave al rojo
 * y un martillo; al otro, un crisol de piedra negra con lava que burbujea, un fuelle y una chimenea que echa humo y chispas.
 * Clic derecho en cualquiera de las dos partes: se abre la forja de llaves (4 fragmentos de la misma caja = su llave, con un
 * juego de habilidad). Se crea en la Mesa del Artífice.
 */
public class GranYunqueBlock extends HorizontalDirectionalBlock {
    public enum Part implements StringRepresentable {
        ANVIL("anvil"), FORGE("forge");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public GranYunqueBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.ANVIL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, PART);
    }

    /** Hacia dónde queda el crisol visto desde el yunque. */
    public static Direction forgeSide(Direction facing) {
        return facing.getClockWise();
    }

    /** Dónde está el yunque (la parte principal) a partir de cualquiera de las dos. */
    public static BlockPos anvilPos(BlockState st, BlockPos pos) {
        return st.getValue(PART) == Part.ANVIL ? pos : pos.relative(forgeSide(st.getValue(FACING)).getOpposite());
    }

    private static Direction toOther(BlockState st) {
        Direction side = forgeSide(st.getValue(FACING));
        return st.getValue(PART) == Part.ANVIL ? side : side.getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos other = ctx.getClickedPos().relative(forgeSide(facing));
        Level level = ctx.getLevel();
        if (!level.getBlockState(other).canBeReplaced(ctx) || !level.getWorldBorder().isWithinBounds(other)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, Part.ANVIL);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(pos.relative(forgeSide(state.getValue(FACING))), state.setValue(PART, Part.FORGE), 3);
            level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
        }
    }

    @Override
    public BlockState updateShape(BlockState st, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos npos) {
        if (dir == toOther(st) && !(neighbor.is(this) && neighbor.getValue(PART) != st.getValue(PART))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(st, dir, neighbor, level, pos, npos);
    }

    /** En creativo, al romper el crisol, quita el yunque sin soltar nada (como la cama). */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState st, Player player) {
        if (!level.isClientSide && player.isCreative() && st.getValue(PART) == Part.FORGE) {
            BlockPos other = pos.relative(toOther(st));
            BlockState os = level.getBlockState(other);
            if (os.is(this) && os.getValue(PART) == Part.ANVIL) {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, other, Block.getId(os));
            }
        }
        super.playerWillDestroy(level, pos, st, player);
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            com.mundosdeaxelis.forja.Forja.open(sp, anvilPos(st, pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(net.minecraft.network.chat.Component.translatable("tooltip.mundos_de_axelis.gran_yunque").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState st, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (st.getValue(PART) == Part.FORGE) {
            if (r.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.LAVA, x + (r.nextDouble() - 0.5) * 0.5, y + 0.65, z + (r.nextDouble() - 0.5) * 0.5, 0, 0, 0);
            }
            Direction back = st.getValue(FACING).getOpposite(); // la chimenea está detrás del crisol
            double cx = x + back.getStepX() * 0.375, cz = z + back.getStepZ() * 0.375;
            level.addParticle(ParticleTypes.LARGE_SMOKE, cx + (r.nextDouble() - 0.5) * 0.15, y + 1.8, cz + (r.nextDouble() - 0.5) * 0.15, 0, 0.04, 0);
            if (r.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.LAVA, cx, y + 1.75, cz, 0, 0, 0);
            }
            if (r.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.6, y + 0.62, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
            }
            if (r.nextInt(10) == 0) {
                level.playLocalSound(x, y + 0.6, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.4F, 0.9F + r.nextFloat() * 0.2F, false);
            }
            if (r.nextInt(40) == 0) {
                level.playLocalSound(x, y + 0.6, z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.8F, false);
            }
        } else if (r.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x + (r.nextDouble() - 0.5) * 0.3, y + 1.0, z + (r.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
        }
    }
}

package com.mundosdeaxelis.world.decor;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Bloque de decoración de las estructuras. Tipos: {@code plain} (sin orientación), {@code floor} (de pie), {@code wall} (pegado a una pared,
 * se puede cruzar), {@code hang} (cuelga del techo); cualquiera puede ser {@code tall} (dos bloques de alto, como una puerta).
 * La forma de choque sale de las cajas del modelo; las llamas y los destellos se dibujan con partículas.
 */
public class EstBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    final EstDecor.Def def;
    private final VoxelShape[] lower = new VoxelShape[4];
    private final VoxelShape[] upper = new VoxelShape[4];

    /** El bloque se crea con subclases pequeñas porque las propiedades se declaran durante el constructor de Block. */
    static Block create(EstDecor.Def d) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().strength(d.kind().equals("wall") ? 0.4f : d.kind().equals("cube") ? 2.0f : 1.2f)
                .sound(switch (d.sound()) {
                    case "metal" -> SoundType.METAL;
                    case "stone" -> SoundType.STONE;
                    case "cloth", "wool" -> SoundType.WOOL;
                    case "chain" -> SoundType.CHAIN;
                    case "amethyst" -> SoundType.AMETHYST_CLUSTER;
                    case "glass" -> SoundType.GLASS;
                    default -> SoundType.WOOD;
                }).lightLevel(s -> d.light());
        if (!d.kind().equals("cube")) {
            p = p.noOcclusion(); // el bloque entero (oro de adorno...) sí tapa a sus vecinos
        }
        if (!d.collide()) {
            p = p.noCollission();
        }
        if (d.tall()) {
            return new Tall(p, d);
        }
        return d.kind().equals("plain") || d.kind().equals("cube") ? new Plain(p, d) : new Facing(p, d);
    }

    private EstBlock(BlockBehaviour.Properties p, EstDecor.Def d) {
        super(p);
        this.def = d;
        for (int i = 0; i < 4; i++) {
            lower[i] = shapeOf(d.shape(), i);
            upper[i] = d.shapeUpper().isEmpty() ? lower[i] : shapeOf(d.shapeUpper(), i);
        }
    }

    /** Gira las cajas del modelo (orientado al norte) tantas veces 90° como pida la orientación (0 norte, 1 este, 2 sur, 3 oeste). */
    private static VoxelShape shapeOf(List<double[]> boxes, int turns) {
        VoxelShape s = Shapes.empty();
        for (double[] b : boxes) {
            double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
            for (int i = 0; i < turns; i++) {
                double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
                x0 = nx0;
                x1 = nx1;
                z0 = nz0;
                z1 = nz1;
            }
            s = Shapes.or(s, Block.box(Math.min(x0, x1), b[1], Math.min(z0, z1), Math.max(x0, x1), b[4], Math.max(z0, z1)));
        }
        return s.isEmpty() ? Shapes.block() : s;
    }

    private static int turns(Direction f) {
        return switch (f) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    @Override
    public String getDescriptionId() {
        return "block.mundos_estructuras." + def.id();
    }

    boolean tall() {
        return def.tall();
    }

    boolean directional() {
        return !def.kind().equals("plain") && !def.kind().equals("cube");
    }

    private int turnsOf(BlockState s) {
        return directional() ? turns(s.getValue(FACING)) : 0;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        boolean up = tall() && s.getValue(HALF) == DoubleBlockHalf.UPPER;
        return (up ? upper : lower)[turnsOf(s)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return def.collide() ? getShape(s, l, p, c) : Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState s, Rotation r) {
        return directional() ? s.setValue(FACING, r.rotate(s.getValue(FACING))) : s;
    }

    @Override
    public BlockState mirror(BlockState s, Mirror m) {
        return directional() ? s.rotate(m.getRotation(s.getValue(FACING))) : s;
    }

    @Override
    public boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
        if (tall() && s.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = l.getBlockState(p.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        switch (def.kind()) {
            case "wall": {
                if (def.soft()) {
                    return true; // tapiz que tapa un pasadizo: no necesita muro detras
                }
                Direction f = s.getValue(FACING);
                BlockPos b = p.relative(f.getOpposite());
                return l.getBlockState(b).isFaceSturdy(l, b, f);
            }
            case "hang":
                return Block.canSupportCenter(l, p.above(), Direction.DOWN);
            case "cube":
                return true; // bloque entero: no necesita apoyo
            default:
                return !l.getBlockState(p.below()).isAir();
        }
    }

    @Override
    public BlockState updateShape(BlockState s, Direction dir, BlockState nb, LevelAccessor l, BlockPos p, BlockPos nbPos) {
        if (tall()) {
            DoubleBlockHalf half = s.getValue(HALF);
            if (dir.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (dir == Direction.UP)) {
                return nb.is(this) && nb.getValue(HALF) != half ? s : Blocks.AIR.defaultBlockState();
            }
            if (half == DoubleBlockHalf.LOWER && dir == Direction.DOWN && !s.canSurvive(l, p)) {
                return Blocks.AIR.defaultBlockState();
            }
            return s;
        }
        return !s.canSurvive(l, p) ? Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity who, ItemStack st) {
        if (tall()) {
            l.setBlock(p.above(), s.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    @Override
    public void playerWillDestroy(Level l, BlockPos p, BlockState s, Player pl) {
        if (tall() && !l.isClientSide && pl.isCreative()) {
            // en creativo no se suelta el objeto por la mitad de abajo
            DoubleBlockHalf half = s.getValue(HALF);
            if (half == DoubleBlockHalf.UPPER) {
                BlockPos lp = p.below();
                BlockState ls = l.getBlockState(lp);
                if (ls.is(s.getBlock()) && ls.getValue(HALF) == DoubleBlockHalf.LOWER) {
                    l.setBlock(lp, Blocks.AIR.defaultBlockState(), 35);
                    l.levelEvent(pl, 2001, lp, Block.getId(ls));
                }
            }
        }
        super.playerWillDestroy(l, p, s, pl);
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        if (def.particles().isEmpty()) {
            return;
        }
        int t = turnsOf(s);
        for (EstDecor.Particle pa : def.particles()) {
            if (r.nextDouble() > pa.rate()) {
                continue;
            }
            double x = pa.x(), z = pa.z();
            for (int i = 0; i < t; i++) {
                double nx = 16 - z;
                z = x;
                x = nx;
            }
            var type = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation(pa.type()));
            if (type instanceof SimpleParticleType sp) {
                l.addParticle((ParticleOptions) sp, p.getX() + x / 16.0, p.getY() + pa.y() / 16.0, p.getZ() + z / 16.0, 0, 0.005, 0);
            }
        }
    }

    // ------------------------------------------------------------------ variantes

    /** Sin orientación (ni dos alturas). */
    private static final class Plain extends EstBlock {
        Plain(BlockBehaviour.Properties p, EstDecor.Def d) {
            super(p, d);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext c) {
            BlockState s = defaultBlockState();
            return s.canSurvive(c.getLevel(), c.getClickedPos()) ? s : null;
        }
    }

    /** Con orientación (FACING). */
    private static final class Facing extends EstBlock {
        Facing(BlockBehaviour.Properties p, EstDecor.Def d) {
            super(p, d);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext c) {
            Direction f = c.getHorizontalDirection().getOpposite();
            if (def.kind().equals("wall")) {
                Direction face = c.getClickedFace();
                if (face.getAxis().isVertical()) {
                    return null;
                }
                f = face;
            }
            BlockState s = defaultBlockState().setValue(FACING, f);
            return s.canSurvive(c.getLevel(), c.getClickedPos()) ? s : null;
        }
    }

    /** Dos bloques de alto (HALF) con o sin orientación. */
    private static final class Tall extends EstBlock {
        Tall(BlockBehaviour.Properties p, EstDecor.Def d) {
            super(p, d);
            registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(HALF);
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext c) {
            BlockPos p = c.getClickedPos();
            if (p.getY() >= c.getLevel().getMaxBuildHeight() - 1 || !c.getLevel().getBlockState(p.above()).canBeReplaced(c)) {
                return null;
            }
            BlockState s = defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
            return s.canSurvive(c.getLevel(), p) ? s : null;
        }
    }
}

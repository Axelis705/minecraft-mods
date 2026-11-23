package com.coleccionista.bloque.deco;

import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Mueble o adorno 3D del complejo (catre aparte). Puede tener orientación (de pie, de pared o colgado del techo), dos bloques de alto,
 * luz, partículas y algunos extras de ambiente:
 * <ul>
 *   <li>{@code apagable}: estado encendida/apagada (otra textura y sin luz); el evento lo cambia (apagones) y con
 *   {@code parpadeo} falla sola de vez en cuando.</li>
 *   <li>{@code zumbido}: zumba como lámpara fluorescente vieja.</li>
 *   <li>{@code goteo}: gotea desde el techo (charcos).</li>
 *   <li>{@code cajon}: se abre como cofre (las pistas de las celdas).</li>
 *   <li>{@code texto}: el evento escribe en él (números de celda, letreros, rayones en la pared).</li>
 * </ul>
 * Son parte del mapa: no se caen aunque les falte el apoyo (el mapa los pone en cualquier orden).
 */
public class DecoBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty ENCENDIDA = BooleanProperty.create("encendida");
    /** La lámpara vieja está fallando en este momento (se apagó sola un instante). */
    public static final BooleanProperty FALLA = BooleanProperty.create("falla");
    /** La ficha del bloque que se está construyendo (las propiedades se declaran dentro del constructor de Block). */
    private static Deco.Def pendiente;

    final Deco.Def def;
    private final boolean orientado, apagable, parpadeo;
    private final VoxelShape[] lower = new VoxelShape[4];
    private final VoxelShape[] upper = new VoxelShape[4];

    static Block crear(BlockBehaviour.Properties p, Deco.Def d) {
        boolean apagable = d.flag("apagable"), parpadeo = apagable && d.flag("parpadeo");
        p = p.noOcclusion().lightLevel(s -> (!apagable || s.getValue(ENCENDIDA)) && !(parpadeo && s.getValue(FALLA)) ? d.light() : 0);
        if (!d.collide()) {
            p = p.noCollission();
        }
        if (d.flag("parpadeo")) {
            p = p.randomTicks();
        }
        pendiente = d;
        try {
            if (d.flag("cajon")) {
                return new Cajon(p, d);
            }
            if (d.flag("texto")) {
                return new Texto(p, d);
            }
            return new DecoBlock(p, d);
        } finally {
            pendiente = null;
        }
    }

    DecoBlock(BlockBehaviour.Properties p, Deco.Def d) {
        super(p);
        this.def = d;
        this.orientado = orientado(d);
        this.apagable = d.flag("apagable");
        this.parpadeo = apagable && d.flag("parpadeo");
        BlockState s = stateDefinition.any();
        if (orientado) {
            s = s.setValue(FACING, Direction.NORTH);
        }
        if (d.tall()) {
            s = s.setValue(HALF, DoubleBlockHalf.LOWER);
        }
        if (apagable) {
            s = s.setValue(ENCENDIDA, true);
        }
        if (parpadeo) {
            s = s.setValue(FALLA, false);
        }
        registerDefaultState(s);
        for (int i = 0; i < 4; i++) {
            lower[i] = forma(d.shape(), i);
            upper[i] = d.shapeUpper().isEmpty() ? lower[i] : forma(d.shapeUpper(), i);
        }
    }

    private static boolean orientado(Deco.Def d) {
        return !d.sub().equals("plain") && !d.sub().equals("cube");
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        Deco.Def d = pendiente;
        if (d == null) {
            return;
        }
        if (orientado(d)) {
            b.add(FACING);
        }
        if (d.tall()) {
            b.add(HALF);
        }
        if (d.flag("apagable")) {
            b.add(ENCENDIDA);
            if (d.flag("parpadeo")) {
                b.add(FALLA);
            }
        }
    }

    /** Gira las cajas del modelo (orientado al norte) tantas veces 90° como pida la orientación (0 norte, 1 este, 2 sur, 3 oeste). */
    private static VoxelShape forma(List<double[]> boxes, int turns) {
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
            double ya = Math.max(0, Math.min(16, b[1])), yb = Math.max(0, Math.min(16, b[4]));
            if (yb - ya < 0.01) {
                yb = Math.min(16, ya + 0.5);
            }
            s = Shapes.or(s, Block.box(Math.max(0, Math.min(x0, x1)), ya, Math.max(0, Math.min(z0, z1)), Math.min(16, Math.max(x0, x1)), yb,
                    Math.min(16, Math.max(z0, z1))));
        }
        return s.isEmpty() ? Block.box(4, 4, 4, 12, 12, 12) : s;
    }

    private static int vueltas(Direction f) {
        return switch (f) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    public Deco.Def def() {
        return def;
    }

    private int vueltas(BlockState s) {
        return orientado ? vueltas(s.getValue(FACING)) : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        boolean up = def.tall() && s.getValue(HALF) == DoubleBlockHalf.UPPER;
        return (up ? upper : lower)[vueltas(s)];
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return def.collide() ? getShape(s, l, p, c) : Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState s, Rotation r) {
        return orientado ? s.setValue(FACING, r.rotate(s.getValue(FACING))) : s;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState s, Mirror m) {
        return orientado ? s.rotate(m.getRotation(s.getValue(FACING))) : s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        BlockState s = defaultBlockState();
        if (orientado) {
            Direction f = c.getHorizontalDirection().getOpposite();
            if (def.sub().equals("wall") && c.getClickedFace().getAxis().isHorizontal()) {
                f = c.getClickedFace();
            }
            s = s.setValue(FACING, f);
        }
        if (def.tall() && !c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c)) {
            return null;
        }
        return s;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState s, Direction dir, BlockState nb, LevelAccessor l, BlockPos p, BlockPos np) {
        if (def.tall() && dir.getAxis() == Direction.Axis.Y) {
            DoubleBlockHalf half = s.getValue(HALF);
            if ((half == DoubleBlockHalf.LOWER) == (dir == Direction.UP) && !(nb.is(this) && nb.getValue(HALF) != half)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return s;
    }

    @Override
    public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity who, ItemStack st) {
        if (def.tall()) {
            l.setBlock(p.above(), s.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    // ------------------------------------------------------------------ ambiente

    /** Qué parte de las lámparas viejas están "malas" y parpadean seguido (las demás solo fallan muy de vez en cuando). */
    public static int porcentajeMalas = 30;

    /** ¿Esta lámpara es de las malas? Fijo según dónde está, así siempre son las mismas. */
    public boolean mala(BlockPos p) {
        return parpadeo && Math.floorMod(Mth.getSeed(p) >> 7, 100) < porcentajeMalas;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moving) {
        super.onPlace(s, l, p, old, moving);
        // una lámpara mala que se acaba de poner (o de prender): empieza a fallar sola
        if (l instanceof ServerLevel sl && parpadeo && s.getValue(ENCENDIDA) && mala(p) && (!old.is(this) || !old.getValue(ENCENDIDA))
                && !sl.getBlockTicks().hasScheduledTick(p, this)) {
            sl.scheduleTick(p, this, 20 + sl.random.nextInt(200));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        if (!parpadeo || !s.getValue(ENCENDIDA)) {
            return;
        }
        if (mala(p)) {
            // por si se perdió su ciclo (mundo viejo): lo vuelve a arrancar
            if (!l.getBlockTicks().hasScheduledTick(p, this)) {
                l.scheduleTick(p, this, 1 + r.nextInt(40));
            }
            return;
        }
        // las buenas también fallan, pero muy de vez en cuando: un parpadeo y vuelve
        if (!s.getValue(FALLA) && r.nextFloat() < 0.35F) {
            l.setBlock(p, s.setValue(FALLA, true), Block.UPDATE_CLIENTS);
            l.scheduleTick(p, this, r.nextFloat() < 0.7F ? 2 + r.nextInt(4) : 20 + r.nextInt(60));
            chispa(l, p, r, 0.5F);
        }
    }

    /**
     * El ciclo de una lámpara mala: prendida un rato, luego una racha de parpadeos (apagones de 1 a 4 ticks, a veces uno largo) con su
     * chispazo, y otra vez prendida. Las buenas solo pasan por aquí para volver de su falla.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        if (!parpadeo) {
            return;
        }
        boolean falla = s.getValue(FALLA);
        if (!s.getValue(ENCENDIDA)) {
            // el evento la apagó: se queda quieta (sin ciclo) hasta que la prendan
            if (falla) {
                l.setBlock(p, s.setValue(FALLA, false), Block.UPDATE_CLIENTS);
            }
            return;
        }
        boolean mala = mala(p);
        if (falla) {
            l.setBlock(p, s.setValue(FALLA, false), Block.UPDATE_CLIENTS);
            if (mala) {
                // ¿sigue la racha? (en promedio 2 o 3 parpadeos seguidos) o descansa de 3 a 20 segundos
                l.scheduleTick(p, this, r.nextFloat() < 0.55F ? 2 + r.nextInt(4) : 60 + r.nextInt(340));
            }
        } else if (mala) {
            l.setBlock(p, s.setValue(FALLA, true), Block.UPDATE_CLIENTS);
            chispa(l, p, r, 1F);
            l.scheduleTick(p, this, r.nextFloat() < 0.85F ? 1 + r.nextInt(4) : 15 + r.nextInt(45));
        }
    }

    /** El chispazo de la lámpara que se apaga sola: el sonido y, a veces, unas chispas. */
    private void chispa(ServerLevel l, BlockPos p, RandomSource r, float fuerza) {
        l.playSound(null, p, ModRegistry.CHISPA.get(), SoundSource.BLOCKS, (0.3F + r.nextFloat() * 0.25F) * fuerza, 0.85F + r.nextFloat() * 0.3F);
        if (r.nextFloat() < 0.3F * fuerza) {
            double y = def.sub().equals("hang") ? 0.6 : 0.5;
            l.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + y, p.getZ() + 0.5, 2 + r.nextInt(4), 0.25, 0.08, 0.25, 0.06);
        }
    }

    /** ¿Está dando luz ahora? */
    public boolean prendida(BlockState s) {
        return (!apagable || s.getValue(ENCENDIDA)) && !(parpadeo && s.getValue(FALLA));
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        boolean on = prendida(s);
        if (def.flag("zumbido") && on && r.nextInt(14) == 0) {
            l.playLocalSound(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, ModRegistry.ZUMBIDO.get(), SoundSource.BLOCKS, 0.25F,
                    0.95F + r.nextFloat() * 0.1F, false);
        }
        if (def.flag("goteo")) {
            if (r.nextInt(3) == 0) {
                double x = p.getX() + 0.3 + r.nextDouble() * 0.4, z = p.getZ() + 0.3 + r.nextDouble() * 0.4;
                // la gota sale del techo (busca hasta 8 bloques arriba)
                for (int dy = 1; dy <= 8; dy++) {
                    if (!l.getBlockState(p.above(dy)).isAir()) {
                        l.addParticle(ParticleTypes.DRIPPING_WATER, x, p.getY() + dy - 0.05, z, 0, 0, 0);
                        break;
                    }
                }
            }
            if (r.nextInt(25) == 0) {
                l.playLocalSound(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.5F,
                        0.8F + r.nextFloat() * 0.4F, false);
            }
        }
        if (def.particles().isEmpty() || !on) {
            return;
        }
        int t = vueltas(s);
        boolean arriba = def.tall() && s.getValue(HALF) == DoubleBlockHalf.UPPER;
        for (Deco.Particle pa : def.particles()) {
            double py = pa.y() - (arriba ? 16 : 0);
            if (py < 0 || py > 16 || r.nextDouble() > pa.rate()) {
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
                l.addParticle((ParticleOptions) sp, p.getX() + x / 16.0, p.getY() + py / 16.0, p.getZ() + z / 16.0, 0, 0.005, 0);
            }
        }
    }

    // ------------------------------------------------------------------ baúl y textos

    /** El baúl de la celda: se abre como cofre (27 casillas). */
    static final class Cajon extends DecoBlock implements EntityBlock {
        Cajon(BlockBehaviour.Properties p, Deco.Def d) {
            super(p, d);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
            return new CajonBE(p, s);
        }

        @Override
        @SuppressWarnings("deprecation")
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            if (l.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (l.getBlockEntity(p) instanceof CajonBE be) {
                pl.openMenu(be);
                l.playSound(null, p, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 0.7F);
            }
            return InteractionResult.CONSUME;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onRemove(BlockState s, Level l, BlockPos p, BlockState ns, boolean moving) {
            if (!s.is(ns.getBlock()) && l.getBlockEntity(p) instanceof CajonBE be) {
                be.clearContent();
            }
            super.onRemove(s, l, p, ns, moving);
        }
    }

    /** Algo donde el evento escribe: número de celda, letrero o rayones en la pared. */
    static final class Texto extends DecoBlock implements EntityBlock {
        Texto(BlockBehaviour.Properties p, Deco.Def d) {
            super(p, d);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
            return new TextoBE(p, s);
        }
    }
}

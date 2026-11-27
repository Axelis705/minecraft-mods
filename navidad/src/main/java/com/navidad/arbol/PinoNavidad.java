package com.navidad.arbol;

import com.axeliscore.fx.Efectos;
import com.axeliscore.fx.Fx;
import com.navidad.Navidad;
import com.navidad.libro.Libro;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El Pino de Navidad para decorar (tres bloques de alto y medio con la estrella: se dibuja {@link #ESCALA} veces más grande que su modelo;
 * lo dibuja {@code PinoRenderer} con todo y adornos). Son tres bloques: la base (guarda los adornos), el de en medio y la punta (los
 * árboles de antes, de dos bloques, siguen sirviendo y les sale la punta solita al darles clic). Clic derecho con un adorno:
 * lo cuelga en la rama libre más cerca de donde hiciste clic (luces y guirnaldas se enredan por todo el árbol; la estrella o el ángel
 * van en la punta). Con la mano vacía: te dice su nivel y sus puntos; agachado con la mano vacía: descuelgas el adorno más cercano.
 * Si lo rompes, sale con todos sus adornos puestos.
 */
public class PinoNavidad extends Block implements EntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** El tercer bloque (arriba del de en medio). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty PUNTA =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("punta");
    /** Qué tanto más grande que el modelo se dibuja el árbol (los lugares de los adornos se escalan igual). */
    public static final float ESCALA = 1.4F;
    private static final VoxelShape ABAJO = Block.box(0, 0, 0, 16, 16, 16), ARRIBA = Block.box(2, 0, 2, 14, 16, 14),
            CIMA = Block.box(4, 0, 4, 12, 13, 12);

    public PinoNavidad(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(PUNTA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(HALF, PUNTA);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return s.getValue(PUNTA) ? CIMA : s.getValue(HALF) == DoubleBlockHalf.LOWER ? ABAJO : ARRIBA;
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return s.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return s.getValue(HALF) == DoubleBlockHalf.LOWER ? new PinoBE(p, s) : null;
    }

    // ------------------------------------------------------------------ tres bloques de alto

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        BlockPos p = c.getClickedPos();
        if (p.getY() >= c.getLevel().getMaxBuildHeight() - 2 || !c.getLevel().getBlockState(p.above()).canBeReplaced(c)
                || !c.getLevel().getBlockState(p.above(2)).canBeReplaced(c)) {
            return null;
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity who, ItemStack st) {
        l.setBlock(p.above(), s.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        l.setBlock(p.above(2), s.setValue(HALF, DoubleBlockHalf.UPPER).setValue(PUNTA, true), 3);
    }

    /** A los árboles de antes (dos bloques) les pone la punta, si arriba hay lugar. */
    static void completar(Level l, BlockPos base) {
        BlockState medio = l.getBlockState(base.above());
        if (medio.getBlock() instanceof PinoNavidad && !medio.getValue(PUNTA) && l.getBlockState(base.above(2)).canBeReplaced()) {
            l.setBlock(base.above(2), medio.setValue(PUNTA, true), 3);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState s, Direction dir, BlockState nb, LevelAccessor l, BlockPos p, BlockPos np) {
        if (dir.getAxis() != Direction.Axis.Y) {
            return s;
        }
        boolean nbPino = nb.is(this);
        if (s.getValue(PUNTA)) {
            // la punta necesita abajo el de en medio
            if (dir == Direction.DOWN && !(nbPino && nb.getValue(HALF) == DoubleBlockHalf.UPPER && !nb.getValue(PUNTA))) {
                return Blocks.AIR.defaultBlockState();
            }
        } else if (s.getValue(HALF) == DoubleBlockHalf.UPPER) {
            // el de en medio necesita abajo la base (la punta es opcional: los árboles viejos no la tienen)
            if (dir == Direction.DOWN && !(nbPino && nb.getValue(HALF) == DoubleBlockHalf.LOWER)) {
                return Blocks.AIR.defaultBlockState();
            }
        } else if (dir == Direction.UP && !(nbPino && nb.getValue(HALF) == DoubleBlockHalf.UPPER && !nb.getValue(PUNTA))) {
            return Blocks.AIR.defaultBlockState();
        }
        return s;
    }

    @Override
    public void playerWillDestroy(Level l, BlockPos p, BlockState s, Player pl) {
        // si rompes la parte de en medio o la punta, el árbol sale de la base (con sus adornos); en creativo no suelta nada
        if (!l.isClientSide && s.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos abajo = base(s, p);
            BlockState sa = l.getBlockState(abajo);
            if (sa.is(this) && sa.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (!pl.isCreative()) {
                    dropResources(sa, l, abajo, l.getBlockEntity(abajo), pl, pl.getMainHandItem());
                }
                l.setBlock(abajo, Blocks.AIR.defaultBlockState(), 35);
                l.levelEvent(pl, 2001, abajo, Block.getId(sa));
            }
        }
        super.playerWillDestroy(l, p, s, pl);
    }

    /** La parte de abajo (la que guarda los adornos). */
    public static BlockPos base(BlockState s, BlockPos p) {
        return s.getValue(PUNTA) ? p.below(2) : s.getValue(HALF) == DoubleBlockHalf.UPPER ? p.below() : p;
    }

    // ------------------------------------------------------------------ colgar, quitar y ver

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack st = p.getItemInHand(hand);
        Adornos.Def d = Adornos.de(st.getItem());
        if (d == null && !st.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos b = base(s, pos);
        if (!(l.getBlockEntity(b) instanceof PinoBE be) || !(p instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        ServerLevel sl = (ServerLevel) l;
        completar(l, b);
        // el clic, en las medidas del modelo (el árbol se dibuja ESCALA veces más grande desde el centro de su base)
        Vec3 donde = hit.getLocation().subtract(b.getX() + 0.5, b.getY(), b.getZ() + 0.5).scale(1.0 / ESCALA);
        if (d != null) {
            ItemStack[] devolver = {ItemStack.EMPTY};
            int antes = be.colgados();
            PinoBE.Resultado r = be.colgar(d, donde, devolver);
            switch (r) {
                case SIN_LUGAR -> sp.displayClientMessage(Component.translatable("navidad.pino.sin_lugar").withStyle(ChatFormatting.GRAY), true);
                case LUCES_LLENAS -> sp.displayClientMessage(Component.translatable("navidad.pino.luces_llenas").withStyle(ChatFormatting.GRAY), true);
                case GUIRNALDAS_LLENAS -> sp.displayClientMessage(Component.translatable("navidad.pino.guirnaldas_llenas")
                        .withStyle(ChatFormatting.GRAY), true);
                default -> {
                    if (!p.getAbilities().instabuild) {
                        st.shrink(1);
                    }
                    if (!devolver[0].isEmpty() && !p.getInventory().add(devolver[0])) {
                        p.drop(devolver[0], false);
                    }
                    colgado(sl, sp, b, be, d, donde);
                    if (be.colgados() > antes || d.tipo() != Adornos.Tipo.COLGANTE) {
                        Libro.evento(sp, "adorno", 1);
                    }
                }
            }
            return InteractionResult.CONSUME;
        }
        if (p.isShiftKeyDown()) {
            ItemStack q = be.quitar(donde);
            if (!q.isEmpty()) {
                if (!p.getInventory().add(q)) {
                    p.drop(q, false);
                }
                l.playSound(null, b, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.4F);
                revisarNivel(sl, sp, b, be);
            }
            return InteractionResult.CONSUME;
        }
        NivelArbol.Estado e = NivelArbol.de(sl, b);
        sp.displayClientMessage(NivelArbol.resumen(e), true);
        l.playSound(null, b, ModRegistry.CASCABEL.get(), SoundSource.BLOCKS, 0.6F, 1.2F + e.nivel() * 0.05F);
        sl.sendParticles(ParticleTypes.END_ROD, b.getX() + 0.5, b.getY() + 2.55 * ESCALA, b.getZ() + 0.5, 6 + e.nivel() * 2, 0.15, 0.1, 0.15, 0.02);
        com.navidad.ArbolSanta.recordar(sp, b, e);
        return InteractionResult.CONSUME;
    }

    /** Acaba de colgarse algo: sonido, brillitos donde quedó y revisar si el árbol subió de nivel. */
    private static void colgado(ServerLevel l, ServerPlayer p, BlockPos b, PinoBE be, Adornos.Def d, Vec3 donde) {
        double x = b.getX() + 0.5, y = b.getY(), z = b.getZ() + 0.5;
        switch (d.tipo()) {
            case PUNTA -> {
                float[] pt = PuntosArbol.PUNTA;
                l.sendParticles(ParticleTypes.END_ROD, x + pt[0] * ESCALA, y + pt[1] * ESCALA + 0.4, z + pt[2] * ESCALA, 24, 0.2, 0.25, 0.2, 0.05);
                l.playSound(null, b, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1F, 1.2F);
                l.playSound(null, b, ModRegistry.CASCABELES.get(), SoundSource.BLOCKS, 0.8F, 1.1F);
            }
            case LUCES, GUIRNALDA -> {
                l.sendParticles(d.tipo() == Adornos.Tipo.LUCES ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON, x, y + 1.2 * ESCALA, z, 30, 0.8, 1.0, 0.8, 0.02);
                l.playSound(null, b, d.tipo() == Adornos.Tipo.LUCES ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.ARMOR_EQUIP_CHAIN,
                        SoundSource.BLOCKS, 1F, 1.3F);
            }
            default -> {
                // el lugar donde quedó (el más cerca del clic que estaba libre antes: ahora tiene este adorno)
                float[] mejor = null;
                double md = Double.MAX_VALUE;
                for (int i = 0; i < be.lugares.length; i++) {
                    if (!be.lugares[i].equals(d.id())) {
                        continue;
                    }
                    float[] q = PuntosArbol.LUGARES[i];
                    double dd = Math.pow(q[0] - donde.x, 2) + Math.pow(q[1] - donde.y, 2) + Math.pow(q[2] - donde.z, 2);
                    if (dd < md) {
                        md = dd;
                        mejor = q;
                    }
                }
                if (mejor != null) {
                    l.sendParticles(ParticleTypes.END_ROD, x + mejor[0] * ESCALA, y + mejor[1] * ESCALA - 0.1, z + mejor[2] * ESCALA, 5, 0.05, 0.05,
                            0.05, 0.015);
                }
                l.playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, 1.4F + l.random.nextFloat() * 0.4F);
            }
        }
        revisarNivel(l, p, b, be);
    }

    /** Si el árbol subió de nivel: ¡a festejar! (título, confeti, sonido y aviso al libro). */
    static void revisarNivel(ServerLevel l, ServerPlayer p, BlockPos b, PinoBE be) {
        NivelArbol.Estado e = NivelArbol.de(l, b);
        com.navidad.ArbolSanta.recordar(p, b, e);
        if (e.nivel() > be.nivelVisto) {
            if (be.nivelVisto > 0 || e.nivel() > 1) {
                Fx.title(p, Component.literal(NivelArbol.estrellas(e.nivel())), Component.translatable("navidad.pino.sube",
                        NivelArbol.nombre(e.nivel())), 0xFFD84A);
                Efectos.lanzar(l, Navidad.MODID + ":confeti_grande", new Vec3(b.getX() + 0.5, b.getY() + 2.6 * ESCALA, b.getZ() + 0.5));
                l.playSound(null, b, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.1F);
                l.playSound(null, b, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1F, 1F);
                Navidad.LOGGER.info("NAVIDAD_PINO {} subió su árbol a nivel {} ({} puntos)", p.getGameProfile().getName(), e.nivel(), e.total());
            }
            be.nivelVisto = e.nivel();
            be.setChanged();
        }
        Libro.nivelArbol(p, e.nivel());
    }
}

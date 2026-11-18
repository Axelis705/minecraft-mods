package com.mundosdeaxelis.block;

import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.item.CrateKeyItem;
import com.mundosdeaxelis.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Un crate permanente: no se puede romper en supervivencia. Con la llave de su tipo en la mano, clic derecho y sale un premio
 * al azar con una ruleta que ven todos los jugadores de alrededor.
 */
public class CrateBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);
    /** Hacia dónde mira el frente del cofre. Los modelos originales tienen el frente al sur, así que ese es el valor por defecto
     *  (los cofres que ya estaban puestos se ven igual que antes). */
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public final CrateKind kind;

    public CrateBlock(CrateKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** El frente queda mirando al jugador que lo coloca; si lo coloca agachado, el frente queda del lado contrario. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction d = ctx.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() ? d : d.getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Efecto propio de cada cofre (solo se ve en el cliente). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double x = pos.getX() + 0.15 + r.nextDouble() * 0.7;
        double z = pos.getZ() + 0.15 + r.nextDouble() * 0.7;
        double y = pos.getY() + 0.9 + r.nextDouble() * 0.5;
        switch (kind) {
            case PETS -> {
                if (r.nextInt(14) == 0) level.addParticle(ParticleTypes.HEART, x, y, z, 0, 0.03, 0);
            }
            case FOREST -> {
                if (r.nextInt(5) == 0) level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0, -0.01, 0);
                if (r.nextInt(12) == 0) level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.02, 0);
            }
            case FORGE -> {
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, (r.nextDouble() - 0.5) * 0.1, 0.06, (r.nextDouble() - 0.5) * 0.1);
                if (r.nextInt(8) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.8, pos.getY() + 1.0, pos.getZ() + 0.8, 0, 0.04, 0);
            }
            case ICE -> {
                if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SNOWFLAKE, x, y + 0.2, z, 0, -0.02, 0);
            }
            case MAGMA -> {
                if (r.nextInt(5) == 0) level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 0.85, z, 0, 0, 0);
                if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.04, 0);
            }
            case MAGIC -> {
                if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.ENCHANT, x, y + 0.3, z, (r.nextDouble() - 0.5) * 0.5, 0.4, (r.nextDouble() - 0.5) * 0.5);
            }
            case RUBY -> {
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.03, 0);
                if (r.nextInt(10) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 1.2, 0, 0.02, 0.02);
            }
            case SKY -> {
                if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.END_ROD, x, y + 0.3, z, 0, 0.02, 0);
                if (r.nextInt(5) == 0) level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 0.2, z, 0, 0.0, 0);
            }
            case ABYSS -> {
                if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.05, 0);
                if (r.nextInt(10) == 0) level.addParticle(ParticleTypes.DRAGON_BREATH, x, pos.getY() + 0.3, z, 0, 0.02, 0);
            }
            default -> { }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CRATE.get(), CrateBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            // sin llave (o agachado) se abre el catálogo de premios con sus porcentajes
            boolean key = false;
            for (InteractionHand h : InteractionHand.values()) {
                if (player.getItemInHand(h).getItem() instanceof CrateKeyItem k && k.kind == kind) {
                    key = true;
                }
            }
            if (!key || player.isShiftKeyDown()) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.mundosdeaxelis.client.ClientBridge.openPrizes(kind));
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity be) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (be.rolling()) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.crate_busy", be.openerName).withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }
        ItemStack held = ItemStack.EMPTY;
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = sp.getItemInHand(h);
            if (s.getItem() instanceof CrateKeyItem k && k.kind == kind) {
                held = s;
                break;
            }
        }
        if (held.isEmpty()) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHEST_LOCKED, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.CONSUME;
        }
        if (be.startRoll((ServerLevel) level, sp) && !sp.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /** Si lo rompe un creativo a media apertura, el premio no se pierde. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrateBlockEntity be) {
            be.dropPending(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}

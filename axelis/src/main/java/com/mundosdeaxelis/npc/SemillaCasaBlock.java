package com.mundosdeaxelis.npc;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Semilla de casa puesta en el suelo (etapa 260). Marca dónde crecerá la casa del habitante rescatado: mientras está puesta se ve
 * un holograma con la casa entera y su tamaño (en rojo lo que estorba, en naranja si el terreno es muy desigual).
 * <ul>
 * <li>Clic derecho: la casa crece (si el sitio está bien) y la semilla se gasta.</li>
 * <li>Agachado y con la mano vacía + clic derecho: gira la casa 90°.</li>
 * <li>Romperla la devuelve (no se pierde).</li>
 * </ul>
 * La casa crece hacia donde mira la semilla (hacia donde mirabas al ponerla), con la puerta hacia ti.
 */
public class SemillaCasaBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 13.0, 13.0);

    public SemillaCasaBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState s, Rotation r) {
        return s.setValue(FACING, r.rotate(s.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState s, Mirror m) {
        return s.rotate(m.getRotation(s.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SemillaCasaBlockEntity(pos, state);
    }

    static String kindAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SemillaCasaBlockEntity be ? be.kind() : NpcKinds.RUDA.id();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof SemillaCasaBlockEntity be) {
            be.setKind(NpcService.KitKind.of(stack));
        }
        if (!level.isClientSide && placer instanceof ServerPlayer sp) {
            NpcKinds.Kind k = NpcKinds.get(NpcService.KitKind.of(stack));
            if (k != null) {
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.casa.semilla_puesta", Component.translatable(k.nameKey()))
                        .withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            Direction next = state.getValue(FACING).getClockWise();
            level.setBlock(pos, state.setValue(FACING, next), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
            player.displayClientMessage(Component.translatable("message.mundos_de_axelis.casa.girada").withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }
        String kind = kindAt(level, pos);
        String err = grow((ServerLevel) level, player instanceof ServerPlayer sp ? sp : null, pos);
        if (err != null) {
            Component msg = "message.mundos_de_axelis.casa.ocupado".equals(err) && NpcService.lastBlocked > 0
                    ? Component.translatable("message.mundos_de_axelis.casa.ocupado_n", NpcService.lastBlocked)
                    : Component.translatable(err);
            player.displayClientMessage(msg.copy().withStyle(ChatFormatting.RED), true);
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.2F);
            return InteractionResult.CONSUME;
        }
        NpcKinds.Kind k = NpcKinds.get(kind);
        if (k != null) {
            player.displayClientMessage(Component.translatable("message.mundos_de_axelis.casa.lista", Component.translatable(k.nameKey()))
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.CONSUME;
    }

    /** Hace crecer la casa de la semilla que hay en {@code pos}; devuelve el texto del error o null si creció. */
    public static String grow(ServerLevel level, @Nullable ServerPlayer sp, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SemillaCasaBlock)) {
            return "message.mundos_de_axelis.casa.unknown";
        }
        String kind = kindAt(level, pos);
        return NpcService.plantHouse(level, sp, kind, pos, state.getValue(FACING), pos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild) {
            // la semilla vuelve a la mano: así se puede poner en otro sitio
            popResource(level, pos, NpcService.kit(kindAt(level, pos)));
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return NpcService.kit(kindAt(level, pos));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.6 + r.nextDouble() * 0.5,
                    pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0.0, 0.02, 0.0);
        }
        if (r.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5,
                    (r.nextDouble() - 0.5) * 0.02, 0.03, (r.nextDouble() - 0.5) * 0.02);
        }
    }
}

package com.pescacoleccion.deco;

import com.axeliscore.block.CoreBlock;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.InfoBlock;
import com.axeliscore.ui.InfoCard;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pesca.PezItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Placa de trofeo: se pone en una pared; clic derecho con un pez lo monta (con su tamaño, quién lo pescó y su récord en la placa);
 * agachado y con la mano vacía se quita. Al mirarla sale el cuadro del pez.
 */
public class TrofeoBlock extends CoreBlock implements EntityBlock, InfoBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape N = Block.box(1, 2, 14.5, 15, 14, 16), S = Block.box(1, 2, 0, 15, 14, 1.5),
            E = Block.box(0, 2, 1, 1.5, 14, 15), W = Block.box(14.5, 2, 1, 16, 14, 15);

    public TrofeoBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction d = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, d);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> S;
            case EAST -> E;
            case WEST -> W;
            default -> N;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrofeoBE(pos, state);
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrofeoBE be)) {
            return InteractionResult.PASS;
        }
        ItemStack mano = player.getItemInHand(hand);
        if (mano.getItem() instanceof PezItem) {
            if (!level.isClientSide()) {
                montar(be, player, mano);
            }
            return Plat.ok(level.isClientSide());
        }
        if (mano.isEmpty() && player.isShiftKeyDown() && !be.pez.isEmpty()) {
            if (!level.isClientSide()) {
                Plat.give(player, be.pez);
                be.pez = ItemStack.EMPTY;
                be.changed();
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1F, 1F);
            }
            return Plat.ok(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    /** (Servidor) Monta el pez que trae en la mano (y le devuelve el que había). */
    public static void montar(TrofeoBE be, Player player, ItemStack mano) {
        ItemStack viejo = be.pez;
        be.pez = mano.copy();
        be.pez.setCount(1);
        if (!player.getAbilities().instabuild) {
            mano.shrink(1);
        }
        if (!viejo.isEmpty()) {
            Plat.give(player, viejo);
        }
        be.changed();
        be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1F, 1.2F);
    }

    @Override
    protected void onPlayerBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrofeoBE be && !be.pez.isEmpty()) {
            Block.popResource(level, pos, be.pez);
            be.pez = ItemStack.EMPTY;
        }
    }

    @Override
    public void axInfo(Level level, BlockPos pos, BlockState state, InfoCard card, boolean detail) {
        if (!(level.getBlockEntity(pos) instanceof TrofeoBE be) || be.pez.isEmpty() || !(be.pez.getItem() instanceof PezItem pz)) {
            card.text(Component.translatable(PescaColeccion.MODID + ".trofeo.vacio"));
            return;
        }
        InfoCard f = pz.card(be.pez, detail);
        if (f == null) {
            return;
        }
        card.title = f.title;
        card.subtitle = f.subtitle;
        card.rareza = f.rareza;
        card.tag = f.tag;
        card.icon = f.icon;
        card.color = f.color;
        card.maxWidth = f.maxWidth;
        card.parts.addAll(f.parts);
        card.parts.removeIf(p -> p.kind == InfoCard.Kind.HINT);
        card.hint(Component.translatable(PescaColeccion.MODID + ".trofeo.quitar"));
    }
}

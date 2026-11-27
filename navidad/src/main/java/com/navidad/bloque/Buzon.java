package com.navidad.bloque;

import com.navidad.Cartas;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El buzón de Santa: un buzón rojo en un poste, con nieve encima. Le das clic con un libro y pluma (o un libro firmado) donde escribiste
 * tu carta a Santa, y la carta sale volando al Polo Norte (ver {@link Cartas}). Sin libro, te explica cómo.
 */
public class Buzon extends HorizontalDirectionalBlock {
    private static final VoxelShape FORMA = Shapes.or(Block.box(6.5, 0, 6.5, 9.5, 9, 9.5), Block.box(3, 9, 3.5, 13, 16, 12.5));

    public Buzon(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        ItemStack st = p.getItemInHand(h);
        boolean libro = st.is(Items.WRITABLE_BOOK) || st.is(Items.WRITTEN_BOOK);
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(p instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (!libro) {
            sp.displayClientMessage(Component.translatable("navidad.buzon.como").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }
        Cartas.mandar(sp, st, pos);
        return InteractionResult.CONSUME;
    }
}

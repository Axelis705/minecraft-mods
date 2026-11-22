package com.mundosdeaxelis.encargo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/** El Tablón de Encargos (etapa 280): un tablero de madera con notas clavadas; al usarlo se ven los encargos del día. */
public class TablonBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape NS = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape EW = Block.box(6, 0, 0, 10, 16, 16);

    public TablonBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return st.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            net.minecraftforge.network.NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) ->
                    new TablonMenu(id, inv, ContainerLevelAccess.create(level, pos), TablonMenu.serverData(sp)),
                    Component.translatable("block.mundos_de_axelis.tablon_encargos")));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable BlockGetter level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mundos_de_axelis.tablon_encargos").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

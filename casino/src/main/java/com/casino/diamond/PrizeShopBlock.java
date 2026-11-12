package com.casino.diamond;

import com.casino.chip.Chips;
import com.casino.decor.Shapes;
import com.casino.net.CasinoNet;
import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Map;

/** Vitrina de la tienda de premios: clic derecho abre la lista; se paga con fichas. El operador puede editar la lista. */
public class PrizeShopBlock extends UseBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{0, 0, 0, 16, 15, 16});

    public PrizeShopBlock(Properties props) {
        super(props);
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
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrizeShopBE(pos, state);
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof PrizeShopBE be) {
            if (!be.stocked) {
                be.stockDefaults();
                be.sync();
            }
            open(sp, be);
        }
        return Plat.ok(level.isClientSide());
    }

    public static boolean canEdit(Player p) {
        return Plat.isOp(p) || Plat.isHost(p);
    }

    static void open(ServerPlayer sp, PrizeShopBE be) {
        CasinoNet.sendTo(sp, new ShopPacket.Open(be.getBlockPos(), be.offers, canEdit(sp)));
    }

    private static PrizeShopBE near(ServerPlayer p, BlockPos pos) {
        if (p == null || !p.level().isLoaded(pos) || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 8 * 8) {
            return null;
        }
        return p.level().getBlockEntity(pos) instanceof PrizeShopBE be ? be : null;
    }

    static void buy(ServerPlayer p, BlockPos pos, int idx) {
        PrizeShopBE be = near(p, pos);
        if (be == null || idx < 0 || idx >= be.offers.size()) {
            return;
        }
        PrizeShopBE.Offer o = be.offers.get(idx);
        if (!Chips.take(p, o.price())) {
            Plat.msg(p, Component.translatable("casino.cashier.no_chips").withStyle(ChatFormatting.RED), true);
            p.level().playSound(null, p.blockPosition(), ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        ItemHandlerHelper.giveItemToPlayer(p, o.item().copy());
        Plat.msg(p, Component.translatable("casino.shop.bought", o.item().getHoverName(), o.price()).withStyle(ChatFormatting.GREEN), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
    }

    /** op: 0 agregar lo que tiene en la mano con ese precio, 1 quitar idx, 2 cambiar precio de idx, 3 lista inicial. */
    static void edit(ServerPlayer p, BlockPos pos, int op, int idx, int price) {
        PrizeShopBE be = near(p, pos);
        if (be == null || !canEdit(p)) {
            return;
        }
        price = Math.max(1, Math.min(10_000_000, price));
        switch (op) {
            case 0 -> {
                ItemStack held = p.getMainHandItem();
                if (held.isEmpty()) {
                    Plat.msg(p, Component.translatable("casino.shop.hold_item").withStyle(ChatFormatting.RED), true);
                    return;
                }
                if (be.offers.size() >= PrizeShopBE.MAX) {
                    return;
                }
                be.offers.add(new PrizeShopBE.Offer(held.copy(), price));
            }
            case 1 -> {
                if (idx >= 0 && idx < be.offers.size()) {
                    be.offers.remove(idx);
                }
            }
            case 2 -> {
                if (idx >= 0 && idx < be.offers.size()) {
                    be.offers.set(idx, new PrizeShopBE.Offer(be.offers.get(idx).item(), price));
                }
            }
            case 3 -> be.stockDefaults();
            default -> {
                return;
            }
        }
        be.stocked = true;
        be.sync();
        open(p, be);
    }
}

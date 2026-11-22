package com.mundosdeaxelis.block;

import com.mundosdeaxelis.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Etapa 278: el <b>Altar Maldito</b> (como los santuarios malditos de Diablo o las cámaras de desafío). Está en las
 * estructuras de tier 1 a 5. Cargado (con su cristal brillando), al tocarlo despierta: salen 3 oleadas de enemigos de su tier
 * (la última con un élite). Si las vences, el altar suelta un buen botín y se apaga hasta el día siguiente.
 */
public class AltarMalditoBlock extends BaseEntityBlock {
    public static final BooleanProperty CARGADO = BooleanProperty.create("cargado");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 13, 15);

    public AltarMalditoBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CARGADO, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(CARGADO);
    }

    @Override
    public RenderShape getRenderShape(BlockState st) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return new AltarMalditoBlockEntity(pos, st);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState st, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ALTAR_MALDITO.get(), AltarMalditoBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof AltarMalditoBlockEntity be) {
            be.activate(sp);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (!st.getValue(CARGADO)) {
            if (r.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.02, 0);
            }
            return;
        }
        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4, pos.getY() + 1.15,
                pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        for (double[] c : new double[][]{{0.2, 0.2}, {0.8, 0.2}, {0.2, 0.8}, {0.8, 0.8}}) {
            if (r.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + c[0], pos.getY() + 1.0, pos.getZ() + c[1], 0, 0.005, 0);
            }
        }
        if (r.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 0, 0.03, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable BlockGetter level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mundos_de_axelis.altar_maldito").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

package com.navidad.musica;

import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La Caja de música navideña: clic derecho y se abre la tapa, sale un pinito que gira sobre su espejo, la manivela da vueltas, salen
 * notitas y toca un villancico de caja de música (sin discos ni tocadiscos). Otro clic: el siguiente villancico. Agachado: se cierra.
 * Al terminar la canción se cierra sola. Se oye con el volumen de "Música de tocadiscos".
 */
public class CajaMusica extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape NS = Block.box(3, 0, 4, 13, 8, 12), EO = Block.box(4, 0, 3, 12, 8, 13);

    public CajaMusica(Properties p) {
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
        return s.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EO;
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new CajaMusicaBE(p, s);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return t == ModRegistry.CAJA_MUSICA_BE.get() ? (lv, p, st, be) -> ((CajaMusicaBE) be).tick() : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!(l.getBlockEntity(pos) instanceof CajaMusicaBE be)) {
            return InteractionResult.PASS;
        }
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (p.isShiftKeyDown()) {
            if (be.sonando) {
                be.parar();
                l.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.6F);
            }
            return InteractionResult.CONSUME;
        }
        if (be.sonando) {
            be.cancion = (be.cancion + 1) % CajaMusicaBE.CANCIONES;
        }
        be.tocar((ServerLevel) l);
        l.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.7F);
        p.displayClientMessage(Component.translatable("navidad.caja_musica.suena",
                Component.translatable("navidad.cancion." + be.cancion)).withStyle(ChatFormatting.GOLD), true);
        com.navidad.libro.Libro.evento((net.minecraft.server.level.ServerPlayer) p, "caja_musica", 1);
        return InteractionResult.CONSUME;
    }
}

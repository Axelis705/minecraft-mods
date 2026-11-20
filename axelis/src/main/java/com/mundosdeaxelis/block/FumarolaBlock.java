package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fumarola (etapa 255): boca de la ladera del volcán que echa vapor sin parar (columnas de humo que se ven de lejos, como las de
 * una fogata) y de vez en cuando silba. No quema ni hace daño: es ambiente. Necesita una entidad de bloque solo para que el humo
 * salga seguido en el cliente (los bloques normales solo se "animan" de vez en cuando).
 */
public class FumarolaBlock extends BaseEntityBlock {
    public FumarolaBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @javax.annotation.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FumarolaBlockEntity(pos, state);
    }

    @javax.annotation.Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, com.mundosdeaxelis.registry.ModBlockEntities.FUMAROLA.get(), FumarolaBlockEntity::clientTick) : null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (r.nextInt(3) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.12F,
                    0.45F + r.nextFloat() * 0.2F, false);
        }
        if (r.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
        }
    }
}

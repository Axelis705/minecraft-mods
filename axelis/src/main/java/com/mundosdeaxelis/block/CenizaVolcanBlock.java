package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Roca de ceniza (etapa 255): en los techos de la dungeon del volcán. Deja caer ceniza gris y, muy de vez en cuando, se oye un
 * retumbar lejano del volcán.
 */
public class CenizaVolcanBlock extends Block {
    private static final BlockParticleOption DUST = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAY_CONCRETE_POWDER.defaultBlockState());

    public CenizaVolcanBlock(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isAir()) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(DUST, pos.getX() + r.nextDouble(), pos.getY() - 0.05, pos.getZ() + r.nextDouble(), 0.0, 0.0, 0.0);
        }
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ASH, pos.getX() + r.nextDouble(), pos.getY() - 0.2, pos.getZ() + r.nextDouble(), 0.0, -0.02, 0.0);
        }
        if (r.nextInt(500) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, com.mundosdeaxelis.registry.ModSounds.VOLCAN_RETUMBO.get(),
                    SoundSource.AMBIENT, 0.55F, 0.75F + r.nextFloat() * 0.3F, false);
        }
    }
}

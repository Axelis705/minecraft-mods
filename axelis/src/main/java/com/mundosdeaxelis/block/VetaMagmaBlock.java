package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Veta de magma (etapa 255): roca de las paredes del volcán con grietas que brillan en la oscuridad (el brillo va en el modelo, con
 * luz propia en la capa de las grietas). De vez en cuando suelta una chispa.
 */
public class VetaMagmaBlock extends Block {
    public VetaMagmaBlock(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(12) != 0) {
            return;
        }
        net.minecraft.core.Direction d = net.minecraft.core.Direction.getRandom(r);
        BlockPos o = pos.relative(d);
        if (!level.getBlockState(o).isAir()) {
            return;
        }
        double x = pos.getX() + 0.5 + d.getStepX() * 0.55 + (d.getStepX() == 0 ? r.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + d.getStepY() * 0.55 + (d.getStepY() == 0 ? r.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + d.getStepZ() * 0.55 + (d.getStepZ() == 0 ? r.nextDouble() - 0.5 : 0);
        level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.005, 0.0);
    }
}
